package com.ferji.inspecciones.data.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ferji.inspecciones.data.repository.PartidaRepository
// --- INICIO DE LA CORRECCIÓN ---
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: PartidaRepository // El repositorio es inyectado por Hilt
) : CoroutineWorker(appContext, workerParams) {
// --- FIN DE LA CORRECCIÓN ---

    override suspend fun doWork(): Result {
        return try {
            Log.d(WORK_NAME, "Iniciando trabajo de subida de datos a Firebase.")
            repository.subirTodosLosCambios() // Llama a la función del repositorio
            Log.d(WORK_NAME, "Trabajo de subida finalizado con éxito.")
            Result.success()
        } catch (e: Exception) {
            Log.e(WORK_NAME, "El trabajo de subida falló. Se reintentará.", e)
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "SyncUploadWork"
    }
}
