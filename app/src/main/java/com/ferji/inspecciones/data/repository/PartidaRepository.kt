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
            Log.d("SYNC_DOWNLOAD", "Documentos principales recibidos de Firebase: ${snapshotPrincipales.documents.size}")

            database.withTransaction {
                for (docPrincipal in snapshotPrincipales.documents) {
                    // Log de datos crudos de Firebase
                    val rawNombre = docPrincipal.getString("nombre")
                    val rawNaturaleza = docPrincipal.getString("naturaleza")
                    val rawTipoSup = docPrincipal.getString("tipoSuperficie")
                    Log.d("SYNC_DOWNLOAD", "─── Doc Firebase ID: ${docPrincipal.id} ───")
                    Log.d("SYNC_DOWNLOAD", "  nombre='$rawNombre', naturaleza='$rawNaturaleza', tipoSuperficie='$rawTipoSup'")
                    Log.d("SYNC_DOWNLOAD", "  Todos los campos: ${docPrincipal.data}")

                    val firebasePrincipal = docPrincipal.toPartidaPrincipalEntityManual()
                    if (firebasePrincipal == null) {
                        Log.e("SYNC_DOWNLOAD", "  ⚠️ No se pudo mapear este documento. Se salta.")
                        continue
                    }
                    Log.d("SYNC_DOWNLOAD", "  Mapeado: nombre='${firebasePrincipal.nombre}', naturaleza=${firebasePrincipal.naturaleza}, tipo='${firebasePrincipal.tipoSuperficie}'")

                    val localPrincipal = partidaPrincipalDao.getByFirebaseId(firebasePrincipal.firebaseId)
                    Log.d("SYNC_DOWNLOAD", "  Existe local con firebaseId='${firebasePrincipal.firebaseId}'? ${localPrincipal != null} (localId=${localPrincipal?.id})")

                    val upsertResult = partidaPrincipalDao.upsert(
                        if (localPrincipal != null) firebasePrincipal.copy(id = localPrincipal.id) else firebasePrincipal
                    )
                    // Room @Upsert devuelve -1 cuando la operación resultó en UPDATE (no INSERT).
                    // En ese caso usamos el id local existente para seguir procesando las hijas.
                    val idPadreLocal: Long = when {
                        upsertResult > 0L -> upsertResult
                        localPrincipal != null -> localPrincipal.id
                        else -> {
                            Log.w("SYNC_DOWNLOAD", "  ⚠️ upsert devolvió $upsertResult y no hay registro local previo. Se salta.")
                            continue
                        }
                    }
                    Log.d("SYNC_DOWNLOAD", "  Guardado/Actualizado (upsertResult=$upsertResult) → idPadreLocal=$idPadreLocal")

                    val snapshotDetalle = remoteDataSource.getPartidasHijas(docPrincipal)
                    Log.d("SYNC_DOWNLOAD", "  Partidas hijas encontradas: ${snapshotDetalle.documents.size}")
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
                // NOTA: La limpieza automática de "huérfanos" (partidas locales que ya no están en Firebase)
                // fue removida intencionalmente para evitar borrados accidentales durante la sincronización.
                // La eliminación de partidas solo debe hacerse explícitamente por el usuario desde el mantenedor.
            }

            // Verificar lo que quedó en la BD local
            Log.d("SYNC_DOWNLOAD", "Sincronización de BAJADA finalizada con éxito.")
        } catch (e: Exception) {
            Log.e("SYNC_DOWNLOAD", "Error CRÍTICO durante la sincronización de BAJADA.", e)
            throw e
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
            Log.d("SYNC_UPLOAD_PADRES", "No hay partidas principales para subir/actualizar.")
            return
        }

        Log.d("SYNC_UPLOAD_PADRES", "Subiendo/Actualizando ${partidasParaSubir.size} partidas principales...")
        for (partidaLocal in partidasParaSubir) {
            try {
                if (partidaLocal.firebaseId.isNotBlank()) {
                    // Ya existe en Firebase → ACTUALIZAR
                    Log.d("SYNC_UPLOAD_PADRES", "Actualizando partida '${partidaLocal.nombre}' en Firebase (${partidaLocal.firebaseId})")
                    remoteDataSource.actualizarPartidaPrincipal(partidaLocal.firebaseId, partidaLocal)
                    val partidaActualizada = partidaLocal.copy(sincronizadoConFirebase = true)
                    partidaPrincipalDao.upsert(partidaActualizada)
                    Log.d("SYNC_UPLOAD_PADRES", "Partida '${partidaLocal.nombre}' actualizada en Firebase.")
                } else {
                    // Nueva → CREAR
                    val firebaseIdGenerado = remoteDataSource.subirPartidaPrincipal(partidaLocal)
                    val partidaActualizada = partidaLocal.copy(
                        firebaseId = firebaseIdGenerado,
                        sincronizadoConFirebase = true
                    )
                    partidaPrincipalDao.upsert(partidaActualizada)
                    Log.d("SYNC_UPLOAD_PADRES", "Partida '${partidaLocal.nombre}' creada en Firebase. ID: $firebaseIdGenerado")
                }
            } catch (e: Exception) {
                Log.e("SYNC_UPLOAD_PADRES", "Error al subir partida principal '${partidaLocal.nombre}' (id: ${partidaLocal.id})", e)
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

                // Si ya tiene un firebaseId REAL (no temporal "local_..."), es una EDICIÓN → UPDATE.
                // Si no, es una partida NUEVA → CREATE.
                val firebaseIdActual = partidaHijaLocal.firebaseId
                val esNueva = firebaseIdActual.isBlank() || firebaseIdActual.startsWith("local_")

                if (esNueva) {
                    val firebaseIdGenerado = remoteDataSource.subirPartidaHija(padreLocal.firebaseId, partidaHijaLocal)
                    val partidaHijaActualizada = partidaHijaLocal.copy(
                        firebaseId = firebaseIdGenerado,
                        sincronizadoConFirebase = true
                    )
                    partidaDao.upsert(partidaHijaActualizada)
                    Log.d("SYNC_UPLOAD_HIJOS", "Partida hija ${partidaHijaLocal.id} CREADA en Firebase bajo padre ${padreLocal.firebaseId}. FirebaseId: $firebaseIdGenerado")
                } else {
                    remoteDataSource.actualizarPartidaHija(padreLocal.firebaseId, firebaseIdActual, partidaHijaLocal)
                    val partidaHijaActualizada = partidaHijaLocal.copy(sincronizadoConFirebase = true)
                    partidaDao.upsert(partidaHijaActualizada)
                    Log.d("SYNC_UPLOAD_HIJOS", "Partida hija ${partidaHijaLocal.id} ACTUALIZADA en Firebase ($firebaseIdActual).")
                }
            } catch (e: Exception) {
                Log.e("SYNC_UPLOAD_HIJOS", "Error al subir/actualizar partida hija local id: ${partidaHijaLocal.id}", e)
            }
        }
    }

    // --- FUNCIONES DAO: Simplemente delegan la llamada al DAO correspondiente ---

    fun getPartidasDePrincipal(idPadre: Long): Flow<List<PartidaEntity>> = partidaDao.getPartidasDePrincipal(idPadre)
    suspend fun getPartidaById(id: Long): PartidaEntity? = partidaDao.getPartidaById(id)
    suspend fun upsertPartida(partida: PartidaEntity) = partidaDao.upsert(partida)
    suspend fun deletePartida(partida: PartidaEntity) = partidaDao.deletePartida(partida)
    fun getAllPartidasPrincipales(): Flow<List<PartidaPrincipalEntity>> = partidaPrincipalDao.getAll()
    suspend fun upsertPartidaPrincipal(partida: PartidaPrincipalEntity) = partidaPrincipalDao.upsert(partida)
    suspend fun deletePartidaPrincipal(partida: PartidaPrincipalEntity) = partidaPrincipalDao.delete(partida)
    fun getPartidasForDano(claveDano: String): Flow<List<PartidaEntity>> = partidaDao.getPartidasForDano(claveDano)
    suspend fun getPartidasForDanoSuspend(claveDano: String): List<PartidaEntity> = partidaDao.getPartidasForDanoSuspend(claveDano)
    suspend fun getPartidaPrincipalById(id: Long): PartidaPrincipalEntity? = partidaPrincipalDao.getById(id)
    suspend fun getPartidaPrincipalByNombre(nombre: String): PartidaPrincipalEntity? = partidaPrincipalDao.getByNombre(nombre)
    suspend fun getPartidasFijas(): List<PartidaPrincipalEntity> = partidaPrincipalDao.getPartidasFijas()
    suspend fun getPartidasDePrincipalSuspend(idPadre: Long): List<PartidaEntity> = partidaDao.getPartidasDePrincipalSuspend(idPadre)
    suspend fun addDanoPartidaCrossRef(crossRef: DanoPartidaCrossRef) = partidaDao.addDanoPartidaCrossRef(crossRef)
    suspend fun removeDanoPartidaCrossRef(crossRef: DanoPartidaCrossRef) = partidaDao.removeDanoPartidaCrossRef(crossRef)
}

// --- FUNCIONES DE MAPEO (Ya las tenías, se mantienen igual) ---

private fun DocumentSnapshot.toPartidaPrincipalEntityManual(): PartidaPrincipalEntity? {
    return try {
        val rawNaturaleza = getString("naturaleza")
        val naturaleza = try {
            rawNaturaleza?.uppercase()?.trim()?.let { PartidaNaturaleza.valueOf(it) } ?: PartidaNaturaleza.VARIABLE
        } catch (e: IllegalArgumentException) {
            Log.e("SYNC_MAP", "⚠️ Valor de naturaleza no reconocido: '$rawNaturaleza' para doc ${this.id}. Usando VARIABLE por defecto.")
            PartidaNaturaleza.VARIABLE
        }
        PartidaPrincipalEntity(
            firebaseId = id,
            nombre = getString("nombre") ?: "",
            tipoSuperficie = getString("tipoSuperficie") ?: "",
            naturaleza = naturaleza,
            sincronizadoConFirebase = true
        )
    } catch (e: Exception) {
        Log.e("SYNC_MAP", "Error mapeando documento ${this.id}: ${e.message}", e)
        null
    }
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
