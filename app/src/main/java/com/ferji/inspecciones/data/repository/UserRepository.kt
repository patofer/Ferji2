package com.ferji.inspecciones.data.repository

import com.ferji.inspecciones.data.dao.UserDao
import com.ferji.inspecciones.data.manager.SessionManager
import com.ferji.inspecciones.data.manager.UserSession
import com.ferji.inspecciones.data.model.UserEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

object UserRoles {
    const val ADMIN = "ADMINISTRATOR"
    const val USER = "USER"
}

data class AdminUser(val rut: String, val nombre: String, val email: String)

class UserRepository @Inject constructor(
    private val sessionManager: SessionManager,
    private val userDao: UserDao
) {
    // La lista de administradores sigue siendo necesaria para asignar el rol
    private val adminUsers: List<AdminUser> = listOf(
        AdminUser("155402946", "Patricio Fernandez", "pfernandeza@gmail.com"),
        AdminUser("16697643k", "Felipe Fernandez", "felipe@ferji.cl")
    )

    // ✅ --- INICIO DE LA SOLUCIÓN --- ✅
    /**
     * Propiedad PÚBLICA que expone un Flow con la sesión del usuario actual.
     * Es la "única fuente de verdad" sobre el estado de la sesión para el resto de la app.
     * Los ViewModels observarán este Flow.
     */
    val currentUserSession: Flow<UserSession> = sessionManager.userSessionFlow
    // ✅ --- FIN DE LA SOLUCIÓN --- ✅


    // La función 'getUserSession()' ya no es necesaria, ya que ahora tenemos la propiedad pública.
    // fun getUserSession(): Flow<UserSession> = sessionManager.userSessionFlow

    suspend fun login(rut: String, nombre: String, email: String) {
        // Asigna el rol basado en la lista de administradores
        val userRole = if (adminUsers.any { it.rut == rut }) UserRoles.ADMIN else UserRoles.USER

        // Guarda la sesión actual
        sessionManager.saveUserSession(rut, nombre, email, userRole)

        // GUARDA O ACTUALIZA AL USUARIO EN LA BASE DE DATOS LOCAL
        userDao.saveUser(UserEntity(rut = rut, nombre = nombre, email = email, rol = userRole))
    }

    suspend fun findUserInDatabase(rut: String): UserEntity? {
        return userDao.findUserByRut(rut)
    }

    suspend fun logout() {
        sessionManager.clearUserSession()
    }
}
