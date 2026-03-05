package com.ferji.inspecciones.utils.email

import android.content.Context
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.activation.DataHandler
import javax.activation.DataSource
import javax.inject.Inject
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.Multipart
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import javax.mail.util.ByteArrayDataSource

/**
 * Implementación de [EmailService] que envía emails via SMTP usando JavaMail.
 * Gratuito, sin dependencia de APIs de pago.
 *
 * Usa Gmail SMTP por defecto. Requiere una "Contraseña de aplicación" (App Password).
 * Las credenciales se leen desde BuildConfig (definidas en local.properties):
 *   SMTP_USER=tu_correo@gmail.com
 *   SMTP_PASSWORD=xxxx_xxxx_xxxx_xxxx
 */
class SmtpEmailService @Inject constructor(
    @ApplicationContext private val context: Context
) : EmailService {

    companion object {
        private const val TAG = "SmtpEmailService"

        // Gmail SMTP via SSL directo (más robusto contra proxies corporativos)
        private const val SMTP_HOST = "smtp.gmail.com"
        private const val SMTP_PORT = "465"

        private val SMTP_USER     get() = com.ferji.inspecciones.BuildConfig.SMTP_USER
        private val SMTP_PASSWORD get() = com.ferji.inspecciones.BuildConfig.SMTP_PASSWORD
    }

    override suspend fun enviarConPdf(
        destinatarios: List<String>,
        cc: List<String>?,
        asunto: String,
        cuerpoHtml: String,
        pdfUri: Uri,
        nombreArchivoAdjunto: String
    ): EmailService.EmailResult = withContext(Dispatchers.IO) {
        try {
            // 1. Leer el PDF desde la URI
            val pdfBytes = leerUriComoBytes(pdfUri)
                ?: return@withContext EmailService.EmailResult.Error("No se pudo leer el PDF desde la URI: $pdfUri")

            // 2. Limpiar credenciales (Gmail genera app passwords con espacios, pero no los acepta)
            val smtpUser = SMTP_USER.trim()
            val smtpPassword = SMTP_PASSWORD.replace(" ", "").trim()


            // 3. Configurar propiedades SMTP con SSL directo (puerto 465)
            val props = Properties().apply {
                put("mail.smtp.auth", "true")
                put("mail.smtp.ssl.enable", "true")
                put("mail.smtp.host", SMTP_HOST)
                put("mail.smtp.port", SMTP_PORT)
                put("mail.smtp.socketFactory.port", SMTP_PORT)
                put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                put("mail.smtp.ssl.trust", SMTP_HOST)
                put("mail.smtp.connectiontimeout", "20000")
                put("mail.smtp.timeout", "20000")
            }

            // 4. Crear sesión con autenticación
            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication() =
                    PasswordAuthentication(smtpUser, smtpPassword)
            })

            // 5. Construir el mensaje
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(smtpUser, "Ferji Inspecciones"))
                setRecipients(
                    Message.RecipientType.TO,
                    destinatarios.map { InternetAddress(it) }.toTypedArray()
                )
                cc?.takeIf { it.isNotEmpty() }?.let {
                    setRecipients(
                        Message.RecipientType.CC,
                        it.map { addr -> InternetAddress(addr) }.toTypedArray()
                    )
                }
                subject = asunto
            }

            // 5. Cuerpo HTML
            val htmlPart = MimeBodyPart().apply {
                setContent(cuerpoHtml, "text/html; charset=utf-8")
            }

            // 6. Adjunto PDF
            val pdfPart = MimeBodyPart().apply {
                val dataSource: DataSource = ByteArrayDataSource(pdfBytes, "application/pdf")
                dataHandler = DataHandler(dataSource)
                fileName = nombreArchivoAdjunto
            }

            // 7. Ensamblar multipart
            val multipart: Multipart = MimeMultipart().apply {
                addBodyPart(htmlPart)
                addBodyPart(pdfPart)
            }
            message.setContent(multipart)

            // 8. Enviar
            Log.d(TAG, "Conectando a SMTP $SMTP_HOST:$SMTP_PORT para enviar a $destinatarios")
            Transport.send(message)
            Log.i(TAG, "Email enviado correctamente a $destinatarios")

            EmailService.EmailResult.Success

        } catch (e: Exception) {
            Log.e(TAG, "Error enviando email via SMTP: ${e.message}", e)
            EmailService.EmailResult.Error("Error al enviar email: ${e.message}", e)
        }
    }

    private fun leerUriComoBytes(uri: Uri): ByteArray? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            Log.e(TAG, "Error leyendo URI $uri: ${e.message}", e)
            null
        }
    }

    override suspend fun enviarConAdjuntos(
        destinatarios: List<String>,
        cc: List<String>?,
        asunto: String,
        cuerpoHtml: String,
        adjuntos: List<EmailService.Adjunto>
    ): EmailService.EmailResult = withContext(Dispatchers.IO) {
        try {
            val smtpUser = SMTP_USER.trim()
            val smtpPassword = SMTP_PASSWORD.replace(" ", "").trim()

            val props = Properties().apply {
                put("mail.smtp.auth", "true")
                put("mail.smtp.ssl.enable", "true")
                put("mail.smtp.host", SMTP_HOST)
                put("mail.smtp.port", SMTP_PORT)
                put("mail.smtp.socketFactory.port", SMTP_PORT)
                put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                put("mail.smtp.ssl.trust", SMTP_HOST)
                put("mail.smtp.connectiontimeout", "20000")
                put("mail.smtp.timeout", "20000")
            }

            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication() =
                    PasswordAuthentication(smtpUser, smtpPassword)
            })

            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(smtpUser, "Ferji Inspecciones"))
                setRecipients(
                    Message.RecipientType.TO,
                    destinatarios.map { InternetAddress(it) }.toTypedArray()
                )
                cc?.takeIf { it.isNotEmpty() }?.let {
                    setRecipients(
                        Message.RecipientType.CC,
                        it.map { addr -> InternetAddress(addr) }.toTypedArray()
                    )
                }
                subject = asunto
            }

            // Cuerpo HTML
            val htmlPart = MimeBodyPart().apply {
                setContent(cuerpoHtml, "text/html; charset=utf-8")
            }

            // Ensamblar multipart con todos los adjuntos
            val multipart: Multipart = MimeMultipart().apply {
                addBodyPart(htmlPart)

                for (adjunto in adjuntos) {
                    val bytes = leerUriComoBytes(adjunto.uri)
                    if (bytes != null) {
                        val part = MimeBodyPart().apply {
                            val dataSource: DataSource = ByteArrayDataSource(bytes, adjunto.mimeType)
                            dataHandler = DataHandler(dataSource)
                            fileName = adjunto.nombreArchivo
                        }
                        addBodyPart(part)
                        Log.d(TAG, "Adjunto añadido: ${adjunto.nombreArchivo} (${bytes.size} bytes)")
                    } else {
                        Log.w(TAG, "No se pudo leer adjunto: ${adjunto.nombreArchivo}")
                    }
                }
            }
            message.setContent(multipart)

            Log.d(TAG, "Conectando a SMTP $SMTP_HOST:$SMTP_PORT para enviar a $destinatarios con ${adjuntos.size} adjuntos")
            Transport.send(message)
            Log.i(TAG, "Email enviado correctamente a $destinatarios con ${adjuntos.size} adjuntos")

            EmailService.EmailResult.Success

        } catch (e: Exception) {
            Log.e(TAG, "Error enviando email con adjuntos via SMTP: ${e.message}", e)
            EmailService.EmailResult.Error("Error al enviar email: ${e.message}", e)
        }
    }
}

