package com.ferji.inspecciones.data.repository

import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.ferji.inspecciones.data.dao.InspeccionDao
import com.ferji.inspecciones.data.model.InspeccionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class InspeccionRepository @Inject constructor(
    private val inspeccionDao: InspeccionDao
) {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInspeccion(inspeccion: InspeccionEntity): Long {
        return inspeccionDao.insert(inspeccion)
    }

    fun getAllInspecciones(): Flow<List<InspeccionEntity>> {
        return inspeccionDao.getAllInspecciones()
    }

    suspend fun getInspeccionById(id: Long): InspeccionEntity? {
        return inspeccionDao.getInspeccionById(id)
    }

    fun getInspeccionesPendientes(): Flow<List<InspeccionEntity>> {
        return inspeccionDao.getInspeccionesByEstado("PENDIENTE")
    }
}