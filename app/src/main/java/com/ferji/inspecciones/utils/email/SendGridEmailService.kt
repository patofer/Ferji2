package com.ferji.inspecciones.utils.email

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.ferji.inspecciones.data.network.sendgrid.Attachment
import com.ferji.inspecciones.data.network.sendgrid.Content
import com.ferji.inspecciones.data.network.sendgrid.EmailAddress
import com.ferji.inspecciones.data.network.sendgrid.Personalization
import com.ferji.inspecciones.data.network.sendgrid.SendGridApiService
import com.ferji.inspecciones.data.network.sendgrid.SendGridMail
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Implementación de [EmailService] que usa la API REST de SendGrid.
 *
 * Para activar esta implementación:
 * 1. Configura SENDGRID_API_KEY en local.properties
 * 2. En [EmailModule], cambia el @Binds para apuntar a esta clase.
 *
 * Ejemplo en EmailModule:
 *   @Binds @Singleton
 *   abstract fun bindEmailService(impl: SendGridEmailService): EmailService
 */
class SendGridEmailService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sendGridApiService: SendGridApiService
) : EmailService {

    companion object {
        private const val TAG = "SendGridEmailService"
        private const val REMITENTE_EMAIL = "inspecciones@ferji.cl"
        private const val REMITENTE_NOMBRE = "Ferji Inspecciones"
    }

    override suspend fun enviarConPdf(
        destinatarios: List<String>,
        cc: List<String>?,
        asunto: String,
        cuerpoHtml: String,
        pdfUri: Uri,
        nombreArchivoAdjunto: String
    ): EmailService.EmailResult {
        return try {
            // 1. Leer el PDF y convertirlo a Base64
            val pdfBase64 = leerUriComoBase64(pdfUri)
                ?: return EmailService.EmailResult.Error("No se pudo leer el archivo PDF.")

            // 2. Construir el objeto SendGrid
            val mail = SendGridMail(
                personalizations = listOf(
                    Personalization(
                        to = destinatarios.map { EmailAddress(email = it) },
                        cc = cc?.map { EmailAddress(email = it) }?.ifEmpty { null }
                    )
                ),
                from = EmailAddress(email = REMITENTE_EMAIL, name = REMITENTE_NOMBRE),
                subject = asunto,
                content = listOf(Content(type = "text/html", value = cuerpoHtml)),
                attachments = listOf(
                    Attachment(
                        content = pdfBase64,
                        filename = nombreArchivoAdjunto,
                        type = "application/pdf",
                        disposition = "attachment"
                    )
                )
            )

            // 3. Llamar a la API
            Log.d(TAG, "Enviando email via SendGrid a: $destinatarios")
            val response = sendGridApiService.sendEmail(mail)

            if (response.isSuccessful) {
                Log.i(TAG, "Email enviado correctamente. HTTP ${response.code()}")
                EmailService.EmailResult.Success
            } else {
                val errorBody = response.errorBody()?.string() ?: "Sin detalle"
                Log.e(TAG, "Error HTTP ${response.code()}: $errorBody")
                EmailService.EmailResult.Error("Error del servidor (${response.code()}): $errorBody")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Excepción al enviar email: ${e.message}", e)
            EmailService.EmailResult.Error("Error inesperado: ${e.message}", e)
        }
    }

    private fun leerUriComoBase64(uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                Base64.encodeToString(inputStream.readBytes(), Base64.NO_WRAP)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error leyendo URI $uri como Base64: ${e.message}", e)
            null
        }
    }

    override suspend fun enviarConAdjuntos(
        destinatarios: List<String>,
        cc: List<String>?,
        asunto: String,
        cuerpoHtml: String,
        adjuntos: List<EmailService.Adjunto>
    ): EmailService.EmailResult {
        return try {
            val attachments = adjuntos.mapNotNull { adjunto ->
                val base64 = leerUriComoBase64(adjunto.uri) ?: return@mapNotNull null
                Attachment(
                    content = base64,
                    filename = adjunto.nombreArchivo,
                    type = adjunto.mimeType,
                    disposition = "attachment"
                )
            }

            val mail = SendGridMail(
                personalizations = listOf(
                    Personalization(
                        to = destinatarios.map { EmailAddress(email = it) },
                        cc = cc?.map { EmailAddress(email = it) }?.ifEmpty { null }
                    )
                ),
                from = EmailAddress(email = REMITENTE_EMAIL, name = REMITENTE_NOMBRE),
                subject = asunto,
                content = listOf(Content(type = "text/html", value = cuerpoHtml)),
                attachments = attachments.ifEmpty { null }
            )

            val response = sendGridApiService.sendEmail(mail)
            if (response.isSuccessful) {
                EmailService.EmailResult.Success
            } else {
                val errorBody = response.errorBody()?.string() ?: "Sin detalle"
                EmailService.EmailResult.Error("Error del servidor (${response.code()}): $errorBody")
            }
        } catch (e: Exception) {
            EmailService.EmailResult.Error("Error inesperado: ${e.message}", e)
        }
    }
}

