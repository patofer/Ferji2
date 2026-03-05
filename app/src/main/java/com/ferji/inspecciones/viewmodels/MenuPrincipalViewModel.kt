package com.ferji.inspecciones.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ferji.inspecciones.data.repository.UserRepository
import com.ferji.inspecciones.data.repository.UserRoles
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MenuPrincipalViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    val esAdministrador: StateFlow<Boolean> = userRepository.currentUserSession
        .map { userSession ->
            userSession.role == UserRoles.ADMIN
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )
}
