package com.ferji.inspecciones.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ferji.inspecciones.domain.usecase.SyncInspeccionToFirebaseUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Worker que sincroniza inspecciones completadas a Firebase en segundo plano.
 * Se ejecuta cuando falla la sincronización inmediata al finalizar una inspección.
 */
@HiltWorker
class SyncInspeccionWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncUseCase: SyncInspeccionToFirebaseUseCase
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "SyncInspeccionWorker"
        const val WORK_NAME = "sync_inspecciones_firebase"
        const val KEY_INSPECCION_ID = "inspeccion_id"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Iniciando sincronización de inspecciones a Firebase...")

        return try {
            val inspeccionId = inputData.getLong(KEY_INSPECCION_ID, -1L)

            if (inspeccionId > 0) {
                val exito = syncUseCase(inspeccionId)
                if (exito) {
                    Log.i(TAG, "Inspección $inspeccionId sincronizada correctamente.")
                    Result.success()
                } else {
                    Log.w(TAG, "Falló sincronización de inspección $inspeccionId. Reintentando...")
                    Result.retry()
                }
            } else {
                val sincronizadas = syncUseCase.sincronizarPendientes()
                Log.i(TAG, "Sincronización masiva completada: $sincronizadas inspecciones subidas.")
                Result.success()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en worker de sincronización: ${e.message}", e)
            Result.retry()
        }
    }
}

