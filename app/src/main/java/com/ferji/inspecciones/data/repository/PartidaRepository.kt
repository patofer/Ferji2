// En: com/ferji/inspecciones/data/repository/PartidaRepository.kt
package com.ferji.inspecciones.data.repository

import com.ferji.inspecciones.data.dao.PartidaDao
import com.ferji.inspecciones.data.model.DanoPartidaCrossRef
import com.ferji.inspecciones.data.model.PartidaEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PartidaRepository @Inject constructor(private val partidaDao: PartidaDao) {

    fun getAllPartidas(): Flow<List<PartidaEntity>> = partidaDao.getAllPartidas()

    suspend fun insertPartida(partida: PartidaEntity): Long = partidaDao.insertPartida(partida)

    suspend fun updatePartida(partida: PartidaEntity) = partidaDao.updatePartida(partida)

    suspend fun deletePartida(partida: PartidaEntity) = partidaDao.deletePartida(partida)

    fun getPartidasForDano(claveDano: String): Flow<List<PartidaEntity>> =
        partidaDao.getPartidasForDano(claveDano)

    suspend fun addDanoPartidaCrossRef(crossRef: DanoPartidaCrossRef) =
        partidaDao.addDanoPartidaCrossRef(crossRef)

    suspend fun removeDanoPartidaCrossRef(crossRef: DanoPartidaCrossRef) =
        partidaDao.removeDanoPartidaCrossRef(crossRef)

    suspend fun upsertPartida(partida: PartidaEntity) {
        partidaDao.upsertPartida(partida)
    }


}
