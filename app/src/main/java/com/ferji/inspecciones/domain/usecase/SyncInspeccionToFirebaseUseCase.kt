package com.ferji.inspecciones.domain.usecase

import android.util.Log
import com.ferji.inspecciones.data.model.HabitacionEntity
import com.ferji.inspecciones.data.model.InspeccionEntity
import com.ferji.inspecciones.data.remote.InspeccionRemoteDataSource
import com.ferji.inspecciones.data.repository.HabitacionRepository
import com.ferji.inspecciones.data.repository.InspeccionRepository
import com.google.firebase.Timestamp
import java.util.Date
import javax.inject.Inject

/**
 * Caso de uso: Sincronizar una inspección completada a Firebase Firestore.
 */
class SyncInspeccionToFirebaseUseCase @Inject constructor(
    private val inspeccionRepository: InspeccionRepository,
    private val habitacionRepository: HabitacionRepository,
    private val remoteDataSource: InspeccionRemoteDataSource
) {
    companion object {
        private const val TAG = "SyncInspeccionUC"
    }

    suspend operator fun invoke(inspeccionId: Long): Boolean {
        val inspeccion = inspeccionRepository.getInspeccionById(inspeccionId)
        if (inspeccion == null) {
            Log.e(TAG, "Inspección $inspeccionId no encontrada.")
            return false
        }

        val habitaciones = habitacionRepository.getHabitacionesPorInspeccionId(inspeccionId)
        val datosInspeccion = inspeccionToFirestoreMap(inspeccion, habitaciones.size)
        val datosHabitaciones = habitaciones.map { habitacionToFirestoreMap(it) }

        // Si ya tiene firebaseId → actualizar documento existente
        if (inspeccion.firebaseId.isNotBlank()) {
            val actualizado = remoteDataSource.actualizarInspeccionCompleta(
                inspeccion.firebaseId, datosInspeccion, datosHabitaciones
            )
            if (actualizado) {
                Log.i(TAG, "Inspección $inspeccionId actualizada en Firebase: ${inspeccion.firebaseId}")
                return true
            } else {
                Log.e(TAG, "Falló la actualización de inspección $inspeccionId en Firebase")
                return false
            }
        }

        // Si no tiene firebaseId → crear documento nuevo
        val firebaseId = remoteDataSource.subirInspeccionCompleta(datosInspeccion, datosHabitaciones)

        return if (firebaseId != null) {
            inspeccionRepository.marcarComoSincronizada(inspeccionId, firebaseId)
            Log.i(TAG, "Inspección $inspeccionId sincronizada a Firebase: $firebaseId")
            true
        } else {
            Log.e(TAG, "Falló la sincronización de inspección $inspeccionId")
            false
        }
    }

    suspend fun sincronizarPendientes(): Int {
        val pendientes = inspeccionRepository.getInspeccionesNoSincronizadas()
        Log.d(TAG, "Inspecciones pendientes de sincronizar: ${pendientes.size}")
        var sincronizadas = 0
        for (inspeccion in pendientes) {
            if (invoke(inspeccion.id)) sincronizadas++
        }
        Log.i(TAG, "Sincronización completada: $sincronizadas/${pendientes.size}")
        return sincronizadas
    }

    private fun inspeccionToFirestoreMap(
        inspeccion: InspeccionEntity,
        totalHabitaciones: Int
    ): Map<String, Any?> = mapOf(
        "siniestro" to inspeccion.siniestro,
        "rut" to inspeccion.rut,
        "direccion" to inspeccion.direccion,
        "rutInspector" to inspeccion.rutInspector,
        "mailInspector" to inspeccion.mail,
        "estado" to inspeccion.estado,
        "fechaCreacion" to Timestamp(inspeccion.fechaCreacion),
        "fechaFinalizacion" to Timestamp(Date()),
        "totalHabitaciones" to totalHabitaciones,
        "totalPresupuesto" to inspeccion.totalPresupuesto,
        "idLocal" to inspeccion.id
    )

    private fun habitacionToFirestoreMap(
        habitacion: HabitacionEntity
    ): Map<String, Any?> = mapOf(
        "nombre" to habitacion.nombre,
        "largo" to habitacion.largo,
        "ancho" to habitacion.ancho,
        "alto" to habitacion.alto,
        "danos" to habitacion.getDanosList(),
        "comentarios" to habitacion.comentarios,
        "cantidadFotos" to habitacion.getFotosList().size,
        "fechaCreacion" to Timestamp(Date(habitacion.fechaCreacion))
    )
}

