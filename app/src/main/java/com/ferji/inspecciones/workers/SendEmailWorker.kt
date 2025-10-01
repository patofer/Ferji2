package com.ferji.inspecciones.workers // O el paquete que prefieras

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ferji.inspecciones.data.model.InspeccionEntity // Asumiendo que puedes serializar esto o pasar ID
import com.ferji.inspecciones.data.network.sendgrid.* // Tus data classes de SendGrid
import com.ferji.inspecciones.data.repository.InspeccionRepository // Si necesitas recuperar la inspección por ID
import com.ferji.inspecciones.utils.FileUtils
import com.google.gson.Gson // Para deserializar InspeccionEntity si la pasas como JSON String
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SendEmailWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val sendGridApiService: SendGridApiService, // Inyectado por Hilt
    // Inyecta el repositorio si prefieres pasar solo el ID y recuperar la entidad aquí
    private val inspeccionRepository: InspeccionRepository,
    private val gson: Gson // Para deserializar objetos complejos pasados como String
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_INSPECCION_ID = "KEY_INSPECCION_ID"
        const val KEY_PDF_URI_STRING = "KEY_PDF_URI_STRING"
        const val TAG = "SendEmailWorker"
    }

    override suspend fun doWork(): Result {
        val inspeccionId = inputData.getLong(KEY_INSPECCION_ID, -1L)
        val pdfUriString = inputData.getString(KEY_PDF_URI_STRING)

        if (inspeccionId == -1L || pdfUriString == null) {
            Log.e(TAG, "Error: Faltan datos de entrada (ID de inspección o URI del PDF).")
            return Result.failure()
        }

        val pdfUri = Uri.parse(pdfUriString)

        return try {
            Log.d(TAG, "Iniciando trabajo de envío de email para inspección ID: $inspeccionId")

            // Obtener la entidad de inspección (si pasaste el ID)
            val inspeccion = inspeccionRepository.getInspeccionById(inspeccionId) // Asume que este método existe y es suspend o se llama en un contexto IO
            if (inspeccion == null) {
                Log.e(TAG, "Error: No se encontró la inspección con ID: $inspeccionId.")
                return Result.failure()
            }

            val pdfBase64 = FileUtils.convertUriToBase64(applicationContext, pdfUri)
            if (pdfBase64 == null) {
                Log.e(TAG, "Error: No se pudo convertir el PDF a Base64.")
                return Result.failure() // Podrías querer reintentar si es un problema temporal
            }

            // Construir el SendGridMail (similar a como lo haces en el ViewModel)
            val fromEmail = EmailAddress(email = "patriciofernandez@gmail.com", name = "Equipo Ferji Inspecciones")
            val nombreClienteParaEmail = "Cliente Inspección" // O obtener de 'inspeccion'
            val toList = listOf(EmailAddress(email = inspeccion.mail ?: return Result.failure(), name = nombreClienteParaEmail))
            val ccEmail = "PFERNANDEZA@GMAIL.COM"
            val ccName = "Ferji Siniestros"
            val ccList = if (inspeccion.mail?.equals(ccEmail, ignoreCase = true) != true) {
                listOf(EmailAddress(email = ccEmail, name = ccName))
            } else { null }

            val personalizations = listOf(Personalization(to = toList, cc = ccList))
            val subject = "Informe Inspección: Siniestro ${inspeccion.siniestro ?: "N/A"} - RUT ${inspeccion.rut ?: "N/A"}"
            val emailBody = """
            Estimado/a ${nombreClienteParaEmail},
            Adjunto encontrará el informe de la inspección realizada para el siniestro N° ${inspeccion.siniestro ?: "No especificado"}.
            Detalles: ...
            Saludos cordiales,
            El Equipo de Ferji Inspecciones
            """.trimIndent() // Completa el cuerpo del email
            val contentList = listOf(Content(type = "text/plain", value = emailBody))
            val nombreArchivoPdf = "Informe_Inspeccion_${inspeccion.siniestro?.replace("[^a-zA-Z0-9.-]", "_") ?: inspeccion.id}.pdf"
            val attachmentsList = listOf(Attachment(content = pdfBase64, filename = nombreArchivoPdf, type = "application/pdf", disposition = "attachment"))

            val mailData = SendGridMail(personalizations, fromEmail, subject, contentList, attachmentsList)

            // --- Llamar a la API de SendGrid ---
            val response = sendGridApiService.sendEmail(mailData)

            if (response.isSuccessful) {
                Log.i(TAG, "Email para inspección ID $inspeccionId enviado exitosamente por SendGrid.")
                Result.success()
            } else {
                Log.e(TAG, "Error al enviar email para inspección ID $inspeccionId. Código: ${response.code()}, Mensaje: ${response.message()}")
                response.errorBody()?.let { Log.e(TAG, "Cuerpo del error: ${it.string()}") }
                // Decide si reintentar o marcar como fallo definitivo
                if (response.code() == 401 || response.code() == 403) { // Errores de autenticación/autorización no deberían reintentarse indefinidamente
                    Result.failure()
                } else {
                    Result.retry() // Para errores de servidor (5xx) o de red temporales
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción en SendEmailWorker: ${e.message}", e)
            Result.retry() // Reintentar en caso de excepciones (ej. problemas de red)
        }
    }
}

