package com.ferji.inspecciones

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.ferji.inspecciones.ui.components.PdfGenerationResult
import com.ferji.inspecciones.ui.events.NuevaInspeccionScreenUiState
import com.ferji.inspecciones.ui.events.NuevaInspeccionUiEvent
import com.ferji.inspecciones.ui.theme.FerjiTheme
import com.ferji.inspecciones.viewmodels.NuevaInspeccionViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import java.io.File


@AndroidEntryPoint
class NuevaInspeccionActivity : ComponentActivity() {

    private val viewModel: NuevaInspeccionViewModel by viewModels()
    private lateinit var nuevaHabitacionLauncher: ActivityResultLauncher<Intent>

    private val REQUEST_WRITE_STORAGE_PERMISSION = 1002




    companion object {
        const val RESULT_INSPECCION_FINALIZADA = 1001
        const val TAG = "NuevaInspActiv"
        const val EXTRA_INSPECCION_ID = "INSPECCION_ID"
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")

        nuevaHabitacionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            Log.d(TAG, "Resultado de NuevaHabitacionActivity - resultCode: ${result.resultCode}")
            if (result.resultCode == RESULT_INSPECCION_FINALIZADA) {
                Log.d(TAG, "RESULT_INSPECCION_FINALIZADA recibido. Procediendo a generar PDF y finalizar.")
                viewModel.finalizarInspeccionYGenerarPdf()
            } else if (result.resultCode == Activity.RESULT_CANCELED) {
                Log.d(TAG, "NuevaHabitacionActivity fue cancelada o devuelta sin finalizar.")
                // Opcional: Resetear algún estado en el ViewModel si es necesario.
                // viewModel.resetSomeStateAfterRoomCancellation()
            } else {
                Log.d(TAG, "Resultado diferente o no manejado de NuevaHabitacionActivity: ${result.resultCode}")
            }
        }

