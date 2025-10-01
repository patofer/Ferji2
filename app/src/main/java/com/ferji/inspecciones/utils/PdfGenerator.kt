package com.ferji.inspecciones.utils // Asegúrate de que el paquete sea correcto

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.ferji.inspecciones.R
import com.ferji.inspecciones.data.model.HabitacionEntity
import com.ferji.inspecciones.data.model.InspeccionEntity
import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.events.Event
import com.itextpdf.kernel.events.IEventHandler
import com.itextpdf.kernel.events.PdfDocumentEvent
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.layout.Canvas
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Image // Asegúrate que este sea el import correcto
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.HorizontalAlignment
import com.itextpdf.layout.properties.Property
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.itextpdf.layout.properties.VerticalAlignment
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
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

            // --- PREPARAR EL LOGO PARA LA MARCA DE AGUA ---
            var logoBytesParaMarcaDeAgua: ByteArray? = null
            try {
                val logoResourceId = R.raw.logo_ferji
                context.resources.openRawResource(logoResourceId).use { inputStream ->
                    logoBytesParaMarcaDeAgua = inputStream.readBytes()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar bytes del logo para marca de agua: ${e.message}", e)
            }

            // --- AÑADIR EL EVENT HANDLER PARA LA MARCA DE AGUA ---
            if (logoBytesParaMarcaDeAgua != null && logoBytesParaMarcaDeAgua!!.isNotEmpty()) {
                // Ajusta la opacidad según necesites (0.0f transparente, 1.0f opaco)
                val watermarkHandler = WatermarkEventHandler(
                    logoBytes = logoBytesParaMarcaDeAgua!!,
                    targetWidthInPoints = 50f,      // <-- Especifica el ancho que quieres (ej. 30f)
                    opacity = 0.9f,         // <-- Especifica la opacidad
                    marginFromTop = 20f,    // <-- Especifica el margen superior
                    marginFromRight = 20f   // <-- Especifica el margen derecho
                )// Opacidad baja
                pdfDocument.addEventHandler(PdfDocumentEvent.END_PAGE, watermarkHandler)
                Log.d(TAG, "WatermarkEventHandler añadido al documento PDF.")
            }

            val document = Document(pdfDocument, PageSize.A4)
            document.setMargins(36f, 36f, 36f, 36f) // Márgenes para el contenido principal

            val titleFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)
            val regularFont = PdfFontFactory.createFont(StandardFonts.HELVETICA)

            // --- EL LOGO COMO ELEMENTO EN LA CABECERA SE ELIMINA ---
            // El WatermarkEventHandler se encargará de ello.

            // --- TÍTULO ---
            document.add(
                Paragraph("INFORME DE INSPECCIÓN")
                    .setFont(titleFont).setFontSize(18f).setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(20f) // Ajusta el margen superior si es necesario
                    .setMarginBottom(20f)
            )

            // --- DATOS GENERALES ---
            addParagraphWithLabel(document, "Siniestro:", inspeccion.siniestro, regularFont)
            addParagraphWithLabel(document, "Dirección:", inspeccion.direccion, regularFont)
            addParagraphWithLabel(document, "RUT Cliente:", inspeccion.rut, regularFont)
            addParagraphWithLabel(document, "Mail Contacto:", inspeccion.mail, regularFont)
            addParagraphWithLabel(document, "Inspector:", inspeccion.rutInspector, regularFont)
            val fechaActual = Date()
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

                val NUMERO_COLUMNAS_DETALLES = 4
                val columnWidths = floatArrayOf(2f, 2f, 3f, 3f)
                val table = Table(UnitValue.createPercentArray(columnWidths)).useAllAvailableWidth()

                table.addHeaderCell(createHeaderCell("Habitación", titleFont))
                table.addHeaderCell(createHeaderCell("Dimensiones (cm)", titleFont))
                table.addHeaderCell(createHeaderCell("Daños", titleFont))
                table.addHeaderCell(createHeaderCell("Observaciones", titleFont))

                habitaciones.forEachIndexed { indexHab, habitacion ->
                    table.addCell(createContentCell(habitacion.nombre, regularFont))
                    val dimensionesStr = "Alto: ${habitacion.alto}\nLargo: ${habitacion.largo}\nAncho: ${habitacion.ancho}"
                    table.addCell(createContentCell(dimensionesStr, regularFont, TextAlignment.LEFT))
                    table.addCell(createContentCell(habitacion.getDanosList().joinToString("\n"), regularFont))
                    table.addCell(createContentCell(habitacion.comentarios, regularFont))

                    val listaDeRutasDeFotos: List<String> = habitacion.getFotosList()
                    Log.d(TAG, "Habitación '${habitacion.nombre}': Iniciando procesamiento de fotos. Número de rutas: ${listaDeRutasDeFotos.size}")

                    if (listaDeRutasDeFotos.isNotEmpty()) {
                        val fotoContainerCell = Cell(1, NUMERO_COLUMNAS_DETALLES)
                            .setPadding(5f)
                            .setBorderTop(SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f))
                            .setBorderBottom(SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f))
                            .setBorderLeft(null).setBorderRight(null)

                        val FOTOS_POR_FILA_INTERNA = 3
                        val internalFotoTableWidths = FloatArray(FOTOS_POR_FILA_INTERNA) { 1f }
                        val fotosTableInterna = Table(UnitValue.createPercentArray(internalFotoTableWidths))
                            .useAllAvailableWidth().setBorder(null)

                        var fotosEnFilaActualInterna = 0
                        var bitmapRotado: Bitmap? = null // Mover fuera del forEach para reciclar en finally
                        listaDeRutasDeFotos.forEachIndexed { index, fotoPathString ->
                            Log.d(TAG, "Procesando foto ${index + 1}/${listaDeRutasDeFotos.size}: '$fotoPathString'")
                            bitmapRotado = null // Resetear para cada foto
                            if (fotoPathString.isNotBlank()) {
                                try {
                                    Log.d(TAG, "Intentando cargar y rotar: '$fotoPathString'")
                                    bitmapRotado = cargarYRotarBitmap(fotoPathString, 150f, 150f) // Ajusta estos tamaños si es necesario

                                    if (bitmapRotado != null) {
                                        Log.d(TAG, "Bitmap rotado obtenido para '$fotoPathString'. Dimensiones: ${bitmapRotado!!.width}x${bitmapRotado!!.height}")
                                        val stream = ByteArrayOutputStream()
                                        Log.d(TAG, "Comprimiendo bitmap a JPEG para '$fotoPathString'...")
                                        val successCompress = bitmapRotado!!.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                                        if (!successCompress) {
                                            Log.w(TAG, "bitmapRotado.compress devolvió false para '$fotoPathString'")
                                        }
                                        val imageBytes = stream.toByteArray()
                                        Log.d(TAG, "Tamaño de imageBytes para '$fotoPathString': ${imageBytes.size}")

                                        if (imageBytes.isNotEmpty()) {
                                            Log.d(TAG, "Creando ImageData para '$fotoPathString'")
                                            val imageData = ImageDataFactory.create(imageBytes)
                                            val image = Image(imageData)
                                                .setAutoScale(true)
                                                .setTextAlignment(TextAlignment.CENTER)

                                            Log.d(TAG, "Añadiendo imagen a la celda para '$fotoPathString'")
                                            val imageCell = Cell().add(image).setBorder(null).setPadding(2f).setTextAlignment(TextAlignment.CENTER)
                                            fotosTableInterna.addCell(imageCell)
                                            fotosEnFilaActualInterna++
                                            Log.d(TAG, "Imagen añadida correctamente para '$fotoPathString'. fotosEnFilaActualInterna: $fotosEnFilaActualInterna")
                                        } else {
                                            addErrorCellToFotoTable(fotosTableInterna, "Error img (vacía)", regularFont)
                                            fotosEnFilaActualInterna++
                                        }
                                    } else {
                                        addErrorCellToFotoTable(fotosTableInterna, "Img no cargada", regularFont)
                                        fotosEnFilaActualInterna++
                                    }

                                    if (fotosEnFilaActualInterna == FOTOS_POR_FILA_INTERNA) {
                                        Log.d(TAG, "Fila interna de fotos completada. Reseteando contador.")
                                        fotosEnFilaActualInterna = 0
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "EXCEPCIÓN al procesar foto para PDF '$fotoPathString': ${e.message}", e)
                                    addErrorCellToFotoTable(fotosTableInterna, "Error img (exc)", regularFont)
                                    fotosEnFilaActualInterna++
                                    if (fotosEnFilaActualInterna == FOTOS_POR_FILA_INTERNA) fotosEnFilaActualInterna = 0
                                } finally {
                                    bitmapRotado?.recycle() // Reciclar el bitmap después de usarlo
                                    Log.d(TAG, "Bloque finally para '$fotoPathString'. Bitmap reciclado (si existía). fotosEnFilaActualInterna: $fotosEnFilaActualInterna")
                                }
                            } else {
                                Log.w(TAG, "Ruta de foto vacía o en blanco encontrada en el índice $index.")
                            }
                        }
                        Log.d(TAG, "Procesamiento de todas las rutas de fotos finalizado para la habitación '${habitacion.nombre}'. fotosEnFilaActualInterna (antes de completar): $fotosEnFilaActualInterna")

                        if (fotosEnFilaActualInterna > 0 && fotosEnFilaActualInterna < FOTOS_POR_FILA_INTERNA) {
                            Log.d(TAG, "Completando la última fila de fotos con ${FOTOS_POR_FILA_INTERNA - fotosEnFilaActualInterna} celdas vacías.")
                            for (i in fotosEnFilaActualInterna until FOTOS_POR_FILA_INTERNA) {
                                fotosTableInterna.addCell(Cell().add(Paragraph(" ")).setBorder(null))
                            }
                        }
                        fotoContainerCell.add(fotosTableInterna)
                        table.addCell(fotoContainerCell)
                        Log.d(TAG, "Contenedor de fotos añadido a la tabla principal para la habitación '${habitacion.nombre}'.")
                    } else {
                        Log.d(TAG, "Habitación '${habitacion.nombre}': No hay rutas de fotos para procesar.")
                        val noFotoCell = Cell(1, NUMERO_COLUMNAS_DETALLES)
                            .add(Paragraph("Sin fotos registradas")
                                .setFont(regularFont).setFontSize(8f)
                                .setTextAlignment(TextAlignment.CENTER).setPadding(5f))
                            .setBorderTop(SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f))
                            .setBorderBottom(SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f))
                            .setBorderLeft(null).setBorderRight(null)
                        table.addCell(noFotoCell)
                    }

                    if (indexHab < habitaciones.size - 1) {
                        val spacerCell = Cell(1, NUMERO_COLUMNAS_DETALLES)
                            .setBorder(null).setHeight(10f)
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
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
        ).setPadding(4f).setBackgroundColor(ColorConstants.LIGHT_GRAY)
    }

    private fun createContentCell(text: String?, font: PdfFont, alignment: TextAlignment = TextAlignment.LEFT): Cell {
        return Cell().add(
            Paragraph(text ?: "N/A").setFont(font).setFontSize(9f)
                .setTextAlignment(alignment)
        ).setPadding(4f)
    }

    private fun addErrorCellToFotoTable(table: Table, errorMessage: String, font: PdfFont) {
        val errorCell = Cell().add(Paragraph(errorMessage).setFont(font).setFontSize(7f)).setBorder(null).setPadding(2f)
        table.addCell(errorCell)
    }

    private fun cargarYRotarBitmap(rutaFoto: String, maxWidth: Float, maxHeight: Float): Bitmap? {
        var originalBitmap: Bitmap? = null // Para asegurar el reciclaje en caso de error temprano
        try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(rutaFoto, options)

            options.inSampleSize = calculateInSampleSize(options, maxWidth.toInt(), maxHeight.toInt())
            options.inJustDecodeBounds = false
            originalBitmap = BitmapFactory.decodeFile(rutaFoto, options)
            if (originalBitmap == null) {
                Log.e(TAG, "BitmapFactory.decodeFile devolvió null después de inSampleSize para: $rutaFoto")
                return null
            }

            val exif = ExifInterface(rutaFoto)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

            val matrix = Matrix()
            var needsRotation = true
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                // Aquí puedes añadir más casos para volteos si es necesario
                ExifInterface.ORIENTATION_NORMAL, ExifInterface.ORIENTATION_UNDEFINED -> needsRotation = false
                else -> needsRotation = false
            }

            if (needsRotation && !matrix.isIdentity) {
                val rotatedBitmap = Bitmap.createBitmap(
                    originalBitmap, 0, 0,
                    originalBitmap.width, originalBitmap.height,
                    matrix, true
                )
                if (rotatedBitmap != originalBitmap) { // Solo recicla el original si se creó uno nuevo
                    originalBitmap.recycle()
                }
                return rotatedBitmap
            }
            return originalBitmap // Devuelve el original (ya escalado) si no se necesitó rotación
        } catch (e: IOException) {
            Log.e(TAG, "IOException al cargar o rotar bitmap '$rutaFoto': ${e.message}")
            originalBitmap?.recycle()
            return null
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "OutOfMemoryError al cargar o rotar bitmap '$rutaFoto': ${e.message}")
            originalBitmap?.recycle()
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Excepción general al cargar o rotar bitmap '$rutaFoto': ${e.message}", e)
            originalBitmap?.recycle()
            return null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (reqWidth <= 0 || reqHeight <= 0) return 1 // No escalar si las dimensiones no son válidas

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}