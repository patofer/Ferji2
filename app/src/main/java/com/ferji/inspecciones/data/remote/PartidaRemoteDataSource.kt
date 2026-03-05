package com.ferji.inspecciones.data.remote

import androidx.work.await
import com.ferji.inspecciones.data.model.PartidaEntity
import com.ferji.inspecciones.data.model.PartidaPrincipalEntity
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Esta clase es la única responsable de comunicarse con Firebase (Firestore).
 * Centraliza toda la lógica de red, manteniendo el repositorio limpio.
 */
@Singleton
class PartidaRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore // Hilt ya sabe cómo proveer esto
) {
    private val partidaPrincipalCollection = firestore.collection("partidas_principales")

    /**
     * Sube una PartidaPrincipal a Firestore y devuelve el ID generado.
     */
    suspend fun subirPartidaPrincipal(partida: PartidaPrincipalEntity): String {
        val documentReference = partidaPrincipalCollection.add(partida).await()
        return documentReference.id
    }

    /**
     * Actualiza una PartidaPrincipal existente en Firestore usando su firebaseId.
     */
    suspend fun actualizarPartidaPrincipal(firebaseId: String, partida: PartidaPrincipalEntity) {
        val datos = mapOf(
            "nombre" to partida.nombre,
            "tipoSuperficie" to partida.tipoSuperficie,
            "naturaleza" to partida.naturaleza.name
        )
        partidaPrincipalCollection.document(firebaseId).update(datos).await()
    }

    /**
     * Sube una PartidaEntity a la subcolección de su padre en Firestore y devuelve el ID generado.
     */
    suspend fun subirPartidaHija(idPadreFirebase: String, partidaHija: PartidaEntity): String {
        val subcoleccionPadreRef = partidaPrincipalCollection.document(idPadreFirebase).collection("partidas")
        val documentReference = subcoleccionPadreRef.add(partidaHija).await()
        return documentReference.id
    }

    suspend fun getCatalogoPartidasPrincipales(): QuerySnapshot {
        return partidaPrincipalCollection.get().await()
    }

    /**
     * Descarga las partidas hijas de una partida principal específica.
     */
    suspend fun getPartidasHijas(documentoPrincipal: DocumentSnapshot): QuerySnapshot {
        return documentoPrincipal.reference.collection("partidas").get().await()
    }
}
