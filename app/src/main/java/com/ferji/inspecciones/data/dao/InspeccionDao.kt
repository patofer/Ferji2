package com.ferji.inspecciones.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ferji.inspecciones.data.model.InspeccionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InspeccionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(inspeccion: InspeccionEntity): Long

    @Query("SELECT * FROM inspecciones ORDER BY fecha_creacion DESC")
    fun getAllInspecciones(): Flow<List<InspeccionEntity>>

    @Query("SELECT * FROM inspecciones WHERE id = :id")
    suspend fun getInspeccionById(id: Long): InspeccionEntity?

    @Query("SELECT * FROM inspecciones WHERE estado = :estado ORDER BY fecha_creacion DESC")
    fun getInspeccionesByEstado(estado: String): Flow<List<InspeccionEntity>>

    @Query("SELECT * FROM inspecciones ORDER BY fecha_creacion DESC LIMIT 1")
    suspend fun getUltimaInspeccion(): InspeccionEntity?

    @Query("UPDATE inspecciones SET estado = :nuevoEstado WHERE id = :id")
    suspend fun actualizarEstado(id: Long, nuevoEstado: String)

    /** Marca una inspección como sincronizada con Firebase */
    @Query("UPDATE inspecciones SET firebase_id = :firebaseId, sincronizado_firebase = 1 WHERE id = :id")
    suspend fun marcarComoSincronizada(id: Long, firebaseId: String)

    /** Obtiene inspecciones que aún no se han subido a Firebase (pendientes y completadas) */
    @Query("SELECT * FROM inspecciones WHERE sincronizado_firebase = 0")
    suspend fun getInspeccionesNoSincronizadas(): List<InspeccionEntity>

    /** Actualiza el total del presupuesto de una inspección */
    @Query("UPDATE inspecciones SET total_presupuesto = :total WHERE id = :id")
    suspend fun actualizarTotalPresupuesto(id: Long, total: Double)

    /** Elimina una inspección por su ID (las habitaciones se borran por CASCADE) */
    @Query("DELETE FROM inspecciones WHERE id = :id")
    suspend fun deleteById(id: Long)
}