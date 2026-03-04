package com.ferji.inspecciones.ui.events

import android.net.Uri
import androidx.compose.ui.graphics.Color
// CAMBIA ESTA IMPORTACIÓN:
// import com.ferji.inspecciones.viewmodels.PdfGenerationResult // <--- ELIMINA o COMENTA ESTA
import com.ferji.inspecciones.ui.components.PdfGenerationResult    // <--- AÑADE ESTA

sealed interface NuevaInspeccionUiEvent {
    data class NavigateToNewRoom(val inspeccionId: Long) : NuevaInspeccionUiEvent
    object NavigateBackToMenu : NuevaInspeccionUiEvent
    data class ShowSnackbar(val message: String, val isError: Boolean = false) : NuevaInspeccionUiEvent
    data class RequestEmailWithPdf(val inspeccionId: Long, val pdfUri: Uri) : NuevaInspeccionUiEvent
    /** Evento para que la Activity lance el cliente de email nativo con el PDF adjunto. */
    data class SendEmailNativo(
        val destinatarios: List<String>,
        val cc: List<String>?,
        val asunto: String,
        val cuerpo: String,
        val pdfUri: Uri
    ) : NuevaInspeccionUiEvent
}

data class NuevaInspeccionScreenUiState(
    val mensajeGlobalUi: String? = null,
    val colorMensajeGlobalUi: Color? = null,
    val isLoading: Boolean = false, // Considera renombrar a algo más específico si 'isLoadingGlobal' es diferente
    val isSendingEmail: Boolean = false,
    val isLoadingGlobal: Boolean = false, // Si 'isLoading' y 'isLoadingGlobal' son para cosas distintas, está bien. Si no, consolida.
    val pdfGenerationResult: PdfGenerationResult = PdfGenerationResult.Idle, // Ahora usa ui.common.PdfGenerationResult
    val isFinalizingAndNavigating: Boolean = false
)

