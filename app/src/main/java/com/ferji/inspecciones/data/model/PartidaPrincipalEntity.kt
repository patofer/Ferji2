package com.ferji.inspecciones.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// Esta es nuestra nueva tabla, mucho más simple.
@Entity(tableName = "partidas_principales")
data class PartidaPrincipalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val tipoSuperficie: String,
    val naturaleza: PartidaNaturaleza = PartidaNaturaleza.VARIABLE
)
