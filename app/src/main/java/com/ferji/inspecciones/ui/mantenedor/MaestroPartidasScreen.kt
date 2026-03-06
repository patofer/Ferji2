package com.ferji.inspecciones.ui.mantenedor

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ferji.inspecciones.data.model.PartidaNaturaleza
import com.ferji.inspecciones.data.model.PartidaPrincipalEntity
import com.ferji.inspecciones.data.model.TipoSuperficie
import com.ferji.inspecciones.ui.components.FerjiEmptyState
import com.ferji.inspecciones.ui.components.FerjiStatusBadge
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Maestro de Partidas", style = MaterialTheme.typography.titleLarge)
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
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(horizontal = Spacing.base, vertical = Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                items(partidas, key = { it.id }) { partida ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPartidaClick(partida.id, partida.nombre) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.level1)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    partida.nombre,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(Spacing.xs))
                                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                    FerjiStatusBadge(
                                        text = partida.tipoSuperficie ?: "—",
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    FerjiStatusBadge(
                                        text = if (partida.naturaleza == PartidaNaturaleza.FIJA) "Fija" else "Variable",
                                        color = if (partida.naturaleza == PartidaNaturaleza.FIJA) FerjiOrange else Tertiary40
                                    )
                                }
                            }
                            IconButton(onClick = {
                                partidaAEditar = partida
                                showDialog = true
                            }) {
                                Icon(
                                    Icons.Outlined.Edit,
                                    contentDescription = "Editar",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { viewModel.eliminarPartida(partida) }) {
                                Icon(
                                    Icons.Outlined.Delete,
                                    contentDescription = "Eliminar",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = "Ver detalles",
                                tint = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) } // Space for FAB
            }
        }
    }

    if (showDialog) {
        PartidaEditDialog(
            partida = partidaAEditar,
            onDismiss = { showDialog = false },
            // Actualizamos la función onConfirm para pasar el nuevo dato
            onConfirm = { id, nombre, tipoSuperficie,naturaleza ->
                // NOTA: Tendrás que actualizar viewModel.guardarPartida para que acepte este nuevo campo
                viewModel.guardarPartida(id, nombre, tipoSuperficie.name,naturaleza)
                showDialog = false
            }
        )
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