        setContent {
            val uiState: NuevaInspeccionScreenUiState by viewModel.uiState.collectAsState()
            // val context = LocalContext.current // Si lo necesitas

            FerjiTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(
                                if (uiState.isFinalizingAndNavigating) { // <--- CONDICIÓN AQUÍ
                                    "Procesando Inspección"
                                } else {
                                    "Nueva Inspección"
                                }
                            ) },
                            actions = {
                                if (uiState.isLoadingGlobal && !uiState.isFinalizingAndNavigating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .size(28.dp) // Un poco más grande para ser visible en la AppBar
                                            .padding(end = 8.dp),
                                        strokeWidth = 3.dp // Un poco más grueso
                                    )
                                }
                            }
                        )
                    }
                ) { paddingValues ->
                    // Contenido Principal de la Pantalla de Nueva Inspección
                    if (uiState.isFinalizingAndNavigating) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                                .background(MaterialTheme.colorScheme.background), // Fondo para que no se vea transparente
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    if (uiState.isSendingEmail) "Enviando email..."
                                    else if (uiState.pdfGenerationResult is PdfGenerationResult.InProgress) "Generando PDF..."
                                    else "Finalizando inspección...",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    } else {
                        // Contenido Principal de la Pantalla de Nueva Inspección
                        PantallaNuevaInspeccion(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                                .padding(16.dp),
                            viewModel = viewModel,
                            isLoading = uiState.isLoadingGlobal // Puedes seguir usando esto para deshabilitar botones en el formulario
                        )
                    }
                }

                // Colectores para eventos (estos pueden permanecer fuera del Scaffold si no afectan directamente su contenido)
                LaunchedEffect(key1 = Unit) {
                    viewModel.uiEvents.collectLatest { event ->
                        when (event) {
                            is NuevaInspeccionUiEvent.NavigateToNewRoom -> {
                                Log.d(TAG, "Navegando a NuevaHabitacion con ID: ${event.inspeccionId}")
                                val intent = Intent(
                                    this@NuevaInspeccionActivity,
                                    NuevaHabitacionActivity::class.java
                                ).apply {
                                    putExtra(EXTRA_INSPECCION_ID, event.inspeccionId)
                                }
                                nuevaHabitacionLauncher.launch(intent)
                            }
                            is NuevaInspeccionUiEvent.ShowSnackbar -> {
                                Toast.makeText(applicationContext, event.message, Toast.LENGTH_LONG).show()
                            }
                            is NuevaInspeccionUiEvent.NavigateBackToMenu -> {
                                Log.d(TAG, "Evento NavigateBackToMenu recibido. Navegando al menú principal y limpiando pila.")
                            //    Toast.makeText(applicationContext, "Inspección completada.", Toast.LENGTH_LONG).show()

                                // Navegar al Menú Principal limpiando la pila anterior
                                val intent = Intent(this@NuevaInspeccionActivity, MenuPrincipalActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)

                                // Finalizar esta actividad (NuevaInspeccionActivity)
                                finish()
                            }
                            is NuevaInspeccionUiEvent.RequestEmailWithPdf -> {
                                Log.d(TAG, "Evento RequestEmailWithPdf recibido. Inspeccion ID: ${event.inspeccionId}, PDF URI: ${event.pdfUri}")
                                Toast.makeText(applicationContext, "Solicitud de envío de email recibida.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                LaunchedEffect(key1 = Unit) {
                    viewModel.pdfGenerationStatus.collectLatest { pdfResult ->
                        when (pdfResult) {
                            is com.ferji.inspecciones.ui.components.PdfGenerationResult.Success -> { // Asegúrate de usar el tipo común
                                val ubicacion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && pdfResult.fileUri != null) {
                                    "Guardado en Descargas (URI)."
                                } else if (pdfResult.filePath != null) {
                                    "Guardado en: ${File(pdfResult.filePath).name} (Descargas)"
                                } else {
                                    "Ubicación desconocida."
                                }
                                val mensajePdf = "PDF '${pdfResult.fileName ?: "desconocido"}' generado. $ubicacion"
                                Log.i(TAG, "PDF Success: $mensajePdf")
                                // Toast.makeText(applicationContext, mensajePdf, Toast.LENGTH_LONG).show() // Opcional
                            }
                            is com.ferji.inspecciones.ui.components.PdfGenerationResult.Error -> {
                                Log.e(TAG, "PDF Error: ${pdfResult.message}")
                                Toast.makeText(applicationContext, "Error PDF: ${pdfResult.message}", Toast.LENGTH_LONG).show()
                            }
                            is com.ferji.inspecciones.ui.components.PdfGenerationResult.InProgress -> {
                                Log.d(TAG, "PDF InProgress")
                                // Ya no necesitas un Toast aquí, el indicador en la TopAppBar o en el contenido es suficiente.
                            }
                            is com.ferji.inspecciones.ui.components.PdfGenerationResult.Idle -> {
                                Log.d(TAG, "PDF Idle")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleFinalizarInspeccionConPdf() {
        if (checkAndRequestStoragePermissionIfNeeded()) {
            // El ViewModel se encarga de la lógica de obtener el ID de inspección actual
            // y luego de activar el indicador isFinalizingInspection y el resto del flujo.
            viewModel.finalizarInspeccionYGenerarPdf()
        } else {
            Log.w(TAG, "Permiso de almacenamiento denegado o pendiente. No se puede generar PDF.")
            Toast.makeText(this, "Se necesita permiso de almacenamiento para generar el PDF.", Toast.LENGTH_LONG).show()
            // Podrías finalizar aquí si el PDF es esencial y el flujo no puede continuar.
            // O resetear algún estado en el ViewModel.
            // viewModel.resetFinalizationAttempt() // Ejemplo
        }
    }

    private fun intentarAbrirPdf(fileUri: Uri?, filePath: String?) {
        val uriToOpen: Uri? = fileUri ?: filePath?.let { path ->
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                val file = File(path)
                if (file.exists()) {
                    val authority = "${applicationContext.packageName}.fileprovider"
                    try {
                        FileProvider.getUriForFile(this@NuevaInspeccionActivity, authority, file)
                    } catch (e: IllegalArgumentException) {
                        Log.e(TAG, "Error al crear URI con FileProvider para < Q: ${e.message}")
                        Toast.makeText(this, "Error al preparar PDF para abrirlo (FileProvider).", Toast.LENGTH_LONG).show()
                        null
                    }
                } else {
                    Log.e(TAG, "Archivo PDF no encontrado en path para < Q: $path")
                    Toast.makeText(this, "Archivo PDF no encontrado para abrir.", Toast.LENGTH_LONG).show()
                    null
                }
            } else null
        }

        if (uriToOpen != null) {
            val openIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uriToOpen, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY) // Opcional: no añade la app de PDF al historial de la app
            }
            try {
                startActivity(Intent.createChooser(openIntent, "Abrir PDF con..."))
            } catch (e: android.content.ActivityNotFoundException) {
                Toast.makeText(this@NuevaInspeccionActivity, "No hay aplicación para abrir PDF.", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "ActivityNotFoundException para abrir PDF: ${e.message}")
            }
        } else {
            Toast.makeText(this@NuevaInspeccionActivity, "No se pudo obtener la URI para abrir el PDF.", Toast.LENGTH_LONG).show()
            Log.w(TAG, "uriToOpen fue null. fileUri: $fileUri, filePath: $filePath")
        }
    }

    private fun checkAndRequestStoragePermissionIfNeeded(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permiso WRITE_EXTERNAL_STORAGE no concedido. Solicitando...")
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_WRITE_STORAGE_PERMISSION)
                return false
            } else {
                Log.d(TAG, "Permiso WRITE_EXTERNAL_STORAGE ya concedido.")
            }
        }
        Log.d(TAG, "Permiso de escritura no es necesario (Android Q+) o ya está concedido para < Q.")
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_WRITE_STORAGE_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Permiso WRITE_EXTERNAL_STORAGE concedido por el usuario.")
                    // Ahora que el permiso está concedido, intenta la acción de nuevo (generar PDF)
                    viewModel.finalizarInspeccionYGenerarPdf()
                } else {
                    Log.w(TAG, "Permiso WRITE_EXTERNAL_STORAGE denegado por el usuario.")
                    Toast.makeText(this, "Permiso de almacenamiento denegado. El PDF no se guardará en Descargas públicas en versiones antiguas de Android.", Toast.LENGTH_LONG).show()
                    // Aquí podrías decidir finalizar la actividad o permitir continuar sin PDF.
                    // Si el PDF es crucial, podrías llamar a finish().
                    // setResult(Activity.RESULT_CANCELED)
                    // finish()
                }
            }
        }
    }
}
