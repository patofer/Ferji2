package com.ferji.inspecciones.data.manager

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_session")

/**
 * Data class que representa los datos del usuario en sesión.
 * AHORA INCLUYE EL ROL.
 */
data class UserSession(
    val rut: String?,
    val nombre: String?,
    val email: String?,
    val role: String? // <-- NUEVO CAMPO PARA EL ROL
)
@Singleton
class SessionManager @Inject constructor(@ApplicationContext private val context: Context) {

    // Definimos las claves para guardar los datos
    private object PreferencesKeys {
        val USER_RUT = stringPreferencesKey("user_rut")
        val USER_NOMBRE = stringPreferencesKey("user_nombre")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_ROLE = stringPreferencesKey("user_role") // <-- NUEVA CLAVE
    }

    // Flow para obtener los datos de la sesión en tiempo real
    val userSessionFlow: Flow<UserSession> = context.dataStore.data.map { preferences ->
        UserSession(
            rut = preferences[PreferencesKeys.USER_RUT],
            nombre = preferences[PreferencesKeys.USER_NOMBRE],
            email = preferences[PreferencesKeys.USER_EMAIL],
            role = preferences[PreferencesKeys.USER_ROLE] // <-- LEER EL ROL
        )
    }

    /**
     * Función para guardar una nueva sesión (login).
     * AHORA TAMBIÉN GUARDA EL ROL.
     */
    suspend fun saveUserSession(rut: String, nombre: String, email: String, role: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_RUT] = rut
            preferences[PreferencesKeys.USER_NOMBRE] = nombre
            preferences[PreferencesKeys.USER_EMAIL] = email
            preferences[PreferencesKeys.USER_ROLE] = role // <-- GUARDAR EL ROL
        }
    }

    /**
     * Función para borrar la sesión (logout).
     */
    suspend fun clearUserSession() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
