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
import com.ferji.inspecciones.domain.model.AppResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
 * Principios aplicados:
 * - Operaciones pesadas ejecutadas en [Dispatchers.IO] para no bloquear el hilo principal.
 * - Uso de [AppResult] en lugar de retornar null para comunicar errores con contexto.
 * - Separación de responsabilidades: recopilación de datos vs. escritura del archivo.
 * - Constantes extraídas para facilitar configuración y mantenimiento.
 *
 * Lógica:
 * 1. Por cada habitación se obtienen los daños seleccionados.
 * 2. Cada daño tiene partidas asociadas de naturaleza VARIABLE.
 * 3. Al final del presupuesto se agrega una sección "GENERALES" con las partidas FIJAS.
 */
class ExcelGenerator(private val context: Context) {

    companion object {
        private const val TAG = "ExcelGenerator"

        // ── Constantes de negocio (fácilmente configurables) ──
        private const val PORCENTAJE_GASTOS_GENERALES = 0.25
        private const val PORCENTAJE_IVA = 0.19

        // ── Colores del Excel ──
        private const val COLOR_HEADER = "34495E"
        private const val COLOR_HABITACION = "2980B9"
        private const val COLOR_SUBTOTAL = "27AE60"
        private const val COLOR_GENERALES = "8E44AD"
        private const val COLOR_DESGLOSE = "C0392B"
        private const val COLOR_TOTAL_FINAL = "8B0000"
        private const val COLOR_FILA_PAR = "FFFFFF"
        private const val COLOR_FILA_IMPAR = "F5F5F5"
        private const val COLOR_BLANCO = "FFFFFF"

        // ── Datos de la empresa (idealmente vendrían de configuración remota) ──
        private const val EMPRESA_NOMBRE = "CONSTRUCCIONES Y ALUMINIOS DEL MAULE"
        private const val EMPRESA_DIRECCION = "CALLE 6 NORTE 2380 TALCA"
        private const val EMPRESA_REGION = "REGION DEL MAULE"
        private const val EMPRESA_TELEFONO = "TELÉFONO: +569320485044"
    }

    data class ExcelResult(
        val uri: Uri?,
        val file: File?,
        val fileName: String,
        /** Total del presupuesto con IVA incluido (Costo Total en $) */
        val totalPresupuesto: Double = 0.0
    )

