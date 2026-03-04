package com.ferji.inspecciones.utils

import android.content.Context
import android.net.Uri
import org.dhatim.fastexcel.Workbook
import org.dhatim.fastexcel.Worksheet
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExcelGenerator(private val context: Context) {

    data class InspeccionConDetalles(
        val id: String,
        val direccion: String,
        val fecha: Date,
        val habitaciones: List<HabitacionConPartidas>
    )

    data class HabitacionConPartidas(
        val nombre: String,
        val partidas: List<PartidaConDanos>
    )

    data class PartidaConDanos(
        val nombre: String,
        val danos: String,
        val costoReparacion: Double
    )

    fun generarPresupuesto(inspeccion: InspeccionConDetalles, uri: Uri) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                crearLibroExcel(outputStream, inspeccion)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun crearLibroExcel(outputStream: OutputStream, inspeccion: InspeccionConDetalles) {
        // En la versión moderna, el Workbook se crea con el OutputStream directamente
        // "MiApp" es el nombre de la aplicación creadora y "1.0" la versión del doc.
        val workbook = Workbook(outputStream, "InspeccionesApp", "1.0")

        val ws = workbook.newWorksheet("Resumen")

        // --- 1. CABECERA E INFORMACIÓN GENERAL ---
        ws.value(0, 0, "Presupuesto de Reparación - Inspección")

        ws.value(1, 0, "Fecha de Generación:")
        ws.value(1, 1, SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date()))

        ws.value(2, 0, "Dirección Inmueble:")
        ws.value(2, 1, inspeccion.direccion)

        ws.value(2, 0, "Dirección Inmueble:")


        // --- 2. CABECERAS DE TABLA (Fila 4) ---
        val filaInicioTabla = 4
        ws.value(filaInicioTabla, 0, "Habitación")
        ws.value(filaInicioTabla, 1, "Partida/Elemento")
        ws.value(filaInicioTabla, 2, "Daños Detectados")
        ws.value(filaInicioTabla, 3, "Costo Reparación ($)")

        // --- 3. LLENADO DE DATOS ---
        var currentRow = filaInicioTabla + 1
        var costoTotal = 0.0

        inspeccion.habitaciones.forEach { habitacion ->
            habitacion.partidas.forEach { partida ->
                ws.value(currentRow, 0, habitacion.nombre)
                ws.value(currentRow, 1, partida.nombre)
                ws.value(currentRow, 2, partida.danos)
                ws.value(currentRow, 3, partida.costoReparacion)

                costoTotal += partida.costoReparacion
                currentRow++
            }
        }

        // --- 4. TOTALES ---
        currentRow++ // Espacio en blanco
        ws.value(currentRow, 2, "COSTO TOTAL")
        ws.value(currentRow, 3, costoTotal)

        // IMPORTANTE: En la versión moderna se usa finish() para escribir y cerrar el flujo
        workbook.finish()
    }
}