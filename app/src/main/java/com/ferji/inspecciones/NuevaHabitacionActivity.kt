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
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope


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
        const val EXTRA_INSPECCION_ID = "extra_inspeccion_id_nueva_habitacion"
    }
    private val viewModel: NuevaHabitacionViewModel by viewModels()
    private var latestTmpUri: Uri? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            getTmpFileUri().let { uri ->
                latestTmpUri = uri
                cameraLauncher.launch(uri)
            }
        } else {
            Toast.makeText(this, "Permiso de cámara denegado.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getTmpFileUri(): Uri {
        val tmpFile = File.createTempFile("tmp_image_file", ".jpg", cacheDir).apply {
            createNewFile()
        }
        return FileProvider.getUriForFile(applicationContext, "${BuildConfig.APPLICATION_ID}.provider", tmpFile)
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            latestTmpUri?.let { uri ->
                Log.d("NuevaHabitacionActivity", "Foto tomada y guardada en URI temporal: $uri")
                val nombreArchivo = "habitacion_${System.currentTimeMillis()}.jpg"
                val rutaFotoGuardada = FileUtils.guardarBitmapEnInterno(
                    this,
                    uri,
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

        lifecycleScope.launch {
            viewModel.finalizarInspeccionEvent.collectLatest { event ->
                when (event) {
                    is NuevaHabitacionViewModel.FinalizarInspeccionEvent.FinalizarAhora -> {
                        Log.d("NuevaHabitacionActivity", "FINALIZAR_EVENTO: Recibido. Preparando para terminar.")
                        val resultCodeToSet = NuevaInspeccionActivity.RESULT_INSPECCION_FINALIZADA
                        runOnUiThread {
                            setResult(resultCodeToSet)
                            finish()
                        }
                    }
                }
            }
        }

        setContent {
            FerjiTheme {
                PantallaNuevaHabitacion(
                    viewModel = viewModel,
                    onTomarFoto = {
                        if (ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            getTmpFileUri().let { uri ->
                                latestTmpUri = uri
                                cameraLauncher.launch(uri)
                            }
                        } else {
                            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    onVolver = {
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    },
                    onTerminarInspeccion = {
                        viewModel.intentarFinalizarInspeccion()
                    }
                )
            }
        }
    }
}

// En: C:/Proyectos/GITHUB/Jerji/Ferji2/app/src/main/java/com/ferji/inspecciones/NuevaHabitacionActivity.kt

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
    val categoriasDisponibles by viewModel.listaCategoriasDisponibles.collectAsState()

    val context = LocalContext.current
    var isCategoriaDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(guardadoState) {
        when (val currentGuardadoState = guardadoState) {
            is NuevaHabitacionViewModel.GuardadoState.Exito -> {
                Toast.makeText(
                    context,
                    "Habitación '${currentGuardadoState.nombreHabitacionGuardada}' guardada.",
                    Toast.LENGTH_LONG
                ).show()
                viewModel.prepararParaNuevaHabitacion()
            }
            is NuevaHabitacionViewModel.GuardadoState.Error -> {
                Toast.makeText(context, currentGuardadoState.mensaje, Toast.LENGTH_LONG).show()
                viewModel.resetearEstadoGuardado()
            }
            else -> {}
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
            OutlinedTextField(
                value = state.nombreHabitacion,
                onValueChange = viewModel::onNombreChange,
                label = { Text("Nombre habitación") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            Text("Tipo de Daño", style = MaterialTheme.typography.titleMedium)
            Divider(modifier = Modifier.padding(vertical = 8.dp))

            ExposedDropdownMenuBox(
                expanded = isCategoriaDropdownExpanded,
                onExpandedChange = { isCategoriaDropdownExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    readOnly = true,
                    // --- INICIO DE LA MODIFICACIÓN: Lógica para mostrar selección múltiple ---
                    value = obtenerTextoDeSeleccion(state),
                    // --- FIN DE LA MODIFICACIÓN ---
                    onValueChange = {},
                    label = { Text("Categoría de Daño") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoriaDropdownExpanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        // Hacemos que el campo de texto sea clickeable para abrir el menú
                        .clickable { isCategoriaDropdownExpanded = true }
                )

                ExposedDropdownMenu(
                    expanded = isCategoriaDropdownExpanded,
                    onDismissRequest = { isCategoriaDropdownExpanded = false }
                ) {
                    // Ítems dinámicos de la base de datos
                    categoriasDisponibles.forEach { categoria ->
                        DropdownMenuItem(
                            text = { Text(categoria.nombre) },
                            onClick = {
                                // --- INICIO DE LA MODIFICACIÓN: Lógica de selección múltiple ---
                                // Simplemente alterna la selección de esta categoría
                                viewModel.onCategoriaToggled(categoria)
                                // No cerramos el menú para permitir múltiples selecciones
                                // isCategoriaDropdownExpanded = false
                                // --- FIN DE LA MODIFICACIÓN ---
                            },
                            // Mantenemos el ícono de 'check' para feedback visual
                            leadingIcon = {
                                if (state.categoriasSeleccionadas.contains(categoria)) {
                                    Icon(Icons.Filled.Done, contentDescription = "Seleccionado")
                                }
                            }
                        )
                    }

                    if (categoriasDisponibles.isNotEmpty()) {
                        Divider()
                    }

                    // Ítem estático para "Otro"
                    DropdownMenuItem(
                        text = { Text("Otro (daño no listado)") },
                        onClick = {
                            // --- INICIO DE LA MODIFICACIÓN: Lógica de selección múltiple ---
                            // Simplemente alterna la selección de "Otro"
                            viewModel.onOtroDanoToggled(!state.otroDanoSeleccionado)
                            // No cerramos el menú
                            // isCategoriaDropdownExpanded = false
                            // --- FIN DE LA MODIFICACIÓN ---
                        },
                        leadingIcon = {
                            if (state.otroDanoSeleccionado) {
                                Icon(Icons.Filled.Done, contentDescription = "Seleccionado")
                            }
                        }
                    )
                }
            }

            // El campo para especificar "Otro" solo aparece si la opción está seleccionada
            if (state.otroDanoSeleccionado) {
                OutlinedTextField(
                    value = textoOtro,
                    onValueChange = viewModel::onTextoOtroDanoChange,
                    label = { Text("Especificar otro tipo de daño") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                )
            }

            // --- El resto de tu UI (medidas, botones, etc.) no necesita cambios ---
            Row( // Campos de medidas (sin cambios)
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = state.alto.takeIf { it != 0 }?.toString() ?: "",
                    onValueChange = { viewModel.onAltoChange(it.toIntOrNull() ?: 0) },
                    label = { Text("Alto (cm)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.largo.takeIf { it != 0 }?.toString() ?: "",
                    onValueChange = { viewModel.onLargoChange(it.toIntOrNull() ?: 0) },
                    label = { Text("Largo (cm)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.ancho.takeIf { it != 0 }?.toString() ?: "",
                    onValueChange = { viewModel.onAnchoChange(it.toIntOrNull() ?: 0) },
                    label = { Text("Ancho (cm)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
            OutlinedTextField( // Comentarios (sin cambios)
                value = state.comentarios,
                onValueChange = viewModel::onComentariosChange,
                label = { Text("Comentarios") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                maxLines = 3
            )

            Spacer(modifier = Modifier.weight(1f)) // Espacio flexible

            val estaCargando = guardadoState is NuevaHabitacionViewModel.GuardadoState.Cargando

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onTomarFoto, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = "Tomar Foto")
                }

                Button(
                    onClick = {
                        if (!estaCargando) {
                            val categoriasEstanVacias = state.categoriasSeleccionadas.isEmpty()
                            val otroDanoEstaVacio = state.otroDanoSeleccionado && textoOtro.isBlank()

                            when {
                                state.nombreHabitacion.isBlank() -> {
                                    Toast.makeText(context, "Ingrese un nombre para la habitación", Toast.LENGTH_SHORT).show()
                                }
                                categoriasEstanVacias && !state.otroDanoSeleccionado -> {
                                    Toast.makeText(context, "Por favor, seleccione un tipo de daño", Toast.LENGTH_SHORT).show()
                                }
                                otroDanoEstaVacio -> {
                                    Toast.makeText(context, "Por favor, especifique el tipo de daño 'otro'", Toast.LENGTH_SHORT).show()
                                }
                                else -> {
                                    viewModel.guardarHabitacionConEstado()
                                }
                            }
                        }
                    },
                    enabled = !estaCargando,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Save, contentDescription = "Guardar y Añadir Otra")
                    Spacer(Modifier.width(4.dp))
                    Text(if (estaCargando) "Guardando..." else "Guardar", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            Button(
                onClick = { if (!estaCargando) onTerminarInspeccion() },
                enabled = !estaCargando,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Icon(Icons.Filled.Done, contentDescription = "Terminar Inspección")
                Spacer(Modifier.width(4.dp))
                Text("Terminar Inspección")
            }
        }
    }
}

/**
 * Función de ayuda para generar el texto que se muestra en el campo de texto del dropdown,
 * basado en las selecciones múltiples.
 */
@Composable
private fun obtenerTextoDeSeleccion(state: NuevaHabitacionViewModel.NuevaHabitacionState): String {
    val selecciones = state.categoriasSeleccionadas.map { it.nombre }.toMutableList()
    if (state.otroDanoSeleccionado) {
        selecciones.add("Otro")
    }

    return when {
        selecciones.isEmpty() -> "Seleccione una o más opciones"
        selecciones.size <= 2 -> selecciones.joinToString(", ")
        else -> "${selecciones.take(2).joinToString(", ")} y ${selecciones.size - 2} más"
    }
}
