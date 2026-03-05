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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ferji.inspecciones.data.model.PartidaEntity
import com.ferji.inspecciones.data.model.UnidadMedida
import com.ferji.inspecciones.viewmodels.PartidaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartidaDetailListScreen(
    partidaPrincipalId: Long,
    partidaPrincipalNombre: String,
    viewModel: PartidaViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    LaunchedEffect(partidaPrincipalId) {
        viewModel.cargarPartidasDe(partidaPrincipalId)
    }

    val partidasHijas by viewModel.partidasDePrincipal.collectAsState()
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
                Icon(Icons.Default.Add, contentDescription = "Añadir Partida")
            }
        }
    ) { padding ->
        if (partidasHijas.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Aún no hay partidas. Presiona el botón '+' para añadir.")
            }
        } else {
            // --- LA CORRECCIÓN CLAVE ESTÁ AQUÍ ---
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 80.dp) // Espacio extra para que el FAB no tape los últimos ítems
            ) {
                items(partidasHijas, key = { it.id }) { partida ->
                    ListItem(
                        headlineContent = { Text(partida.descripcion) },
                        supportingContent = { Text("Precio: $${partida.precioUnitario} / ${partida.unidad}") },
                        trailingContent = {
                            Row {
                                IconButton(onClick = {
                                    partidaAEditar = partida
                                    showDialog = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar")
                                }
                                IconButton(onClick = { viewModel.eliminarPartida(partida) }) {
                                    Icon(Icons.Default.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    )
                    Divider()
                }
            }
        }
    }

    if (showDialog) {
        PartidaDetalleEditDialog(
            partida = partidaAEditar,
            onDismiss = { showDialog = false },
            onConfirm = { id, descripcion, unidad, precio ->
                viewModel.crearOActualizarPartida(
                    id = id,
                    descripcion = descripcion,
                    unidad = UnidadMedida.valueOf(unidad),
                    precio = precio,
                    partidaPrincipalId = partidaPrincipalId
                )
                showDialog = false
            }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PartidaDetalleEditDialog(
    partida: PartidaEntity?,
    onDismiss: () -> Unit,
    onConfirm: (id: Long?, descripcion: String, unidad: String, precio: Double) -> Unit
) {
    var descripcion by remember(partida) { mutableStateOf(partida?.descripcion ?: "") }
    var precio by remember(partida) { mutableStateOf(partida?.precioUnitario?.toString() ?: "") }
    var selectedUnidad by remember(partida) { mutableStateOf(UnidadMedida.values().find { it.name == partida?.unidad }) }
    var isDropdownExpanded by remember { mutableStateOf(false) }

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

                ExposedDropdownMenuBox(
                    expanded = isDropdownExpanded,
                    onExpandedChange = { isDropdownExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        readOnly = true,
                        value = selectedUnidad?.descripcion ?: "Seleccione una unidad",
                        onValueChange = {},
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
            Button(
                onClick = {
                    val precioDouble = precio.toDoubleOrNull()
                    val unidadString = selectedUnidad?.name
                    if (descripcion.isNotBlank() && unidadString != null && precioDouble != null) {
                        onConfirm(partida?.id, descripcion, unidadString, precioDouble)
                    }
                },
                enabled = descripcion.isNotBlank() && selectedUnidad != null && precio.toDoubleOrNull() != null
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

