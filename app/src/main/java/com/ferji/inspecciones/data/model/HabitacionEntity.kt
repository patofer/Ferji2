package com.ferji.inspecciones.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.ferji.inspecciones.utils.GsonUtils

@Entity(
    tableName = "habitaciones",
    foreignKeys = [
        ForeignKey(
            entity = InspeccionEntity::class,
            parentColumns = ["id"],
            childColumns = ["inspeccion_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class HabitacionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "inspeccion_id")
    val inspeccionId: Long,

    @ColumnInfo(name = "nombre")
    val nombre: String,

    @ColumnInfo(name = "alto")
    val alto: Int,

    @ColumnInfo(name = "largo")  // ✅ Asegúrate que sea 'largo'
    val largo: Int,

    @ColumnInfo(name = "ancho")
    val ancho: Int,

    @ColumnInfo(name = "danos")
    val danos: String, // JSON con daños seleccionados

    @ColumnInfo(name = "fotos")
    val fotos: String, // JSON con rutas de fotos

    @ColumnInfo(name = "comentarios")
    val comentarios: String,

    @ColumnInfo(name = "tipo_dano")
    val tipoDano: String,

    @ColumnInfo(name = "fecha_creacion")
    val fechaCreacion: Long = System.currentTimeMillis()  // ✅ Cambia Date por Long
) {
    // Métodos helper para convertir JSON
    fun getDanosList(): List<String> {
        return GsonUtils.jsonToList(danos)
    }

    fun getFotosList(): List<String> {
        return GsonUtils.jsonToList(fotos)
    }


    // Método helper para obtener Date desde Long
    fun getFechaCreacionDate(): java.util.Date {
        return java.util.Date(fechaCreacion)
    }
}