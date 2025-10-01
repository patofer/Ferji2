package com.ferji.inspecciones.viewmodels

import androidx.compose.foundation.text2.input.insert
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ferji.inspecciones.data.dao.PartidaPrincipalDao
import com.ferji.inspecciones.data.model.PartidaNaturaleza
import com.ferji.inspecciones.data.model.PartidaPrincipalEntity
import com.ferji.inspecciones.data.model.TipoSuperficie
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PartidaPrincipalViewModel @Inject constructor(
    private val dao: PartidaPrincipalDao
) : ViewModel() {

    // Expone la lista de partidas principales para que la UI la observe.
    val partidasPrincipales = dao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Función para crear o actualizar (Upsert).
    fun guardarPartida(id: Long?, nombre: String, tipoSuperficie: TipoSuperficie, naturaleza: PartidaNaturaleza) {
        viewModelScope.launch {
            if (nombre.isNotBlank()) {
                val partida = PartidaPrincipalEntity(
                    id = id ?: 0,
                    nombre = nombre,
                    tipoSuperficie = tipoSuperficie.name,
                    naturaleza = naturaleza // <-- Guarda la nueva propiedad
                )

                    dao.upsert(partida)

            }
        }
    }

    // Función para eliminar.
    fun eliminarPartida(partida: PartidaPrincipalEntity) {
        viewModelScope.launch {
            dao.delete(partida)
        }
    }
}
