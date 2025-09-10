package com.ferji.inspecciones.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "inspecciones")
data class InspeccionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "rut")
    val rut: String,

    @ColumnInfo(name = "siniestro")
    val siniestro: String,

    @ColumnInfo(name = "direccion")
    val direccion: String,

    @ColumnInfo(name = "rut_inspector")
    val rutInspector: String,

    @ColumnInfo(name = "mail")
    val mail: String,

    @ColumnInfo(name = "fecha_creacion")
    val fechaCreacion: Date = Date(),

    @ColumnInfo(name = "estado", defaultValue = "PENDIENTE")
    val estado: String = "PENDIENTE"
)