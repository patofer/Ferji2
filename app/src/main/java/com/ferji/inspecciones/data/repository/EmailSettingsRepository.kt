package com.ferji.inspecciones.data.repository

import android.util.Log
import com.ferji.inspecciones.data.model.EmailSettingsEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositorio para gestionar la configuración de emails en Firestore.
 * Colección: configuraciones / Documento: email_settings
 */
@Singleton
class EmailSettingsRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "EmailSettingsRepo"
        private const val COLLECTION = "configuraciones"
        private const val DOCUMENT = "email_settings"
    }

    /**
     * Obtiene la configuración actual de emails desde Firestore.
     * Si no existe, devuelve una configuración por defecto.
     */
    suspend fun obtenerConfiguracion(): EmailSettingsEntity {
        return try {
            val doc = firestore.collection(COLLECTION).document(DOCUMENT).get().await()
            if (doc.exists()) {
                val settings = EmailSettingsEntity.fromMap(doc.data ?: emptyMap())
                Log.d(TAG, "Configuración cargada: $settings")
                settings
            } else {
                Log.d(TAG, "No existe configuración en Firestore, usando valores por defecto.")
                EmailSettingsEntity()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener configuración: ${e.message}", e)
            EmailSettingsEntity()
        }
    }

    /**
     * Guarda/actualiza la configuración de emails en Firestore.
     */
    suspend fun actualizarConfiguracion(settings: EmailSettingsEntity): Boolean {
        return try {
            firestore.collection(COLLECTION).document(DOCUMENT)
                .set(settings.toMap())
                .await()
            Log.d(TAG, "Configuración guardada correctamente: $settings")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error al guardar configuración: ${e.message}", e)
            false
        }
    }

    /**
     * Crea el documento en Firestore con valores por defecto si no existe.
     * Llamar una sola vez al iniciar la app (ej: después del login).
     */
    suspend fun inicializarSiNoExiste() {
        try {
            val doc = firestore.collection(COLLECTION).document(DOCUMENT).get().await()
            if (!doc.exists()) {
                val defaults = EmailSettingsEntity(
                    emailAdmin = "patriciofernande@gmail.com",
                    emailCc = "",
                    enviarInspeccionAlInspector = true,
                    enviarPresupuestoAlInspector = false
                )
                firestore.collection(COLLECTION).document(DOCUMENT)
                    .set(defaults.toMap())
                    .await()
                Log.d(TAG, "Documento email_settings creado en Firestore con valores por defecto.")
            } else {
                Log.d(TAG, "Documento email_settings ya existe en Firestore.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al inicializar configuración: ${e.message}", e)
        }
    }
}

