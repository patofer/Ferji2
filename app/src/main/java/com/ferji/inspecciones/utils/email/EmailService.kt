package com.ferji.inspecciones.utils.email

import android.net.Uri

/**
 * Contrato para el servicio de envío de emails.
 *
 * Permite intercambiar la implementación (SMTP/JavaMail, SendGrid, etc.)
 * sin modificar el código que lo consume (ViewModels, etc.).
 *
 * Para cambiar de proveedor, solo modifica el binding en [EmailModule].
 */
interface EmailService {

    sealed class EmailResult {
        object Success : EmailResult()
        data class Error(val message: String, val cause: Throwable? = null) : EmailResult()
    }

    /** Representa un archivo adjunto para el email */
    data class Adjunto(
        val uri: Uri,
        val nombreArchivo: String,
        val mimeType: String  // "application/pdf", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", etc.
    )

    /**
     * Envía un email con un PDF adjunto de forma automática y silenciosa.
     * Debe ejecutarse desde una corrutina.
     *
     * @param destinatarios Lista de emails principales.
     * @param cc Lista de emails en copia (opcional).
     * @param asunto Asunto del email.
     * @param cuerpoHtml Cuerpo del email en formato HTML.
     * @param pdfUri URI del PDF a adjuntar (content:// o file://).
     * @param nombreArchivoAdjunto Nombre que tendrá el archivo en el email.
     */
    suspend fun enviarConPdf(
        destinatarios: List<String>,
        cc: List<String>? = null,
        asunto: String,
        cuerpoHtml: String,
        pdfUri: Uri,
        nombreArchivoAdjunto: String
    ): EmailResult

    /**
     * Envía un email con múltiples adjuntos (PDF, Excel, etc.).
     * Debe ejecutarse desde una corrutina.
     *
     * @param destinatarios Lista de emails principales.
     * @param cc Lista de emails en copia (opcional).
     * @param asunto Asunto del email.
     * @param cuerpoHtml Cuerpo del email en formato HTML.
     * @param adjuntos Lista de archivos adjuntos.
     */
    suspend fun enviarConAdjuntos(
        destinatarios: List<String>,
        cc: List<String>? = null,
        asunto: String,
        cuerpoHtml: String,
        adjuntos: List<Adjunto>
    ): EmailResult
}
