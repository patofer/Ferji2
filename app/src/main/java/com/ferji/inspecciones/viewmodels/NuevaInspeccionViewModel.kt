package com.ferji.inspecciones.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ferji.inspecciones.data.model.HabitacionEntity
import com.ferji.inspecciones.data.model.InspeccionEntity
import com.ferji.inspecciones.data.repository.EmailSettingsRepository
import com.ferji.inspecciones.data.repository.HabitacionRepository
import com.ferji.inspecciones.data.repository.InspeccionRepository
import com.ferji.inspecciones.data.repository.PartidaRepository
import com.ferji.inspecciones.data.repository.UserRepository
import com.ferji.inspecciones.ui.components.PdfGenerationResult
import com.ferji.inspecciones.ui.events.NuevaInspeccionScreenUiState
import com.ferji.inspecciones.ui.events.NuevaInspeccionUiEvent
import com.ferji.inspecciones.utils.email.EmailService
import com.ferji.inspecciones.utils.ExcelGenerator
import com.ferji.inspecciones.utils.PdfGenerator
import com.ferji.inspecciones.utils.esEmailValido
import com.ferji.inspecciones.utils.validarRutChileno
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class NuevaInspeccionViewModel @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val inspeccionRepository: InspeccionRepository,
    private val habitacionRepository: HabitacionRepository,
    private val partidaRepository: PartidaRepository,
    private val emailService: EmailService,
    private val userRepository: UserRepository,
    private val emailSettingsRepository: EmailSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NuevaInspeccionScreenUiState())
    val uiState: StateFlow<NuevaInspeccionScreenUiState> = _uiState.asStateFlow()

    private val _uiEvents = Channel<NuevaInspeccionUiEvent>()
    val uiEvents = _uiEvents.receiveAsFlow()

    private val _pdfGenerationStatus = MutableStateFlow<PdfGenerationResult>(PdfGenerationResult.Idle)
    val pdfGenerationStatus: StateFlow<PdfGenerationResult> = _pdfGenerationStatus.asStateFlow()

    // --- Campos de Formulario y Estados de Validación ---
    var rut by mutableStateOf("")
        private set
    var siniestro by mutableStateOf("")
        private set
    var direccion by mutableStateOf("")
        private set
    var rutInspector by mutableStateOf("")
        private set
    var mail by mutableStateOf("")
        private set

    var isMailValid by mutableStateOf(true)
        private set
    var isRutValid by mutableStateOf(true)
        private set
    var todosCamposLlenos by mutableStateOf(false)
        private set
    var isRutInspectorValid by mutableStateOf(true)
        private set

    private var currentInspeccionIdForPdf: Long? = null
    private var currentPdfUriForEmail: Uri? = null

    init {
        viewModelScope.launch {
            val sesionActual = userRepository.currentUserSession.firstOrNull()
            sesionActual?.let { user ->
                user.rut?.let { onRutInspectorChange(it) }
                user.email?.let { onMailChange(it) }
                Log.d("NuevaInspVM", "Campos inicializados desde sesión: RUT Inspector=${user.rut}, Email=${user.email}")
            }
        }
    }

    private fun actualizarTodosCamposLlenos() {
        val rutNoVacio = rut.isNotBlank()
        val rutFormatoValido = rut.validarRutChileno()
        val siniestroNoVacio = siniestro.isNotBlank()
        val direccionNoVacia = direccion.isNotBlank()
        val rutInspectorNoVacio = rutInspector.isNotBlank()
        val rutInspectorFormatoValido = rutInspector.validarRutChileno()
        val mailNoVacio = mail.isNotBlank()

        val condicionesCumplidas = rutNoVacio && rutFormatoValido &&
                siniestroNoVacio &&
                direccionNoVacia &&
                rutInspectorNoVacio && rutInspectorFormatoValido &&
                mailNoVacio && isMailValid

        if (todosCamposLlenos != condicionesCumplidas) {
            todosCamposLlenos = condicionesCumplidas
        }
    }

    fun onRutChange(nuevoRut: String) {
        rut = nuevoRut
        isRutValid = if (nuevoRut.isBlank()) true else nuevoRut.validarRutChileno()
        actualizarTodosCamposLlenos()
    }

    fun onSiniestroChange(newSiniestro: String) {
        siniestro = newSiniestro
        actualizarTodosCamposLlenos()
    }

    fun onDireccionChange(newDireccion: String) {
        direccion = newDireccion
        actualizarTodosCamposLlenos()
    }

    fun onRutInspectorChange(nuevoRutInspector: String) {
        rutInspector = nuevoRutInspector
        isRutInspectorValid = if (nuevoRutInspector.isBlank()) true else nuevoRutInspector.validarRutChileno()
        actualizarTodosCamposLlenos()
    }

    fun onMailChange(nuevoMail: String) {
        mail = nuevoMail
        isMailValid = nuevoMail.esEmailValido()
        actualizarTodosCamposLlenos()
    }

    fun guardarInspeccion() {
        if (!todosCamposLlenos) {
            viewModelScope.launch {
                _uiEvents.send(NuevaInspeccionUiEvent.ShowSnackbar("Por favor, complete todos los campos.", isError = true))
            }
            return
        }
        viewModelScope.launch {
            try {
                val nuevaInspeccion = InspeccionEntity(
                    rut = rut,
                    siniestro = siniestro,
                    direccion = direccion,
                    rutInspector = rutInspector,
                    mail = mail
                )
                val inspeccionId = inspeccionRepository.insertInspeccion(nuevaInspeccion)
                currentInspeccionIdForPdf = inspeccionId
                Log.d("NuevaInspVM", "Inspección guardada, ID: $inspeccionId")
                _uiEvents.send(NuevaInspeccionUiEvent.NavigateToNewRoom(inspeccionId))
            } catch (e: Exception) {
                Log.e("NuevaInspVM", "Error al guardar inspección", e)
                _uiEvents.send(NuevaInspeccionUiEvent.ShowSnackbar("Error al guardar inspección: ${e.message}", isError = true))
            }
        }
    }

    fun finalizarInspeccionYGenerarPdf(inspeccionIdParam: Long? = null) {
        val idDeInspeccionParaPdf = inspeccionIdParam ?: currentInspeccionIdForPdf
        if (idDeInspeccionParaPdf == null) {
            viewModelScope.launch {
                _uiEvents.send(NuevaInspeccionUiEvent.ShowSnackbar("ID de inspección no disponible para generar PDF.", true))
            }
            return
        }
        currentInspeccionIdForPdf = idDeInspeccionParaPdf

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingGlobal = true, isFinalizingAndNavigating = true) }
            _pdfGenerationStatus.value = PdfGenerationResult.InProgress
            Log.d("NuevaInspVM", "finalizarInspeccionYGenerarPdf: isLoadingGlobal = true, generando PDF...")

            try {
                val inspeccion: InspeccionEntity? = inspeccionRepository.getInspeccionById(idDeInspeccionParaPdf)
                val habitaciones: List<HabitacionEntity> = habitacionRepository.getHabitacionesPorInspeccionId(idDeInspeccionParaPdf)

                if (inspeccion == null) {
                    Log.e("NuevaInspVM", "No se encontró la inspección con ID: $idDeInspeccionParaPdf para generar el PDF.")
                    _pdfGenerationStatus.value = PdfGenerationResult.Error("No se encontró la inspección para el PDF.")
                    _uiState.update { it.copy(isLoadingGlobal = false) }
                    return@launch
                }

                Log.d("NuevaInspVM", "Intentando generar PDF para inspección ID: ${inspeccion.id} con ${habitaciones.size} habitaciones.")

                // Marcar inspección como COMPLETADA
                inspeccionRepository.actualizarEstado(idDeInspeccionParaPdf, "COMPLETADA")
                Log.d("NuevaInspVM", "Inspección $idDeInspeccionParaPdf marcada como COMPLETADA")

                val pdfCreationResult = PdfGenerator.createPdf(applicationContext, inspeccion, habitaciones, partidaRepository)

                val pdfUriParaEmail: Uri? = when {
                    pdfCreationResult?.uri != null -> pdfCreationResult.uri
                    pdfCreationResult?.file != null -> FileProvider.getUriForFile(
                        applicationContext,
                        "${applicationContext.packageName}.provider",
                        pdfCreationResult.file
                    )
                    else -> null
                }

                if (pdfUriParaEmail != null) {
                    val nombreArchivo = pdfUriParaEmail.lastPathSegment ?: "inspeccion_${inspeccion.id}.pdf"
                    _pdfGenerationStatus.value = PdfGenerationResult.Success(
                        fileName = nombreArchivo,
                        filePath = pdfCreationResult?.file?.absolutePath,
                        fileUri = pdfUriParaEmail
                    )
                    currentPdfUriForEmail = pdfUriParaEmail
                    Log.d("NuevaInspVM", "PDF generado exitosamente: $nombreArchivo, URI: $pdfUriParaEmail")

                    proceedToFinalizeAndExit()

                } else {
                    Log.e("NuevaInspVM", "PdfGenerator.createPdf devolvió null o no se pudo obtener URI.")
                    _pdfGenerationStatus.value = PdfGenerationResult.Error("No se pudo generar o guardar el PDF.")
                    _uiState.update { it.copy(isLoadingGlobal = false, isFinalizingAndNavigating = false) }
                }

            } catch (e: Exception) {
                Log.e("NuevaInspVM", "Excepción al generar PDF: ${e.message}", e)
                _pdfGenerationStatus.value = PdfGenerationResult.Error("Error generando PDF: ${e.message}")
                _uiState.update { it.copy(isLoadingGlobal = false, isFinalizingAndNavigating = false) }
            }
        }
    }

    private suspend fun proceedToFinalizeAndExit() {
        Log.d("NuevaInspVM", "proceedToFinalizeAndExit: Iniciando proceso de envío de email y navegación...")

        val inspeccionId = currentInspeccionIdForPdf
        val pdfUri = currentPdfUriForEmail

        if (inspeccionId != null && pdfUri != null) {
            _uiState.update { it.copy(isSendingEmail = true) }
            Log.d("NuevaInspVM", "proceedToFinalizeAndExit: isSendingEmail = true, intentando enviar email...")

            prepararYEnviarEmailNativo(inspeccionId, pdfUri)

            _uiState.update { it.copy(isSendingEmail = false) }
            Log.d("NuevaInspVM", "proceedToFinalizeAndExit: isSendingEmail = false")

        } else {
            Log.w("NuevaInspVM", "proceedToFinalizeAndExit: No hay ID de inspección o URI de PDF para enviar email. Saltando envío.")
            _uiEvents.send(NuevaInspeccionUiEvent.ShowSnackbar("No se pudo preparar el email (faltan datos del PDF).", isError = true))
            _uiState.update { it.copy(isSendingEmail = false) }
        }

        Log.d("NuevaInspVM", "proceedToFinalizeAndExit: Operación de email completada. Enviando NavigateBackToMenu.")
        _uiEvents.send(NuevaInspeccionUiEvent.NavigateBackToMenu)

        _uiState.update { it.copy(isLoadingGlobal = false, isSendingEmail = false, pdfGenerationResult = PdfGenerationResult.Idle) }
        currentInspeccionIdForPdf = null
        currentPdfUriForEmail = null
        Log.d("NuevaInspVM", "proceedToFinalizeAndExit: Estados reseteados, finalizando.")
    }

    /**
     * Usa la configuración dinámica de Firestore para determinar destinatarios.
     * Envía emails separados al admin y al inspector según las reglas configuradas:
     * - Admin: siempre recibe PDF + Excel
     * - Inspector: recibe PDF solo si enviarInspeccionAlInspector=true,
     *              recibe Excel solo si enviarPresupuestoAlInspector=true
     */
    private suspend fun prepararYEnviarEmailNativo(inspeccionId: Long, pdfUri: Uri) {
        val inspeccion: InspeccionEntity? = try {
            inspeccionRepository.getInspeccionById(inspeccionId)
        } catch (e: Exception) {
            Log.e("NuevaInspVM", "Error al obtener inspección $inspeccionId para email: ${e.message}", e)
            _uiEvents.send(NuevaInspeccionUiEvent.ShowSnackbar("Error obteniendo datos para el email.", isError = true))
            null
        }

        if (inspeccion == null) {
            Log.e("NuevaInspVM", "No se encontró la inspección $inspeccionId para enviar email.")
            _uiEvents.send(NuevaInspeccionUiEvent.ShowSnackbar("Error: No se pudo encontrar la inspección para el email.", isError = true))
            return
        }

        // --- Cargar configuración dinámica desde Firestore ---
        val emailSettings = emailSettingsRepository.obtenerConfiguracion()
        Log.d("NuevaInspVM", "Configuración de email cargada: $emailSettings")

        // --- Generar el presupuesto Excel ---
        val habitaciones = habitacionRepository.getHabitacionesPorInspeccionId(inspeccionId)
        val excelGenerator = ExcelGenerator(applicationContext)
        val excelResult = excelGenerator.generarPresupuesto(inspeccion, habitaciones, partidaRepository)

        // --- Preparar adjuntos base ---
        val nombrePdf = "Inspeccion_${inspeccion.siniestro}_${inspeccion.rut}.pdf"
        val adjuntoPdf = EmailService.Adjunto(
            uri = pdfUri,
            nombreArchivo = nombrePdf,
            mimeType = "application/pdf"
        )
        val adjuntoExcel = if (excelResult?.uri != null) {
            EmailService.Adjunto(
                uri = excelResult.uri,
                nombreArchivo = excelResult.fileName,
                mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            )
        } else null

        if (adjuntoExcel != null) {
            Log.d("NuevaInspVM", "Excel generado: ${excelResult?.fileName}")
        } else {
            Log.w("NuevaInspVM", "No se pudo generar el presupuesto Excel.")
        }

        // --- CC general ---
        val ccList = mutableListOf<String>()
        if (emailSettings.emailCc.isNotBlank() && emailSettings.emailCc.esEmailValido()) {
            ccList.add(emailSettings.emailCc)
        }

        // ═══════════════════════════════════════════════
        // 1. EMAIL AL ADMINISTRADOR (siempre PDF + Excel)
        // ═══════════════════════════════════════════════
        if (emailSettings.emailAdmin.isNotBlank() && emailSettings.emailAdmin.esEmailValido()) {
            val adjuntosAdmin = mutableListOf(adjuntoPdf)
            if (adjuntoExcel != null) adjuntosAdmin.add(adjuntoExcel)

            val ccAdmin = ccList.filterNot { it.equals(emailSettings.emailAdmin, ignoreCase = true) }.ifEmpty { null }

            val cuerpoAdmin = buildCuerpoHtml(inspeccion, adjuntoExcel != null)

            Log.d("NuevaInspVM", "Enviando email ADMIN a: ${emailSettings.emailAdmin}, CC: $ccAdmin, adjuntos: ${adjuntosAdmin.size}")
            val resultadoAdmin = emailService.enviarConAdjuntos(
                destinatarios = listOf(emailSettings.emailAdmin),
                cc = ccAdmin,
                asunto = "Informe Inspección: Siniestro ${inspeccion.siniestro} - RUT ${inspeccion.rut}",
                cuerpoHtml = cuerpoAdmin,
                adjuntos = adjuntosAdmin
            )
            when (resultadoAdmin) {
                is EmailService.EmailResult.Success -> {
                    Log.i("NuevaInspVM", "Email ADMIN enviado correctamente a ${emailSettings.emailAdmin}")
                }
                is EmailService.EmailResult.Error -> {
                    Log.e("NuevaInspVM", "Error enviando email ADMIN: ${resultadoAdmin.message}")
                    _uiEvents.send(NuevaInspeccionUiEvent.ShowSnackbar("Error enviando al admin: ${resultadoAdmin.message}", isError = true))
                }
            }
        } else {
            Log.w("NuevaInspVM", "No hay email de administrador configurado. Saltando envío al admin.")
        }

        // ═══════════════════════════════════════════════
        // 2. EMAIL AL INSPECTOR (según reglas de envío)
        // ═══════════════════════════════════════════════
        val emailInspector = inspeccion.mail
        val enviarPdfAlInspector = emailSettings.enviarInspeccionAlInspector
        val enviarExcelAlInspector = emailSettings.enviarPresupuestoAlInspector

        // Solo enviar si al menos una regla está activa y el email es válido
        if ((enviarPdfAlInspector || enviarExcelAlInspector) && emailInspector.esEmailValido()) {
            // No enviar al inspector si es el mismo que el admin (ya lo recibió)
            if (!emailInspector.equals(emailSettings.emailAdmin, ignoreCase = true)) {
                val adjuntosInspector = mutableListOf<EmailService.Adjunto>()
                if (enviarPdfAlInspector) adjuntosInspector.add(adjuntoPdf)
                if (enviarExcelAlInspector && adjuntoExcel != null) adjuntosInspector.add(adjuntoExcel)

                if (adjuntosInspector.isNotEmpty()) {
                    val tieneExcel = enviarExcelAlInspector && adjuntoExcel != null
                    val cuerpoInspector = buildCuerpoHtml(inspeccion, tieneExcel)

                    Log.d("NuevaInspVM", "Enviando email INSPECTOR a: $emailInspector, adjuntos: ${adjuntosInspector.size} (PDF=$enviarPdfAlInspector, Excel=$enviarExcelAlInspector)")
                    val resultadoInspector = emailService.enviarConAdjuntos(
                        destinatarios = listOf(emailInspector),
                        cc = null,
                        asunto = "Informe Inspección: Siniestro ${inspeccion.siniestro} - RUT ${inspeccion.rut}",
                        cuerpoHtml = cuerpoInspector,
                        adjuntos = adjuntosInspector
                    )
                    when (resultadoInspector) {
                        is EmailService.EmailResult.Success -> {
                            Log.i("NuevaInspVM", "Email INSPECTOR enviado correctamente a $emailInspector")
                        }
                        is EmailService.EmailResult.Error -> {
                            Log.e("NuevaInspVM", "Error enviando email INSPECTOR: ${resultadoInspector.message}")
                            _uiEvents.send(NuevaInspeccionUiEvent.ShowSnackbar("Error enviando al inspector: ${resultadoInspector.message}", isError = true))
                        }
                    }
                }
            } else {
                Log.d("NuevaInspVM", "Inspector y admin son el mismo email, no se envía duplicado.")
            }
        } else {
            Log.d("NuevaInspVM", "No se envía email al inspector (reglas: PDF=$enviarPdfAlInspector, Excel=$enviarExcelAlInspector, email válido=${emailInspector.esEmailValido()})")
        }

        _uiEvents.send(NuevaInspeccionUiEvent.ShowSnackbar("Proceso de envío de emails completado."))
    }

    /**
     * Genera el cuerpo HTML del email.
     */
    private fun buildCuerpoHtml(inspeccion: InspeccionEntity, incluirExcel: Boolean): String {
        return """
            <html><body>
            <p>Estimado/a,</p>
            <p>Adjunto encontrará el informe de la inspección${if (incluirExcel) " y el presupuesto de reparación" else ""}
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
                ${if (incluirExcel) "<li>Presupuesto de Reparación (Excel)</li>" else ""}
            </ul>
            <p>Si tiene alguna consulta, no dude en contactarnos.</p>
            <p>Saludos cordiales,<br/><strong>Equipo Ferji Inspecciones</strong></p>
            </body></html>
        """.trimIndent()
    }
}

