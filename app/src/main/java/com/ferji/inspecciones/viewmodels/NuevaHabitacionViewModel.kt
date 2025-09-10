package com.ferji.inspecciones.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ferji.inspecciones.data.model.HabitacionEntity
import com.ferji.inspecciones.data.repository.HabitacionRepository
import com.ferji.inspecciones.utils.GsonUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class NuevaHabitacionViewModel @Inject constructor(
    private val habitacionRepository: HabitacionRepository,
    // ✅ No es necesario inspeccionRepository si no se usa
) : ViewModel() {

    companion object {
        private const val TAG = "DEBUG_HABITACION"
    }

    private val _state = MutableStateFlow(NuevaHabitacionState())
    val state = _state.asStateFlow()

    private val _guardadoState = MutableStateFlow<GuardadoState>(GuardadoState.Idle)
    val guardadoState = _guardadoState.asStateFlow()

    fun init(inspeccionId: Long) {
        if (_state.value.inspeccionId == -1L && inspeccionId != -1L) {
            _state.update { it.copy(inspeccionId = inspeccionId) }
        }
    }

    fun onNombreChange(nombre: String) {
        _state.update { it.copy(nombreHabitacion = nombre) }
    }

    fun onAltoChange(alto: Int) {
        _state.update { it.copy(alto = alto) }
    }

    fun onLargoChange(largo: Int) {
        _state.update { it.copy(largo = largo) }
    }

    fun onAnchoChange(ancho: Int) {
        _state.update { it.copy(ancho = ancho) }
    }

    fun onComentariosChange(comentarios: String) {
        _state.update { it.copy(comentarios = comentarios) }
    }

    // ✅ Nueva función para manejar la selección de un solo daño desde el ComboBox
    fun onDanoSeleccionado(dano: String) {
        _state.update { it.copy(danoSeleccionado = dano) }
    }

    fun agregarFoto(rutaFoto: String) {
        val nuevasFotos = _state.value.fotosTomadas + rutaFoto
        _state.update { it.copy(fotosTomadas = nuevasFotos) }
    }

    fun eliminarFoto(rutaFoto: String) {
        val nuevasFotos = _state.value.fotosTomadas - rutaFoto
        _state.update { it.copy(fotosTomadas = nuevasFotos) }
    }

    fun guardarHabitacionConEstado() {
        viewModelScope.launch {
            _guardadoState.value = GuardadoState.Cargando
            try {
                val habitacion = withContext(Dispatchers.IO) {
                    val habitacionEntity = HabitacionEntity(
                        inspeccionId = state.value.inspeccionId,
                        nombre = state.value.nombreHabitacion,
                        alto = state.value.alto,
                        largo = state.value.largo,
                        ancho = state.value.ancho,
                        // ✅ Usa el nuevo campo 'danoSeleccionado'
                        tipoDano = state.value.danoSeleccionado,
                        comentarios = state.value.comentarios,
                        fotos = GsonUtils.listToJson(state.value.fotosTomadas),
                        danos = GsonUtils.listToJson(listOf(state.value.danoSeleccionado))
                    )
                    Log.d(TAG, "Guardando habitación: $habitacionEntity")
                    habitacionRepository.insertHabitacion(habitacionEntity)
                }
                _guardadoState.value = GuardadoState.Exito(habitacion)
                reiniciarFormulario() // ✅ Reinicia el formulario para una nueva entrada
            } catch (e: Exception) {
                Log.e(TAG, "ERROR Guardando habitación: " + e.message)
                _guardadoState.value = GuardadoState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    fun reiniciarFormulario() {
        val currentInspeccionId = _state.value.inspeccionId
        _state.update {
            NuevaHabitacionState(
                inspeccionId = currentInspeccionId,
                danoSeleccionado = it.danoSeleccionado // ✅ Preserva el último daño seleccionado
            )
        }
    }

    fun resetearEstadoGuardado() {
        _guardadoState.value = GuardadoState.Idle
    }

    data class NuevaHabitacionState(
        val inspeccionId: Long = -1,
        val nombreHabitacion: String = "",
        val alto: Int = 0,
        val largo: Int = 0,
        val ancho: Int = 0,
        val comentarios: String = "",
        val danoSeleccionado: String = "daño muro", // ✅ Campo para el daño único
        val fotosTomadas: List<String> = emptyList()
    )

    // ✅ Solo una clase GuardadoState, fuera del ViewModel
    sealed class GuardadoState {
        object Idle : GuardadoState()
        object Cargando : GuardadoState()
        data class Exito(val habitacionId: Long) : GuardadoState()
        data class Error(val mensaje: String) : GuardadoState()
    }
}

