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
import com.ferji.inspecciones.data.repository.PartidaRepository
import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
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
import java.text.NumberFormat
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

    suspend fun createPdf(
        context: Context,
        inspeccion: InspeccionEntity,
        habitaciones: List<HabitacionEntity>,
        partidaRepository: PartidaRepository? = null
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

                habitaciones.forEachIndexed { indexHab, habitacion ->
                    // ═══ TABLA DE DATOS DE LA HABITACIÓN ═══
                    val columnWidths = floatArrayOf(2f, 2f, 3f, 3f)
                    val table = Table(UnitValue.createPercentArray(columnWidths)).useAllAvailableWidth()

                    // Solo agregar headers en la primera habitación o al inicio de página nueva
                    table.addHeaderCell(createHeaderCell("Habitación", titleFont))
                    table.addHeaderCell(createHeaderCell("Dimensiones (cm)", titleFont))
                    table.addHeaderCell(createHeaderCell("Daños", titleFont))
                    table.addHeaderCell(createHeaderCell("Observaciones", titleFont))

                    table.addCell(createContentCell(habitacion.nombre, regularFont))
                    val dimensionesParts = mutableListOf("Largo: ${habitacion.largo}")
                    if (habitacion.ancho > 0) dimensionesParts.add("Ancho: ${habitacion.ancho}")
                    dimensionesParts.add("Alto: ${habitacion.alto}")
                    val dimensionesStr = dimensionesParts.joinToString("\n")
                    table.addCell(createContentCell(dimensionesStr, regularFont, TextAlignment.LEFT))
                    table.addCell(createContentCell(habitacion.getDanosList().joinToString("\n"), regularFont))
                    table.addCell(createContentCell(habitacion.comentarios, regularFont))

                    document.add(table)

                    // ═══ FOTOS DE LA HABITACIÓN (fuera de la tabla, con tamaño fijo) ═══
                    val listaDeRutasDeFotos: List<String> = habitacion.getFotosList()
                    Log.d(TAG, "Habitación '${habitacion.nombre}': ${listaDeRutasDeFotos.size} fotos.")

                    if (listaDeRutasDeFotos.isNotEmpty()) {
                        document.add(
                            Paragraph("Fotos - ${habitacion.nombre}")
                                .setFont(titleFont).setFontSize(9f)
                                .setMarginTop(4f).setMarginBottom(4f)
                                .setFontColor(DeviceRgb(100, 100, 100))
                        )

                        // Tamaño fijo para cada foto en puntos (≈ 5.3cm × 5.3cm)
                        val FOTO_ANCHO_PTS = 150f
                        val FOTO_ALTO_PTS = 150f
                        val FOTOS_POR_FILA = 3

                        val fotosTableWidths = FloatArray(FOTOS_POR_FILA) { 1f }
                        val fotosTable = Table(UnitValue.createPercentArray(fotosTableWidths))
                            .useAllAvailableWidth()
                            .setBorder(null)
                            .setKeepTogether(false) // Permitir que la tabla se divida entre páginas

                        var fotosEnFila = 0
                        var bitmapRotado: Bitmap? = null

                        listaDeRutasDeFotos.forEachIndexed { index, fotoPathString ->
                            bitmapRotado = null
                            if (fotoPathString.isNotBlank()) {
                                try {
                                    bitmapRotado = cargarYRotarBitmap(fotoPathString, 300f, 300f)

                                    if (bitmapRotado != null) {
                                        val stream = ByteArrayOutputStream()
                                        bitmapRotado!!.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                                        val imageBytes = stream.toByteArray()

                                        if (imageBytes.isNotEmpty()) {
                                            val imageData = ImageDataFactory.create(imageBytes)
                                            val image = Image(imageData)
                                                // Tamaño FIJO — no se encoge para caber en la página
                                                .setWidth(FOTO_ANCHO_PTS)
                                                .setHeight(FOTO_ALTO_PTS)
                                                .setHorizontalAlignment(HorizontalAlignment.CENTER)

                                            val imageCell = Cell()
                                                .add(image)
                                                .setBorder(null)
                                                .setPadding(3f)
                                                .setTextAlignment(TextAlignment.CENTER)
                                                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                                            fotosTable.addCell(imageCell)
                                            fotosEnFila++
                                        } else {
                                            addErrorCellToFotoTable(fotosTable, "Error img", regularFont)
                                            fotosEnFila++
                                        }
                                    } else {
                                        addErrorCellToFotoTable(fotosTable, "Img no cargada", regularFont)
                                        fotosEnFila++
                                    }

                                    if (fotosEnFila == FOTOS_POR_FILA) {
                                        fotosEnFila = 0
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error procesando foto '$fotoPathString': ${e.message}", e)
                                    addErrorCellToFotoTable(fotosTable, "Error", regularFont)
                                    fotosEnFila++
                                    if (fotosEnFila == FOTOS_POR_FILA) fotosEnFila = 0
                                } finally {
                                    bitmapRotado?.recycle()
                                }
                            }
                        }

                        // Completar última fila con celdas vacías
                        if (fotosEnFila > 0 && fotosEnFila < FOTOS_POR_FILA) {
                            for (i in fotosEnFila until FOTOS_POR_FILA) {
                                fotosTable.addCell(Cell().add(Paragraph(" ")).setBorder(null))
                            }
                        }

                        document.add(fotosTable)
                    } else {
                        document.add(
                            Paragraph("Sin fotos registradas")
                                .setFont(regularFont).setFontSize(8f).setItalic()
                                .setTextAlignment(TextAlignment.CENTER)
                                .setMarginBottom(4f)
                        )
                    }

                    // Separador entre habitaciones
                    if (indexHab < habitaciones.size - 1) {
                        document.add(Paragraph("").setMarginBottom(12f))
                    }
                }
            } else {
                document.add(Paragraph("No se registraron habitaciones.").setFont(regularFont).setItalic())
            }

            // ═══════════════════════════════════════════════════════════════
            //  NOTA: El presupuesto detallado se genera únicamente en el
            //  archivo Excel (ExcelGenerator). NO se incluye en el PDF.
            // ═══════════════════════════════════════════════════════════════

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

    // ═══════════════════════════════════════════════════════════════
    //  PRESUPUESTO DETALLADO EN PDF
    // ═══════════════════════════════════════════════════════════════

    private val nf: NumberFormat = NumberFormat.getIntegerInstance(Locale("es", "CL")).apply {
        isGroupingUsed = true
    }

    private fun formatMonto(valor: Double): String = "$${nf.format(valor.toLong())}"

    private fun redondear2(valor: Double): Double = (valor * 100.0).toLong() / 100.0

    // Colores para el presupuesto
    private val COLOR_HEADER_HAB = DeviceRgb(41, 128, 185)       // Azul oscuro
    private val COLOR_HEADER_PARTIDAS = DeviceRgb(52, 73, 94)    // Gris oscuro
    private val COLOR_FILA_PAR = DeviceRgb(245, 245, 245)        // Gris muy claro
    private val COLOR_TOTAL_HAB = DeviceRgb(39, 174, 96)         // Verde
    private val COLOR_TOTAL_GENERAL = DeviceRgb(192, 57, 43)     // Rojo oscuro
    private val COLOR_GENERALES = DeviceRgb(142, 68, 173)        // Morado

    private fun addPresupuestoDetallado(
        document: Document,
        presupuesto: ExcelGenerator.PresupuestoCompleto,
        titleFont: PdfFont,
        regularFont: PdfFont
    ) {
        document.add(com.itextpdf.layout.element.AreaBreak())

        document.add(
            Paragraph("PRESUPUESTO DETALLADO DE REPARACIÓN")
                .setFont(titleFont).setFontSize(16f).setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(10f).setMarginBottom(20f)
        )

        // ══════════════════════════════════════════
        //  HABITACIONES (solo partidas variables)
        // ══════════════════════════════════════════
        for (hab in presupuesto.habitaciones) {
            // Encabezado de habitación — dimensiones dinámicas según datos disponibles
            val tieneAncho = hab.anchoCm > 0
            val habHeaderWidths = if (tieneAncho) floatArrayOf(4f, 1f, 1f, 1f) else floatArrayOf(4f, 1f, 1f)
            val habHeaderTable = Table(UnitValue.createPercentArray(habHeaderWidths)).useAllAvailableWidth()
            habHeaderTable.addCell(
                Cell().add(Paragraph(hab.nombre.uppercase()).setFont(titleFont).setFontSize(11f).setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(COLOR_HEADER_HAB).setPadding(6f).setBorder(null)
            )
            habHeaderTable.addCell(
                Cell().add(Paragraph("Largo: ${String.format(Locale.getDefault(), "%.2f", hab.largoCm / 100.0)}")
                    .setFont(regularFont).setFontSize(8f).setFontColor(ColorConstants.WHITE).setTextAlignment(TextAlignment.CENTER))
                    .setBackgroundColor(COLOR_HEADER_HAB).setPadding(6f).setBorder(null)
            )
            if (tieneAncho) {
                habHeaderTable.addCell(
                    Cell().add(Paragraph("Ancho: ${String.format(Locale.getDefault(), "%.2f", hab.anchoCm / 100.0)}")
                        .setFont(regularFont).setFontSize(8f).setFontColor(ColorConstants.WHITE).setTextAlignment(TextAlignment.CENTER))
                        .setBackgroundColor(COLOR_HEADER_HAB).setPadding(6f).setBorder(null)
                )
            }
            habHeaderTable.addCell(
                Cell().add(Paragraph("Alto: ${String.format(Locale.getDefault(), "%.2f", hab.altoCm / 100.0)}")
                    .setFont(regularFont).setFontSize(8f).setFontColor(ColorConstants.WHITE).setTextAlignment(TextAlignment.CENTER))
                    .setBackgroundColor(COLOR_HEADER_HAB).setPadding(6f).setBorder(null)
            )
            document.add(habHeaderTable)

            // Tabla de partidas variables
            if (hab.lineasVariables.isNotEmpty()) {
                val partidasTable = Table(UnitValue.createPercentArray(floatArrayOf(5f, 1.2f, 1.2f, 1.5f, 1.5f))).useAllAvailableWidth()
                partidasTable.addCell(createPresupuestoHeaderCell("Descripción", titleFont))
                partidasTable.addCell(createPresupuestoHeaderCell("Medida", titleFont))
                partidasTable.addCell(createPresupuestoHeaderCell("Cantidad", titleFont))
                partidasTable.addCell(createPresupuestoHeaderCell("P. Unitario", titleFont))
                partidasTable.addCell(createPresupuestoHeaderCell("P. Total", titleFont))

                hab.lineasVariables.forEachIndexed { index, linea ->
                    addLineaPartida(partidasTable, linea, index, regularFont)
                }

                // Total habitación
                partidasTable.addCell(
                    Cell(1, 4).add(Paragraph("TOTAL ${hab.nombre.uppercase()}")
                        .setFont(titleFont).setFontSize(9f).setFontColor(ColorConstants.WHITE).setTextAlignment(TextAlignment.RIGHT))
                        .setBackgroundColor(COLOR_TOTAL_HAB).setPadding(5f).setBorder(null)
                )
                partidasTable.addCell(
                    Cell().add(Paragraph(formatMonto(hab.totalHabitacion))
                        .setFont(titleFont).setFontSize(9f).setFontColor(ColorConstants.WHITE).setTextAlignment(TextAlignment.RIGHT))
                        .setBackgroundColor(COLOR_TOTAL_HAB).setPadding(5f).setBorder(null)
                )
                document.add(partidasTable)
            } else {
                document.add(Paragraph("  Sin partidas asociadas.").setFont(regularFont).setFontSize(9f).setItalic().setMarginBottom(5f))
            }

            document.add(Paragraph("").setMarginBottom(12f))
        }

        // ══════════════════════════════════════════
        //  GENERALES (gastos fijos)
        // ══════════════════════════════════════════
        if (presupuesto.lineasFijasGlobales.isNotEmpty()) {
            val generalesHeaderTable = Table(UnitValue.createPercentArray(floatArrayOf(1f))).useAllAvailableWidth()
            generalesHeaderTable.addCell(
                Cell().add(Paragraph("GENERALES")
                    .setFont(titleFont).setFontSize(11f).setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(COLOR_GENERALES).setPadding(6f).setBorder(null)
            )
            document.add(generalesHeaderTable)

            val generalesTable = Table(UnitValue.createPercentArray(floatArrayOf(5f, 1.2f, 1.2f, 1.5f, 1.5f))).useAllAvailableWidth()
            generalesTable.addCell(createPresupuestoHeaderCell("Descripción", titleFont))
            generalesTable.addCell(createPresupuestoHeaderCell("Medida", titleFont))
            generalesTable.addCell(createPresupuestoHeaderCell("Cantidad", titleFont))
            generalesTable.addCell(createPresupuestoHeaderCell("P. Unitario", titleFont))
            generalesTable.addCell(createPresupuestoHeaderCell("P. Total", titleFont))

            presupuesto.lineasFijasGlobales.forEachIndexed { index, linea ->
                addLineaPartida(generalesTable, linea, index, regularFont)
            }

            // Subtotal generales
            generalesTable.addCell(
                Cell(1, 4).add(Paragraph("SUBTOTAL GENERALES")
                    .setFont(titleFont).setFontSize(9f).setFontColor(ColorConstants.WHITE).setTextAlignment(TextAlignment.RIGHT))
                    .setBackgroundColor(COLOR_GENERALES).setPadding(5f).setBorder(null)
            )
            generalesTable.addCell(
                Cell().add(Paragraph(formatMonto(presupuesto.totalFijas))
                    .setFont(titleFont).setFontSize(9f).setFontColor(ColorConstants.WHITE).setTextAlignment(TextAlignment.RIGHT))
                    .setBackgroundColor(COLOR_GENERALES).setPadding(5f).setBorder(null)
            )
            document.add(generalesTable)
            document.add(Paragraph("").setMarginBottom(12f))
        }

        // ═══ TOTAL GENERAL ═══
        val totalTable = Table(UnitValue.createPercentArray(floatArrayOf(5f, 2f))).useAllAvailableWidth()
        totalTable.addCell(
            Cell().add(Paragraph("TOTAL GENERAL PRESUPUESTO")
                .setFont(titleFont).setFontSize(13f).setFontColor(ColorConstants.WHITE).setTextAlignment(TextAlignment.RIGHT))
                .setBackgroundColor(COLOR_TOTAL_GENERAL).setPadding(8f).setBorder(null)
        )
        totalTable.addCell(
            Cell().add(Paragraph(formatMonto(presupuesto.totalGeneral))
                .setFont(titleFont).setFontSize(13f).setFontColor(ColorConstants.WHITE).setTextAlignment(TextAlignment.RIGHT))
                .setBackgroundColor(COLOR_TOTAL_GENERAL).setPadding(8f).setBorder(null)
        )
        document.add(totalTable)
    }

    /** Agrega una fila de línea de partida a la tabla */
    private fun addLineaPartida(table: Table, linea: ExcelGenerator.LineaPresupuesto, index: Int, font: PdfFont) {
        val bgColor = if (index % 2 == 0) null else COLOR_FILA_PAR
        val border = SolidBorder(ColorConstants.LIGHT_GRAY, 0.3f)

        fun celda(text: String, alignment: TextAlignment = TextAlignment.LEFT): Cell {
            val cell = Cell().add(Paragraph(text).setFont(font).setFontSize(8f).setTextAlignment(alignment))
                .setPadding(4f).setBorder(border)
            if (bgColor != null) cell.setBackgroundColor(bgColor)
            return cell
        }

        table.addCell(celda(linea.descripcion))
        table.addCell(celda(linea.unidad, TextAlignment.CENTER))
        table.addCell(celda(String.format(Locale.getDefault(), "%.2f", redondear2(linea.cantidad)), TextAlignment.CENTER))
        table.addCell(celda(formatMonto(linea.precioUnitario), TextAlignment.RIGHT))
        table.addCell(celda(formatMonto(linea.subtotal), TextAlignment.RIGHT))
    }

    private fun createPresupuestoHeaderCell(text: String, font: PdfFont): Cell {
        return Cell().add(
            Paragraph(text).setFont(font).setFontSize(8f).setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.WHITE)
        ).setPadding(4f).setBackgroundColor(COLOR_HEADER_PARTIDAS).setBorder(null)
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