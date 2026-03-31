package com.ferji.inspecciones.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fuente de datos remota para inspecciones en Firestore.
 *
 * Estructura en Firestore (pensada para informes y dashboards):
 *
 * inspecciones/
 *   {firebaseId}/
 *     ├─ siniestro: "W123456"
 *     ├─ rut: "12345678-9"
 *     ├─ direccion: "Calle 1 Norte 123, Talca"
 *     ├─ rutInspector: "15540294-6"
 *     ├─ mailInspector: "inspector@mail.com"
 *     ├─ estado: "COMPLETADA"
 *     ├─ fechaCreacion: Timestamp
 *     ├─ fechaFinalizacion: Timestamp
 *     ├─ totalHabitaciones: 3
 *     ├─ habitaciones/ (subcolección)
 *     │     {habId}/
 *     │       ├─ nombre: "Cocina"
 *     │       ├─ largo: 500
 *     │       ├─ ancho: 400
 *     │       ├─ alto: 280
 *     │       ├─ danos: ["Fisura Cielo", "Cerámica piso"]
 *     │       ├─ comentarios: "Daño severo"
 *     │       ├─ cantidadFotos: 3
 *     │       └─ fechaCreacion: Timestamp
 *
 * Esta estructura permite:
 * - Consultar inspecciones por estado, inspector, fecha, siniestro
 * - Generar informes de productividad por inspector
 * - Dashboard de inspecciones pendientes vs completadas
 * - Estadísticas de tipos de daño más frecuentes
 */
@Singleton
class InspeccionRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "InspeccionRemote"
        private const val COLLECTION_INSPECCIONES = "inspecciones"
        private const val SUBCOLLECTION_HABITACIONES = "habitaciones"
    }

    /**
     * Sube una inspección completa (datos + habitaciones) a Firestore.
     * Usa batch write para garantizar atomicidad.
     *
     * @return El firebaseId del documento creado, o null si falla.
     */
    suspend fun subirInspeccionCompleta(
        datosInspeccion: Map<String, Any?>,
        habitaciones: List<Map<String, Any?>>
    ): String? {
        return try {
            val batch = firestore.batch()

            // 1. Crear documento de la inspección
            val inspeccionRef = firestore.collection(COLLECTION_INSPECCIONES).document()
            val firebaseId = inspeccionRef.id
            batch.set(inspeccionRef, datosInspeccion)

            // 2. Crear documentos de habitaciones como subcolección
            for (habitacion in habitaciones) {
                val habRef = inspeccionRef.collection(SUBCOLLECTION_HABITACIONES).document()
                batch.set(habRef, habitacion)
            }

            // 3. Ejecutar todo en una sola transacción
            batch.commit().await()

            Log.i(TAG, "Inspección subida a Firebase: $firebaseId con ${habitaciones.size} habitaciones")
            firebaseId

        } catch (e: Exception) {
            Log.e(TAG, "Error subiendo inspección a Firebase: ${e.message}", e)
            null
        }
    }

    /**
     * Actualiza el estado de una inspección en Firestore.
     */
    suspend fun actualizarEstado(firebaseId: String, nuevoEstado: String): Boolean {
        return try {
            firestore.collection(COLLECTION_INSPECCIONES)
                .document(firebaseId)
                .update("estado", nuevoEstado)
                .await()
            Log.d(TAG, "Estado actualizado en Firebase: $firebaseId -> $nuevoEstado")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando estado en Firebase: ${e.message}", e)
            false
        }
    }

    /**
     * Actualiza una inspección existente y reemplaza sus habitaciones.
     * Se usa cuando una inspección PENDIENTE pasa a COMPLETADA.
     */
    suspend fun actualizarInspeccionCompleta(
        firebaseId: String,
        datosInspeccion: Map<String, Any?>,
        habitaciones: List<Map<String, Any?>>
    ): Boolean {
        return try {
            val batch = firestore.batch()
            val inspeccionRef = firestore.collection(COLLECTION_INSPECCIONES).document(firebaseId)

            // 1. Actualizar datos de la inspección
            batch.set(inspeccionRef, datosInspeccion)

            // 2. Eliminar habitaciones anteriores
            val habsAnteriores = inspeccionRef.collection(SUBCOLLECTION_HABITACIONES).get().await()
            for (doc in habsAnteriores.documents) {
                batch.delete(doc.reference)
            }

            // 3. Crear habitaciones nuevas (con datos actualizados)
            for (habitacion in habitaciones) {
                val habRef = inspeccionRef.collection(SUBCOLLECTION_HABITACIONES).document()
                batch.set(habRef, habitacion)
            }

            batch.commit().await()
            Log.i(TAG, "Inspección actualizada en Firebase: $firebaseId con ${habitaciones.size} habitaciones")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando inspección en Firebase: ${e.message}", e)
            false
        }
    }

    /**
     * Verifica si una inspección ya existe en Firestore por número de siniestro.
     * Útil para evitar duplicados.
     */
    suspend fun existeInspeccionPorSiniestro(siniestro: String): Boolean {
        return try {
            val query = firestore.collection(COLLECTION_INSPECCIONES)
                .whereEqualTo("siniestro", siniestro)
                .limit(1)
                .get()
                .await()
            !query.isEmpty
        } catch (e: Exception) {
            Log.e(TAG, "Error verificando existencia en Firebase: ${e.message}", e)
            false
        }
    }
}

