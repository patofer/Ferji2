package com.ferji.inspecciones.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey // <-- 1. IMPORTAR ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Index

// El enum no cambia


@Entity(
    tableName = "partidas",
    indices = [
        Index(value = ["codigo"], unique = false),
        Index(value = ["partida_principal_id"]) // <-- 2. AÑADIR ÍNDICE PARA LA CLAVE FORÁNEA
    ],
    // --- INICIO DE LA CORRECCIÓN ---
    foreignKeys = [
        ForeignKey(
            entity = PartidaPrincipalEntity::class, // La tabla padre
            parentColumns = ["id"],                 // La columna de la clave primaria en el padre
            childColumns = ["partida_principal_id"],// La columna de la clave foránea en esta tabla
            onDelete = ForeignKey.CASCADE           // ¡IMPORTANTE! Si se borra una PartidaPrincipal, se borran todas sus PartidaEntity hijas.
        )
    ]
    // --- FIN DE LA CORRECCIÓN ---
)
data class PartidaEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    // --- INICIO DE LA CORRECCIÓN ---
    @ColumnInfo(name = "partida_principal_id") // 3. AÑADIR LA COLUMNA DE LA CLAVE FORÁNEA
    val partidaPrincipalId: Long,
    // --- FIN DE LA CORRECCIÓN ---

    @ColumnInfo(name = "descripcion")
    val descripcion: String,

    @ColumnInfo(name = "unidad")
    val unidad: String,

    @ColumnInfo(name = "precio_unitario")
    val precioUnitario: Double,

    @ColumnInfo(name = "codigo")
    val codigo: String? = null
)
