package com.ferji.inspecciones.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
// No necesitas FileProvider aquí si la URI ya es una content:// URI (de MediaStore o de FileProvider)

object EmailSender {

    fun sendEmailWithAttachment(
        context: Context,
        recipients: Array<String>,
        ccRecipients: Array<String>?,
        bccRecipients: Array<String>?,
        subject: String,
        body: String,
        attachmentUri: Uri,
        chooserTitle: String = "Enviar inspección vía email..."
    ) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            // Establece el tipo MIME para indicar que es un email.
            // "message/rfc822" es estándar para emails.
            // Si quieres que el selector muestre más opciones (ej. apps que manejan PDFs),
            // podrías usar el tipo MIME del adjunto ("application/pdf"), pero
            // "message/rfc822" es más específico para clientes de email.
            type = "message/rfc822"
            // También puedes usar ACTION_SEND_MULTIPLE si tienes varios adjuntos

            putExtra(Intent.EXTRA_EMAIL, recipients) // Destinatarios principales
            ccRecipients?.let { if (it.isNotEmpty()) putExtra(Intent.EXTRA_CC, it) } // CC
            bccRecipients?.let { if (it.isNotEmpty()) putExtra(Intent.EXTRA_BCC, it) } // CCO / BCC
            putExtra(Intent.EXTRA_SUBJECT, subject) // Asunto
            putExtra(Intent.EXTRA_TEXT, body) // Cuerpo del email (texto plano)
            // Para HTML, usa Html.fromHtml() y EXTRA_HTML_TEXT

            // Adjuntar el archivo
            putExtra(Intent.EXTRA_STREAM, attachmentUri)

            // Otorgar permiso temporal a la app de email para leer la URI del adjunto.
            // Esto es crucial, especialmente para content:// URIs de FileProvider o MediaStore.
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            // Iniciar la actividad con un selector para que el usuario elija la app de email.
            context.startActivity(Intent.createChooser(intent, chooserTitle))
        } catch (ex: android.content.ActivityNotFoundException) {
            Toast.makeText(
                context,
                "No se encontraron aplicaciones de email instaladas.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
