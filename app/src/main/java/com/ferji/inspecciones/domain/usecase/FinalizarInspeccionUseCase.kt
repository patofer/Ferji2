package com.ferji.inspecciones.domain.usecase

import com.ferji.inspecciones.data.repository.InspeccionRepository
import javax.inject.Inject

/**
 * Caso de uso: Finalizar una inspección marcándola como COMPLETADA.
 *
 * Separa la lógica de negocio del ViewModel, haciendo el código más
 * testeable y reutilizable.
 */
class FinalizarInspeccionUseCase @Inject constructor(
    private val inspeccionRepository: InspeccionRepository
) {
    /**
     * Marca la inspección como completada.
     * @throws IllegalStateException si la inspección no existe.
     */
    suspend operator fun invoke(inspeccionId: Long) {
        val inspeccion = inspeccionRepository.getInspeccionById(inspeccionId)
            ?: throw IllegalStateException("No se encontró la inspección con ID: $inspeccionId")

        inspeccionRepository.actualizarEstado(inspeccionId, "COMPLETADA")
    }
}

