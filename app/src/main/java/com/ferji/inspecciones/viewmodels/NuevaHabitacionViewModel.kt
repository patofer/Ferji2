package com.ferji.inspecciones.viewmodels

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.size
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ferji.inspecciones.data.model.HabitacionEntity
import com.ferji.inspecciones.data.repository.HabitacionRepository
import com.ferji.inspecciones.data.repository.InspeccionRepository
import com.ferji.inspecciones.utils.GsonUtils
import com.ferji.inspecciones.utils.PdfGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject


@HiltViewModel
class NuevaHabitacionViewModel @Inject constructor(
    private val habitacionRepository: HabitacionRepository,
) : ViewModel() {

    companion object {
        private const val TAG = "DEBUG_HABITACION"
        private const val TAG_PDF = "NuevaInspeccionVM_PDF"
    }

    private val _state = MutableStateFlow(NuevaHabitacionState())
    val state = _state.asStateFlow()

    private val _guardadoState = MutableStateFlow<GuardadoState>(GuardadoState.Idle)
    val guardadoState = _guardadoState.asStateFlow()

    // Estado para manejar el texto de "otro" daño
    private val _textoOtroDano = MutableStateFlow("")
    val textoOtroDano = _textoOtroDano.asStateFlow()

    sealed class FinalizarInspeccionEvent {
        object FinalizarAhora : FinalizarInspeccionEvent()
    }


    private val _pdfGenerationStatus = MutableSharedFlow<PdfGenerationResult>()
    val pdfGenerationStatus = _pdfGenerationStatus.asSharedFlow()

    private val _finalizarInspeccionEvent = MutableSharedFlow<FinalizarInspeccionEvent>()
    val finalizarInspeccionEvent = _finalizarInspeccionEvent.asSharedFlow()

    private val _preparandoParaFinalizar = MutableStateFlow(false)
    val preparandoParaFinalizar: StateFlow<Boolean> = _preparandoParaFinalizar.asStateFlow()


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

    // ✅ Función para manejar selección/deselección de daños
    fun onDanoSeleccionado(dano: String) {
        _state.update { it.copy(danoSeleccionado = dano) }

        // Si se selecciona algo diferente de "otro", limpiar el texto
        if (dano != "otro") {
            _textoOtroDano.value = ""
        }
    }

    // ✅ Función para actualizar el texto de "otro" daño
    fun onTextoOtroDanoChange(texto: String) {
        _textoOtroDano.value = texto
    }

    fun agregarFoto(rutaFoto: String) {
        val nuevasFotos = _state.value.fotosTomadas + rutaFoto
        _state.update { it.copy(fotosTomadas = nuevasFotos) }
    }

    fun eliminarFoto(rutaFoto: String) {
        val nuevasFotos = _state.value.fotosTomadas - rutaFoto
        _state.update { it.copy(fotosTomadas = nuevasFotos) }
    }



    // En NuevaHabitacionViewModel.kt

// ... (asegúrate de que _preparandoParaFinalizar, TAG, _state, textoOtroDano, etc., estén definidos)

    fun guardarHabitacionConEstado(finalizarDespues: Boolean = false) {

        // Validaciones previas a la corutina (rápidas)
        if (_state.value.inspeccionId == -1L) {
            _guardadoState.value = GuardadoState.Error("ID de inspección no válido.")
            if (finalizarDespues) {
                Log.d(TAG, "Guardado falló (ID inspección) y era para finalizar. Reseteando preparandoParaFinalizar.")
                _preparandoParaFinalizar.value = false // Resetear si intentábamos finalizar
            }
            return
        }

        if (_state.value.nombreHabitacion.isBlank()) {
            _guardadoState.value = GuardadoState.Error("El nombre de la habitación no puede estar vacío.")
            if (finalizarDespues) {
                Log.d(TAG, "Guardado falló (nombre vacío) y era para finalizar. Reseteando preparandoParaFinalizar.")
                _preparandoParaFinalizar.value = false // Resetear si intentábamos finalizar
            }
            return
        }

        if (_state.value.danoSeleccionado.isEmpty()) {
            _guardadoState.value = GuardadoState.Error("Por favor, seleccione un tipo de daño.")
            if (finalizarDespues) {
                Log.d(TAG, "Guardado falló (daño no seleccionado) y era para finalizar. Reseteando preparandoParaFinalizar.")
                _preparandoParaFinalizar.value = false // Resetear si intentábamos finalizar
            }
            return
        }

        if (_state.value.danoSeleccionado == "otro" && textoOtroDano.value.isBlank()) {
            _guardadoState.value = GuardadoState.Error("Por favor, especifique el tipo de daño 'otro'.")
            if (finalizarDespues) {
                Log.d(TAG, "Guardado falló (otro daño vacío) y era para finalizar. Reseteando preparandoParaFinalizar.")
                _preparandoParaFinalizar.value = false // Resetear si intentábamos finalizar
            }
            return
        }

        // Iniciar corutina para operaciones de base de datos y de red (si las hubiera)
        viewModelScope.launch {
            _guardadoState.value = GuardadoState.Cargando
            // Si estamos intentando finalizar, _preparandoParaFinalizar ya debería estar true
            // desde la llamada de 'intentarFinalizarInspeccion'.

            val nombreHabitacionActual = _state.value.nombreHabitacion
            try {
                val listaDeDanos: List<String> = if (state.value.danoSeleccionado == "otro") {
                    // Si 'otro' está seleccionado pero el texto está vacío, ya lo validamos arriba.
                    // Aquí asumimos que si es 'otro', textoOtroDano.value tiene contenido.
                    listOf(textoOtroDano.value)
                } else {
                    // Si no es 'otro', y danoSeleccionado no está vacío (validado arriba).
                    listOf(state.value.danoSeleccionado)
                }

                // Serializar la lista a JSON
                val danosJson = GsonUtils.toJson(listaDeDanos)

                val habitacion = HabitacionEntity(
                    inspeccionId = _state.value.inspeccionId,
                    nombre = nombreHabitacionActual,
                    alto = _state.value.alto,
                    largo = _state.value.largo,
                    ancho = _state.value.ancho,
                    danos = danosJson,
                    fotos = GsonUtils.listToJson(_state.value.fotosTomadas),
                    comentarios = _state.value.comentarios
                )

                Log.d(TAG, "Guardando habitación: $habitacion")
                val habitacionId = withContext(Dispatchers.IO) {
                    habitacionRepository.insertHabitacion(habitacion)
                }
                Log.d(TAG, "Habitación guardada con ID: $habitacionId")
                _guardadoState.value = GuardadoState.Exito(habitacionId, nombreHabitacionActual)

                if (finalizarDespues) {
                    Log.d(TAG, "Habitación guardada como parte de la finalización. Emitiendo FinalizarAhora.")
                    _finalizarInspeccionEvent.emit(FinalizarInspeccionEvent.FinalizarAhora)
                    // _preparandoParaFinalizar permanece true, ya que la Activity se cerrará.
                } else {
                    // Solo preparar para nueva habitación si NO estamos finalizando la inspección completa.
                    Log.d(TAG, "Habitación guardada (no para finalizar). Preparando para nueva habitación.")
                    prepararParaNuevaHabitacionLogica()
                }

            } catch (e: Exception) {
                Log.e(TAG, "ERROR Guardando habitación: ${e.message}", e)
                _guardadoState.value = GuardadoState.Error(e.message ?: "Error desconocido al guardar")

                // ----> LÓGICA CLAVE AÑADIDA AQUÍ <----
                if (finalizarDespues) {
                    Log.d(TAG, "Error al guardar durante intento de finalización. Reseteando preparandoParaFinalizar.")
                    _preparandoParaFinalizar.value = false // Resetear para que la UI no se quede bloqueada
                }
            }
        }
    }


    fun intentarFinalizarInspeccion() {
        val currentNombre = _state.value.nombreHabitacion
        // Considera qué campos hacen que una habitación sea "pendiente de guardar"
        // Por ejemplo, si el nombre está lleno, asumimos que hay algo que guardar.
        if (currentNombre.isNotBlank()) {
            Log.d(TAG, "Terminar Inspección: Hay datos pendientes, intentando guardar primero.")
            guardarHabitacionConEstado(finalizarDespues = true)
        } else {
            // No hay datos pendientes (o no son válidos para guardar), finalizar directamente
            Log.d(TAG, "Terminar Inspección: No hay datos pendientes, finalizando directamente.")
            viewModelScope.launch {
                _finalizarInspeccionEvent.emit(FinalizarInspeccionEvent.FinalizarAhora)
            }
        }
    }






    private fun prepararParaNuevaHabitacionLogica() {
        val currentInspeccionId = _state.value.inspeccionId
        _state.value = NuevaHabitacionState(inspeccionId = currentInspeccionId)
        _textoOtroDano.value = "" // Resetear el texto de "otro"
        // _guardadoState.value = GuardadoState.Idle // Se maneja en el LaunchedEffect de la UI
    }
    fun prepararParaNuevaHabitacion() {
        val currentInspeccionId = _state.value.inspeccionId
        _state.value = NuevaHabitacionState(inspeccionId = currentInspeccionId) // Reinicia el estado
        _guardadoState.value = GuardadoState.Idle // Resetea el estado de guardado
    }

    // ✅ Función para preparar la lista completa de daños
    private fun prepararDanosCompletos(): String {
        // ✅ Ahora solo tenemos un daño seleccionado, no un set
        val danoSeleccionado = _state.value.danoSeleccionado

        // ✅ Si es "otro" y tiene texto, usar el texto personalizado
        return if (danoSeleccionado == "otro" && _textoOtroDano.value.isNotBlank()) {
            _textoOtroDano.value
        } else {
            danoSeleccionado
        }
    }

    fun reiniciarFormulario() {
        val currentInspeccionId = _state.value.inspeccionId
        _state.update {
            NuevaHabitacionState(
                inspeccionId = currentInspeccionId
                // Todos los demás campos tendrán sus valores por defecto
            )
        }
        _textoOtroDano.value = ""
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
        val danoSeleccionado: String = "", // ✅ Múltiples daños
        val fotosTomadas: List<String> = emptyList()
    )

    sealed class GuardadoState {
        object Idle : GuardadoState()
        object Cargando : GuardadoState()
        data class Exito(val habitacionId: Long, val nombreHabitacionGuardada: String) : GuardadoState()
        data class Error(val mensaje: String) : GuardadoState()
    }

    sealed class PdfGenerationResult {
        data class Success(
            val filePath: String?,
            val fileUri: Uri?,
            val fileName: String
        ) : PdfGenerationResult()
        data class Error(val message: String) : PdfGenerationResult()
        object InProgress : PdfGenerationResult()
        object Idle : PdfGenerationResult()
    }
}