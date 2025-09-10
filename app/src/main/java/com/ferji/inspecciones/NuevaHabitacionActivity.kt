package com.ferji.inspecciones

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

import com.ferji.inspecciones.ui.theme.FerjiTheme
import com.ferji.inspecciones.utils.FileUtils
import com.ferji.inspecciones.viewmodels.NuevaHabitacionViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NuevaHabitacionActivity : ComponentActivity() {

    private val viewModel: NuevaHabitacionViewModel by viewModels()

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

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            val nombreArchivo = "habitacion_${System.currentTimeMillis()}.jpg"
            Log.d("NuevaHabitacionActivity", "Intentando guardar bitmap como: $nombreArchivo")

            val rutaFoto = FileUtils.guardarBitmapEnInterno(
                this,
                it,   // Bitmap de la foto
                nombreArchivo
            )

            if (rutaFoto.isNotBlank()) {
                Log.d("NuevaHabitacionActivity", "Bitmap guardado en: $rutaFoto")
                viewModel.agregarFoto(rutaFoto)
            } else {
                Log.e("NuevaHabitacionActivity", "Error: rutaFoto está vacía")
                Toast.makeText(this, "Error al guardar la foto.", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this, "No se pudo capturar la foto", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val inspeccionId = intent.getLongExtra("INSPECCION_ID", -1L)
        viewModel.init(inspeccionId)
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
                            cameraLauncher.launch(null)
                        } else {
                            // Si no tienes el permiso, solicítalo al usuario
                            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    onVolver = { finish() },
                    onGuardarCompleto = { habitacionId ->
                        finish()
                    }
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaNuevaHabitacion(
    viewModel: NuevaHabitacionViewModel,
    onTomarFoto: () -> Unit,
    onVolver: () -> Unit,
    onGuardarCompleto: (Long) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val guardadoState by viewModel.guardadoState.collectAsState()
    val context = LocalContext.current

    // Observar el estado de guardado
    LaunchedEffect(guardadoState) {
        when (guardadoState) {
            is NuevaHabitacionViewModel.GuardadoState.Exito -> {
                val id = (guardadoState as NuevaHabitacionViewModel.GuardadoState.Exito).habitacionId
                onGuardarCompleto(id)
            }
            is NuevaHabitacionViewModel.GuardadoState.Error -> {
                val error = (guardadoState as NuevaHabitacionViewModel.GuardadoState.Error).mensaje
                android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_LONG).show()
                viewModel.resetearEstadoGuardado()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nueva Habitación - ${state.nombreHabitacion}") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            val estaCargando = guardadoState is NuevaHabitacionViewModel.GuardadoState.Cargando

            ExtendedFloatingActionButton(
                onClick = { // onClick siempre tiene una lambda
                    if (!estaCargando) { // Solo ejecuta la lógica si NO está cargando
                        if (state.nombreHabitacion.isNotBlank()) {
                            viewModel.guardarHabitacionConEstado()
                        } else {
                            android.widget.Toast.makeText(
                                context,
                                "Ingresa un nombre para la habitación",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        // Opcional: Podrías mostrar un Toast indicando que ya se está guardando
                        // android.widget.Toast.makeText(context, "Guardando...", android.widget.Toast.LENGTH_SHORT).show()
                        // O simplemente no hacer nada si el usuario hace clic mientras carga
                    }
                },
                icon = {
                    // El icono podría seguir cambiando si lo deseas, o mantenerse fijo
                    val icono = if (estaCargando) {
                        // Podrías usar un CircularProgressIndicator aquí si lo deseas
                        // o un icono diferente para indicar carga
                        Icons.Filled.Save // O, por ejemplo, Icons.Filled.HourglassTop
                    } else {
                        Icons.Filled.Save
                    }
                    Icon(icono, contentDescription = "Guardar")
                },
                text = {
                    // El texto podría seguir cambiando
                    val texto = if (estaCargando) {
                        "Guardando..."
                    } else {
                        "Guardar Habitación"
                    }
                    Text(texto)
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Campo nombre
            OutlinedTextField(
                value = state.nombreHabitacion,
                onValueChange = { viewModel.onNombreChange(it) },
                label = { Text("Nombre habitación") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            // Campos dimensiones
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = state.alto.toString(),
                    onValueChange = { viewModel.onAltoChange(it.toIntOrNull()?: 0) },
                    label = { Text("Alto (m)") },
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = state.largo.toString(),
                    onValueChange = { viewModel.onLargoChange(it.toIntOrNull() ?: 0) },
                    label = { Text("Largo (m)") },
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = state.ancho.toString(),
                    onValueChange = { viewModel.onAnchoChange(it.toIntOrNull() ?: 0) },
                    label = { Text("Ancho (m)") },
                    modifier = Modifier.weight(1f)
                )
            }

            // Comentarios
            OutlinedTextField(
                value = state.comentarios,
                onValueChange = { viewModel.onComentariosChange(it) },
                label = { Text("Comentarios") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                maxLines = 3
            )

            // Selector de daños
            Text(
                "Tipo de daños:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            // Botón para tomar foto
            Button(
                onClick = onTomarFoto,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
            ) {
                Icon(Icons.Filled.CameraAlt, contentDescription = "Cámara")
                Spacer(Modifier.width(8.dp))
                Text("Tomar Foto")
            }

            // Mostrar fotos tomadas
            state.fotosTomadas.forEach { foto ->
                Text(
                    "Foto: $foto",
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}