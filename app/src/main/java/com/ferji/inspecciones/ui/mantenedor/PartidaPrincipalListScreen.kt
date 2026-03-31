package com.ferji.inspecciones.ui.mantenedor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
 * Pantalla que muestra la lista de Partidas Principales (categorías maestras).
 * Al hacer clic en una, navega a la pantalla de detalles de precios.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartidaPrincipalListScreen(
    viewModel: PartidaPrincipalViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onPartidaPrincipalClick: (id: Long, nombre: String) -> Unit
) {
    val partidasPrincipales by viewModel.partidasPrincipales.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    FerjiTitleBar(
                        subtitle = if (partidasPrincipales.isNotEmpty())
                            "Gestionar Precios · ${partidasPrincipales.size}" else "Gestionar Precios",
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
        }
    ) { padding ->
        if (partidasPrincipales.isEmpty()) {
            FerjiEmptyState(
                icon = "💰",
                title = "Sin categorías",
                subtitle = "Crea categorías en el Maestro de Partidas",
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
                // ═══ RESUMEN ═══
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            FerjiStatCard(
                                value = "${partidasPrincipales.size}",
                                label = "Categorías",
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            FerjiStatCard(
                                value = "${partidasPrincipales.count { it.tipoSuperficie == TipoSuperficie.MURO.name }}",
                                label = "Muros",
                                color = Primary40,
                                modifier = Modifier.weight(1f)
                            )
                            FerjiStatCard(
                                value = "${partidasPrincipales.count { it.tipoSuperficie == TipoSuperficie.PISO.name }}",
                                label = "Pisos",
                                color = Tertiary40,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        FerjiGradientDivider()
                        Text(
                            "Selecciona una categoría para gestionar sus precios",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = Spacing.xs)
                        )
                    }
                }

                // ═══ LISTADO ═══
                items(partidasPrincipales, key = { it.id }) { partida ->
                    CategoriaPrecioCard(
                        partida = partida,
                        onClick = { onPartidaPrincipalClick(partida.id, partida.nombre) }
                    )
                }

                item { Spacer(modifier = Modifier.height(Spacing.lg)) }
            }
        }
    }
}

@Composable
private fun CategoriaPrecioCard(
    partida: PartidaPrincipalEntity,
    onClick: () -> Unit
) {
    val tipoColor = when (partida.tipoSuperficie) {
        TipoSuperficie.MURO.name -> Primary40
        TipoSuperficie.PISO.name -> Tertiary40
        TipoSuperficie.CIELO.name -> Color(0xFF5C6BC0)
        else -> MaterialTheme.colorScheme.outline
    }

    val naturalezaEsFija = partida.naturaleza == PartidaNaturaleza.FIJA

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.level1)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Franja lateral de color
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                    .background(tipoColor)
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ícono tipo superficie
                Box(
                    modifier = Modifier
                        .size(40.dp)
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
                        modifier = Modifier.size(22.dp),
                        tint = tipoColor
                    )
                }

                Spacer(modifier = Modifier.width(Spacing.sm))

                // Nombre y badges
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = partida.nombre,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        // Badge tipo
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = tipoColor.copy(alpha = 0.1f),
                            contentColor = tipoColor
                        ) {
                            Text(
                                text = partida.tipoSuperficie,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        // Badge naturaleza
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (naturalezaEsFija) FerjiOrange.copy(alpha = 0.1f) else Tertiary40.copy(alpha = 0.1f),
                            contentColor = if (naturalezaEsFija) FerjiOrange else Tertiary40
                        ) {
                            Text(
                                text = if (naturalezaEsFija) "Fija" else "Variable",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Flecha
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Ver precios",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
