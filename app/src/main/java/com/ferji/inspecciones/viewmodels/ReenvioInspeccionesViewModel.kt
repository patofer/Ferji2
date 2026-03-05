package com.ferji.inspecciones.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ferji.inspecciones.data.model.InspeccionEntity
import com.ferji.inspecciones.data.repository.HabitacionRepository
import com.ferji.inspecciones.data.repository.InspeccionRepository
import com.ferji.inspecciones.data.repository.PartidaRepository
import com.ferji.inspecciones.utils.ExcelGenerator
import com.ferji.inspecciones.utils.PdfGenerator
import com.ferji.inspecciones.utils.email.EmailService
import com.ferji.inspecciones.utils.esEmailValido
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReenvioInspeccionesViewModel @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val inspeccionRepository: InspeccionRepository,
    private val habitacionRepository: HabitacionRepository,
    private val partidaRepository: PartidaRepository,
    private val emailService: EmailService
) : ViewModel() {

    companion object {
        private const val TAG = "ReenvioInspVM"
    }

    // Estado de búsqueda
    private val _textoBusqueda = MutableStateFlow("")
    val textoBusqueda: StateFlow<String> = _textoBusqueda.asStateFlow()

    // Lista de todas las inspecciones
    private val _todasLasInspecciones = inspeccionRepository.getAllInspecciones()

    // Lista filtrada combinando búsqueda + datos
    val inspeccionesFiltradas: StateFlow<List<InspeccionEntity>> = combine(
        _todasLasInspecciones,
        _textoBusqueda
    ) { inspecciones, filtro ->
        if (filtro.isBlank()) {
            inspecciones
        } else {
            val filtroLower = filtro.lowercase()
            inspecciones.filter { insp ->
                insp.siniestro.lowercase().contains(filtroLower) ||
                insp.rutInspector.lowercase().contains(filtroLower) ||
                insp.direccion.lowercase().contains(filtroLower) ||
                insp.rut.lowercase().contains(filtroLower)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Estado del reenvío
    data class ReenvioState(
        val isLoading: Boolean = false,
        val inspeccionIdEnProceso: Long? = null,
        val mensaje: String? = null,
        val isError: Boolean = false
    )

    private val _reenvioState = MutableStateFlow(ReenvioState())
    val reenvioState: StateFlow<ReenvioState> = _reenvioState.asStateFlow()

    fun onBusquedaChange(texto: String) {
        _textoBusqueda.value = texto
    }

    fun limpiarMensaje() {
        _reenvioState.value = _reenvioState.value.copy(mensaje = null)
    }

    /**
     * Genera PDF + Excel y envía email para la inspección seleccionada.
     */
    fun reenviarInspeccion(inspeccion: InspeccionEntity) {
        if (_reenvioState.value.isLoading) return // Evitar doble clic

        viewModelScope.launch {
            _reenvioState.value = ReenvioState(isLoading = true, inspeccionIdEnProceso = inspeccion.id)

            try {
                Log.d(TAG, "Reenviando inspección ID=${inspeccion.id}, Siniestro=${inspeccion.siniestro}")

                // 1. Obtener habitaciones
                val habitaciones = habitacionRepository.getHabitacionesPorInspeccionId(inspeccion.id)
                Log.d(TAG, "Habitaciones: ${habitaciones.size}")

                // 2. Generar PDF
                val pdfResult = PdfGenerator.createPdf(
                    applicationContext, inspeccion, habitaciones, partidaRepository
                )
                if (pdfResult == null) {
                    _reenvioState.value = ReenvioState(mensaje = "Error al generar el PDF.", isError = true)
                    return@launch
                }

                val pdfUri: Uri = when {
                    pdfResult.uri != null -> pdfResult.uri
                    pdfResult.file != null -> FileProvider.getUriForFile(
                        applicationContext,
                        "${applicationContext.packageName}.provider",
                        pdfResult.file
                    )
                    else -> {
                        _reenvioState.value = ReenvioState(mensaje = "No se pudo obtener URI del PDF.", isError = true)
                        return@launch
                    }
                }
                Log.d(TAG, "PDF generado: ${pdfResult.fileName}")

                // 3. Generar Excel
                val excelGenerator = ExcelGenerator(applicationContext)
                val excelResult = excelGenerator.generarPresupuesto(inspeccion, habitaciones, partidaRepository)
                Log.d(TAG, "Excel: ${excelResult?.fileName ?: "No generado"}")

                // 4. Preparar adjuntos
                val adjuntos = mutableListOf<EmailService.Adjunto>()
                adjuntos.add(
                    EmailService.Adjunto(
                        uri = pdfUri,
                        nombreArchivo = "Inspeccion_${inspeccion.siniestro}_${inspeccion.rut}.pdf",
                        mimeType = "application/pdf"
                    )
                )
                if (excelResult?.uri != null) {
                    adjuntos.add(
                        EmailService.Adjunto(
                            uri = excelResult.uri,
                            nombreArchivo = excelResult.fileName,
                            mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        )
                    )
                }

                // 5. Enviar email
                val destinatario = inspeccion.mail
                if (!destinatario.esEmailValido()) {
                    _reenvioState.value = ReenvioState(
                        mensaje = "Email inválido: $destinatario", isError = true
                    )
                    return@launch
                }

                val asunto = "Informe Inspección: Siniestro ${inspeccion.siniestro} - RUT ${inspeccion.rut}"
                val cuerpoHtml = """
                    <html><body>
                    <p>Estimado/a,</p>
                    <p>Adjunto encontrará el informe de la inspección y el presupuesto de reparación
                    para el <strong>siniestro N° ${inspeccion.siniestro}</strong>.</p>
                    <table border="1" cellpadding="6" cellspacing="0">
                        <tr><th>RUT Cliente</th><td>${inspeccion.rut}</td></tr>
                        <tr><th>Dirección</th><td>${inspeccion.direccion}</td></tr>
                        <tr><th>Inspector Asignado</th><td>${inspeccion.rutInspector}</td></tr>
                    </table>
                    <br/>
                    <p><strong>Documentos adjuntos:</strong></p>
                    <ul>
                        <li>Informe de Inspección (PDF)</li>
                        ${if (excelResult?.uri != null) "<li>Presupuesto de Reparación (Excel)</li>" else ""}
                    </ul>
                    <p>Saludos cordiales,<br/><strong>Equipo Ferji Inspecciones</strong></p>
                    </body></html>
                """.trimIndent()

                val cc = listOf("patriciofernande@gmail.com")
                    .filterNot { it.equals(destinatario, ignoreCase = true) }
                    .ifEmpty { null }

                val resultado = emailService.enviarConAdjuntos(
                    destinatarios = listOf(destinatario),
                    cc = cc,
                    asunto = asunto,
                    cuerpoHtml = cuerpoHtml,
                    adjuntos = adjuntos
                )

                when (resultado) {
                    is EmailService.EmailResult.Success -> {
                        Log.i(TAG, "Email enviado a $destinatario")
                        _reenvioState.value = ReenvioState(
                            mensaje = "✅ Enviado a $destinatario\nSiniestro: ${inspeccion.siniestro}",
                            isError = false
                        )
                    }
                    is EmailService.EmailResult.Error -> {
                        Log.e(TAG, "Error: ${resultado.message}")
                        _reenvioState.value = ReenvioState(
                            mensaje = "Error al enviar: ${resultado.message}",
                            isError = true
                        )
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Excepción: ${e.message}", e)
                _reenvioState.value = ReenvioState(
                    mensaje = "Error: ${e.message}",
                    isError = true
                )
            }
        }
    }
}

