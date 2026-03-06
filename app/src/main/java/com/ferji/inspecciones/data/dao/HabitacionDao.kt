package com.ferji.inspecciones.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ferji.inspecciones.data.model.HabitacionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitacionDao {
    @Insert
    suspend fun insert(habitacion: HabitacionEntity): Long

    @Query("SELECT * FROM habitaciones WHERE inspeccion_id = :inspeccionId")
    fun getHabitacionesByInspeccion(inspeccionId: Long): Flow<List<HabitacionEntity>>

    @Update
    suspend fun update(habitacion: HabitacionEntity)

    @Delete
    suspend fun delete(habitacion: HabitacionEntity)

    @Query("DELETE FROM habitaciones WHERE inspeccion_id = :inspeccionId")
    suspend fun deleteByInspeccionId(inspeccionId: Long)

    @Query("SELECT * FROM habitaciones WHERE inspeccion_id = :idDeInspeccion")
    suspend fun getHabitacionesPorInspeccionId(idDeInspeccion: Long): List<HabitacionEntity>

    @Query("SELECT COUNT(*) FROM habitaciones WHERE inspeccion_id = :inspeccionId")
    suspend fun contarHabitaciones(inspeccionId: Long): Int
}