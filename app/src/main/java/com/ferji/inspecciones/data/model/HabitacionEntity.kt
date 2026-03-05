package com.ferji.inspecciones.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.ferji.inspecciones.utils.GsonUtils // Asegúrate de que esta importación sea correcta
import java.util.Date // Necesario para getFechaCreacionDate

@Entity(
    tableName = "habitaciones",
    foreignKeys = [
        ForeignKey(
            entity = InspeccionEntity::class,
            parentColumns = ["id"], // Columna PK en InspeccionEntity
            childColumns = ["inspeccion_id"], // Columna FK en esta tabla (HabitacionEntity)
            onDelete = ForeignKey.CASCADE // Acción al eliminar una InspeccionEntity padre
        )
    ]
    // Considera añadir un índice para inspeccion_id si haces muchas búsquedas por él:
    // indices = [Index(value = ["inspeccion_id"])]
)
data class HabitacionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "inspeccion_id", index = true) // Añadir index = true es buena práctica para FKs
    val inspeccionId: Long,

    @ColumnInfo(name = "nombre")
    val nombre: String,

    @ColumnInfo(name = "alto")
    val alto: Int, // Se guardan como enteros en centímetros (ej: 380 = 3.80 metros)

    @ColumnInfo(name = "largo")
    val largo: Int,

    @ColumnInfo(name = "ancho")
    val ancho: Int,

    @ColumnInfo(name = "danos")
    val danos: String, // Representación JSON de una lista de strings con los daños

    @ColumnInfo(name = "fotos")
    val fotos: String, // Representación JSON de una lista de strings con las rutas/URIs de las fotos

    @ColumnInfo(name = "comentarios")
    val comentarios: String,

    @ColumnInfo(name = "fecha_creacion")
    val fechaCreacion: Long = System.currentTimeMillis()
) {
    /**
     * Devuelve la lista de daños deserializada desde el String JSON.
     * Retorna una lista vacía si 'danos' es nulo, vacío o hay un error en la deserialización.
     */
    fun getDanosList(): List<String> {
        // Asegúrate de que GsonUtils.jsonToStringList maneje null/blank y errores de parseo
        return GsonUtils.jsonToStringList(danos)
    }

    /**
     * Devuelve la lista de rutas de fotos deserializada desde el String JSON.
     * Retorna una lista vacía si 'fotos' es nulo, vacío o hay un error en la deserialización.
     */
    fun getFotosList(): List<String> {
        // Asegúrate de que GsonUtils.jsonToStringList maneje null/blank y errores de parseo
        return GsonUtils.jsonToStringList(fotos) // CORREGIDO: usa el campo 'fotos'
    }

    /**
     * Devuelve la fecha de creación como un objeto [java.util.Date].
     */
    fun getFechaCreacionDate(): Date {
        return Date(fechaCreacion)
    }
}
