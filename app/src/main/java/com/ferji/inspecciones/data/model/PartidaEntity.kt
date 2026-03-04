package com.ferji.inspecciones.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@Entity(
    tableName = "partidas",
    indices = [
        Index(value = ["codigo"], unique = false),
        Index(value = ["partida_principal_id"]),
        Index(value = ["firebaseId"], unique = true)
    ],
    foreignKeys = [
        ForeignKey(
            entity = PartidaPrincipalEntity::class,
            parentColumns = ["id"],
            childColumns = ["partida_principal_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
@IgnoreExtraProperties
data class PartidaEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @get:PropertyName("partida_principal_id")
    @set:PropertyName("partida_principal_id")
    @ColumnInfo(name = "partida_principal_id")
    var partidaPrincipalId: Long = 0L,

    @ColumnInfo(name = "descripcion")
    val descripcion: String = "",

    @ColumnInfo(name = "unidad")
    val unidad: String = "",

    @get:PropertyName("precio_unitario")
    @set:PropertyName("precio_unitario")
    @ColumnInfo(name = "precio_unitario")
    var precioUnitario: Double = 0.0,

    @ColumnInfo(name = "codigo")
    val codigo: String? = null,

    val firebaseId: String = "",

    // ✅ --- LA CORRECCIÓN MÁS IMPORTANTE --- ✅
    // Una nueva entidad, por defecto, NO está sincronizada.
    @get:Exclude
    val sincronizadoConFirebase: Boolean = false,

    @get:Exclude
    val eliminado: Boolean = false
)
