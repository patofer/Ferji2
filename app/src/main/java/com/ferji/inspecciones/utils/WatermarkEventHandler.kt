package com.ferji.inspecciones.utils


import android.util.Log
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.events.Event
import com.itextpdf.kernel.events.IEventHandler
import com.itextpdf.kernel.events.PdfDocumentEvent
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.layout.Canvas
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.properties.Property

// ... (imports de PdfGenerator) ...

// ... (otros imports) ...
import com.itextpdf.kernel.geom.Rectangle // Para obtener el tamaño de la página

class WatermarkEventHandler(
    private val logoBytes: ByteArray,
    private val targetWidthInPoints: Float,
    private val opacity: Float = 1.0f,
    private val marginFromTop: Float = 20f,
    private val marginFromRight: Float = 20f
) : IEventHandler {
    private val watermarkTag = "WatermarkLogoHandler"

    override fun handleEvent(event: Event) {
        val docEvent = event as PdfDocumentEvent
        val pdfDoc = docEvent.document
        val page = docEvent.page
        val pageSize: Rectangle = page.pageSize

        val pdfCanvas = PdfCanvas(page.newContentStreamBefore(), page.resources, pdfDoc)
        val canvas = Canvas(pdfCanvas, pageSize)

        try {
            if (logoBytes.isNotEmpty()) {
                val logoImageData = ImageDataFactory.create(logoBytes)
                val image = Image(logoImageData)

                Log.d(watermarkTag, "Imagen original XObject - Ancho: ${image.xObject.width}, Alto: ${image.xObject.height}")

                // --- INTENTO CON setWidth (COMENTADO PORQUE NO FUNCIONÓ) ---
                /*
                image.setWidth(targetWidthInPoints)
                image.setAutoScaleHeight(true)
                Log.d(watermarkTag, "Después de setWidth($targetWidthInPoints) - Ancho Escalado: ${image.imageScaledWidth}, Alto Escalado: ${image.imageScaledHeight}")
                */

                // --- INTENTO CON scaleAbsolute() ---
                // Usar las dimensiones del XObject como base para el ratio, ya que imageWidth/Height a veces pueden ser 0 antes del layout.
                val originalImageXObjectWidth = image.xObject.width
                val originalImageXObjectHeight = image.xObject.height

                if (originalImageXObjectWidth > 0) { // Evitar división por cero
                    // Calcular la altura deseada manteniendo la proporción original
                    val scaleRatio = targetWidthInPoints / originalImageXObjectWidth
                    val targetHeightInPoints = originalImageXObjectHeight * scaleRatio

                    image.scaleAbsolute(targetWidthInPoints, targetHeightInPoints)
                    Log.d(watermarkTag, "Después de scaleAbsolute($targetWidthInPoints, $targetHeightInPoints) - Ancho Escalado: ${image.imageScaledWidth}, Alto Escalado: ${image.imageScaledHeight}")
                } else {
                    Log.w(watermarkTag, "El ancho original (XObject) de la imagen es 0 o inválido, no se puede usar scaleAbsolute. Intentando setWidth como fallback.")
                    // Como fallback MUY BÁSICO si XObject no tiene dimensiones, intenta con setWidth
                    image.setWidth(targetWidthInPoints)
                    image.setAutoScaleHeight(true)
                    Log.d(watermarkTag, "Fallback a setWidth($targetWidthInPoints) - Ancho Escalado: ${image.imageScaledWidth}, Alto Escalado: ${image.imageScaledHeight}")
                }

                if (image.imageScaledWidth <= 0 || image.imageScaledHeight <= 0) {
                    Log.e(watermarkTag, "El tamaño de la imagen escalada es inválido: ${image.imageScaledWidth}x${image.imageScaledHeight}. No se dibujará la imagen.")
                    return // No intentar añadir una imagen con dimensiones inválidas
                }

                val imageX = pageSize.right - marginFromRight - image.imageScaledWidth
                val imageY = pageSize.top - marginFromTop - image.imageScaledHeight

                if (opacity < 1.0f) {
                    canvas.setProperty(Property.OPACITY, opacity)
                }
                canvas.add(image.setFixedPosition(imageX, imageY))

                Log.d(watermarkTag, "WatermarkEventHandler: targetWidthInPoints=$targetWidthInPoints, opacity=$opacity, imageScaledWidth=${image.imageScaledWidth}, imageX=$imageX, imageY=$imageY")

            }
        } catch (e: Exception) {
            Log.e(watermarkTag, "Error al añadir el logo de fondo: ${e.message}", e)
        } finally {
            canvas.close()
        }
    }
}
