package com.ferji.inspecciones.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ferji.inspecciones.data.model.UserEntity


@Dao
interface UserDao {
    // Upsert es perfecto aquí: si el usuario existe, lo actualiza; si no, lo inserta.
    @Upsert
    suspend fun saveUser(user: UserEntity)

    // Función para buscar un usuario por su RUT. Devuelve nullable por si no lo encuentra.
    @Query("SELECT * FROM user_history WHERE rut = :rut LIMIT 1")
    suspend fun findUserByRut(rut: String): UserEntity?
}