// En: com/ferji/inspecciones/data/dao/PartidaDao.kt
package com.ferji.inspecciones.data.dao

import androidx.room.*
import com.ferji.inspecciones.data.model.DanoPartidaCrossRef
import com.ferji.inspecciones.data.model.PartidaEntity
import com.ferji.inspecciones.data.model.PartidaPrincipalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PartidaDao {
    // --- Gestión de Partidas ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPartida(partida: PartidaEntity): Long

    @Update
    suspend fun updatePartida(partida: PartidaEntity)

    @Delete
    suspend fun deletePartida(partida: PartidaEntity)

    @Query("SELECT * FROM partidas ORDER BY descripcion ASC")
    fun getAllPartidas(): Flow<List<PartidaEntity>>

    @Query("SELECT * FROM partidas WHERE id = :id")
    suspend fun getPartidaById(id: Long): PartidaEntity?

    // --- Gestión de Relaciones Dano-Partida ---
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addDanoPartidaCrossRef(crossRef: DanoPartidaCrossRef)

    @Delete
    suspend fun removeDanoPartidaCrossRef(crossRef: DanoPartidaCrossRef)

    @Transaction
    @Query("""
            SELECT P.* FROM partidas P
            INNER JOIN danos_partidas_cross_ref C ON P.id = C.partidaId
            WHERE C.claveDano = :claveDano
        """)
    fun getPartidasForDano(claveDano: String): Flow<List<PartidaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPartida(partida: PartidaEntity)

    // --- AÑADIR ESTE MÉTODO ---
    @Query("SELECT * FROM partidas WHERE partida_principal_id = :partidaPrincipalId")
    fun getPartidasByPrincipalId(partidaPrincipalId: Long): Flow<List<PartidaEntity>>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(partidas: List<PartidaEntity>) // <-- AÑADIR

    @Query("DELETE FROM partidas")
    suspend fun deleteAll() // <-- AÑADIR


    @Query("SELECT * FROM partidas WHERE firebaseId = :firebaseId LIMIT 1")
    suspend fun getByFirebaseId(firebaseId: String): PartidaEntity?

    @Upsert
    suspend fun upsert(partida: PartidaEntity)

    @Query("SELECT * FROM partidas WHERE sincronizadoConFirebase = 0")
    suspend fun getNoSincronizadas(): List<PartidaEntity>


    @Query("SELECT * FROM partidas WHERE partida_principal_id = :idPadre")
    fun getPartidasDePrincipal(idPadre: Long): Flow<List<PartidaEntity>>

    @Query("SELECT * FROM partidas WHERE partida_principal_id = :idPadre AND eliminado = 0")
    suspend fun getPartidasDePrincipalSuspend(idPadre: Long): List<PartidaEntity>

    @Query("SELECT * FROM partidas WHERE eliminado = 1")
    suspend fun getEliminadas(): List<PartidaEntity>

    @Transaction
    @Query("""
        SELECT P.* FROM partidas P
        INNER JOIN danos_partidas_cross_ref C ON P.id = C.partidaId
        WHERE C.claveDano = :claveDano
    """)
    suspend fun getPartidasForDanoSuspend(claveDano: String): List<PartidaEntity>

    @Query("SELECT * FROM partidas_principales WHERE id = :id")
    suspend fun getPartidaPrincipalById(id: Long): PartidaPrincipalEntity?
}
