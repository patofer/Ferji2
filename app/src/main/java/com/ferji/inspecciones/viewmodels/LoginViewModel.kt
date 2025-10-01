package com.ferji.inspecciones.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ferji.inspecciones.data.model.UserEntity
import com.ferji.inspecciones.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _loginSuccessEvent = MutableSharedFlow<Unit>()
    val loginSuccessEvent = _loginSuccessEvent.asSharedFlow()

    // <-- CAMBIO: La función ahora es 'suspend' y devuelve UserEntity?
    suspend fun findUserByRut(rut: String): UserEntity? {
        return userRepository.findUserInDatabase(rut)
    }

    fun onLoginClicked(rut: String, nombre: String, email: String) {
        // Aquí puedes mantener tus validaciones de formato de RUT y email
        // ...

        viewModelScope.launch {
            userRepository.login(rut, nombre, email)
            _loginSuccessEvent.emit(Unit)
        }
    }
}
