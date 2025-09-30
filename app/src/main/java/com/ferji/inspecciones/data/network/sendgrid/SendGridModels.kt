package com.ferji.inspecciones.data.network.sendgrid

import com.google.gson.annotations.SerializedName // Si usas Gson

// Si usas Moshi, sería com.squareup.moshi.Json

data class SendGridMail(
    val personalizations: List<Personalization>,
    val from: EmailAddress,
    val subject: String,
    val content: List<Content>,
    val attachments: List<Attachment>? = null // Hacerlo opcional por si no hay adjuntos
)

data class Personalization(
    val to: List<EmailAddress>,
    val cc: List<EmailAddress>? = null, // Opcional
    val bcc: List<EmailAddress>? = null, // Opcional
    val subject: String? = null // El subject también puede ir aquí, pero es común tenerlo a nivel raíz del SendGridMail
)

data class EmailAddress(
    val email: String,
    val name: String? = null // El nombre es opcional
)

data class Content(
    val type: String, // e.g., "text/plain", "text/html"
    val value: String
)

data class Attachment(
    val content: String, // Base64 encoded content
    val filename: String,
    val type: String, // e.g., "application/pdf"
    val disposition: String // e.g., "attachment"
)