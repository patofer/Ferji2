package com.ferji.inspecciones.data.model

/**
 * Modelo de datos para la configuración de emails.
 * Se persiste en Firestore: configuraciones/email_settings
 */
data class EmailSettingsEntity(
    val emailAdmin: String = "",
    val emailCc: String = "",
    val enviarInspeccionAlInspector: Boolean = false,
    val enviarPresupuestoAlInspector: Boolean = false
) {
    /** Convierte a Map para guardar en Firestore */
    fun toMap(): Map<String, Any> = mapOf(
        "emailAdmin" to emailAdmin,
        "emailCc" to emailCc,
        "enviarInspeccionAlInspector" to enviarInspeccionAlInspector,
        "enviarPresupuestoAlInspector" to enviarPresupuestoAlInspector
    )

    companion object {
        /** Crea una instancia desde un Map de Firestore */
        fun fromMap(map: Map<String, Any?>): EmailSettingsEntity {
            return EmailSettingsEntity(
                emailAdmin = map["emailAdmin"] as? String ?: "",
                emailCc = map["emailCc"] as? String ?: "",
                enviarInspeccionAlInspector = map["enviarInspeccionAlInspector"] as? Boolean ?: false,
                enviarPresupuestoAlInspector = map["enviarPresupuestoAlInspector"] as? Boolean ?: false
            )
        }
    }
}

