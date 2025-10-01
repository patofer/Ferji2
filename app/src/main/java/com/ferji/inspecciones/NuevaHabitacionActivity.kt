package com.ferji.inspecciones

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.ferji.inspecciones.ui.components.DanoDropdown


import com.ferji.inspecciones.ui.theme.FerjiTheme
import com.ferji.inspecciones.utils.FileUtils
import com.ferji.inspecciones.viewmodels.NuevaHabitacionViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class NuevaHabitacionActivity : ComponentActivity() {

    private val _preparandoParaFinalizar = MutableStateFlow(false)
    val preparandoParaFinalizar: StateFlow<Boolean> = _preparandoParaFinalizar.asStateFlow()

    companion object {
        const val EXTRA_INSPECCION_ID = "extra_inspeccion_id_nueva_habitacion" // ¡AQUÍ ESTÁ!
        // Puedes usar otro nombre para la constante si prefieres,
        // pero debe coincidir con cómo la usas en NuevaInspeccionActivity
    }
    private val viewModel: NuevaHabitacionViewModel by viewModels()
    private var latestTmpUri: Uri? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permiso concedido, ahora puedes lanzar la cámara
            cameraLauncher.launch(null)
        } else {
            // Permiso denegado, muestra un mensaje al usuario
            Toast.makeText(this, "Permiso de cámara denegado.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getTmpFileUri(): Uri {
        // Crea un archivo temporal en el directorio cache de tu app
        val tmpFile = File.createTempFile("tmp_image_file", ".jpg", cacheDir).apply {
            createNewFile()
            // Opcional: deleteOnExit() // Si quieres que se borre cuando la VM termine,
            // pero como lo procesas y guardas en otro lado, quizás no sea necesario.
        }
        // Obtiene el URI usando FileProvider (asegúrate que tu FileProvider esté configurado
        // en AndroidManifest.xml y res/xml/file_paths.xml)
        return FileProvider.getUriForFile(applicationContext, "${BuildConfig.APPLICATION_ID}.provider", tmpFile)
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture() // <-- CAMBIO AQUÍ
    ) { success: Boolean -> // 'success' indica si la foto se guardó en el URI que le pasaste
        if (success) {
            latestTmpUri?.let { uri ->
                Log.d("NuevaHabitacionActivity", "Foto tomada y guardada en URI temporal: $uri")
                val nombreArchivo = "habitacion_${System.currentTimeMillis()}.jpg"
                // Ahora llamas a FileUtils.guardarBitmapEnInterno con el URI
                val rutaFotoGuardada = FileUtils.guardarBitmapEnInterno(
                    this,
                    uri, // <-- Pasas el URI del archivo temporal de la cámara
                    nombreArchivo
                )

                if (rutaFotoGuardada != null) {
                    Log.d("NuevaHabitacionActivity", "Bitmap procesado y guardado en: $rutaFotoGuardada")
                    viewModel.agregarFoto(rutaFotoGuardada)
                } else {
                    Log.e("NuevaHabitacionActivity", "Error: rutaFoto está vacía después de procesar el URI")
                    Toast.makeText(this, "Error al procesar y guardar la foto.", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                Log.e("NuevaHabitacionActivity", "latestTmpUri es null después de tomar la foto con éxito.")
                Toast.makeText(this, "Error al obtener URI de la foto.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.d("NuevaHabitacionActivity", "La toma de fotos no fue exitosa o fue cancelada.")
            Toast.makeText(this, "No se pudo capturar la foto", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val inspeccionId = intent.getLongExtra("INSPECCION_ID", -1L)
        viewModel.init(inspeccionId)

        // --- OBSERVAR EL NUEVO EVENTO PARA FINALIZAR ---
        lifecycleScope.launch {
            viewModel.finalizarInspeccionEvent.collectLatest { event ->
                when (event) {
                    is NuevaHabitacionViewModel.FinalizarInspeccionEvent.FinalizarAhora -> {
                        Log.d("NuevaHabitacionActivity", "FINALIZAR_EVENTO: Recibido. Preparando para terminar.")
                        val resultCodeToSet = NuevaInspeccionActivity.RESULT_INSPECCION_FINALIZADA
                        Log.d("NuevaHabitacionActivity", "FINALIZAR_EVENTO: Intentando establecer resultado: $resultCodeToSet en hilo: ${Thread.currentThread().name}")

                        // Forzar ejecución en el hilo principal de forma explícita para setResult y finish
                        runOnUiThread {
                            Log.d("NuevaHabitacionActivity", "FINALIZAR_EVENTO (runOnUiThread): Estableciendo resultado: $resultCodeToSet")
                            setResult(resultCodeToSet)
                            Log.d("NuevaHabitacionActivity", "FINALIZAR_EVENTO (runOnUiThread): Llamando a finish().")
                            finish()
                            Log.d("NuevaHabitacionActivity", "FINALIZAR_EVENTO (runOnUiThread): finish() llamado.")
                        }
                        Log.d("NuevaHabitacionActivity", "FINALIZAR_EVENTO: Bloque runOnUiThread despachado.")
                    }
                }
            }
        }
        // --- FIN OBSERVAR ---
        setContent {
            FerjiTheme {
                val context = LocalContext.current
                PantallaNuevaHabitacion(
                    viewModel = viewModel,
                    onTomarFoto = {
                        // Lógica para verificar y solicitar el permiso antes de abrir la cámara
                        if (ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            // Si ya tienes el permiso, lanza la cámara directamente
                            latestTmpUri = getTmpFileUri()
                            cameraLauncher.launch(latestTmpUri)
                        } else {
                            // Si no tienes el permiso, solicítalo al usuario
                            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    onVolver = {
                        Log.d("NuevaHabitacionActivity", "ON_VOLVER: Estableciendo resultado: RESULT_CANCELED")
                        setResult(Activity.RESULT_CANCELED)
                        Log.d("NuevaHabitacionActivity", "ON_VOLVER: Llamando a finish().")
                        finish()
                        Log.d("NuevaHabitacionActivity", "ON_VOLVER: finish() llamado.")
                    }
                    ,
                    // ...
                    onTerminarInspeccion = {
                        Log.d("NuevaHabitacionActivity", "onTerminarInspeccion: Delegando al ViewModel.")
                        viewModel.intentarFinalizarInspeccion()
                        // NO LLAMES A setResult NI A finish() AQUÍ.
                        // Deja que el observador de finalizarInspeccionEvent lo haga.
                    }
                )
            }
        }
    }
}
// NuevaHabitacionActivity.kt
// ... (tus importaciones y la clase Activity se mantienen igual que en el último ejemplo completo)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaNuevaHabitacion(
    viewModel: NuevaHabitacionViewModel,
    onTomarFoto: () -> Unit,
    onVolver: () -> Unit,
    onTerminarInspeccion: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val guardadoState by viewModel.guardadoState.collectAsState()
    val textoOtro by viewModel.textoOtroDano.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(guardadoState) {
        when (val currentGuardadoState = guardadoState) {
            is NuevaHabitacionViewModel.GuardadoState.Exito -> {
                Toast.makeText(
                    context,
                    "Habitación '${currentGuardadoState.nombreHabitacionGuardada}' guardada. Ingrese la siguiente.",
                    Toast.LENGTH_LONG
                ).show()
                viewModel.prepararParaNuevaHabitacion()
            }
            is NuevaHabitacionViewModel.GuardadoState.Error -> {
                Toast.makeText(context, currentGuardadoState.mensaje, Toast.LENGTH_LONG).show()
                viewModel.resetearEstadoGuardado()
            }
            else -> {} // Idle, Cargando
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.nombreHabitacion.isNotBlank()) "Editando: ${state.nombreHabitacion}" else "Nueva Habitación") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            // --- CAMPOS DEL FORMULARIO ---
            OutlinedTextField(
                value = state.nombreHabitacion,
                onValueChange = { viewModel.onNombreChange(it) },
                label = { Text("Nombre habitación") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DanoDropdown(
                    danoSeleccionado = state.danoSeleccionado,
                    onDanoSeleccionado = { viewModel.onDanoSeleccionado(it) },
                    textoOtro = textoOtro,
                    onTextoOtroChange = { viewModel.onTextoOtroDanoChange(it) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = state.alto.takeIf { it != 0 }?.toString() ?: "",
                    onValueChange = { viewModel.onAltoChange(it.toIntOrNull() ?: 0) },
                    label = { Text("Alto (cm)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number // <--- ¡AQUÍ ESTÁ LA CLAVE!
                        // imeAction = ImeAction.Next // Opcional: Cambia el botón Enter a "Siguiente"
                    ),
                    singleLine = true // Recomendado para campos numéricos
                )
                OutlinedTextField(
                    value = state.largo.takeIf { it != 0 }?.toString() ?: "",
                    onValueChange = { viewModel.onLargoChange(it.toIntOrNull() ?: 0) },
                    label = { Text("Largo (cm)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number // <--- TECLADO NUMÉRICO
                        // imeAction = ImeAction.Next // Opcional
                    ),
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.ancho.takeIf { it != 0 }?.toString() ?: "",
                    onValueChange = { viewModel.onAnchoChange(it.toIntOrNull() ?: 0) },
                    label = { Text("Ancho (cm)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number // <--- TECLADO NUMÉRICO
                        // imeAction = ImeAction.Done // Opcional: Cambia el botón Enter a "Hecho" si es el último campo
                    ),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = state.comentarios,
                onValueChange = { viewModel.onComentariosChange(it) },
                label = { Text("Comentarios") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                maxLines = 3
            )

//            // Mostrar fotos tomadas
//            if (state.fotosTomadas.isNotEmpty()) {
//                Text("Fotos:", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
//                state.fotosTomadas.forEach { foto ->
//                    Text(
//                        "Foto: $foto",
//                        modifier = Modifier.padding(vertical = 2.dp)
//                    )
//                }
//                Spacer(modifier = Modifier.height(8.dp)) // Espacio después de las fotos
//            }


            // Espacio para empujar los botones hacia abajo
            Spacer(modifier = Modifier.weight(1f))

            // --- BOTONES DE ACCIÓN ---
            val estaCargando = guardadoState is NuevaHabitacionViewModel.GuardadoState.Cargando

            // Fila para "Tomar Foto" y "Guardar Otra"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp), // Espacio debajo de esta fila de botones
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Botón "Tomar Foto"
                Button(
                    onClick = onTomarFoto,
                    modifier = Modifier.weight(1f) // Ocupa la mitad del espacio disponible
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = "Cámara")
                    Spacer(Modifier.width(3.dp))
                    Text("Tomar Foto", fontSize = 11.sp)
                }

                // Botón "Guardar y Añadir Otra"
                Button(
                    onClick = {
                        if (!estaCargando) {
                            if (state.nombreHabitacion.isBlank()) {
                                Toast.makeText(context, "Ingrese un nombre para la habitación", Toast.LENGTH_SHORT).show()
                            } else if (state.danoSeleccionado.isEmpty()) {
                                Toast.makeText(context, "Por favor, seleccione un tipo de daño", Toast.LENGTH_SHORT).show()
                            } else if (state.danoSeleccionado == "otro" && textoOtro.isBlank()) {
                                Toast.makeText(context, "Por favor, especifique el tipo de daño 'otro'", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.guardarHabitacionConEstado()
                            }
                        }
                    },
                    enabled = !estaCargando,
                    modifier = Modifier.weight(1f) // Ocupa la otra mitad del espacio disponible
                ) {
                    Icon(Icons.Filled.Save, contentDescription = "Guardar y Añadir Otra")
                    Spacer(Modifier.width(4.dp))
                    Text(if (estaCargando) "Guardando..." else "Guardar habitación", fontSize = 11.sp)
                }
            } // Fin del Row de "Tomar Foto" y "Guardar Otra"

            // Botón "Terminar Inspección" (en su propia línea)
            Button(
                onClick = {
                    if (!estaCargando) {

                        onTerminarInspeccion()
                    }
                },
                enabled = !estaCargando,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier.fillMaxWidth() // Ocupa todo el ancho
            ) {
                Icon(Icons.Filled.Done, contentDescription = "Terminar Inspección")
                Spacer(Modifier.width(4.dp))
                Text("Terminar Inspección", fontSize = 13.sp)
            }
            // Fin del botón "Terminar Inspección"

        } // Fin del Column principal
    } // Fin del Scaffold
}


