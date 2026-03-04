package com.ferji.inspecciones.data.repository

import android.util.Log
import androidx.room.withTransaction
import com.ferji.inspecciones.data.dao.PartidaDao
import com.ferji.inspecciones.data.dao.PartidaPrincipalDao
import com.ferji.inspecciones.data.database.AppDatabase
import com.ferji.inspecciones.data.model.DanoPartidaCrossRef
import com.ferji.inspecciones.data.model.PartidaEntity
import com.ferji.inspecciones.data.model.PartidaNaturaleza
import com.ferji.inspecciones.data.model.PartidaPrincipalEntity
import com.ferji.inspecciones.data.remote.PartidaRemoteDataSource
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PartidaRepository @Inject constructor(
    private val partidaDao: PartidaDao,
    private val partidaPrincipalDao: PartidaPrincipalDao,
    private val remoteDataSource: PartidaRemoteDataSource,
    private val database: AppDatabase
) {

    /**
     * Sincroniza el catálogo completo desde Firebase hacia la base de datos local.
     * Esta es una operación de "bajada" de datos.
     */
    suspend fun sincronizarCatalogoCompleto() {
        try {
            Log.d("SYNC_DOWNLOAD", "Iniciando sincronización de BAJADA...")
            val snapshotPrincipales = remoteDataSource.getCatalogoPartidasPrincipales()

            // Usamos una transacción para asegurar que todas las operaciones se completen
            // o ninguna lo haga, manteniendo la consistencia de los datos.
            database.withTransaction {
                for (docPrincipal in snapshotPrincipales.documents) {
                    val firebasePrincipal = docPrincipal.toPartidaPrincipalEntityManual() ?: continue
                    val localPrincipal = partidaPrincipalDao.getByFirebaseId(firebasePrincipal.firebaseId)

                    // Guarda o actualiza la partida principal y obtiene su ID local
                    val idPadreLocal = partidaPrincipalDao.upsert(
                        if (localPrincipal != null) firebasePrincipal.copy(id = localPrincipal.id) else firebasePrincipal
                    )
                    if (idPadreLocal == -1L) continue // Si falla, salta a la siguiente

                    // Descarga y guarda las partidas hijas
                    val snapshotDetalle = remoteDataSource.getPartidasHijas(docPrincipal)
                    for (docDetalle in snapshotDetalle.documents) {
                        val firebaseDetalle = docDetalle.toPartidaEntityManual() ?: continue
                        val localDetalle = partidaDao.getByFirebaseId(firebaseDetalle.firebaseId)
                        val detalleParaGuardar = firebaseDetalle.copy(
                            partidaPrincipalId = idPadreLocal,
                            id = localDetalle?.id ?: 0L
                        )
                        partidaDao.upsert(detalleParaGuardar)
                    }
                }
            }
            Log.d("SYNC_DOWNLOAD", "Sincronización de BAJADA finalizada con éxito.")
        } catch (e: Exception) {
            Log.e("SYNC_DOWNLOAD", "Error CRÍTICO durante la sincronización de BAJADA.", e)
            throw e // Relanzamos para que el ViewModel pueda capturarlo y notificar al usuario.
        }
    }

    /**
     * Sube todos los cambios locales (partidas no sincronizadas) a Firebase.
     * Esta es una operación de "subida" de datos, típicamente ejecutada en segundo plano.
     */
    suspend fun subirTodosLosCambios() {
        Log.d("SYNC_UPLOAD", "Iniciando subida de cambios locales...")
        subirPartidasPrincipalesLocales()
        subirPartidasHijasLocales()
        Log.d("SYNC_UPLOAD", "Proceso de subida finalizado.")
    }

    private suspend fun subirPartidasPrincipalesLocales() {
        val partidasParaSubir = partidaPrincipalDao.getNoSincronizadas()
        if (partidasParaSubir.isEmpty()) {
            Log.d("SYNC_UPLOAD_PADRES", "No hay partidas principales nuevas para subir.")
            return
        }

        Log.d("SYNC_UPLOAD_PADRES", "Subiendo ${partidasParaSubir.size} partidas principales...")
        for (partidaLocal in partidasParaSubir) {
            try {
                val firebaseIdGenerado = remoteDataSource.subirPartidaPrincipal(partidaLocal)
                val partidaActualizada = partidaLocal.copy(
                    firebaseId = firebaseIdGenerado,
                    sincronizadoConFirebase = true
                )
                partidaPrincipalDao.upsert(partidaActualizada)
                Log.d("SYNC_UPLOAD_PADRES", "Partida principal local ${partidaLocal.id} subida. Firebase ID: $firebaseIdGenerado")
            } catch (e: Exception) {
                Log.e("SYNC_UPLOAD_PADRES", "Error al subir partida principal local id: ${partidaLocal.id}", e)
            }
        }
    }

    private suspend fun subirPartidasHijasLocales() {
        val partidasHijasParaSubir = partidaDao.getNoSincronizadas()
        if (partidasHijasParaSubir.isEmpty()) {
            Log.d("SYNC_UPLOAD_HIJOS", "No hay partidas hijas nuevas para subir.")
            return
        }

        Log.d("SYNC_UPLOAD_HIJOS", "Subiendo ${partidasHijasParaSubir.size} partidas hijas...")
        for (partidaHijaLocal in partidasHijasParaSubir) {
            try {
                val padreLocal = partidaPrincipalDao.getById(partidaHijaLocal.partidaPrincipalId)
                if (padreLocal == null || padreLocal.firebaseId.isBlank()) {
                    Log.w("SYNC_UPLOAD_HIJOS", "No se puede subir partida hija ${partidaHijaLocal.id}. Su padre no existe o no está sincronizado.")
                    continue
                }

                val firebaseIdGenerado = remoteDataSource.subirPartidaHija(padreLocal.firebaseId, partidaHijaLocal)
                val partidaHijaActualizada = partidaHijaLocal.copy(
                    firebaseId = firebaseIdGenerado,
                    sincronizadoConFirebase = true
                )
                partidaDao.upsert(partidaHijaActualizada)
                Log.d("SYNC_UPLOAD_HIJOS", "Partida hija ${partidaHijaLocal.id} subida a padre ${padreLocal.firebaseId}. Firebase ID: $firebaseIdGenerado")
            } catch (e: Exception) {
                Log.e("SYNC_UPLOAD_HIJOS", "Error al subir partida hija local id: ${partidaHijaLocal.id}", e)
            }
        }
    }

    // --- FUNCIONES DAO: Simplemente delegan la llamada al DAO correspondiente ---

    fun getPartidasDePrincipal(idPadre: Long): Flow<List<PartidaEntity>> = partidaDao.getPartidasDePrincipal(idPadre)
    suspend fun upsertPartida(partida: PartidaEntity) = partidaDao.upsert(partida)
    suspend fun deletePartida(partida: PartidaEntity) = partidaDao.deletePartida(partida)
    fun getAllPartidasPrincipales(): Flow<List<PartidaPrincipalEntity>> = partidaPrincipalDao.getAll()
    suspend fun upsertPartidaPrincipal(partida: PartidaPrincipalEntity) = partidaPrincipalDao.upsert(partida)
    suspend fun deletePartidaPrincipal(partida: PartidaPrincipalEntity) = partidaPrincipalDao.delete(partida)
    fun getPartidasForDano(claveDano: String): Flow<List<PartidaEntity>> = partidaDao.getPartidasForDano(claveDano)
    suspend fun addDanoPartidaCrossRef(crossRef: DanoPartidaCrossRef) = partidaDao.addDanoPartidaCrossRef(crossRef)
    suspend fun removeDanoPartidaCrossRef(crossRef: DanoPartidaCrossRef) = partidaDao.removeDanoPartidaCrossRef(crossRef)
}

// --- FUNCIONES DE MAPEO (Ya las tenías, se mantienen igual) ---

private fun DocumentSnapshot.toPartidaPrincipalEntityManual(): PartidaPrincipalEntity? {
    return try {
        PartidaPrincipalEntity(
            firebaseId = id,
            nombre = getString("nombre") ?: "",
            tipoSuperficie = getString("tipoSuperficie") ?: "",
            naturaleza = getString("naturaleza")?.let { PartidaNaturaleza.valueOf(it) } ?: PartidaNaturaleza.VARIABLE,
            sincronizadoConFirebase = true
        )
    } catch (e: Exception) { null }
}
private fun DocumentSnapshot.toPartidaEntityManual(): PartidaEntity? {
    return try {
        PartidaEntity(
            firebaseId = id,
            partidaPrincipalId = 0L, // Se asigna correctamente durante la transacción
            descripcion = getString("descripcion") ?: "",
            unidad = getString("unidad") ?: "",
            precioUnitario = getDouble("precio_unitario") ?: 0.0,
            codigo = get("codigo")?.toString(),
            sincronizadoConFirebase = true
        )
    } catch (e: Exception) { null }
}
