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
}