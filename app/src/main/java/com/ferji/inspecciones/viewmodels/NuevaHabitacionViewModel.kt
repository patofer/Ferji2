package com.ferji.inspecciones.viewmodels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ferji.inspecciones.data.model.HabitacionEntity
import com.ferji.inspecciones.data.model.PartidaNaturaleza
import com.ferji.inspecciones.data.model.PartidaPrincipalEntity
import com.ferji.inspecciones.data.repository.HabitacionRepository
import com.ferji.inspecciones.data.repository.PartidaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class NuevaHabitacionViewModel @Inject constructor(
    private val habitacionRepository: HabitacionRepository,
    private val partidaRepository: PartidaRepository
) : ViewModel() {

    companion object {
        private const val TAG = "NuevaHabitacionVM"
    }

    private val _state = MutableStateFlow(NuevaHabitacionState())
    val state: StateFlow<NuevaHabitacionState> = _state.asStateFlow()

    // Este Flow contendrá la lista de CATEGORÍAS (Partidas Principales) disponibles
    private val _listaCategoriasDisponibles = MutableStateFlow<List<PartidaPrincipalEntity>>(emptyList())
    val listaCategoriasDisponibles: StateFlow<List<PartidaPrincipalEntity>> = _listaCategoriasDisponibles.asStateFlow()

    // Otros StateFlows para la UI
    private val _guardadoState = MutableStateFlow<GuardadoState>(GuardadoState.Idle)
    val guardadoState: StateFlow<GuardadoState> = _guardadoState.asStateFlow()

    private val _textoOtroDano = MutableStateFlow("")
    val textoOtroDano: StateFlow<String> = _textoOtroDano.asStateFlow()

    private val _finalizarInspeccionEvent = MutableSharedFlow<FinalizarInspeccionEvent>()
    val finalizarInspeccionEvent: SharedFlow<FinalizarInspeccionEvent> = _finalizarInspeccionEvent.asSharedFlow()

    private val _preparandoParaFinalizar = MutableStateFlow(false)
    val preparandoParaFinalizar: StateFlow<Boolean> = _preparandoParaFinalizar.asStateFlow()

    init {
        Log.d(TAG, "ViewModel inicializado. Cargando categorías de daños...")
        loadCategorias()
    }

    fun init(inspeccionIdRecibido: Long) {
        if (_state.value.inspeccionId == -1L && inspeccionIdRecibido != -1L) {
            _state.update { it.copy(inspeccionId = inspeccionIdRecibido) }
        }
    }

    /**
     * Carga la lista de categorías (Partidas Principales VARIABLES) desde la base de datos.
     * Las partidas FIJAS (globales) se excluyen porque se agregan automáticamente al presupuesto.
     */
    private fun loadCategorias() {
        viewModelScope.launch {
            // 1. Sincronizar desde Firebase (fuente de verdad) antes de cargar
            try {
                withContext(Dispatchers.IO) {
                    Log.d(TAG, "Sincronizando catálogo desde Firebase antes de cargar categorías...")
                    partidaRepository.sincronizarCatalogoCompleto()
                    Log.d(TAG, "Sincronización completada.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al sincronizar desde Firebase: ${e.message}. Se usarán datos locales.", e)
            }

            // 2. Cargar TODAS las partidas y filtrar VARIABLES
            partidaRepository.getAllPartidasPrincipales().collect { todas ->
                Log.d(TAG, "══════════════════════════════════════")
                Log.d(TAG, "TODAS las partidas principales en BD: ${todas.size}")
                todas.forEach { pp ->
                    Log.d(TAG, "  → id=${pp.id}, nombre='${pp.nombre}', naturaleza=${pp.naturaleza}, firebaseId='${pp.firebaseId}'")
                }
                Log.d(TAG, "══════════════════════════════════════")

                val variables = todas.filter { it.naturaleza == PartidaNaturaleza.VARIABLE }
                Log.d(TAG, "Categorías VARIABLES para el combobox: ${variables.size}")
                variables.forEach { pp ->
                    Log.d(TAG, "  ✅ VARIABLE: id=${pp.id}, nombre='${pp.nombre}'")
                }

                _listaCategoriasDisponibles.value = variables
            }
        }
    }

    // --- Métodos para actualizar el estado desde la UI ---
    fun reintentarCargaCategorias() {
        Log.d(TAG, "Reintentando carga de categorías...")
        loadCategorias()
    }

    fun onNombreChange(nombre: String) { _state.update { it.copy(nombreHabitacion = nombre) } }
    fun onAltoChange(alto: Int) { _state.update { it.copy(alto = alto) } }
    fun onLargoChange(largo: Int) { _state.update { it.copy(largo = largo) } }
    fun onAnchoChange(ancho: Int) { _state.update { it.copy(ancho = ancho) } }
    fun onComentariosChange(comentarios: String) { _state.update { it.copy(comentarios = comentarios) } }
    fun onTextoOtroDanoChange(texto: String) { _textoOtroDano.value = texto }

    /**
     * Alterna la selección de una CATEGORÍA de daño.
     */
    fun onCategoriaToggled(categoria: PartidaPrincipalEntity) {
        _state.update { currentState ->
            val nuevasCategorias = currentState.categoriasSeleccionadas.toMutableSet()
            if (nuevasCategorias.contains(categoria)) {
                nuevasCategorias.remove(categoria)
            } else {
                nuevasCategorias.add(categoria)
            }
            currentState.copy(categoriasSeleccionadas = nuevasCategorias.toList())
        }
    }

    fun onOtroDanoToggled(seleccionado: Boolean) {
        _state.update { it.copy(otroDanoSeleccionado = seleccionado) }
        if (!seleccionado) {
            _textoOtroDano.value = ""
        }
    }

    fun agregarFoto(rutaFoto: String) {
        _state.update { it.copy(fotosTomadas = it.fotosTomadas + rutaFoto) }
    }

    fun eliminarFoto(rutaFoto: String) {
        _state.update { it.copy(fotosTomadas = it.fotosTomadas - rutaFoto) }
    }

    // --- LÓGICA DE GUARDADO ---
    private fun obtenerDescripcionesCompletasDeDanosParaGuardar(): List<String> {
        val descripciones = mutableListOf<String>()
        // Añade los nombres de las categorías seleccionadas
        _state.value.categoriasSeleccionadas.forEach { categoria ->
            descripciones.add(categoria.nombre)
        }
        // Añade la descripción de "otro" si está seleccionada y no está vacía
        if (_state.value.otroDanoSeleccionado && _textoOtroDano.value.isNotBlank()) {
            descripciones.add(_textoOtroDano.value)
        }
        return descripciones.distinct()
    }

    fun guardarHabitacionConEstado(finalizarDespues: Boolean = false) {
        val currentState = _state.value
        val descripcionesDanos = obtenerDescripcionesCompletasDeDanosParaGuardar()

        // Validaciones...
        if (currentState.nombreHabitacion.isBlank()) {
            _guardadoState.value = GuardadoState.Error("El nombre de la habitación no puede estar vacío.")
            if (finalizarDespues) _preparandoParaFinalizar.value = false
            return
        }
        if (currentState.largo <= 0) {
            _guardadoState.value = GuardadoState.Error("El largo es obligatorio.")
            if (finalizarDespues) _preparandoParaFinalizar.value = false
            return
        }
        if (currentState.alto <= 0) {
            _guardadoState.value = GuardadoState.Error("El alto es obligatorio.")
            if (finalizarDespues) _preparandoParaFinalizar.value = false
            return
        }
        // Nota: el ancho es opcional (ej: muros de fachada solo usan largo × alto)
        if (descripcionesDanos.isEmpty()) {
            _guardadoState.value = GuardadoState.Error("Por favor, seleccione al menos un tipo de daño.")
            if (finalizarDespues) _preparandoParaFinalizar.value = false
            return
        }
        if (currentState.otroDanoSeleccionado && _textoOtroDano.value.isBlank()) {
            _guardadoState.value = GuardadoState.Error("Por favor, especifique el tipo de daño 'Otro'.")
            if (finalizarDespues) _preparandoParaFinalizar.value = false
            return
        }

        viewModelScope.launch {
            _guardadoState.value = GuardadoState.Cargando
            val danosJsonString = try { Json.encodeToString(descripcionesDanos) } catch (e: Exception) { Log.e(TAG, "Error serializando daños: $e"); "[]" }
            val fotosJsonString = try { Json.encodeToString(currentState.fotosTomadas) } catch (e: Exception) { Log.e(TAG, "Error serializando fotos: $e"); "[]" }

            try {
                val habitacion = HabitacionEntity(
                    inspeccionId = currentState.inspeccionId,
                    nombre = currentState.nombreHabitacion,
                    alto = currentState.alto,
                    largo = currentState.largo,
                    ancho = currentState.ancho,
                    danos = danosJsonString,
                    fotos = fotosJsonString,
                    comentarios = currentState.comentarios
                )

                Log.d(TAG, "Guardando habitación: $habitacion")
                val habitacionId = withContext(Dispatchers.IO) {
                    habitacionRepository.insertHabitacion(habitacion)
                }
                Log.d(TAG, "Habitación guardada con ID: $habitacionId")
                _guardadoState.value = GuardadoState.Exito(habitacionId, currentState.nombreHabitacion)

                if (finalizarDespues) {
                    _finalizarInspeccionEvent.emit(FinalizarInspeccionEvent.FinalizarAhora)
                }
            } catch (e: Exception) {
                Log.e(TAG, "ERROR Guardando habitación: ${e.message}", e)
                _guardadoState.value = GuardadoState.Error(e.message ?: "Error desconocido al guardar")
                if (finalizarDespues) {
                    _preparandoParaFinalizar.value = false
                }
            }
        }
    }

    fun intentarFinalizarInspeccion() {
        val s = state.value
        val tieneDatos = s.nombreHabitacion.isNotBlank() ||
                obtenerDescripcionesCompletasDeDanosParaGuardar().isNotEmpty() ||
                s.fotosTomadas.isNotEmpty() ||
                s.alto > 0 || s.largo > 0 || s.ancho > 0
        if (tieneDatos) {
            _preparandoParaFinalizar.value = true
            guardarHabitacionConEstado(finalizarDespues = true)
        } else {
            viewModelScope.launch {
                _finalizarInspeccionEvent.emit(FinalizarInspeccionEvent.FinalizarAhora)
            }
        }
    }

    fun prepararParaNuevaHabitacion() {
        val currentInspeccionId = _state.value.inspeccionId
        _state.value = NuevaHabitacionState(inspeccionId = currentInspeccionId)
        _textoOtroDano.value = ""
        _guardadoState.value = GuardadoState.Idle
    }

    fun resetearEstadoGuardado() {
        _guardadoState.value = GuardadoState.Idle
    }

    // --- DEFINICIONES DE ESTADO Y EVENTOS ---

    data class NuevaHabitacionState(
        val inspeccionId: Long = -1,
        val nombreHabitacion: String = "",
        val alto: Int = 0,
        val largo: Int = 0,
        val ancho: Int = 0,
        val comentarios: String = "",
        // La lista de seleccionados ahora contiene objetos PartidaPrincipalEntity
        val categoriasSeleccionadas: List<PartidaPrincipalEntity> = emptyList(),
        val otroDanoSeleccionado: Boolean = false,
        val fotosTomadas: List<String> = emptyList()
    )

    sealed class GuardadoState {
        object Idle : GuardadoState()
        object Cargando : GuardadoState()
        data class Exito(val habitacionId: Long, val nombreHabitacionGuardada: String) : GuardadoState()
        data class Error(val mensaje: String) : GuardadoState()
    }

    sealed class FinalizarInspeccionEvent {
        object FinalizarAhora : FinalizarInspeccionEvent()
    }

    sealed class PdfGenerationResult {
        data class Success(val filePath: String?, val fileUri: Uri?, val fileName: String) : PdfGenerationResult()
        data class Error(val message: String) : PdfGenerationResult()
        object InProgress : PdfGenerationResult()
        object Idle : PdfGenerationResult()
    }
}
