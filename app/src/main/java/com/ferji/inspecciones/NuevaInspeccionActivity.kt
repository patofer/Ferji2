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
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.* // Necesario para MaterialTheme, etc.
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.ferji.inspecciones.ui.components.PdfGenerationResult
import com.ferji.inspecciones.ui.events.NuevaInspeccionUiEvent
import com.ferji.inspecciones.ui.theme.FerjiTheme
import com.ferji.inspecciones.viewmodels.NuevaInspeccionViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import java.io.File
import com.ferji.inspecciones.ui.events.NuevaInspeccionScreenUiState





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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")

        nuevaHabitacionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            Log.d(TAG, "Resultado de NuevaHabitacionActivity - resultCode: ${result.resultCode}")
            if (result.resultCode == RESULT_INSPECCION_FINALIZADA) { // Solo reacciona a este
                Log.d(TAG, "RESULT_INSPECCION_FINALIZADA recibido. Procediendo a generar PDF y finalizar.")
                // El ViewModel se encarga de isFinalizingInspection
                viewModel.finalizarInspeccionYGenerarPdf() // Ya no necesitas handleFinalizarInspeccionConPdf() como intermediario
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
            FerjiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background // <-- ESTO ES CLAVE
                ) { // Contenedor para la pantalla y el overlay de carga
                    PantallaNuevaInspeccion(viewModel = viewModel) // Tu pantalla de formulario

                    // Indicador de Carga Global mientras se finaliza la inspección
                    if (uiState.isLoadingGlobal) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.5f), // Fondo semitransparente
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "Finalizando inspección...",
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }

                    // Colector para eventos UI generales del ViewModel
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
                                // En NuevaInspeccionActivity.kt, dentro de setContent -> LaunchedEffect(key1 = Unit) { viewModel.uiEvents.collectLatest { event ->
                                is NuevaInspeccionUiEvent.NavigateBackToMenu -> {
                                    Log.d(TAG, "Evento NavigateBackToMenu recibido. Volviendo al menú.")
                                    // El Toast "Inspección completada" puede ser útil aquí.
                                    Toast.makeText(applicationContext, "Inspeccion completada y finalizando.", Toast.LENGTH_LONG).show()
                                    setResult(Activity.RESULT_OK) // Informa éxito a la actividad anterior (Menú Principal)
                                    finish() // Cierra NuevaInspeccionActivity
                                }
                                is NuevaInspeccionUiEvent.RequestEmailWithPdf -> {
                                    // Maneja este evento si es relevante para la Activity.
                                    // En el flujo actual, el ViewModel inicia el envío de email
                                    // automáticamente como parte de proceedToFinalizeAndExit().
                                    // Esta rama podría usarse si tienes un botón manual "Reenviar Email".
                                    Log.d(TAG, "Evento RequestEmailWithPdf recibido (manual/opcional). Inspeccion ID: ${event.inspeccionId}, PDF URI: ${event.pdfUri}")
                                    Toast.makeText(applicationContext, "Solicitud de envío de email (manual) recibida.", Toast.LENGTH_SHORT).show()
                                    // Podrías llamar a viewModel.solicitarEnvioDeEmail() aquí si es un reintento
                                    // o si este evento se dispara por una acción explícita del usuario.
                                }
                            }
                        }
                    }

                    // Colector para el estado de generación de PDF (principalmente para logs y Toasts informativos)
                    LaunchedEffect(key1 = Unit) {
                        viewModel.pdfGenerationStatus.collectLatest { pdfResult ->
                            when (pdfResult) {
                                is PdfGenerationResult.Success -> {
                                    // Determinar la ubicación del archivo para el mensaje
                                    val ubicacion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && pdfResult.fileUri != null) {
                                        "Guardado en Descargas (URI)."
                                    } else if (pdfResult.filePath != null) {
                                        "Guardado en: ${File(pdfResult.filePath).name} (Descargas)" // Muestra solo el nombre del archivo
                                    } else {
                                        "Ubicación desconocida."
                                    }
                                    val mensajePdf = "PDF '${pdfResult.fileName ?: "desconocido"}' generado. $ubicacion" // Usar fileName si está disponible
                                    //Toast.makeText(applicationContext, mensajePdf, Toast.LENGTH_LONG).show()
                                    Log.i(TAG, "PDF Success. Nombre: ${pdfResult.fileName}, Path: ${pdfResult.filePath}, URI: ${pdfResult.fileUri}")
                                    // La lógica de solicitar email y finalizar la actividad ahora es manejada
                                    // por el ViewModel a través de proceedToFinalizeAndExit()
                                }
                                is PdfGenerationResult.Error -> {
                                //    Toast.makeText(applicationContext, "Error PDF: ${pdfResult.message}", Toast.LENGTH_LONG).show()
                                    Log.e(TAG, "PDF Error: ${pdfResult.message}")
                                    // Si hay un error de PDF, proceedToFinalizeAndExit en el VM no se llamará.
                                    // La UI de carga (isFinalizingInspection) no se activará desde este error.
                                    // El ViewModel debería resetear isFinalizingInspection = false si el error de PDF
                                    // ocurre dentro de la corutina de finalizarInspeccionYGenerarPdf.
                                }
                                is PdfGenerationResult.InProgress -> {
                                    Log.d(TAG, "PDF InProgress")
                                    // El indicador de carga global (isFinalizingInspection) es más prominente.
                                    // Podrías mostrar un Toast aquí si el indicador global no existiera.
                                    // Toast.makeText(applicationContext, "Generando PDF...", Toast.LENGTH_SHORT).show()
                                }
                                is PdfGenerationResult.Idle -> {
                                    Log.d(TAG, "PDF Idle")
                                }
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
