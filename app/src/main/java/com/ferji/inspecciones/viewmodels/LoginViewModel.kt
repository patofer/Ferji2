package com.ferji.inspecciones.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ferji.inspecciones.data.model.UserEntity
// --- 1. AÑADIR IMPORTACIONES NECESARIAS ---
import com.ferji.inspecciones.data.repository.EmailSettingsRepository
import com.ferji.inspecciones.data.repository.PartidaRepository
import com.ferji.inspecciones.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    // --- 2. INYECTAR LAS DEPENDENCIAS ADICIONALES ---
    private val userRepository: UserRepository,
    private val partidaRepository: PartidaRepository,
    private val firebaseAuth: FirebaseAuth,
    private val emailSettingsRepository: EmailSettingsRepository
) : ViewModel() {

    private val _loginSuccessEvent = MutableSharedFlow<Unit>()
    val loginSuccessEvent = _loginSuccessEvent.asSharedFlow()

    private val _loginErrorEvent = MutableSharedFlow<String>()
    val loginErrorEvent = _loginErrorEvent.asSharedFlow()


    suspend fun findUserByRut(rut: String): UserEntity? {
        return userRepository.findUserInDatabase(rut)
    }

    /**
     * Esta función se ejecuta cuando el usuario presiona "Ingresar".
     * Ahora se encarga de:
     * 1. Validar los campos.
     * 2. Iniciar sesión anónimamente con Firebase.
     * 3. Sincronizar las partidas maestras.
     * 4. Guardar el usuario localmente.
     * 5. Notificar a la UI que el login fue exitoso.
     */
    fun onLoginClicked(rut: String, nombre: String, email: String) {
        // Validación básica de campos
        if (rut.isBlank() || nombre.isBlank() || email.isBlank()) {
            viewModelScope.launch {
                _loginErrorEvent.emit("Todos los campos son obligatorios.")
            }
            return
        }

        viewModelScope.launch {
            try {
                // --- 3. LÓGICA DE AUTENTICACIÓN ANÓNIMA ---
                val currentUser = firebaseAuth.currentUser
                if (currentUser == null) {
                    // Si no hay sesión activa, creamos una anónima
                    Log.d("LoginViewModel", "No hay sesión activa. Iniciando sesión anónima...")
                    firebaseAuth.signInAnonymously().await()
                    Log.d("LoginViewModel", "Login anónimo exitoso. UID: ${firebaseAuth.currentUser?.uid}")
                } else {
                    // Si ya existe una sesión (de una vez anterior), simplemente la usamos
                    Log.d("LoginViewModel", "Ya existe una sesión anónima. UID: ${currentUser.uid}")
                }

                // --- 4. SINCRONIZAR DATOS (¡EL PASO CLAVE!) ---
                // Ahora que `request.auth` NO es null, esta llamada funcionará.
                Log.d("LoginViewModel", "Sincronizando partidas principales...")
                partidaRepository.sincronizarCatalogoCompleto()

                // --- Inicializar configuración de emails en Firestore si no existe ---
                Log.d("LoginViewModel", "Inicializando configuración de emails...")
                emailSettingsRepository.inicializarSiNoExiste()

                // --- 5. LÓGICA ORIGINAL: GUARDAR USUARIO Y NAVEGAR ---
                // Mantenemos tu lógica de guardar el usuario en la sesión local.
                userRepository.login(rut, nombre, email)
                _loginSuccessEvent.emit(Unit) // Notifica a la UI que el login fue exitoso para que navegue.

            } catch (e: Exception) {
                // Capturamos cualquier error durante el proceso
                Log.e("LoginViewModel", "Error durante el proceso de login: ${e.message}", e)
                _loginErrorEvent.emit("Error de conexión: ${e.message}")
            }
        }
    }
}
