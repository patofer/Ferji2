package com.ferji.inspecciones.ui.mantenedor

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ferji.inspecciones.data.model.PartidaNaturaleza
import com.ferji.inspecciones.data.model.PartidaPrincipalEntity
import com.ferji.inspecciones.data.model.TipoSuperficie
import com.ferji.inspecciones.ui.components.FerjiEmptyState
import com.ferji.inspecciones.ui.components.FerjiGradientDivider
import com.ferji.inspecciones.ui.components.FerjiStatCard
import com.ferji.inspecciones.ui.components.FerjiTitleBar
import com.ferji.inspecciones.ui.theme.*
import com.ferji.inspecciones.viewmodels.PartidaPrincipalViewModel

/**
 * Pantalla para gestionar (CRUD) las Partidas Principales.
 * Al hacer clic en un ítem, navega a su pantalla de detalle.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaestroPartidasScreen(
    viewModel: PartidaPrincipalViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onPartidaClick: (id: Long, nombre: String) -> Unit
) {
    val partidas by viewModel.partidasPrincipales.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var partidaAEditar by remember { mutableStateOf<PartidaPrincipalEntity?>(null) }
    var partidaAEliminar by remember { mutableStateOf<PartidaPrincipalEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    FerjiTitleBar(
                        subtitle = if (partidas.isNotEmpty()) "Maestro de Partidas · ${partidas.size}" else "Maestro de Partidas",
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
        if (partidas.isEmpty()) {
            FerjiEmptyState(
                icon = "📋",
                title = "Sin partidas",
                subtitle = "Añade una con el botón '+'",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(horizontal = Spacing.base, vertical = Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                // ═══ RESUMEN / ESTADÍSTICAS ═══
                item {
                    val variables = partidas.count { it.naturaleza == PartidaNaturaleza.VARIABLE }
                    val fijas = partidas.count { it.naturaleza == PartidaNaturaleza.FIJA }

                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            FerjiStatCard(
                                value = "${partidas.size}",
                                label = "Total",
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            FerjiStatCard(
                                value = "$variables",
                                label = "Variables",
                                color = Tertiary40,
                                modifier = Modifier.weight(1f)
                            )
                            FerjiStatCard(
                                value = "$fijas",
                                label = "Fijas",
                                color = FerjiOrange,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        FerjiGradientDivider()
                    }
                }

                // ═══ LISTADO DE PARTIDAS ═══
                items(partidas, key = { it.id }) { partida ->
                    PartidaPrincipalCard(
                        partida = partida,
                        onClick = { onPartidaClick(partida.id, partida.nombre) },
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
        PartidaEditDialog(
            partida = partidaAEditar,
            onDismiss = { showDialog = false },
            onConfirm = { id, nombre, tipoSuperficie, naturaleza ->
                viewModel.guardarPartida(id, nombre, tipoSuperficie.name, naturaleza)
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
            title = {
                Text("Eliminar Partida", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text("¿Eliminar \"${partida.nombre}\"?")
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        "⚠️ Se eliminará la partida y todas sus sub-partidas asociadas.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.eliminarPartida(partida)
                        partidaAEliminar = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { partidaAEliminar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// ═══════════════════════════════════════════
// TARJETA DE PARTIDA PRINCIPAL — REDISEÑADA
// ═══════════════════════════════════════════

@Composable
private fun PartidaPrincipalCard(
    partida: PartidaPrincipalEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val tipoColor = when (partida.tipoSuperficie) {
        TipoSuperficie.MURO.name -> Primary40
        TipoSuperficie.PISO.name -> Tertiary40
        TipoSuperficie.CIELO.name -> Color(0xFF5C6BC0)
        else -> MaterialTheme.colorScheme.outline
    }

    val naturalezaEsFija = partida.naturaleza == PartidaNaturaleza.FIJA
    val naturalezaIcon: ImageVector = if (naturalezaEsFija) Icons.Outlined.Lock else Icons.Outlined.Tune
    val naturalezaLabel = if (naturalezaEsFija) "Fija" else "Variable"
    val naturalezaColor = if (naturalezaEsFija) FerjiOrange else Tertiary40

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.level1)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // ── Franja lateral de color por tipo ──
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                    .background(tipoColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(Spacing.md)
            ) {
                // ── Header: Ícono + Nombre + Flecha ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Ícono tipo superficie
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(tipoColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (partida.tipoSuperficie) {
                                TipoSuperficie.MURO.name -> Icons.Outlined.GridOn
                                TipoSuperficie.PISO.name -> Icons.Outlined.Layers
                                TipoSuperficie.CIELO.name -> Icons.Outlined.Roofing
                                else -> Icons.Outlined.Category
                            },
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = tipoColor
                        )
                    }

                    Spacer(modifier = Modifier.width(Spacing.sm))

                    // Nombre
                    Text(
                        text = partida.nombre,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Flecha de navegación
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Ver detalles",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.sm))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(Spacing.sm))

                // ── Footer: Badges + Acciones ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Badges
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        // Badge tipo superficie
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = tipoColor.copy(alpha = 0.1f),
                            contentColor = tipoColor
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = when (partida.tipoSuperficie) {
                                        TipoSuperficie.MURO.name -> Icons.Outlined.GridOn
                                        TipoSuperficie.PISO.name -> Icons.Outlined.Layers
                                        TipoSuperficie.CIELO.name -> Icons.Outlined.Roofing
                                        else -> Icons.Outlined.Category
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = partida.tipoSuperficie ?: "—",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Badge naturaleza
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = naturalezaColor.copy(alpha = 0.1f),
                            contentColor = naturalezaColor
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = naturalezaIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = naturalezaLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Botones de acción
                    Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Edit,
                                contentDescription = "Editar",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(32.dp)
                        ) {
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

/**
 * Diálogo para editar/crear el nombre de una PartidaPrincipal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PartidaEditDialog(
    partida: PartidaPrincipalEntity?,
    onDismiss: () -> Unit,
    onConfirm: (id: Long?, nombre: String, tipoSuperficie: TipoSuperficie, naturaleza: PartidaNaturaleza) -> Unit
) {
    var nombre by remember(partida) { mutableStateOf(partida?.nombre ?: "") }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    // Estado para el tipo de superficie (muro, piso, etc.)
    var selectedTipo by remember(partida) {
        mutableStateOf(
            TipoSuperficie.values().find { it.name == partida?.tipoSuperficie } ?: TipoSuperficie.MURO
        )
    }

    // --- ESTADO PARA LA NATURALEZA DE LA PARTIDA ---
    var selectedNaturaleza by remember(partida) {
        mutableStateOf(partida?.naturaleza ?: PartidaNaturaleza.VARIABLE)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (partida == null) "Nueva Partida" else "Editar Partida") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // ... (TextField del Nombre y Dropdown de TipoSuperficie se mantienen igual) ...

                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre de la Partida") },
                    //...
                )

                ExposedDropdownMenuBox(
                    expanded = isDropdownExpanded,
                    onExpandedChange = { isDropdownExpanded = it },
                    // ...
                ) {
                    // ... (código del DropdownMenu de TipoSuperficie)
                }

                // --- SELECTOR PARA LA NATURALEZA DE LA PARTIDA ---
                Column {
                    Text("Naturaleza de la Partida", style = MaterialTheme.typography.bodyMedium)
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedNaturaleza == PartidaNaturaleza.VARIABLE,
                            onClick = { selectedNaturaleza = PartidaNaturaleza.VARIABLE }
                        )
                        Text("Variable (por m², ml, etc.)", Modifier.clickable { selectedNaturaleza = PartidaNaturaleza.VARIABLE })
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedNaturaleza == PartidaNaturaleza.FIJA,
                            onClick = { selectedNaturaleza = PartidaNaturaleza.FIJA }
                        )
                        Text("Fija (Global)", Modifier.clickable { selectedNaturaleza = PartidaNaturaleza.FIJA })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nombre.isNotBlank()) {
                        onConfirm(partida?.id, nombre, selectedTipo,selectedNaturaleza)
                    }
                },
                enabled = nombre.isNotBlank()
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
