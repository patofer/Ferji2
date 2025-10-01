package com.ferji.inspecciones.data.model

import androidx.room.Embedded
import androidx.room.Relation

// Esta clase no es una tabla, es un objeto de datos para las consultas.
data class PartidaPrincipalWithDetails(
    @Embedded // Incluye todos los campos de la PartidaPrincipalEntity
    val partidaPrincipal: PartidaPrincipalEntity,

    @Relation(
        parentColumn = "id", // La columna 'id' de la tabla padre (PartidaPrincipalEntity)
        entityColumn = "partida_principal_id" // La columna 'partida_principal_id' de la tabla hija (PartidaEntity)
    )
    val detalles: List<PartidaEntity> // La lista de todas las partidas hijas que coinciden
)
