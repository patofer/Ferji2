package com.ferji.inspecciones.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ferji.inspecciones.data.manager.UserSession
import com.ferji.inspecciones.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    // Define los posibles estados de la sesión para que la UI sepa qué mostrar
    sealed class SessionState {
        object LOADING : SessionState() // Cargando, aún no sabemos el estado
        data class LoggedIn(val data: UserSession) : SessionState() // Usuario con sesión activa
        object LoggedOut : SessionState() // No hay sesión
    }

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.LOADING)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    init {
        // En cuanto el ViewModel se crea, se suscribe a los cambios de la sesión
        viewModelScope.launch {
            userRepository.getUserSession().collect { sessionData ->
                // Si el RUT guardado está vacío o es nulo, el usuario no está logueado
                if (sessionData.rut.isNullOrBlank()) {
                    _sessionState.value = SessionState.LoggedOut
                } else {
                    // Si hay datos, el usuario está logueado
                    _sessionState.value = SessionState.LoggedIn(sessionData)
                }
            }
        }
    }

    // Función para que el usuario pueda cerrar su sesión desde la UI
    fun logout() {
        viewModelScope.launch {
            userRepository.logout()
            // El `collect` en el bloque init se encargará de actualizar el estado a LoggedOut
        }
    }
}
