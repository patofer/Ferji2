package com.ferji.inspecciones.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Entidad que representa una partida principal o categoría de partidas.
 *
 * @param id El ID local autogenerado por Room. Es la clave primaria en la base de datos local.
 * @param firebaseId El ID del documento correspondiente en Firebase. Se usa como clave única para la sincronización.
 * @param nombre El nombre descriptivo de la partida principal (ej: "PINTURAS", "ACABADOS").
 * @param tipoSuperficie El tipo de superficie al que aplica (ej: "MUROS", "PLAFONES").
 */
@IgnoreExtraProperties // Necesario para que Firebase ignore campos extra al deserializar.
@Entity(
    tableName = "partidas_principales",
    // Creamos un índice sobre 'firebaseId' para hacer las búsquedas más rápidas
    // y asegurar que cada registro de Firebase sea único en nuestra tabla local.
    indices = [Index(value = ["firebaseId"], unique = true)]
)
data class PartidaPrincipalEntity(
    // Room se encargará de generar este ID para los nuevos registros creados en la app.
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Este campo guardará el ID del documento de Firestore para poder hacer la lógica de "Upsert".
    val firebaseId: String = "",

    // Se añaden valores por defecto para cumplir con el requisito de Firebase
    // de tener un constructor sin argumentos.
    val nombre: String = "",
    val tipoSuperficie: String = "",
    val naturaleza: PartidaNaturaleza = PartidaNaturaleza.VARIABLE,
    @get:Exclude
    val sincronizadoConFirebase: Boolean = true
)
