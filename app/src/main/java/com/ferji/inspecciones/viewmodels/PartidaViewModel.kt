// En: com/ferji/inspecciones/viewmodels/PartidaViewModel.kt
package com.ferji.inspecciones.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.ferji.inspecciones.data.model.PartidaEntity
import com.ferji.inspecciones.data.model.UnidadMedida
import com.ferji.inspecciones.data.repository.PartidaRepository
import com.ferji.inspecciones.data.workers.DeleteWorker
import com.ferji.inspecciones.data.workers.SyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PartidaViewModel @Inject constructor(
    private val repository: PartidaRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _idPartidaPrincipal = MutableStateFlow(0L)

    /**
     * Un Flow que emite la lista de partidas hijas para la partida principal actualmente seleccionada.
     * Se actualiza automáticamente cada vez que la base de datos cambia y filtra los elementos
     * marcados para eliminación para que no se muestren en la UI.
     */
    val partidasDePrincipal: StateFlow<List<PartidaEntity>> = _idPartidaPrincipal
        .flatMapLatest { idPadre ->
            if (idPadre > 0) {
                repository.getPartidasDePrincipal(idPadre)
                    .map { list -> list.filter { !it.eliminado } } // Filtra los eliminados
            } else {
                flowOf(emptyList<PartidaEntity>())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Función PÚBLICA para que la UI le diga al ViewModel qué partida principal observar.
     * Esto actualiza el valor del StateFlow privado, lo que reactiva el Flow 'partidasDePrincipal'.
     */
    fun cargarPartidasDe(idPadre: Long) {
        _idPartidaPrincipal.value = idPadre
    }

    /**
     * Crea una nueva partida hija localmente y programa un trabajo con WorkManager para subirla.
     */
    fun crearOActualizarPartida(
        id: Long?,
        descripcion: String,
        unidad: UnidadMedida,
        precio: Double,
        partidaPrincipalId: Long?
    ) {
        if (partidaPrincipalId == null || descripcion.isBlank()) {
            Log.e("PartidaVM", "Faltan datos para crear/actualizar la partida.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val esNuevo = (id == null || id == 0L)

            val partida = if (esNuevo) {
                // SI ES NUEVO: Room autogenera el ID.
                // Generamos un firebaseId temporal único para evitar conflictos con el
                // índice UNIQUE sobre firebaseId.
                PartidaEntity(
                    descripcion = descripcion,
                    unidad = unidad.name,
                    precioUnitario = precio,
                    partidaPrincipalId = partidaPrincipalId,
                    firebaseId = "local_${java.util.UUID.randomUUID()}",
                    sincronizadoConFirebase = false,
                    eliminado = false
                )
            } else {
                // SI ES UNA EDICIÓN: Recuperar la partida existente para conservar su firebaseId.
                // Sin esto, el firebaseId queda vacío ("") y causa:
                // SQLiteConstraintException: UNIQUE constraint failed: partidas.firebaseId
                val existente = repository.getPartidaById(id!!)
                PartidaEntity(
                    id = id,
                    descripcion = descripcion,
                    unidad = unidad.name,
                    precioUnitario = precio,
                    partidaPrincipalId = partidaPrincipalId,
                    firebaseId = existente?.firebaseId ?: "local_${java.util.UUID.randomUUID()}",
                    sincronizadoConFirebase = false, // Marcamos como no sincronizado para re-subir
                    eliminado = false
                )
            }

            repository.upsertPartida(partida)
            Log.d("PartidaVM", "Partida guardada localmente: $partida")

            // Programar subida tanto si es nueva (crear en Firebase) como si es edición (actualizar en Firebase).
            programarTrabajoDeSubida()
        }
    }


    /**
     * Marca una partida para ser eliminada y programa un trabajo con WorkManager para que la borre
     * de Firebase y de la base de datos local.
     */
    fun eliminarPartida(partida: PartidaEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Marcar la entidad como 'eliminado = true' para que desaparezca de la UI al instante.
            repository.upsertPartida(partida.copy(eliminado = true))
            Log.d("PartidaVM", "Marcando para eliminación la partida: ${partida.id}")

            // 2. Programar el trabajo de eliminación en segundo plano.
            programarTrabajoDeEliminacion()
        }
    }

    /**
     * Configura y pone en cola un trabajo único para subir los cambios a Firebase.
     */
    private fun programarTrabajoDeSubida() {
        Log.d("PartidaVM", "Programando trabajo de subida con WorkManager.")
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val uploadWorkRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(appContext).enqueueUniqueWork(
            SyncWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP, // Si ya hay un trabajo de subida, no crea otro
            uploadWorkRequest
        )
    }

    /**
     * Configura y pone en cola un trabajo único para limpiar los registros eliminados.
     */
    private fun programarTrabajoDeEliminacion() {
        Log.d("PartidaVM", "Programando trabajo de eliminación con WorkManager.")
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val deleteWorkRequest = OneTimeWorkRequestBuilder<DeleteWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(appContext).enqueueUniqueWork(
            DeleteWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP, // Si ya hay un trabajo de limpieza, no crea otro
            deleteWorkRequest
        )
    }
}
