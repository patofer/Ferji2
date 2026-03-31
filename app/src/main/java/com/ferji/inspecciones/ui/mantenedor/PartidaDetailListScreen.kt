package com.ferji.inspecciones.ui.mantenedor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ferji.inspecciones.data.model.PartidaEntity
import com.ferji.inspecciones.data.model.UnidadMedida
import com.ferji.inspecciones.ui.components.FerjiEmptyState
import com.ferji.inspecciones.ui.components.FerjiGradientDivider
import com.ferji.inspecciones.ui.components.FerjiTitleBar
import com.ferji.inspecciones.ui.theme.*
import com.ferji.inspecciones.viewmodels.PartidaViewModel
import java.text.NumberFormat
import java.util.Locale

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
    var partidaAEliminar by remember { mutableStateOf<PartidaEntity?>(null) }

    val formatoPrecio = remember { NumberFormat.getNumberInstance(Locale("es", "CL")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    FerjiTitleBar(
                        subtitle = if (partidasHijas.isNotEmpty())
                            "$partidaPrincipalNombre · ${partidasHijas.size}" else partidaPrincipalNombre,
                        compact = true
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    partidaAEditar = null
                    showDialog = true
                },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text("Nueva Partida")
            }
        }
    ) { padding ->
        if (partidasHijas.isEmpty()) {
            FerjiEmptyState(
                icon = "💲",
                title = "Sin partidas de detalle",
                subtitle = "Añade sub-partidas con el botón '+'",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = Spacing.base, vertical = Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                // ═══ RESUMEN ═══
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        // Info de la categoría
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(Spacing.md),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                            ) {
                                Icon(
                                    Icons.Outlined.PriceChange,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = partidaPrincipalNombre,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${partidasHijas.size} sub-partida${if (partidasHijas.size != 1) "s" else ""} registrada${if (partidasHijas.size != 1) "s" else ""}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        FerjiGradientDivider()
                    }
                }

                // ═══ LISTADO DE SUB-PARTIDAS ═══
                items(partidasHijas, key = { it.id }) { partida ->
                    PartidaDetalleCard(
                        partida = partida,
                        formatoPrecio = formatoPrecio,
                        onEdit = {
                            partidaAEditar = partida
                            showDialog = true
                        },
                        onDelete = {
                            partidaAEliminar = partida
                        }
                    )
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    // ═══ DIÁLOGO CREAR/EDITAR ═══
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

    // ═══ DIÁLOGO CONFIRMAR ELIMINACIÓN ═══
    if (partidaAEliminar != null) {
        val partida = partidaAEliminar!!
        AlertDialog(
            onDismissRequest = { partidaAEliminar = null },
            icon = {
                Icon(
                    Icons.Outlined.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Eliminar Sub-Partida", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text("¿Eliminar \"${partida.descripcion}\"?")
                    Text(
                        "Precio: $${formatoPrecio.format(partida.precioUnitario)} / ${partida.unidad}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.eliminarPartida(partida)
                        partidaAEliminar = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { partidaAEliminar = null }) { Text("Cancelar") }
            }
        )
    }
}

// ═══════════════════════════════════════════
// TARJETA DE SUB-PARTIDA — REDISEÑADA
// ═══════════════════════════════════════════

@Composable
private fun PartidaDetalleCard(
    partida: PartidaEntity,
    formatoPrecio: NumberFormat,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.level1)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Franja lateral verde (precio)
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                    .background(FerjiGreen)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(Spacing.md)
            ) {
                // ── Header: Descripción ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Ícono
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(FerjiGreen.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Payments,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = FerjiGreen
                        )
                    }

                    Spacer(modifier = Modifier.width(Spacing.sm))

                    Text(
                        text = partida.descripcion,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.sm))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(Spacing.sm))

                // ── Footer: Precio + Unidad + Acciones ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Badge precio
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = FerjiGreen.copy(alpha = 0.1f),
                            contentColor = FerjiGreen
                        ) {
                            Text(
                                text = "$ ${formatoPrecio.format(partida.precioUnitario)}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                        // Badge unidad
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ) {
                            Text(
                                text = partida.unidad,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Botones
                    Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Outlined.Edit,
                                contentDescription = "Editar",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = "Eliminar",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════
// DIÁLOGO CREAR/EDITAR SUB-PARTIDA
// ═══════════════════════════════════════════

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
        title = { Text(if (partida == null) "Nueva Sub-Partida" else "Editar Sub-Partida") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
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
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                        shape = RoundedCornerShape(10.dp)
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
                    label = { Text("Precio Unitario ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    leadingIcon = {
                        Icon(Icons.Outlined.AttachMoney, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
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
