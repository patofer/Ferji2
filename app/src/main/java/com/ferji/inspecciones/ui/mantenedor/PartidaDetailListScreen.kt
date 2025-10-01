package com.ferji.inspecciones.ui.mantenedor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ferji.inspecciones.data.model.PartidaEntity
// --- INICIO CAMBIO 1: AÑADIR IMPORTS ---
import com.ferji.inspecciones.data.model.UnidadMedida
// --- FIN CAMBIO 1 ---
import com.ferji.inspecciones.viewmodels.PartidaDetailViewModel

/**
 * Pantalla para GESTIONAR (CRUD) las Partidas de Detalle para una Partida Principal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartidaDetailListScreen(
    partidaPrincipalId: Long,
    partidaPrincipalNombre: String,
    viewModel: PartidaDetailViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    LaunchedEffect(partidaPrincipalId) {
        viewModel.loadPartidasDeDetalle(partidaPrincipalId)
    }

    val partidasDeDetalle by viewModel.partidasDeDetalle.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var partidaAEditar by remember { mutableStateOf<PartidaEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(partidaPrincipalNombre) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                partidaAEditar = null
                showDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Añadir Partida de Detalle")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            items(partidasDeDetalle, key = { it.id }) { partida ->
                ListItem(
                    headlineContent = { Text(partida.descripcion) },
                    supportingContent = { Text("Unidad: ${partida.unidad} | Precio: ${partida.precioUnitario}") },
                    trailingContent = {
                        Row {
                            IconButton(onClick = {
                                partidaAEditar = partida
                                showDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar")
                            }
                            IconButton(onClick = { viewModel.eliminarPartidaDetalle(partida) }) {
                                Icon(Icons.Default.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                )
                Divider()
            }
        }
    }

    if (showDialog) {
        PartidaDetalleEditDialog(
            partida = partidaAEditar,
            onDismiss = { showDialog = false },
            onConfirm = { id, descripcion, unidad, precio ->
                viewModel.guardarPartidaDetalle(id, descripcion, unidad, precio)
                showDialog = false
            }
        )
    }
}


/**
 * Diálogo reutilizable para crear o editar una Partida de Detalle, ahora con Dropdown para la unidad.
 */
@OptIn(ExperimentalMaterial3Api::class) // Necesario para ExposedDropdownMenuBox
@Composable
private fun PartidaDetalleEditDialog(
    partida: PartidaEntity?,
    onDismiss: () -> Unit,
    onConfirm: (id: Long?, descripcion: String, unidad: String, precio: Double) -> Unit
) {
    // --- INICIO CAMBIO 2: ESTADOS ADAPTADOS PARA EL DROPDOWN ---
    var descripcion by remember(partida) { mutableStateOf(partida?.descripcion ?: "") }
    var precio by remember(partida) { mutableStateOf(partida?.precioUnitario?.toString() ?: "") }

    // Estado para la unidad de medida, ahora de tipo `UnidadMedida?`
    var selectedUnidad by remember(partida) {
        // Intenta encontrar el enum que coincida con el string de la partida, o null si no hay partida/no coincide.
        mutableStateOf(
            UnidadMedida.values().find { it.name == partida?.unidad }
        )
    }
    // Estado para controlar la expansión del dropdown
    var isDropdownExpanded by remember { mutableStateOf(false) }
    // --- FIN CAMBIO 2 ---

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (partida == null) "Nueva Partida" else "Editar Partida") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth()
                )

                // --- INICIO CAMBIO 3: REEMPLAZO DEL TEXTFIELD POR DROPDOWN ---
                ExposedDropdownMenuBox(
                    expanded = isDropdownExpanded,
                    onExpandedChange = { isDropdownExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(), // Ancla el menú a este campo
                        readOnly = true,
                        value = selectedUnidad?.descripcion ?: "Seleccione una unidad", // Muestra descripción o placeholder
                        onValueChange = {}, // Vacío porque es de solo lectura
                        label = { Text("Unidad de Medida") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) }
                    )

                    ExposedDropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false }
                    ) {
                        UnidadMedida.values().forEach { unidad ->
                            DropdownMenuItem(
                                text = { Text(unidad.descripcion) },
                                onClick = {
                                    selectedUnidad = unidad
                                    isDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                // --- FIN CAMBIO 3 ---

                OutlinedTextField(
                    value = precio,
                    onValueChange = { precio = it },
                    label = { Text("Precio Unitario") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val precioDouble = precio.toDoubleOrNull()
                // --- INICIO CAMBIO 4: VALIDACIÓN Y LLAMADA A onConfirm ---
                val unidadString = selectedUnidad?.name // Obtiene "M2", "ML", etc.
                if (descripcion.isNotBlank() && unidadString != null && precioDouble != null) {
                    onConfirm(partida?.id, descripcion, unidadString, precioDouble)
                }
                // --- FIN CAMBIO 4 ---
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

