package com.ferji.inspecciones.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ferji.inspecciones.data.model.EmailSettingsEntity
import com.ferji.inspecciones.data.repository.EmailSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConfiguracionViewModel @Inject constructor(
    private val emailSettingsRepository: EmailSettingsRepository
) : ViewModel() {

    companion object {
        private const val TAG = "ConfiguracionVM"
    }

    data class ConfiguracionUiState(
        val emailAdmin: String = "",
        val emailCc: String = "",
        val enviarInspeccionAlInspector: Boolean = false,
        val enviarPresupuestoAlInspector: Boolean = false,
        val enviarImagenesAlInspector: Boolean = false,
        val isLoading: Boolean = true,
        val isSaving: Boolean = false
    )

    sealed class ConfiguracionEvent {
        data class ShowMessage(val message: String) : ConfiguracionEvent()
    }

    private val _uiState = MutableStateFlow(ConfiguracionUiState())
    val uiState: StateFlow<ConfiguracionUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ConfiguracionEvent>()
    val events = _events.asSharedFlow()

    init {
        cargarConfiguracion()
    }

    private fun cargarConfiguracion() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val settings = emailSettingsRepository.obtenerConfiguracion()
            _uiState.update {
                it.copy(
                    emailAdmin = settings.emailAdmin,
                    emailCc = settings.emailCc,
                    enviarInspeccionAlInspector = settings.enviarInspeccionAlInspector,
                    enviarPresupuestoAlInspector = settings.enviarPresupuestoAlInspector,
                    enviarImagenesAlInspector = settings.enviarImagenesAlInspector,
                    isLoading = false
                )
            }
            Log.d(TAG, "Configuración cargada en UI")
        }
    }

    fun onEmailAdminChange(value: String) {
        _uiState.update { it.copy(emailAdmin = value) }
    }

    fun onEmailCcChange(value: String) {
        _uiState.update { it.copy(emailCc = value) }
    }

    fun onEnviarInspeccionAlInspectorChange(value: Boolean) {
        _uiState.update { it.copy(enviarInspeccionAlInspector = value) }
    }

    fun onEnviarPresupuestoAlInspectorChange(value: Boolean) {
        _uiState.update { it.copy(enviarPresupuestoAlInspector = value) }
    }

    fun onEnviarImagenesAlInspectorChange(value: Boolean) {
        _uiState.update { it.copy(enviarImagenesAlInspector = value) }
    }

    fun guardarConfiguracion() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.emailAdmin.isBlank()) {
                _events.emit(ConfiguracionEvent.ShowMessage("El email del administrador es obligatorio."))
                return@launch
            }

            _uiState.update { it.copy(isSaving = true) }

            val settings = EmailSettingsEntity(
                emailAdmin = state.emailAdmin.trim(),
                emailCc = state.emailCc.trim(),
                enviarInspeccionAlInspector = state.enviarInspeccionAlInspector,
                enviarPresupuestoAlInspector = state.enviarPresupuestoAlInspector,
                enviarImagenesAlInspector = state.enviarImagenesAlInspector
            )

            val exito = emailSettingsRepository.actualizarConfiguracion(settings)
            _uiState.update { it.copy(isSaving = false) }

            if (exito) {
                _events.emit(ConfiguracionEvent.ShowMessage("Configuración guardada correctamente."))
            } else {
                _events.emit(ConfiguracionEvent.ShowMessage("Error al guardar la configuración."))
            }
        }
    }
}

