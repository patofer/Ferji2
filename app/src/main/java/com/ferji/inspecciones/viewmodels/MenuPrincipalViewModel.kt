package com.ferji.inspecciones.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ferji.inspecciones.data.repository.UserRepository // <-- Asegúrate de tener este import
import com.ferji.inspecciones.data.repository.UserRoles
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject



@HiltViewModel
class MenuPrincipalViewModel @Inject constructor(
    // Inyectamos el repositorio que maneja la sesión del usuario
    private val userRepository: UserRepository
) : ViewModel() {

    /**
     * Un StateFlow que emite `true` si el usuario actual es administrador, y `false` en caso contrario.
     * La UI se suscribirá a este Flow para reaccionar a los cambios.
     */
    val esAdministrador: StateFlow<Boolean> = userRepository.currentUserSession // <-- Ahora usamos la propiedad correcta
        .map { userSession ->
            // La lógica ahora se basa en el campo 'rol' de UserSession
            userSession.role == UserRoles.ADMIN
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )
}
