package com.ferji.inspecciones.data.dao

import androidx.room.*
import com.ferji.inspecciones.data.model.PartidaPrincipalEntity
import com.ferji.inspecciones.data.model.PartidaPrincipalWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface PartidaPrincipalDao {

    // Usamos 'REPLACE' para que la misma función sirva para crear y actualizar (Upsert).
    @Upsert
    suspend fun upsert(partida: PartidaPrincipalEntity): Long

    @Delete
    suspend fun delete(partida: PartidaPrincipalEntity)

    // Obtiene todas las partidas principales, ordenadas por nombre, como un Flow.
    @Query("SELECT * FROM partidas_principales ORDER BY nombre ASC")
    fun getAll(): Flow<List<PartidaPrincipalEntity>>

    @Transaction // Esencial para que la operación de leer dos tablas sea atómica y segura.
    @Query("SELECT * FROM partidas_principales WHERE id = :id")
    fun getPartidaPrincipalWithDetails(id: Long): Flow<PartidaPrincipalWithDetails?>

    @Query("DELETE FROM partidas_principales")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(partidas: List<PartidaPrincipalEntity>)

    @Query("SELECT * FROM partidas_principales WHERE firebaseId = :firebaseId LIMIT 1")
    suspend fun getByFirebaseId(firebaseId: String): PartidaPrincipalEntity?

    @Query("SELECT * FROM partidas_principales WHERE sincronizadoConFirebase = 0")
    suspend fun getNoSincronizadas(): List<PartidaPrincipalEntity>

    @Query("SELECT * FROM partidas_principales WHERE id = :id")
    suspend fun getById(id: Long): PartidaPrincipalEntity?

}
