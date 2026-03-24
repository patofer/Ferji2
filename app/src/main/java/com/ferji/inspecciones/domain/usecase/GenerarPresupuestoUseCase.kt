package com.ferji.inspecciones.domain.usecase

import com.ferji.inspecciones.data.model.HabitacionEntity
import com.ferji.inspecciones.data.model.InspeccionEntity
import com.ferji.inspecciones.data.repository.HabitacionRepository
import com.ferji.inspecciones.data.repository.InspeccionRepository
import com.ferji.inspecciones.data.repository.PartidaRepository
import javax.inject.Inject

/**
 * Caso de uso: Recopilar todos los datos necesarios para generar un presupuesto.
 *
 * Encapsula la lógica de negocio de obtener la inspección, habitaciones y
 * datos de partidas, separándola del ViewModel y de los generadores de archivos.
 *
 * Ventajas:
 * - Testeable de forma unitaria (sin depender de Context/ViewModel).
 * - Reutilizable por ExcelGenerator, PdfGenerator, o cualquier otro formato.
 * - El ViewModel se mantiene delgado y enfocado en la UI.
 */
class GenerarPresupuestoUseCase @Inject constructor(
    private val inspeccionRepository: InspeccionRepository,
    private val habitacionRepository: HabitacionRepository,
    private val partidaRepository: PartidaRepository
) {

    data class DatosPresupuesto(
        val inspeccion: InspeccionEntity,
        val habitaciones: List<HabitacionEntity>,
        val partidaRepository: PartidaRepository
    )

    /**
     * Obtiene todos los datos necesarios para generar presupuesto/PDF.
     * Lanza IllegalStateException si la inspección no existe.
     */
    suspend operator fun invoke(inspeccionId: Long): DatosPresupuesto {
        val inspeccion = inspeccionRepository.getInspeccionById(inspeccionId)
            ?: throw IllegalStateException("No se encontró la inspección con ID: $inspeccionId")

        val habitaciones = habitacionRepository.getHabitacionesPorInspeccionId(inspeccionId)

        return DatosPresupuesto(
            inspeccion = inspeccion,
            habitaciones = habitaciones,
            partidaRepository = partidaRepository
        )
    }
}

