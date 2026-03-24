package com.ferji.inspecciones.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import com.ferji.inspecciones.data.model.HabitacionEntity
import com.ferji.inspecciones.data.model.InspeccionEntity
import com.ferji.inspecciones.data.model.PartidaEntity
import com.ferji.inspecciones.data.model.PartidaNaturaleza
import com.ferji.inspecciones.data.repository.PartidaRepository
import org.dhatim.fastexcel.BorderSide
import org.dhatim.fastexcel.BorderStyle
import org.dhatim.fastexcel.Workbook
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Genera un presupuesto en Excel (.xlsx) basado en los datos de la inspección.
 *
 * Lógica:
 * 1. Por cada habitación se obtienen los daños seleccionados.
 * 2. Cada daño tiene partidas asociadas de naturaleza VARIABLE.
 * 3. Al final del presupuesto se agrega una sección "GENERALES" con las partidas FIJAS.
 */
class ExcelGenerator(private val context: Context) {

    companion object {
        private const val TAG = "ExcelGenerator"
    }

    data class ExcelResult(
        val uri: Uri?,
        val file: File?,
        val fileName: String
    )

    suspend fun generarPresupuesto(
        inspeccion: InspeccionEntity,
        habitaciones: List<HabitacionEntity>,
        partidaRepository: PartidaRepository
    ): ExcelResult? {
        val fechaStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Presupuesto_${inspeccion.siniestro}_${fechaStr}.xlsx"

        return try {
            val presupuesto = recopilarDatos(habitaciones, partidaRepository)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        escribirExcel(outputStream, inspeccion, presupuesto)
                    }
                    Log.i(TAG, "Excel generado en Descargas: $fileName")
                    ExcelResult(uri = uri, file = null, fileName = fileName)
                } else null
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)
                file.outputStream().use { outputStream ->
                    escribirExcel(outputStream, inspeccion, presupuesto)
                }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                ExcelResult(uri = uri, file = file, fileName = fileName)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generando Excel: ${e.message}", e)
            null
        }
    }

    // ─── Modelos internos ───────────────────────────────────────────

    /** Resultado completo del presupuesto: habitaciones + gastos fijos globales */
    data class PresupuestoCompleto(
        val habitaciones: List<HabitacionPresupuesto>,
        val lineasFijasGlobales: List<LineaPresupuesto>
    ) {
        val totalHabitaciones get() = habitaciones.sumOf { it.totalHabitacion }
        val totalFijas get() = lineasFijasGlobales.sumOf { it.subtotal }
        val totalGeneral get() = totalHabitaciones + totalFijas
    }

    data class HabitacionPresupuesto(
        val nombre: String,
        val altoCm: Int,
        val largoCm: Int,
        val anchoCm: Int,
        val danosTexto: List<String>,
        val lineasVariables: List<LineaPresupuesto>
    ) {
        val totalHabitacion get() = lineasVariables.sumOf { it.subtotal }
    }

    data class LineaPresupuesto(
        val categoriaPrincipal: String,
        val tipoSuperficie: String,
        val descripcion: String,
        val unidad: String,
        val precioUnitario: Double,
        val cantidad: Double,
        val subtotal: Double
    )

    // ─── Recopilación de datos ──────────────────────────────────────

    suspend fun recopilarDatos(
        habitaciones: List<HabitacionEntity>,
        repo: PartidaRepository
    ): PresupuestoCompleto {

        // 1. Obtener partidas principales FIJAS → gastos generales (una sola vez)
        val principalesFijas = repo.getPartidasFijas()
        val lineasFijas = mutableListOf<LineaPresupuesto>()
        for (ppFija in principalesFijas) {
            val hijasActivas = repo.getPartidasDePrincipalSuspend(ppFija.id).filter { !it.eliminado }
            for (hija in hijasActivas) {
                lineasFijas.add(
                    LineaPresupuesto(
                        categoriaPrincipal = ppFija.nombre,
                        tipoSuperficie = ppFija.tipoSuperficie,
                        descripcion = hija.descripcion,
                        unidad = hija.unidad,
                        precioUnitario = hija.precioUnitario,
                        cantidad = 1.0,
                        subtotal = hija.precioUnitario
                    )
                )
            }
        }

        // 2. Por cada habitación, buscar partidas VARIABLES
        //    Los daños guardados en la habitación son los NOMBRES de las PartidaPrincipal
        //    (ej: "Fisura techo", "Fisura Pared"), por lo que buscamos por nombre.
        val habsPresupuesto = habitaciones.map { habitacion ->
            val nombresCategorias = habitacion.getDanosList()
            Log.d(TAG, "─── Habitación: '${habitacion.nombre}' ───")
            Log.d(TAG, "  Categorías seleccionadas (daños guardados): $nombresCategorias")

            val lineasVariables = mutableListOf<LineaPresupuesto>()

            for (nombreCategoria in nombresCategorias) {
                // Buscar la PartidaPrincipal por su nombre
                val principal = repo.getPartidaPrincipalByNombre(nombreCategoria)
                if (principal == null) {
                    Log.w(TAG, "  ⚠️ No se encontró PartidaPrincipal con nombre='$nombreCategoria'. Se salta.")
                    continue
                }
                if (principal.naturaleza != PartidaNaturaleza.VARIABLE) {
                    Log.d(TAG, "  Saltando '${principal.nombre}' porque es ${principal.naturaleza}")
                    continue
                }

                Log.d(TAG, "  ✅ Categoría encontrada: id=${principal.id}, nombre='${principal.nombre}', tipo='${principal.tipoSuperficie}'")

                // Obtener las partidas hijas activas de esta categoría
                val hijasActivas = repo.getPartidasDePrincipalSuspend(principal.id).filter { !it.eliminado }
                Log.d(TAG, "  Partidas hijas activas: ${hijasActivas.size}")

                for (hija in hijasActivas) {
                    val cantidad = calcularCantidad(hija, habitacion, principal.tipoSuperficie)
                    val subtotal = hija.precioUnitario * cantidad
                    Log.d(TAG, "    → '${hija.descripcion}' ${hija.unidad} cant=$cantidad pu=${hija.precioUnitario} total=$subtotal")

                    lineasVariables.add(
                        LineaPresupuesto(
                            categoriaPrincipal = principal.nombre,
                            tipoSuperficie = principal.tipoSuperficie,
                            descripcion = hija.descripcion,
                            unidad = hija.unidad,
                            precioUnitario = hija.precioUnitario,
                            cantidad = cantidad,
                            subtotal = subtotal
                        )
                    )
                }
            }

            Log.d(TAG, "  Total líneas para '${habitacion.nombre}': ${lineasVariables.size}")

            HabitacionPresupuesto(
                nombre = habitacion.nombre,
                altoCm = habitacion.alto,
                largoCm = habitacion.largo,
                anchoCm = habitacion.ancho,
                danosTexto = nombresCategorias,
                lineasVariables = lineasVariables.sortedBy { it.categoriaPrincipal }
            )
        }

        Log.d(TAG, "═══ Resumen presupuesto: ${habsPresupuesto.size} habitaciones, ${lineasFijas.size} gastos fijos ═══")

        return PresupuestoCompleto(
            habitaciones = habsPresupuesto,
            lineasFijasGlobales = lineasFijas
        )
    }

    /**
     * Calcula la cantidad según la unidad de medida de la partida,
     * las dimensiones de la habitación y el tipo de superficie de la partida principal.
     *
     * Dimensiones de la habitación están en centímetros (ej: 380 = 3.80 metros).
     */
    private fun calcularCantidad(
        partida: PartidaEntity,
        habitacion: HabitacionEntity,
        tipoSuperficie: String
    ): Double {
        val altoM = habitacion.alto / 100.0
        val largoM = habitacion.largo / 100.0
        val anchoM = habitacion.ancho / 100.0

        // Verificar si la descripción de la partida HIJA contiene "muro"
        val esMuroPorDescripcionHija = partida.descripcion.uppercase().contains("MURO")

        return when (partida.unidad.uppercase()) {
            "M2" -> {
                if (esMuroPorDescripcionHija) {
                    2.0 * (largoM + anchoM) * altoM
                } else {
                    when (tipoSuperficie.uppercase()) {
                        "PISO" -> largoM * anchoM
                        "CIELO" -> largoM * anchoM
                        else -> largoM * anchoM
                    }
                }
            }
            "ML" -> 2.0 * (largoM + anchoM)
            "U", "GL" -> 1.0
            else -> 1.0
        }
    }

    // ─── Escritura del Excel ────────────────────────────────────────

    private fun escribirExcel(
        outputStream: OutputStream,
        inspeccion: InspeccionEntity,
        presupuesto: PresupuestoCompleto
    ) {
        val workbook = Workbook(outputStream, "FerjiInspecciones", "1.0")
        val ws = workbook.newWorksheet("Presupuesto")
        var row = 0

        // ── Encabezado del documento ──
        ws.value(row, 0, "PRESUPUESTO DE REPARACIÓN")
        ws.style(row, 0).bold().fontSize(14).set()
        row += 2

        // Datos del siniestro (izquierda) + Datos de la empresa (derecha)
        val encabezadoRow = row
        ws.value(row, 0, "Siniestro:"); ws.style(row, 0).bold().set(); ws.value(row, 1, inspeccion.siniestro)
        ws.value(row, 5, "CONSTRUCCIONES Y ALUMINIOS DEL MAULE"); ws.style(row, 5).bold().fontSize(11).set()
        row++
        ws.value(row, 0, "RUT Cliente:"); ws.style(row, 0).bold().set(); ws.value(row, 1, inspeccion.rut)
        ws.value(row, 5, "CALLE 6 NORTE 2380 TALCA"); ws.style(row, 5).fontSize(10).set()
        row++
        ws.value(row, 0, "Dirección:"); ws.style(row, 0).bold().set(); ws.value(row, 1, inspeccion.direccion)
        ws.value(row, 5, "REGION DEL MAULE"); ws.style(row, 5).fontSize(10).set()
        row++
        ws.value(row, 0, "Inspector:"); ws.style(row, 0).bold().set(); ws.value(row, 1, inspeccion.rutInspector)
        ws.value(row, 5, "TELÉFONO: +569320485044"); ws.style(row, 5).fontSize(10).set()
        row++
        ws.value(row, 0, "Fecha:"); ws.style(row, 0).bold().set()
        ws.value(row, 1, SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())); row++
        row++

        // ── Cabecera de columnas ──
        val headerCols = arrayOf("Descripción", "Alto", "Ancho", "Largo", "Medida", "Cantidad", "Precio Unitario", "Total")
        for (c in headerCols.indices) {
            ws.value(row, c, headerCols[c])
            ws.style(row, c).bold().fillColor("34495E").fontColor("FFFFFF")
                .borderStyle(BorderSide.BOTTOM, BorderStyle.THIN).set()
        }
        row++

        // ══════════════════════════════════════════
        //  HABITACIONES (solo partidas VARIABLES)
        // ══════════════════════════════════════════
        for (hab in presupuesto.habitaciones) {
            val altoM = hab.altoCm / 100.0
            val anchoM = hab.anchoCm / 100.0
            val largoM = hab.largoCm / 100.0

            ws.value(row, 0, hab.nombre)
            ws.value(row, 1, redondear2(altoM))
            ws.value(row, 2, redondear2(anchoM))
            ws.value(row, 3, redondear2(largoM))
            for (c in 0..7) { ws.style(row, c).bold().fillColor("2980B9").fontColor("FFFFFF").set() }
            ws.style(row, 1).bold().fillColor("2980B9").fontColor("FFFFFF").format("0.00").set()
            ws.style(row, 2).bold().fillColor("2980B9").fontColor("FFFFFF").format("0.00").set()
            ws.style(row, 3).bold().fillColor("2980B9").fontColor("FFFFFF").format("0.00").set()
            row++

            for ((index, linea) in hab.lineasVariables.withIndex()) {
                ws.value(row, 0, linea.descripcion)
                ws.value(row, 4, linea.unidad)
                ws.value(row, 5, redondear2(linea.cantidad))
                ws.value(row, 6, redondear2(linea.precioUnitario))
                ws.value(row, 7, redondear2(linea.subtotal))
                val fill = if (index % 2 != 0) "F5F5F5" else "FFFFFF"
                if (index % 2 != 0) { for (c in 0..7) { ws.style(row, c).fillColor(fill).set() } }
                ws.style(row, 5).fillColor(fill).format("#,##0.00").set()
                ws.style(row, 6).fillColor(fill).format("#,##0").set()
                ws.style(row, 7).fillColor(fill).format("#,##0").set()
                row++
            }

            ws.value(row, 0, "Subtotal ${hab.nombre}")
            ws.value(row, 7, redondear2(hab.totalHabitacion))
            for (c in 0..7) { ws.style(row, c).bold().fillColor("27AE60").fontColor("FFFFFF").set() }
            ws.style(row, 7).bold().fillColor("27AE60").fontColor("FFFFFF").format("$ #,##0").set()
            row += 2
        }

        // ══════════════════════════════════════════
        //  GENERALES (gastos fijos)
        // ══════════════════════════════════════════
        if (presupuesto.lineasFijasGlobales.isNotEmpty()) {
            ws.value(row, 0, "GENERALES")
            for (c in 0..7) { ws.style(row, c).bold().fillColor("8E44AD").fontColor("FFFFFF").set() }
            row++

            for ((index, linea) in presupuesto.lineasFijasGlobales.withIndex()) {
                ws.value(row, 0, linea.descripcion)
                ws.value(row, 4, linea.unidad)
                ws.value(row, 5, redondear2(linea.cantidad))
                ws.value(row, 6, redondear2(linea.precioUnitario))
                ws.value(row, 7, redondear2(linea.subtotal))
                val fill = if (index % 2 != 0) "F5F5F5" else "FFFFFF"
                if (index % 2 != 0) { for (c in 0..7) { ws.style(row, c).fillColor(fill).set() } }
                ws.style(row, 5).fillColor(fill).format("#,##0.00").set()
                ws.style(row, 6).fillColor(fill).format("#,##0").set()
                ws.style(row, 7).fillColor(fill).format("#,##0").set()
                row++
            }

            ws.value(row, 0, "Subtotal Generales")
            ws.value(row, 7, redondear2(presupuesto.totalFijas))
            for (c in 0..7) { ws.style(row, c).bold().fillColor("8E44AD").fontColor("FFFFFF").set() }
            ws.style(row, 7).bold().fillColor("8E44AD").fontColor("FFFFFF").format("$ #,##0").set()
            row += 2
        }

        // ═══ DESGLOSE FINAL ═══
        val costoDirecto = presupuesto.totalGeneral
        val gastosGenerales = redondear2(costoDirecto * 0.25)
        val costoNeto = redondear2(costoDirecto + gastosGenerales)
        val iva = redondear2(costoNeto * 0.19)
        val costoTotal = redondear2(costoNeto + iva)

        // COSTO DIRECTO DE OBRA
        ws.value(row, 0, "COSTO DIRECTO DE OBRA")
        ws.value(row, 7, redondear2(costoDirecto))
        for (c in 0..7) { ws.style(row, c).bold().fontSize(12).fillColor("C0392B").fontColor("FFFFFF").set() }
        ws.style(row, 7).bold().fontSize(12).fillColor("C0392B").fontColor("FFFFFF").format("$ #,##0").set()
        row++

        // GASTOS GENERALES Y UTILIDADES 25%
        ws.value(row, 0, "GASTOS GENERALES Y UTILIDADES 25%")
        ws.value(row, 7, gastosGenerales)
        for (c in 0..7) { ws.style(row, c).bold().fontSize(12).fillColor("C0392B").fontColor("FFFFFF").set() }
        ws.style(row, 7).bold().fontSize(12).fillColor("C0392B").fontColor("FFFFFF").format("$ #,##0").set()
        row++

        // COSTO NETO
        ws.value(row, 0, "COSTO NETO")
        ws.value(row, 7, costoNeto)
        for (c in 0..7) { ws.style(row, c).bold().fontSize(12).fillColor("C0392B").fontColor("FFFFFF").set() }
        ws.style(row, 7).bold().fontSize(12).fillColor("C0392B").fontColor("FFFFFF").format("$ #,##0").set()
        row++

        // IVA 19%
        ws.value(row, 0, "IVA 19%")
        ws.value(row, 7, iva)
        for (c in 0..7) { ws.style(row, c).bold().fontSize(12).fillColor("C0392B").fontColor("FFFFFF").set() }
        ws.style(row, 7).bold().fontSize(12).fillColor("C0392B").fontColor("FFFFFF").format("$ #,##0").set()
        row++

        // COSTO TOTAL EN $
        ws.value(row, 0, "COSTO TOTAL EN $")
        ws.value(row, 7, costoTotal)
        for (c in 0..7) { ws.style(row, c).bold().fontSize(14).fillColor("8B0000").fontColor("FFFFFF").set() }
        ws.style(row, 7).bold().fontSize(14).fillColor("8B0000").fontColor("FFFFFF").format("$ #,##0").set()
        row += 2

        // ═══ OBSERVACIONES ═══
        ws.value(row, 0, "OBSERVACIONES:")
        ws.style(row, 0).bold().fontSize(12).set()
        row += 2

        val observaciones = listOf(
            "1) Para la determinación de los precios unitarios, estos deben incluir lo siguiente:\n" +
                    "   -Material\n" +
                    "   -Herramientas\n" +
                    "   -Perdida\n" +
                    "   -Mano de Obra\n" +
                    "   -Leyes Sociales\n" +
                    "   Referente a lo anterior, estos son los ítems mínimos requeridos para el cálculo del precio unitario.",
            "2) Para terminaciones de Muros, Tabiques y Cielos, se debe siempre aplicar como primera mano pintura base aparejo Sipa, con la finalidad de cubrir imperfecciones y manchas de la superficie, para luego dar terminación con Esmaltes al Agua.",
            "3) Para baños y cocinas es necesario el retiro y reposición de artefactos para las reparaciones de muros y pisos. (lavaplatos, lavamanos, griferías etc.)",
            "4) Para pisos y muros de cerámicos, es necesario el retiro completo de las palmetas, debido a que estas son descontinuadas rápidamente del mercado.",
            "5) Para el Ítem de Preparación de Superficie, en importante comprobar y verificar las superficies a reparar encuentren en óptimas condiciones, ya sean secas, limpias, y libres de imperfecciones etc."
        )

        for (obs in observaciones) {
            ws.value(row, 0, obs)
            ws.style(row, 0).wrapText(true).fontSize(10).set()
            row += 2
        }

        // ── Anchos de columna ──
        ws.width(0, 50.0); ws.width(1, 10.0); ws.width(2, 10.0); ws.width(3, 10.0)
        ws.width(4, 10.0); ws.width(5, 12.0); ws.width(6, 16.0); ws.width(7, 16.0)

        workbook.finish()
        Log.i(TAG, "Excel escrito con ${presupuesto.habitaciones.size} habitaciones + ${presupuesto.lineasFijasGlobales.size} gastos fijos")
    }

    /** Redondea a 2 decimales */
    private fun redondear2(valor: Double): Double = (valor * 100.0).roundToInt() / 100.0
}