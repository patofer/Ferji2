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
    private val userDao: UserDao // <-- 1. Inyecta el DAO de usuario
) {
    // La lista de administradores sigue siendo necesaria para asignar el rol
    private val adminUsers: List<AdminUser> = listOf(
        AdminUser("155402946", "Patricio Fernandez", "pfernandeza@gmail.com"),
        AdminUser("123456789", "Otro Admin", "otro.admin@email.com")
    )

    fun getUserSession(): Flow<UserSession> = sessionManager.userSessionFlow

    suspend fun login(rut: String, nombre: String, email: String) {
        // Asigna el rol basado en la lista de administradores
        val userRole = if (adminUsers.any { it.rut == rut }) UserRoles.ADMIN else UserRoles.USER

        // Guarda la sesión actual
        sessionManager.saveUserSession(rut, nombre, email, userRole)

        // <-- 2. GUARDA O ACTUALIZA AL USUARIO EN LA BASE DE DATOS
        userDao.saveUser(UserEntity(rut = rut, nombre = nombre, email = email))
    }

    // <-- 3. NUEVA FUNCIÓN PARA BUSCAR EN LA BASE DE DATOS
    suspend fun findUserInDatabase(rut: String): UserEntity? {
        return userDao.findUserByRut(rut)
    }

    suspend fun logout() {
        sessionManager.clearUserSession()
    }
}
