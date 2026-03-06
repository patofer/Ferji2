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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.ferji.inspecciones.ui.components.PdfGenerationResult
import com.ferji.inspecciones.ui.components.FerjiTitleBar
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

    // --- CAMBIO CLAVE 1: ELIMINAR la constante de permiso antigua ---
    // private val REQUEST_WRITE_STORAGE_PERMISSION = 1002 // Ya no se necesita

    // --- CAMBIO CLAVE 2: AÑADIR el nuevo Launcher de Permisos ---
    // Este reemplaza a onRequestPermissionsResult
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permiso concedido, ahora sí podemos llamar a la función que genera el PDF.
            Log.d(TAG, "Permiso WRITE_EXTERNAL_STORAGE concedido por el usuario.")
            viewModel.finalizarInspeccionYGenerarPdf()
        } else {
            // Permiso denegado. Informamos al usuario.
            Log.w(TAG, "Permiso WRITE_EXTERNAL_STORAGE denegado por el usuario.")
            Toast.makeText(this, "Permiso de almacenamiento denegado. El PDF no se guardará.", Toast.LENGTH_LONG).show()
            // Aquí puedes decidir si quieres finalizar la inspección sin PDF,
            // o simplemente informar y dejar que el usuario decida. Por ahora, solo informamos.
        }
    }

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
                Log.d(TAG, "RESULT_INSPECCION_FINALIZADA recibido. Iniciando flujo para generar PDF.")
                // --- CAMBIO CLAVE 3: LLAMAR a la nueva función de permisos ---
                // En lugar de llamar directamente a viewModel, iniciamos el flujo de permisos.
                handleFinalizarInspeccionConPdf()
            } else if (result.resultCode == Activity.RESULT_CANCELED) {
                Log.d(TAG, "NuevaHabitacionActivity fue cancelada o devuelta sin finalizar.")
            } else {
                Log.d(TAG, "Resultado diferente o no manejado de NuevaHabitacionActivity: ${result.resultCode}")
            }
        }

        // --- El resto del código de setContent permanece exactamente igual ---
        setContent {
            val uiState: NuevaInspeccionScreenUiState by viewModel.uiState.collectAsState()

            FerjiTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                FerjiTitleBar(
                                    subtitle = if (uiState.isFinalizingAndNavigating) {
                                        "Procesando Inspección"
                                    } else {
                                        "Nueva Inspección"
                                    },
                                    compact = true
                                )
                            },
                            navigationIcon = {
                                if (!uiState.isFinalizingAndNavigating) {
                                    IconButton(onClick = { finish() }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                                    }
                                }
                            },
                            actions = {
                                if (uiState.isLoadingGlobal && !uiState.isFinalizingAndNavigating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .padding(end = 8.dp),
                                        strokeWidth = 3.dp
                                    )
                                }
                            }
                        )
                    }
                ) { paddingValues ->
                    if (uiState.isFinalizingAndNavigating) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                                .background(MaterialTheme.colorScheme.background),
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
                        PantallaNuevaInspeccion(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                                .padding(16.dp),
                            viewModel = viewModel,
                            isLoading = uiState.isLoadingGlobal
                        )
                    }
                }

                LaunchedEffect(key1 = Unit) {
                    viewModel.uiEvents.collectLatest { event ->
                        when (event) {
                            is NuevaInspeccionUiEvent.NavigateToNewRoom -> {
                                val intent = Intent(this@NuevaInspeccionActivity, NuevaHabitacionActivity::class.java)
                                    .apply { putExtra(EXTRA_INSPECCION_ID, event.inspeccionId) }
                                nuevaHabitacionLauncher.launch(intent)
                            }
                            is NuevaInspeccionUiEvent.ShowSnackbar -> {
                                Toast.makeText(applicationContext, event.message, Toast.LENGTH_LONG).show()
                            }
                            is NuevaInspeccionUiEvent.NavigateBackToMenu -> {
                                val intent = Intent(this@NuevaInspeccionActivity, MenuPrincipalActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            }
                            is NuevaInspeccionUiEvent.RequestEmailWithPdf -> {
                                // Evento legacy, no se usa actualmente
                            }
                            is NuevaInspeccionUiEvent.SendEmailNativo -> {
                                // El envío de email ahora es automático desde EmailService.
                                // Este evento ya no requiere acción en la Activity.
                            }
                        }
                    }
                }

                LaunchedEffect(key1 = Unit) {
                    viewModel.pdfGenerationStatus.collectLatest { pdfResult ->
                        // Esta lógica se mantiene
                        when (pdfResult) {
                            is PdfGenerationResult.Success -> {
                                val ubicacion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && pdfResult.fileUri != null) {
                                    "Guardado en Descargas."
                                } else if (pdfResult.filePath != null) {
                                    "Guardado en: ${File(pdfResult.filePath).name}"
                                } else { "Ubicación desconocida." }
                                Log.i(TAG, "PDF '${pdfResult.fileName ?: "desconocido"}' generado. $ubicacion")
                            }
                            is PdfGenerationResult.Error -> {
                                Log.e(TAG, "PDF Error: ${pdfResult.message}")
                                Toast.makeText(applicationContext, "Error PDF: ${pdfResult.message}", Toast.LENGTH_LONG).show()
                            }
                            else -> { /* InProgress e Idle no necesitan acción aquí */ }
                        }
                    }
                }
            }
        }
    }

    // --- CAMBIO CLAVE 4: REEMPLAZAR la lógica de permisos ---
    // Esta función ahora centraliza la verificación de permisos.
    private fun handleFinalizarInspeccionConPdf() {
        // En Android 10 (Q) y superior, NO se necesita permiso para escribir en la carpeta de Descargas pública.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Log.d(TAG, "Android 10+ detectado. No se necesita permiso. Procediendo a generar PDF.")
            viewModel.finalizarInspeccionYGenerarPdf()
            return
        }

        // Para versiones ANTERIORES a Android 10, sí necesitamos verificar el permiso.
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED -> {
                // Ya tenemos el permiso, procedemos.
                Log.d(TAG, "Permiso WRITE_EXTERNAL_STORAGE ya concedido.")
                viewModel.finalizarInspeccionYGenerarPdf()
            }
            else -> {
                // No tenemos el permiso, lo solicitamos con nuestro nuevo launcher.
                Log.d(TAG, "Permiso WRITE_EXTERNAL_STORAGE no concedido. Solicitando...")
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    // La función intentarAbrirPdf se mantiene como está
    private fun intentarAbrirPdf(fileUri: Uri?, filePath: String?) {
        val uriToOpen: Uri? = fileUri ?: filePath?.let { path ->
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                val file = File(path)
                if (file.exists()) {
                    val authority = "${applicationContext.packageName}.provider"
                    try {
                        FileProvider.getUriForFile(this@NuevaInspeccionActivity, authority, file)
                    } catch (e: IllegalArgumentException) {
                        Log.e(TAG, "Error al crear URI con FileProvider: ${e.message}")
                        null
                    }
                } else {
                    Log.e(TAG, "Archivo PDF no encontrado en path: $path")
                    null
                }
            } else null
        }

        if (uriToOpen != null) {
            val openIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uriToOpen, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            }
            try {
                startActivity(Intent.createChooser(openIntent, "Abrir PDF con..."))
            } catch (e: android.content.ActivityNotFoundException) {
                Toast.makeText(this, "No hay aplicación para abrir PDF.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "No se pudo obtener la URI para abrir el PDF.", Toast.LENGTH_LONG).show()
        }
    }

    // --- CAMBIO CLAVE 5: ELIMINAR CÓDIGO OBSOLETO ---
    // El método checkAndRequestStoragePermissionIfNeeded ya no es necesario.
    // El método onRequestPermissionsResult que causaba el error ha sido eliminado.
}
