package com.ferji.inspecciones.data.repository

import com.ferji.inspecciones.data.dao.HabitacionDao
import com.ferji.inspecciones.data.model.HabitacionEntity
import kotlinx.coroutines.flow.Flow

class HabitacionRepository(private val habitacionDao: HabitacionDao) {
    suspend fun insertHabitacion(habitacion: HabitacionEntity): Long {
        return habitacionDao.insert(habitacion)
    }

    fun getHabitacionesByInspeccion(inspeccionId: Long): Flow<List<HabitacionEntity>> {
        return habitacionDao.getHabitacionesByInspeccion(inspeccionId)
    }

    suspend fun updateHabitacion(habitacion: HabitacionEntity) {
        habitacionDao.update(habitacion)
    }
}