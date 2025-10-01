// En: com/ferji/inspecciones/data/model/Dan_Partida_CrossRef.kt
package com.ferji.inspecciones.data.model

import androidx.room.Entity

@Entity(tableName = "danos_partidas_cross_ref", primaryKeys = ["claveDano", "partidaId"])
data class DanoPartidaCrossRef(
    val claveDano: String, // Clave del daño, ej: "2" para "Fisura en cielo"
    val partidaId: Long    // ID de la partida asociada desde la tabla PartidaEntity.kt
)