    /**
     * Genera el presupuesto Excel de forma segura en [Dispatchers.IO].
     *
     * @return [AppResult.Success] con el resultado del archivo, o [AppResult.Error] con detalles del fallo.
     */
    suspend fun generarPresupuesto(
        inspeccion: InspeccionEntity,
        habitaciones: List<HabitacionEntity>,
        partidaRepository: PartidaRepository
    ): AppResult<ExcelResult> = withContext(Dispatchers.IO) {
        val fechaStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Presupuesto_${inspeccion.siniestro}_${fechaStr}.xlsx"

        try {
            val presupuesto = recopilarDatos(habitaciones, partidaRepository)

            // Calcular total con gastos generales e IVA (misma fórmula que en escribirExcel)
            val costoDirecto = presupuesto.totalGeneral
            val gastosGenerales = redondear2(costoDirecto * PORCENTAJE_GASTOS_GENERALES)
            val costoNeto = redondear2(costoDirecto + gastosGenerales)
            val iva = redondear2(costoNeto * PORCENTAJE_IVA)
            val costoTotal = redondear2(costoNeto + iva)

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
                    Log.i(TAG, "Excel generado en Descargas: $fileName (Total: $costoTotal)")
                    AppResult.Success(ExcelResult(uri = uri, file = null, fileName = fileName, totalPresupuesto = costoTotal))
                } else {
                    AppResult.Error("No se pudo crear el archivo en Descargas. Verifique permisos de almacenamiento.")
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)
                file.outputStream().use { outputStream ->
                    escribirExcel(outputStream, inspeccion, presupuesto)
                }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                AppResult.Success(ExcelResult(uri = uri, file = file, fileName = fileName, totalPresupuesto = costoTotal))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generando Excel: ${e.message}", e)
            AppResult.Error("Error generando presupuesto: ${e.localizedMessage}", e)
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
     *
     * Reglas de cálculo para M2:
     * - Descripción contiene "MURO" y ancho = 0 → largo × alto (muro individual, ej: fachada)
     * - Descripción contiene "MURO" y ancho > 0 → perímetro × alto: 2*(largo+ancho)*alto
     * - Cualquier otra superficie                → largo × ancho (superficie de piso/cielo)
     *
     * Reglas para otras unidades:
     * - ML y ancho = 0 → solo largo (metros lineales de un muro individual)
     * - ML y ancho > 0 → perímetro: 2*(largo+ancho)
     * - U, GL           → 1 unidad
     */
    private fun calcularCantidad(
        partida: PartidaEntity,
        habitacion: HabitacionEntity,
        tipoSuperficie: String
    ): Double {
        val altoM = habitacion.alto / 100.0
        val largoM = habitacion.largo / 100.0
        val anchoM = habitacion.ancho / 100.0

        val esMuroPorDescripcionHija = partida.descripcion.uppercase().contains("MURO")
        val tieneAncho = habitacion.ancho > 0

        return when (partida.unidad.uppercase()) {
            "M2" -> {
                if (esMuroPorDescripcionHija) {
                    if (tieneAncho) {
                        // Habitación completa: perímetro × alto (4 muros)
                        2.0 * (largoM + anchoM) * altoM
                    } else {
                        // Muro individual (ej: fachada): largo × alto
                        largoM * altoM
                    }
                } else {
                    // Superficie plana (piso, cielo, etc.)
                    if (tieneAncho) {
                        largoM * anchoM
                    } else {
                        // Sin ancho: usar largo como única medida de superficie
                        largoM * altoM
                    }
                }
            }
            "ML" -> {
                if (tieneAncho) {
                    2.0 * (largoM + anchoM) // Perímetro completo
                } else {
                    largoM // Solo largo (muro individual)
                }
            }
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
        ws.value(row, 5, EMPRESA_NOMBRE); ws.style(row, 5).bold().fontSize(11).set()
        row++
        ws.value(row, 0, "RUT Cliente:"); ws.style(row, 0).bold().set(); ws.value(row, 1, inspeccion.rut)
        ws.value(row, 5, EMPRESA_DIRECCION); ws.style(row, 5).fontSize(10).set()
        row++
        ws.value(row, 0, "Dirección:"); ws.style(row, 0).bold().set(); ws.value(row, 1, inspeccion.direccion)
        ws.value(row, 5, EMPRESA_REGION); ws.style(row, 5).fontSize(10).set()
        row++
        ws.value(row, 0, "Inspector:"); ws.style(row, 0).bold().set(); ws.value(row, 1, inspeccion.rutInspector)
        ws.value(row, 5, EMPRESA_TELEFONO); ws.style(row, 5).fontSize(10).set()
        row++
        ws.value(row, 0, "Fecha:"); ws.style(row, 0).bold().set()
        ws.value(row, 1, SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())); row++
        row++

        // ── Cabecera de columnas ──
        val headerCols = arrayOf("Descripción", "Largo", "Ancho", "Alto", "Medida", "Cantidad", "Precio Unitario", "Total")
        for (c in headerCols.indices) {
            ws.value(row, c, headerCols[c])
            ws.style(row, c).bold().fillColor(COLOR_HEADER).fontColor(COLOR_BLANCO)
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
            ws.value(row, 1, redondear2(largoM))
            if (hab.anchoCm > 0) ws.value(row, 2, redondear2(anchoM)) // Vacío si no tiene ancho
            ws.value(row, 3, redondear2(altoM))
            for (c in 0..7) { ws.style(row, c).bold().fillColor(COLOR_HABITACION).fontColor(COLOR_BLANCO).set() }
            ws.style(row, 1).bold().fillColor(COLOR_HABITACION).fontColor(COLOR_BLANCO).format("0.00").set()
            ws.style(row, 2).bold().fillColor(COLOR_HABITACION).fontColor(COLOR_BLANCO).format("0.00").set()
            ws.style(row, 3).bold().fillColor(COLOR_HABITACION).fontColor(COLOR_BLANCO).format("0.00").set()
            row++

            for ((index, linea) in hab.lineasVariables.withIndex()) {
                ws.value(row, 0, linea.descripcion)
                ws.value(row, 4, linea.unidad)
                ws.value(row, 5, redondear2(linea.cantidad))
                ws.value(row, 6, redondear2(linea.precioUnitario))
                ws.value(row, 7, redondear2(linea.subtotal))
                val fill = if (index % 2 != 0) COLOR_FILA_IMPAR else COLOR_FILA_PAR
                if (index % 2 != 0) { for (c in 0..7) { ws.style(row, c).fillColor(fill).set() } }
                ws.style(row, 5).fillColor(fill).format("#,##0.00").set()
                ws.style(row, 6).fillColor(fill).format("#,##0").set()
                ws.style(row, 7).fillColor(fill).format("#,##0").set()
                row++
            }

            ws.value(row, 0, "Subtotal ${hab.nombre}")
            ws.value(row, 7, redondear2(hab.totalHabitacion))
            for (c in 0..7) { ws.style(row, c).bold().fillColor(COLOR_SUBTOTAL).fontColor(COLOR_BLANCO).set() }
            ws.style(row, 7).bold().fillColor(COLOR_SUBTOTAL).fontColor(COLOR_BLANCO).format("$ #,##0").set()
            row += 2
        }

        // ══════════════════════════════════════════
        //  GENERALES (gastos fijos)
        // ══════════════════════════════════════════
        if (presupuesto.lineasFijasGlobales.isNotEmpty()) {
            ws.value(row, 0, "GENERALES")
            for (c in 0..7) { ws.style(row, c).bold().fillColor(COLOR_GENERALES).fontColor(COLOR_BLANCO).set() }
            row++

            for ((index, linea) in presupuesto.lineasFijasGlobales.withIndex()) {
                ws.value(row, 0, linea.descripcion)
                ws.value(row, 4, linea.unidad)
                ws.value(row, 5, redondear2(linea.cantidad))
                ws.value(row, 6, redondear2(linea.precioUnitario))
                ws.value(row, 7, redondear2(linea.subtotal))
                val fill = if (index % 2 != 0) COLOR_FILA_IMPAR else COLOR_FILA_PAR
                if (index % 2 != 0) { for (c in 0..7) { ws.style(row, c).fillColor(fill).set() } }
                ws.style(row, 5).fillColor(fill).format("#,##0.00").set()
                ws.style(row, 6).fillColor(fill).format("#,##0").set()
                ws.style(row, 7).fillColor(fill).format("#,##0").set()
                row++
            }

            ws.value(row, 0, "Subtotal Generales")
            ws.value(row, 7, redondear2(presupuesto.totalFijas))
            for (c in 0..7) { ws.style(row, c).bold().fillColor(COLOR_GENERALES).fontColor(COLOR_BLANCO).set() }
            ws.style(row, 7).bold().fillColor(COLOR_GENERALES).fontColor(COLOR_BLANCO).format("$ #,##0").set()
            row += 2
        }

        // ═══ DESGLOSE FINAL ═══
        val costoDirecto = presupuesto.totalGeneral
        val gastosGenerales = redondear2(costoDirecto * PORCENTAJE_GASTOS_GENERALES)
        val costoNeto = redondear2(costoDirecto + gastosGenerales)
        val iva = redondear2(costoNeto * PORCENTAJE_IVA)
        val costoTotal = redondear2(costoNeto + iva)

        // COSTO DIRECTO DE OBRA
        ws.value(row, 0, "COSTO DIRECTO DE OBRA")
        ws.value(row, 7, redondear2(costoDirecto))
        for (c in 0..7) { ws.style(row, c).bold().fontSize(12).fillColor(COLOR_DESGLOSE).fontColor(COLOR_BLANCO).set() }
        ws.style(row, 7).bold().fontSize(12).fillColor(COLOR_DESGLOSE).fontColor(COLOR_BLANCO).format("$ #,##0").set()
        row++

        // GASTOS GENERALES Y UTILIDADES
        ws.value(row, 0, "GASTOS GENERALES Y UTILIDADES ${(PORCENTAJE_GASTOS_GENERALES * 100).toInt()}%")
        ws.value(row, 7, gastosGenerales)
        for (c in 0..7) { ws.style(row, c).bold().fontSize(12).fillColor(COLOR_DESGLOSE).fontColor(COLOR_BLANCO).set() }
        ws.style(row, 7).bold().fontSize(12).fillColor(COLOR_DESGLOSE).fontColor(COLOR_BLANCO).format("$ #,##0").set()
        row++

        // COSTO NETO
        ws.value(row, 0, "COSTO NETO")
        ws.value(row, 7, costoNeto)
        for (c in 0..7) { ws.style(row, c).bold().fontSize(12).fillColor(COLOR_DESGLOSE).fontColor(COLOR_BLANCO).set() }
        ws.style(row, 7).bold().fontSize(12).fillColor(COLOR_DESGLOSE).fontColor(COLOR_BLANCO).format("$ #,##0").set()
        row++

        // IVA
        ws.value(row, 0, "IVA ${(PORCENTAJE_IVA * 100).toInt()}%")
        ws.value(row, 7, iva)
        for (c in 0..7) { ws.style(row, c).bold().fontSize(12).fillColor(COLOR_DESGLOSE).fontColor(COLOR_BLANCO).set() }
        ws.style(row, 7).bold().fontSize(12).fillColor(COLOR_DESGLOSE).fontColor(COLOR_BLANCO).format("$ #,##0").set()
        row++

        // COSTO TOTAL EN $
        ws.value(row, 0, "COSTO TOTAL EN $")
        ws.value(row, 7, costoTotal)
        for (c in 0..7) { ws.style(row, c).bold().fontSize(14).fillColor(COLOR_TOTAL_FINAL).fontColor(COLOR_BLANCO).set() }
        ws.style(row, 7).bold().fontSize(14).fillColor(COLOR_TOTAL_FINAL).fontColor(COLOR_BLANCO).format("$ #,##0").set()
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
