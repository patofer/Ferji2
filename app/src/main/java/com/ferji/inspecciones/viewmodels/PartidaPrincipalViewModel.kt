package com.ferji.inspecciones.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.ferji.inspecciones.data.model.PartidaNaturaleza
import com.ferji.inspecciones.data.model.PartidaPrincipalEntity
import com.ferji.inspecciones.data.repository.PartidaRepository
import com.ferji.inspecciones.data.workers.SyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PartidaPrincipalViewModel @Inject constructor(
    private val repository: PartidaRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    /**
     * Propiedad PÚBLICA que expone la lista de todas las partidas principales.
     * La UI se suscribe a este StateFlow para recibir actualizaciones en tiempo real.
     */
    val partidasPrincipales: StateFlow<List<PartidaPrincipalEntity>> = repository.getAllPartidasPrincipales()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Guarda una partida principal (nueva o existente) en la base de datos local
     * y programa un trabajo de subida si es una entidad nueva.
     */
    fun guardarPartida(id: Long?, nombre: String, tipoSuperficie: String, naturaleza: PartidaNaturaleza) {
        viewModelScope.launch(Dispatchers.IO) {
            if (nombre.isNotBlank()) {
                val esNuevo = id == null
                val partida = PartidaPrincipalEntity(
                    id = id ?: 0,
                    nombre = nombre,
                    tipoSuperficie = tipoSuperficie,
                    naturaleza = naturaleza,
                    sincronizadoConFirebase = !esNuevo // Se marca para subir si es nuevo
                )
                repository.upsertPartidaPrincipal(partida)
                Log.d("PartidaPrincipalVM", "Partida principal guardada localmente: $partida")

                // Si es un elemento nuevo, se programa la subida.
                if (esNuevo) {
                    programarTrabajoDeSubida()
                }
            }
        }
    }

    /**
     * Elimina una Partida Principal de la base de datos local.
     * TODO: Mejorar con lógica de "marcado para eliminar" y un Worker de borrado en Firebase,
     * similar a como se hizo en PartidaViewModel.
     */
    fun eliminarPartida(partida: PartidaPrincipalEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d("PartidaPrincipalVM", "Eliminando partida principal: ${partida.id}")
            repository.deletePartidaPrincipal(partida)
            // Aquí se debería llamar a un futuro Worker que borre de Firebase.
        }
    }

    /**
     * Configura y pone en cola un trabajo único con WorkManager para subir los cambios locales a Firebase.
     */
    private fun programarTrabajoDeSubida() {
        Log.d("PartidaPrincipalVM", "Programando trabajo de subida con WorkManager.")
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val uploadWorkRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(appContext).enqueueUniqueWork(
            SyncWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            uploadWorkRequest
        )
    }

    fun sincronizarDatos() {
        viewModelScope.launch {
            try {
                // Ejecutamos la operación de red y base de datos en un hilo de fondo.
                withContext(Dispatchers.IO) {
                    Log.d("PartidaPrincipalVM", "Iniciando sincronización de catálogo de partidas...")
                    repository.sincronizarCatalogoCompleto()
                }
            } catch (e: Exception) {
                Log.e("PartidaPrincipalVM", "Fallo la sincronización de bajada", e)
                // Aquí podrías usar un SharedFlow para notificar a la UI de un error de conexión.
            }
        }
    }
}
