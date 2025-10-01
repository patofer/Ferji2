// En: com/ferji/inspecciones/viewmodels/PartidaViewModel.kt
package com.ferji.inspecciones.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ferji.inspecciones.data.model.DanoPartidaCrossRef
import com.ferji.inspecciones.data.model.PartidaEntity
import com.ferji.inspecciones.data.model.UnidadMedida
import com.ferji.inspecciones.data.repository.PartidaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PartidaViewModel @Inject constructor(private val repository: PartidaRepository) : ViewModel() {

    // ... (resto de tus propiedades sin cambios) ...

    val _partidasMaestro = MutableStateFlow<List<PartidaEntity>>(emptyList())
    val partidasMaestro: StateFlow<List<PartidaEntity>> = _partidasMaestro.asStateFlow()

    val _partidasAsociadas = MutableStateFlow<List<PartidaEntity>>(emptyList())
    val partidasAsociadas: StateFlow<List<PartidaEntity>> = _partidasAsociadas.asStateFlow()

    private var claveDanoActual: String? = null

    init {
        viewModelScope.launch {
            repository.getAllPartidas().collect {
                _partidasMaestro.value = it
            }
        }
    }

    fun loadPartidasParaDano(claveDano: String) {
        this.claveDanoActual = claveDano
        viewModelScope.launch {
            repository.getPartidasForDano(claveDano).collect {
                _partidasAsociadas.value = it
            }
        }
    }

    fun crearOActualizarPartida(
        id: Long?,
        descripcion: String,
        unidad: UnidadMedida,
        precio: Double,
        partidaPrincipalId: Long?
    ) {
        if (partidaPrincipalId == null || descripcion.isBlank()) {
            return
        }

        viewModelScope.launch {
            // --- INICIO DE LA CORRECCIÓN ---
            // Se convierte el enum 'unidad' a su representación en String usando '.name'
            val partida = PartidaEntity(
                id = id ?: 0,
                descripcion = descripcion,
                unidad = unidad.name, // ¡Corrección aplicada aquí!
                precioUnitario = precio,
                partidaPrincipalId = partidaPrincipalId
            )
            // --- FIN DE LA CORRECCIÓN ---
            repository.upsertPartida(partida)
        }
    }

    fun eliminarPartida(partida: PartidaEntity) {
        viewModelScope.launch {
            repository.deletePartida(partida)
        }
    }

    fun asociarPartidaADano(partidaId: Long) {
        val claveDano = claveDanoActual ?: return
        viewModelScope.launch {
            repository.addDanoPartidaCrossRef(
                DanoPartidaCrossRef(claveDano = claveDano, partidaId = partidaId)
            )
        }
    }

    fun desasociarPartidaDeDano(partidaId: Long) {
        val claveDano = claveDanoActual ?: return
        viewModelScope.launch {
            repository.removeDanoPartidaCrossRef(
                DanoPartidaCrossRef(claveDano = claveDano, partidaId = partidaId)
            )
        }
    }
}
