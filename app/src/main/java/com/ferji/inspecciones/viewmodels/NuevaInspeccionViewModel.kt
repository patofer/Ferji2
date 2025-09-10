package com.ferji.inspecciones.viewmodels

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ferji.inspecciones.data.model.InspeccionEntity
import com.ferji.inspecciones.data.repository.InspeccionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

// ✅ HiltViewModel y @Inject son necesarios para que Hilt lo maneje
@HiltViewModel
class NuevaInspeccionViewModel @Inject constructor(
    private val repository: InspeccionRepository
) : ViewModel() {

    // ✅ Define los eventos de la UI para la navegación y los mensajes
    sealed class UiEvent {
        data class NavigateToNewRoom(val inspeccionId: Long) : UiEvent()
        data class ShowSnackbar(val message: String) : UiEvent()
    }
    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents: SharedFlow<UiEvent> = _uiEvents.asSharedFlow()

    var rut by mutableStateOf("")
    var siniestro by mutableStateOf("")
    var direccion by mutableStateOf("")
    var rutInspector by mutableStateOf("")
    var mail by mutableStateOf("")
    var mensaje by mutableStateOf("")

    val todosCamposLlenos: Boolean
        get() = rut.isNotBlank() &&
                siniestro.isNotBlank() &&
                direccion.isNotBlank() &&
                rutInspector.isNotBlank() &&
                mail.isNotBlank()

    fun guardarInspeccion() {
        // ✅ It now accesses the properties directly from the ViewModel
        Log.d("NUEVA_INSPECCION_UI", "guardarInspeccion ")
        if (rut.isBlank() || siniestro.isBlank() || direccion.isBlank() || rutInspector.isBlank() || mail.isBlank()) {
            viewModelScope.launch {
                _uiEvents.emit(UiEvent.ShowSnackbar("Por favor complete todos los campos."))
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inspeccion = InspeccionEntity(
                    rut = rut, // ✅ Use the ViewModel's property
                    siniestro = siniestro,
                    direccion = direccion,
                    rutInspector = rutInspector,
                    mail = mail
                )
                val idInspeccion = repository.insertInspeccion(inspeccion)
                Log.d("NUEVA_INSPECCION_UI", "guardarInspeccion ")
                _uiEvents.emit(UiEvent.NavigateToNewRoom(idInspeccion))
            } catch (e: Exception) {
                _uiEvents.emit(UiEvent.ShowSnackbar("Error al guardar: ${e.message}"))
            }
        }
    }
}