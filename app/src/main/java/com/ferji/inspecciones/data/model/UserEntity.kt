package com.ferji.inspecciones.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_history")
data class UserEntity(
    @PrimaryKey val rut: String,
    val nombre: String,
    val email: String,
    val rol: String? = null
)