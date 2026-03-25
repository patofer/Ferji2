package com.ferji.inspecciones

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.ferji.inspecciones.BuildConfig
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.ferji.inspecciones.ui.components.FerjiColoredSectionHeader
import com.ferji.inspecciones.ui.components.FerjiGradientDivider
import com.ferji.inspecciones.ui.components.FerjiTitleBar
import com.ferji.inspecciones.ui.theme.*
import com.ferji.inspecciones.utils.FileUtils
import com.ferji.inspecciones.viewmodels.NuevaHabitacionViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NuevaHabitacionActivity : ComponentActivity() {

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
            Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getTmpFileUri(): Uri {
        val tmpFile = java.io.File.createTempFile("ferji_photo_", ".jpg", cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }
        return FileProvider.getUriForFile(
            applicationContext,
            "${BuildConfig.APPLICATION_ID}.provider",
            tmpFile
        )
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { isSuccess ->
        if (isSuccess) {
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
                title = {
                    FerjiTitleBar(
                        subtitle = if (state.nombreHabitacion.isNotBlank()) state.nombreHabitacion
                        else "Nueva Habitación",
                        compact = true
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Gradiente decorativo de fondo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.md, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // ═══ Nombre de Habitación ═══
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        FerjiColoredSectionHeader(
                            title = "Identificación",
                            backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            icon = {
                                Icon(
                                    Icons.Outlined.MeetingRoom,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        )

                        OutlinedTextField(
                            value = state.nombreHabitacion,
                            onValueChange = viewModel::onNombreChange,
                            label = { Text("Nombre habitación") },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.MeetingRoom,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // ═══ Tipo de Daño ═══
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        FerjiColoredSectionHeader(
                            title = "Tipo de Daño",
                            backgroundColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            icon = {
                                Icon(
                                    Icons.Outlined.Warning,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        )

                        if (categoriasDisponibles.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(Spacing.lg),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                                ) {
                                    Icon(
                                        Icons.Outlined.CloudOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = "No se encontraron categorías de daño",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "Verifique su conexión a internet y presione reintentar para sincronizar las categorías desde el servidor.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
                                        textAlign = TextAlign.Center
                                    )
                                    OutlinedButton(
                                        onClick = { viewModel.reintentarCargaCategorias() },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(Spacing.xs))
                                        Text("Reintentar sincronización")
                                    }
                                }
                            }
                        } else {
                            ExposedDropdownMenuBox(
                                expanded = isCategoriaDropdownExpanded,
                                onExpandedChange = { isCategoriaDropdownExpanded = it },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    readOnly = true,
                                    value = obtenerTextoDeSeleccion(state),
                                    onValueChange = {},
                                    label = { Text("Categoría de Daño") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Outlined.Warning,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoriaDropdownExpanded)
                                    },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                        .clickable { isCategoriaDropdownExpanded = true },
                                    shape = RoundedCornerShape(12.dp)
                                )

                                ExposedDropdownMenu(
                                    expanded = isCategoriaDropdownExpanded,
                                    onDismissRequest = { isCategoriaDropdownExpanded = false }
                                ) {
                                    categoriasDisponibles.forEach { categoria ->
                                        DropdownMenuItem(
                                            text = { Text(categoria.nombre) },
                                            onClick = { viewModel.onCategoriaToggled(categoria) },
                                            leadingIcon = {
                                                Icon(
                                                    if (state.categoriasSeleccionadas.contains(categoria))
                                                        Icons.Filled.CheckCircle
                                                    else
                                                        Icons.Outlined.RadioButtonUnchecked,
                                                    contentDescription = null,
                                                    tint = if (state.categoriasSeleccionadas.contains(categoria))
                                                        MaterialTheme.colorScheme.primary
                                                    else
                                                        MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        )
                                    }

                                    if (categoriasDisponibles.isNotEmpty()) {
                                        HorizontalDivider()
                                    }

                                    DropdownMenuItem(
                                        text = { Text("Otro (daño no listado)") },
                                        onClick = { viewModel.onOtroDanoToggled(!state.otroDanoSeleccionado) },
                                        leadingIcon = {
                                            Icon(
                                                if (state.otroDanoSeleccionado)
                                                    Icons.Filled.CheckCircle
                                                else
                                                    Icons.Outlined.RadioButtonUnchecked,
                                                contentDescription = null,
                                                tint = if (state.otroDanoSeleccionado)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // ═══ Chips de categorías seleccionadas ═══
                if (state.categoriasSeleccionadas.isNotEmpty() || state.otroDanoSeleccionado) {
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        state.categoriasSeleccionadas.forEach { categoria ->
                            InputChip(
                                selected = true,
                                onClick = { viewModel.onCategoriaToggled(categoria) },
                                label = {
                                    Text(
                                        categoria.nombre,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                trailingIcon = {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Quitar ${categoria.nombre}",
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                shape = RoundedCornerShape(20.dp)
                            )
                        }
                        if (state.otroDanoSeleccionado) {
                            InputChip(
                                selected = true,
                                onClick = { viewModel.onOtroDanoToggled(false) },
                                label = {
                                    Text(
                                        "Otro",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                trailingIcon = {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Quitar otro",
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                shape = RoundedCornerShape(20.dp)
                            )
                        }
                    }
                }

                if (state.otroDanoSeleccionado) {
                    OutlinedTextField(
                        value = textoOtro,
                        onValueChange = viewModel::onTextoOtroDanoChange,
                        label = { Text("Especificar otro tipo de daño") },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // ═══ Medidas ═══
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        FerjiColoredSectionHeader(
                            title = "Medidas (cm)",
                            backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            icon = {
                                Icon(
                                    Icons.Outlined.Straighten,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            OutlinedTextField(
                                value = state.largo.takeIf { it != 0 }?.toString() ?: "",
                                onValueChange = { viewModel.onLargoChange(it.toIntOrNull() ?: 0) },
                                label = { Text("Largo") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = state.ancho.takeIf { it != 0 }?.toString() ?: "",
                                onValueChange = { viewModel.onAnchoChange(it.toIntOrNull() ?: 0) },
                                label = { Text("Ancho") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = state.alto.takeIf { it != 0 }?.toString() ?: "",
                                onValueChange = { viewModel.onAltoChange(it.toIntOrNull() ?: 0) },
                                label = { Text("Alto") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                // ═══ Comentarios ═══
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        FerjiColoredSectionHeader(
                            title = "Comentarios",
                            backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            icon = {
                                Icon(
                                    Icons.Outlined.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        )

                        OutlinedTextField(
                            value = state.comentarios,
                            onValueChange = viewModel::onComentariosChange,
                            label = { Text("Comentarios adicionales") },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // ═══ Fotos ═══
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        FerjiColoredSectionHeader(
                            title = "Fotos",
                            backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            icon = {
                                Icon(
                                    Icons.Outlined.PhotoCamera,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        )

                        if (state.fotosTomadas.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No hay fotos tomadas aún",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                            ) {
                                state.fotosTomadas.take(4).forEach { fotoRuta ->
                                    val bitmap = remember(fotoRuta) {
                                        try {
                                            // Cargar con inSampleSize para evitar OOM con fotos grandes
                                            val options = BitmapFactory.Options().apply {
                                                inJustDecodeBounds = true
                                            }
                                            BitmapFactory.decodeFile(fotoRuta, options)
                                            options.inSampleSize = calculateInSampleSize(options, 120, 120)
                                            options.inJustDecodeBounds = false
                                            BitmapFactory.decodeFile(fotoRuta, options)
                                        } catch (_: Exception) {
                                            null
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (bitmap != null) {
                                            Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription = "Foto",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Icon(
                                                Icons.Outlined.BrokenImage,
                                                contentDescription = null,
                                                modifier = Modifier.size(24.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                if (state.fotosTomadas.size > 4) {
                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(FerjiOrange.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "+${state.fotosTomadas.size - 4}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = FerjiOrange
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // ═══ Botones de acción ═══
                FerjiGradientDivider()
                Spacer(modifier = Modifier.height(4.dp))

                val estaCargando = guardadoState is NuevaHabitacionViewModel.GuardadoState.Cargando

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    OutlinedButton(
                        onClick = onTomarFoto,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Outlined.CameraAlt, contentDescription = "Foto", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Foto", style = MaterialTheme.typography.labelMedium)
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
                                        Toast.makeText(context, "Seleccione un tipo de daño", Toast.LENGTH_SHORT).show()
                                    }
                                    otroDanoEstaVacio -> {
                                        Toast.makeText(context, "Especifique el daño en 'Otro'", Toast.LENGTH_SHORT).show()
                                    }
                                    else -> {
                                        viewModel.guardarHabitacionConEstado()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        if (estaCargando) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Icon(Icons.Outlined.Save, contentDescription = "Guardar", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Guardar", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                FilledTonalButton(
                    onClick = onTerminarInspeccion,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = "Terminar", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(Spacing.xs))
                    Text("Terminar Inspección", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(Spacing.sm))
            }
        }
    }
}

/**
 * Función de ayuda para generar el texto que se muestra en el campo de texto del dropdown.
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

/**
 * Calcula un inSampleSize adecuado para cargar bitmaps redimensionados.
 * Evita OOM al cargar fotos de alta resolución para thumbnails pequeños.
 */
private fun calculateInSampleSize(
    options: BitmapFactory.Options,
    reqWidth: Int,
    reqHeight: Int
): Int {
    val (height: Int, width: Int) = options.outHeight to options.outWidth
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

