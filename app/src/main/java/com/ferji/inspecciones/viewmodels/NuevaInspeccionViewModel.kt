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
import com.ferji.inspecciones.data.repository.HabitacionRepository
import com.ferji.inspecciones.data.repository.InspeccionRepository
import com.ferji.inspecciones.data.repository.UserRepository
import com.ferji.inspecciones.ui.components.PdfGenerationResult
import com.ferji.inspecciones.ui.events.NuevaInspeccionScreenUiState
import com.ferji.inspecciones.ui.events.NuevaInspeccionUiEvent
import com.ferji.inspecciones.utils.email.EmailService
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
    private val emailService: EmailService,
    private val userRepository: UserRepository
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
    // Fin Campos de Formulario

    private var currentInspeccionIdForPdf: Long? = null
    private var currentPdfUriForEmail: Uri? = null


    init {
        // --- BLOQUE `init` ACTUALIZADO PARA AUTOCOMPLETAR CAMPOS ---
        viewModelScope.launch {
            // ✅ CORRECCIÓN: Usamos la propiedad 'currentUserSession' en lugar de la función 'getUserSession()'
            val sesionActual = userRepository.currentUserSession.firstOrNull()
            sesionActual?.let { user ->
                // Asignamos el RUT y el Email del usuario logeado a los campos correspondientes.
                // Esto automáticamente llamará a las funciones on...Change y validará los campos.
                user.rut?.let { onRutInspectorChange(it) }
                user.email?.let { onMailChange(it) }
                Log.d("NuevaInspVM", "Campos inicializados desde sesión: RUT Inspector=${user.rut}, Email=${user.email}")
            }
        }
    }

    // (El resto de las funciones no necesita cambios para esta funcionalidad)

    private fun actualizarTodosCamposLlenos() {
        val rutNoVacio = rut.isNotBlank()
        // Llamada correcta a la función de extensión
        val rutFormatoValido = rut.validarRutChileno()
        val siniestroNoVacio = siniestro.isNotBlank()
        val direccionNoVacia = direccion.isNotBlank()
        val rutInspectorNoVacio = rutInspector.isNotBlank()
        // Llamada correcta a la función de extensión
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
        // Llamada correcta a la función de extensión
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
        // Llamada correcta a la función de extensión
        isRutInspectorValid = if (nuevoRutInspector.isBlank()) true else nuevoRutInspector.validarRutChileno()
        actualizarTodosCamposLlenos()
    }

    fun onMailChange(nuevoMail: String) {
        mail = nuevoMail
        // Llamada correcta a la función de extensión
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
                val pdfCreationResult = PdfGenerator.createPdf(applicationContext, inspeccion, habitaciones)

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
                    _uiState.update { it.copy(isLoadingGlobal = false,isFinalizingAndNavigating = false) }
                }

            } catch (e: Exception) {
                Log.e("NuevaInspVM", "Excepción al generar PDF: ${e.message}", e)
                _pdfGenerationStatus.value = PdfGenerationResult.Error("Error generando PDF: ${e.message}")
                _uiState.update { it.copy(isLoadingGlobal = false,isFinalizingAndNavigating = false) }
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

            // Enviar email usando el cliente nativo del dispositivo
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
     * Envía el email de forma **automática y silenciosa** usando SendGrid (sin abrir ninguna app).
     * Construye el HTML del cuerpo, convierte el PDF a Base64 y llama a la API directamente.
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

        val destinatario = inspeccion.mail
        if (!destinatario.esEmailValido()) {
            Log.e("NuevaInspVM", "Email del destinatario inválido: $destinatario")
            _uiEvents.send(NuevaInspeccionUiEvent.ShowSnackbar("Email del destinatario inválido: $destinatario", isError = true))
            return
        }

        val asunto = "Informe Inspección: Siniestro ${inspeccion.siniestro} - RUT ${inspeccion.rut}"
        val nombrePdf = "Inspeccion_${inspeccion.siniestro}_${inspeccion.rut}.pdf"

        val cuerpoHtml = """
            <html><body>
            <p>Estimado/a,</p>
            <p>Adjunto encontrará el informe de la inspección realizada para el
            <strong>siniestro N° ${inspeccion.siniestro}</strong>.</p>
            <table border="1" cellpadding="6" cellspacing="0">
                <tr><th>RUT Cliente</th><td>${inspeccion.rut}</td></tr>
                <tr><th>Dirección</th><td>${inspeccion.direccion}</td></tr>
                <tr><th>Inspector Asignado</th><td>${inspeccion.rutInspector}</td></tr>
            </table>
            <br/>
            <p>Si tiene alguna consulta, no dude en contactarnos.</p>
            <p>Saludos cordiales,<br/><strong>Equipo Ferji Inspecciones</strong></p>
            </body></html>
        """.trimIndent()

        val cc = listOf("patriciofernande@gmail.com")
            .filterNot { it.equals(destinatario, ignoreCase = true) }
            .ifEmpty { null }

        Log.d("NuevaInspVM", "Enviando email automático a: $destinatario")
        val resultado = emailService.enviarConPdf(
            destinatarios = listOf(destinatario),
            cc = cc,
            asunto = asunto,
            cuerpoHtml = cuerpoHtml,
            pdfUri = pdfUri,
            nombreArchivoAdjunto = nombrePdf
        )

        when (resultado) {
            is EmailService.EmailResult.Success -> {
                Log.i("NuevaInspVM", "Email enviado correctamente a $destinatario")
                _uiEvents.send(NuevaInspeccionUiEvent.ShowSnackbar("Informe enviado correctamente a $destinatario"))
            }
            is EmailService.EmailResult.Error -> {
                Log.e("NuevaInspVM", "Error enviando email: ${resultado.message}")
                _uiEvents.send(NuevaInspeccionUiEvent.ShowSnackbar("Error al enviar email: ${resultado.message}", isError = true))
            }
        }
    }
}
