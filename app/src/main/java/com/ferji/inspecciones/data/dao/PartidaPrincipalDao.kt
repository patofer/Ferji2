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

    // Obtiene solo las partidas principales VARIABLES (excluye las FIJAS/globales)
    @Query("SELECT * FROM partidas_principales WHERE naturaleza = 'VARIABLE' ORDER BY nombre ASC")
    fun getAllVariables(): Flow<List<PartidaPrincipalEntity>>

    @Transaction // Esencial para que la operación de leer dos tablas sea atómica y segura.
    @Query("SELECT * FROM partidas_principales WHERE id = :id")
    fun getPartidaPrincipalWithDetails(id: Long): Flow<PartidaPrincipalWithDetails?>

    // NOTA: se eliminó deleteAll() a propósito para evitar borrados masivos accidentales.
    // La eliminación de partidas principales solo debe hacerse explícitamente por el usuario desde el mantenedor.

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(partidas: List<PartidaPrincipalEntity>)

    @Query("SELECT * FROM partidas_principales WHERE firebaseId = :firebaseId LIMIT 1")
    suspend fun getByFirebaseId(firebaseId: String): PartidaPrincipalEntity?

    @Query("SELECT * FROM partidas_principales WHERE sincronizadoConFirebase = 0")
    suspend fun getNoSincronizadas(): List<PartidaPrincipalEntity>

    /**
     * Devuelve las partidas principales que ya fueron subidas a Firebase alguna vez
     * (excluye las locales con firebaseId vacío o temporal "local_...").
     */
    @Query("SELECT * FROM partidas_principales WHERE firebaseId != '' AND firebaseId NOT LIKE 'local\\_%' ESCAPE '\\'")
    suspend fun getAllSincronizadas(): List<PartidaPrincipalEntity>

    @Query("SELECT * FROM partidas_principales WHERE id = :id")
    suspend fun getById(id: Long): PartidaPrincipalEntity?

    @Query("SELECT * FROM partidas_principales WHERE naturaleza = 'FIJA'")
    suspend fun getPartidasFijas(): List<PartidaPrincipalEntity>

    @Query("SELECT * FROM partidas_principales WHERE nombre = :nombre LIMIT 1")
    suspend fun getByNombre(nombre: String): PartidaPrincipalEntity?

}
