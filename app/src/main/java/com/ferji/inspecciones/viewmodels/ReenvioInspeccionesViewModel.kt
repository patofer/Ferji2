package com.ferji.inspecciones.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ferji.inspecciones.data.model.InspeccionEntity
import com.ferji.inspecciones.data.repository.EmailSettingsRepository
import com.ferji.inspecciones.data.repository.HabitacionRepository
import com.ferji.inspecciones.data.repository.InspeccionRepository
import com.ferji.inspecciones.data.repository.PartidaRepository
import com.ferji.inspecciones.utils.PdfGenerator
import com.ferji.inspecciones.utils.ExcelGenerator
import com.ferji.inspecciones.domain.model.AppResult
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
    private val emailService: EmailService,
    private val emailSettingsRepository: EmailSettingsRepository
) : ViewModel() {

    companion object {
        private const val TAG = "ReenvioInspVM"
    }

    // Estado de búsqueda
    private val _textoBusqueda = MutableStateFlow("")
    val textoBusqueda: StateFlow<String> = _textoBusqueda.asStateFlow()

    // Lista solo de inspecciones completadas
    private val _todasLasInspecciones = inspeccionRepository.getInspeccionesByEstado("COMPLETADA")

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
     * Incluye: PDF (siempre), Presupuesto Excel (siempre), Fotos (según configuración).
     */
    fun reenviarInspeccion(inspeccion: InspeccionEntity) {
        if (_reenvioState.value.isLoading) return

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

                // 3. Cargar configuración de emails desde Firestore
                val emailSettings = emailSettingsRepository.obtenerConfiguracion()
                Log.d(TAG, "Configuración de email: $emailSettings")

                // 4. Determinar destinatarios
                val destinatarios = mutableListOf<String>()
                if (emailSettings.emailAdmin.isNotBlank() && emailSettings.emailAdmin.esEmailValido()) {
                    destinatarios.add(emailSettings.emailAdmin)
                }
                // En reenvío, siempre incluir al inspector
                val emailInspector = inspeccion.mail
                if (emailInspector.esEmailValido() && !destinatarios.contains(emailInspector)) {
                    destinatarios.add(emailInspector)
                }

                if (destinatarios.isEmpty()) {
                    _reenvioState.value = ReenvioState(
                        mensaje = "No hay destinatarios configurados.", isError = true
                    )
                    return@launch
                }

                // CC
                val ccList = mutableListOf<String>()
                if (emailSettings.emailCc.isNotBlank() && emailSettings.emailCc.esEmailValido()) {
                    if (!destinatarios.contains(emailSettings.emailCc)) {
                        ccList.add(emailSettings.emailCc)
                    }
                }
                val cc = ccList.ifEmpty { null }

                // 5. Preparar adjuntos: PDF + Excel + Fotos
                val adjuntos = mutableListOf<EmailService.Adjunto>()

                // PDF siempre se adjunta
                adjuntos.add(
                    EmailService.Adjunto(
                        uri = pdfUri,
                        nombreArchivo = "Inspeccion_${inspeccion.siniestro}_${inspeccion.rut}.pdf",
                        mimeType = "application/pdf"
                    )
                )

                // Generar Excel (presupuesto)
                val excelGenerator = ExcelGenerator(applicationContext)
                val excelResult = excelGenerator.generarPresupuesto(inspeccion, habitaciones, partidaRepository)
                val adjuntoExcel = when (excelResult) {
                    is AppResult.Success -> {
                        if (excelResult.data.uri != null) {
                            EmailService.Adjunto(
                                uri = excelResult.data.uri,
                                nombreArchivo = excelResult.data.fileName,
                                mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                            )
                        } else null
                    }
                    else -> null
                }
                if (adjuntoExcel != null) {
                    adjuntos.add(adjuntoExcel)
                    Log.d(TAG, "Excel adjuntado: ${adjuntoExcel.nombreArchivo}")
                } else {
                    Log.w(TAG, "No se pudo generar el presupuesto Excel para reenvío")
                }

                // Adjuntar fotos si la configuración lo permite
                if (emailSettings.enviarImagenesAlInspector) {
                    val siniestroLimpio = inspeccion.siniestro.replace("[^a-zA-Z0-9]".toRegex(), "")
                    for (hab in habitaciones) {
                        val fotos = hab.getFotosList()
                        val nombreHab = hab.nombre.replace("[^a-zA-Z0-9]".toRegex(), "_")
                        fotos.forEachIndexed { idx, fotoPath ->
                            try {
                                val file = java.io.File(fotoPath)
                                if (file.exists()) {
                                    val extension = file.extension.ifBlank { "jpg" }
                                    val nombreFoto = "${nombreHab}_${siniestroLimpio}_${idx + 1}.$extension"
                                    val fotoUri = FileProvider.getUriForFile(
                                        applicationContext,
                                        "${applicationContext.packageName}.provider",
                                        file
                                    )
                                    adjuntos.add(
                                        EmailService.Adjunto(
                                            uri = fotoUri,
                                            nombreArchivo = nombreFoto,
                                            mimeType = "image/$extension"
                                        )
                                    )
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error preparando foto '$fotoPath': ${e.message}")
                            }
                        }
                    }
                    Log.d(TAG, "Total fotos adjuntadas: ${adjuntos.size - (if (adjuntoExcel != null) 2 else 1)}")
                }

                // Determinar qué documentos se adjuntaron para el cuerpo del email
                val tieneExcel = adjuntoExcel != null
                val numFotos = adjuntos.count { it.mimeType.startsWith("image/") }

                val asunto = "Informe Inspección: Siniestro ${inspeccion.siniestro} - RUT ${inspeccion.rut}"
                val cuerpoHtml = """
                    <html><body>
                    <p>Estimado/a,</p>
                    <p>Adjunto encontrará el informe de la inspección${if (tieneExcel) " y el presupuesto de reparación" else ""}
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
                        ${if (tieneExcel) "<li>Presupuesto de Reparación (Excel)</li>" else ""}
                        ${if (numFotos > 0) "<li>Fotografías de la inspección ($numFotos imágenes)</li>" else ""}
                    </ul>
                    <p>Saludos cordiales,<br/><strong>Equipo Ferji Inspecciones</strong></p>
                    </body></html>
                """.trimIndent()

                Log.d(TAG, "Enviando email a: $destinatarios, CC: $cc")
                val resultado = emailService.enviarConAdjuntos(
                    destinatarios = destinatarios,
                    cc = cc,
                    asunto = asunto,
                    cuerpoHtml = cuerpoHtml,
                    adjuntos = adjuntos
                )

                when (resultado) {
                    is EmailService.EmailResult.Success -> {
                        Log.i(TAG, "Email enviado a $destinatarios")
                        _reenvioState.value = ReenvioState(
                            mensaje = "✅ Inspección enviada correctamente\nSiniestro: ${inspeccion.siniestro}",
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

