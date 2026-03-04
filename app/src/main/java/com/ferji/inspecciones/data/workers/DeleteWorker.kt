package com.ferji.inspecciones.data.workers

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.size
import androidx.compose.ui.geometry.isEmpty
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ferji.inspecciones.data.dao.PartidaDao
import com.ferji.inspecciones.data.dao.PartidaPrincipalDao
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await

@HiltWorker
class DeleteWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val partidaDao: PartidaDao,
    private val partidaPrincipalDao: PartidaPrincipalDao
) : CoroutineWorker(appContext, workerParams) {

    private val partidaPrincipalCollection = Firebase.firestore.collection("partidas_principales")

    override suspend fun doWork(): Result {
        return try {
            Log.d("DeleteWorker", "Trabajo de eliminación iniciado.")

            val partidasParaEliminar = partidaDao.getEliminadas()
            if (partidasParaEliminar.isEmpty()) {
                Log.d("DeleteWorker", "No hay partidas marcadas para eliminar.")
                return Result.success()
            }

            Log.d("DeleteWorker", "Eliminando ${partidasParaEliminar.size} partidas de Firebase...")

            for (partida in partidasParaEliminar) {
                // Se necesita el firebaseId del padre para encontrar la subcolección correcta
                val padre = partidaPrincipalDao.getById(partida.partidaPrincipalId)
                if (padre == null || padre.firebaseId.isBlank() || partida.firebaseId.isBlank()) {
                    Log.w("DeleteWorker", "Saltando eliminación de partida local ${partida.id} porque falta información del padre o de firebase.")
                    // Si no hay info, no se puede borrar de Firebase, así que la borramos solo localmente para limpiar.
                    partidaDao.deletePartida(partida)
                    continue
                }

                // Borramos de Firebase
                partidaPrincipalCollection
                    .document(padre.firebaseId)
                    .collection("partidas")
                    .document(partida.firebaseId)
                    .delete()
                    .await()

                // Una vez borrado con éxito de Firebase, lo borramos de la base de datos local
                partidaDao.deletePartida(partida)
                Log.d("DeleteWorker", "Partida ${partida.firebaseId} eliminada con éxito de Firebase y Room.")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("DeleteWorker", "El trabajo de eliminación falló. Se reintentará.", e)
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "CleanupDeletedWork"
    }
}
