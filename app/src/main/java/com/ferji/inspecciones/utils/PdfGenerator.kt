package com.ferji.inspecciones.utils // Asegúrate de que el paquete sea correcto

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.ferji.inspecciones.R
import com.ferji.inspecciones.data.model.HabitacionEntity
import com.ferji.inspecciones.data.model.InspeccionEntity
import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.HorizontalAlignment
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.itextpdf.layout.properties.VerticalAlignment
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGenerator {
    private const val TAG = "PdfGenerator"

    data class PdfCreationResult(
        val file: File?,
        val uri: Uri?,
        val fileName: String
    )

    fun createPdf(
        context: Context,
        inspeccion: InspeccionEntity,
        habitaciones: List<HabitacionEntity>
    ): PdfCreationResult? {
        var outputStream: OutputStream? = null
        var fileUri: Uri? = null
        var legacyFile: File? = null

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val baseFileNameCleaned = inspeccion.siniestro?.replace("[^a-zA-Z0-9.-]".toRegex(), "_") ?: "Inspeccion"
        val finalFileName = "${baseFileNameCleaned}_${timeStamp}.pdf"

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, finalFileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + File.separator + "FerjiInspecciones")
                }
                fileUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (fileUri == null) {
                    Log.e(TAG, "MediaStore.Downloads.insert devolvió null.")
                    return null
                }
                outputStream = resolver.openOutputStream(fileUri)
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val appSpecificDir = File(downloadsDir, "FerjiInspecciones")
                if (!appSpecificDir.exists()) {
                    appSpecificDir.mkdirs()
                }
                legacyFile = File(appSpecificDir, finalFileName)
                outputStream = FileOutputStream(legacyFile)
            }

            if (outputStream == null) {
                Log.e(TAG, "No se pudo obtener OutputStream.")
                return null
            }

            val writer = PdfWriter(outputStream)
            val pdfDocument = PdfDocument(writer)
            val document = Document(pdfDocument, PageSize.A4)
            document.setMargins(36f, 36f, 36f, 36f)

            val titleFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)
            val regularFont = PdfFontFactory.createFont(StandardFonts.HELVETICA)

            // --- LOGO ---
            try {
                val logoResourceId = R.raw.logo_ferji // Asegúrate que esté en res/raw/
                context.resources.openRawResource(logoResourceId).use { inputStream ->
                    val logoBytes = inputStream.readBytes()
                    if (logoBytes.isNotEmpty()) {
                        val logoImageData = ImageDataFactory.create(logoBytes)
                        val logoImage = Image(logoImageData).setWidth(80f).setAutoScaleHeight(true)
                        val headerTable = Table(UnitValue.createPercentArray(floatArrayOf(80f, 20f))).useAllAvailableWidth()
                        headerTable.setBorder(SolidBorder(ColorConstants.WHITE, 0f))
                        headerTable.addCell(Cell().add(Paragraph(" ")).setBorder(null))
                        headerTable.addCell(Cell().add(logoImage.setHorizontalAlignment(HorizontalAlignment.RIGHT)).setBorder(null))
                        document.add(headerTable)
                        Log.d(TAG, "Logo añadido.")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al añadir el logo: ${e.message}", e)
            }

            // --- TÍTULO ---
            document.add(
                Paragraph("INFORME DE INSPECCIÓN")
                    .setFont(titleFont).setFontSize(18f).setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(10f).setMarginBottom(20f)
            )

            // --- DATOS GENERALES ---
            addParagraphWithLabel(document, "Siniestro:", inspeccion.siniestro, regularFont)
            addParagraphWithLabel(document, "Dirección:", inspeccion.direccion, regularFont)
            addParagraphWithLabel(document, "RUT Cliente:", inspeccion.rut, regularFont)
            addParagraphWithLabel(document, "Mail Contacto:", inspeccion.mail, regularFont)
            addParagraphWithLabel(document, "Inspector:", inspeccion.rutInspector, regularFont)
            val fechaActual = java.util.Date()
            val fechaFormateada = try { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(fechaActual) }
            catch (e: Exception) { Log.e(TAG, "Error formateando fecha actual: ${e.message}", e); "N/A" }
            addParagraphWithLabel(document, "Fecha Informe:", fechaFormateada, regularFont, marginBottom = 15f)

            // --- DETALLE DE HABITACIONES ---
            if (habitaciones.isNotEmpty()) {
                document.add(
                    Paragraph("DETALLE DE HABITACIONES")
                        .setFont(titleFont).setFontSize(14f)
                        .setMarginTop(10f).setMarginBottom(8f)
                )

                val NUMERO_COLUMNAS_DETALLES = 4 // Nombre, Dimensiones, Daños, Observaciones
                // Define los anchos para las columnas de detalles
                val columnWidths = floatArrayOf(2f, 2f, 3f, 3f) // Ajusta estos porcentajes según necesites
                val table = Table(UnitValue.createPercentArray(columnWidths)).useAllAvailableWidth()

                // Encabezados de la tabla (solo para detalles, las fotos van debajo)
                table.addHeaderCell(createHeaderCell("Habitación", titleFont))
                table.addHeaderCell(createHeaderCell("Dimensiones (cm)", titleFont))
                table.addHeaderCell(createHeaderCell("Daños", titleFont))
                table.addHeaderCell(createHeaderCell("Observaciones", titleFont))

                habitaciones.forEachIndexed { indexHab, habitacion ->
                    // Fila 1: Detalles de la habitación
                    table.addCell(createContentCell(habitacion.nombre, regularFont))
                    val dimensionesStr = "Alto: ${habitacion.alto}\nLargo: ${habitacion.largo}\nAncho: ${habitacion.ancho}"
                    table.addCell(createContentCell(dimensionesStr, regularFont, TextAlignment.LEFT))
                    table.addCell(createContentCell(habitacion.getDanosList().joinToString("\n"), regularFont))
                    table.addCell(createContentCell(habitacion.comentarios, regularFont))

                    // Fila 2: Contenedor de Fotos para esta habitación (abarca todas las columnas)
                    val listaDeRutasDeFotos: List<String> = habitacion.getFotosList()
                    if (listaDeRutasDeFotos.isNotEmpty()) {
                        val fotoContainerCell = Cell(1, NUMERO_COLUMNAS_DETALLES) // (rowspan, colspan)
                            .setPadding(5f)
                            .setBorderTop(SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f)) // Borde superior para separar de los detalles
                            .setBorderBottom(SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f))// Borde inferior
                            .setBorderLeft(null) // Sin bordes laterales para la celda contenedora
                            .setBorderRight(null)

                        // Tabla interna para organizar las fotos, ejemplo: 3 fotos por fila
                        val FOTOS_POR_FILA_INTERNA = 3
                        val internalFotoTableWidths = FloatArray(FOTOS_POR_FILA_INTERNA) { 1f } // Columnas de igual ancho
                        val fotosTableInterna = Table(UnitValue.createPercentArray(internalFotoTableWidths))
                            .useAllAvailableWidth()
                            .setBorder(null) // Sin borde para esta tabla interna

                        var fotosEnFilaActualInterna = 0
                        listaDeRutasDeFotos.forEach { fotoPathString ->
                            if (fotoPathString.isNotBlank()) {
                                try {
                                    val file = File(fotoPathString)
                                    if (file.exists()) {
                                        val imageBytes = file.readBytes()
                                        if (imageBytes.isNotEmpty()) {
                                            val imageData = ImageDataFactory.create(imageBytes)
                                            val image = Image(imageData)
                                                .setAutoScale(true) // iText intentará escalar para que quepa en la celda
                                                .setTextAlignment(TextAlignment.CENTER)
                                            // .setMaxHeight(80f) // Opcional: limitar altura

                                            val imageCell = Cell().add(image).setBorder(null).setPadding(2f).setTextAlignment(TextAlignment.CENTER)
                                            fotosTableInterna.addCell(imageCell)
                                            fotosEnFilaActualInterna++

                                            if (fotosEnFilaActualInterna == FOTOS_POR_FILA_INTERNA) {
                                                // La tabla interna automáticamente pasa a la siguiente fila
                                                // cuando se llena el número de columnas definidas
                                                fotosEnFilaActualInterna = 0
                                            }
                                        } else { Log.w(TAG, "Bytes vacíos: $fotoPathString") }
                                    } else {
                                        Log.w(TAG, "Foto no existe: $fotoPathString")
                                        val errorCell = Cell().add(Paragraph("Img no encontrada").setFont(regularFont).setFontSize(7f)).setBorder(null).setPadding(2f)
                                        fotosTableInterna.addCell(errorCell)
                                        fotosEnFilaActualInterna++
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error cargando foto '$fotoPathString': ${e.message}", e)
                                    val errorCell = Cell().add(Paragraph("Error img").setFont(regularFont).setFontSize(7f)).setBorder(null).setPadding(2f)
                                    fotosTableInterna.addCell(errorCell)
                                    fotosEnFilaActualInterna++
                                } finally {
                                    if (fotosEnFilaActualInterna == FOTOS_POR_FILA_INTERNA) {
                                        fotosEnFilaActualInterna = 0 // Reset para la siguiente iteración
                                    }
                                }
                            }
                        }
                        // Completar la última fila de la tabla interna de fotos si no está llena
                        if (fotosEnFilaActualInterna > 0) {
                            for (i in fotosEnFilaActualInterna until FOTOS_POR_FILA_INTERNA) {
                                fotosTableInterna.addCell(Cell().add(Paragraph(" ")).setBorder(null)) // Celda vacía
                            }
                        }
                        fotoContainerCell.add(fotosTableInterna)
                        table.addCell(fotoContainerCell)
                    } else {
                        // Si no hay fotos, añade una celda que abarque y diga "Sin fotos"
                        val noFotoCell = Cell(1, NUMERO_COLUMNAS_DETALLES)
                            .add(Paragraph("Sin fotos registradas")
                                .setFont(regularFont).setFontSize(8f)
                                .setTextAlignment(TextAlignment.CENTER).setPadding(5f))
                            .setBorderTop(SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f))
                            .setBorderBottom(SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f))
                            .setBorderLeft(null).setBorderRight(null)
                        table.addCell(noFotoCell)
                    }
                    // Añade un pequeño espacio visual después de cada habitación completa (detalles + fotos)
                    if (indexHab < habitaciones.size - 1) {
                        val spacerCell = Cell(1, NUMERO_COLUMNAS_DETALLES)
                            .setBorder(null) // Sin bordes
                            .setHeight(10f)  // Altura del espacio
                        table.addCell(spacerCell)
                    }
                }
                document.add(table)
            } else {
                document.add(Paragraph("No se registraron habitaciones.").setFont(regularFont).setItalic())
            }

            document.close()
            Log.i(TAG, "PDF generado: $finalFileName. Uri: $fileUri, Path: ${legacyFile?.absolutePath}")
            return PdfCreationResult(legacyFile, fileUri, finalFileName)

        } catch (e: Exception) {
            Log.e(TAG, "Error al generar PDF: ${e.message}", e)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && fileUri != null) {
                try { context.contentResolver.delete(fileUri, null, null) }
                catch (deleteEx: Exception) { Log.e(TAG, "Error eliminando URI de MediaStore: ${deleteEx.message}") }
            }
            return null
        } finally {
            try { outputStream?.close() }
            catch (e: Exception) { Log.e(TAG, "Error cerrando outputStream: ${e.message}") }
        }
    }

    private fun addParagraphWithLabel(document: Document, label: String, value: String?, font: PdfFont, fontSize: Float = 10f, marginBottom: Float = 2f) {
        val p = Paragraph()
            .setFont(font).setFontSize(fontSize).setMarginBottom(marginBottom)
        p.add(com.itextpdf.layout.element.Text(label).setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)))
        p.add(" ${value ?: "N/A"}")
        document.add(p)
    }

    private fun createHeaderCell(text: String, font: PdfFont): Cell {
        return Cell().add(
            Paragraph(text).setFont(font).setFontSize(10f).setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE) // Mejor alineación vertical
        ).setPadding(4f).setBackgroundColor(ColorConstants.LIGHT_GRAY) // Fondo para cabeceras
    }

    private fun createContentCell(text: String?, font: PdfFont, alignment: TextAlignment = TextAlignment.LEFT): Cell {
        return Cell().add(
            Paragraph(text ?: "N/A").setFont(font).setFontSize(9f)
                .setTextAlignment(alignment)
        ).setPadding(4f)
    }
}
