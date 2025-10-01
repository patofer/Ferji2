package com.ferji.inspecciones.viewmodels


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// Asumo que tienes el DAO inyectado aquí. Si no, necesitarás añadirlo.
import com.ferji.inspecciones.data.dao.PartidaDao
import com.ferji.inspecciones.data.model.PartidaEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PartidaDetailViewModel @Inject constructor(
    private val partidaDao: PartidaDao // Asegúrate de que el DAO esté inyectado
) : ViewModel() {

    private val _partidasDeDetalle = MutableStateFlow<List<PartidaEntity>>(emptyList())
    val partidasDeDetalle = _partidasDeDetalle.asStateFlow()

    private var partidaPrincipalIdActual: Long? = null

    // Este método ya lo tienes y está bien
    fun loadPartidasDeDetalle(partidaPrincipalId: Long) {
        partidaPrincipalIdActual = partidaPrincipalId
        viewModelScope.launch {
            // Asumo que tu PartidaDao tiene este método
            partidaDao.getPartidasByPrincipalId(partidaPrincipalId).collect { lista ->
                _partidasDeDetalle.value = lista
            }
        }
    }

    // --- INICIO DE LA CORRECCIÓN: AÑADIR ESTAS FUNCIONES ---

    /**
     * Guarda (crea o actualiza) una partida de detalle en la base de datos.
     */
    fun guardarPartidaDetalle(id: Long?, descripcion: String, unidad: String, precio: Double) {
        val idPrincipal = partidaPrincipalIdActual ?: return // No hacer nada si no hay un ID principal

        viewModelScope.launch {
            val partida = PartidaEntity(
                id = id ?: 0L, // Si el ID es nulo, es una nueva partida (Room se encarga del autoincremento)
                descripcion = descripcion,
                unidad = unidad,
                precioUnitario = precio,
                partidaPrincipalId = idPrincipal // Asocia esta partida con la partida principal correcta
            )
            // Asumo que tu DAO tiene un método 'insert' o 'upsert'
            partidaDao.insertPartida(partida)
        }
    }

    /**
     * Elimina una partida de detalle de la base de datos.
     */
    fun eliminarPartidaDetalle(partida: PartidaEntity) {
        viewModelScope.launch {
            // Asumo que tu DAO tiene un método 'delete'
            partidaDao.deletePartida(partida)
        }
    }

    // --- FIN DE LA CORRECCIÓN ---
}
