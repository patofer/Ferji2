package com.ferji.inspecciones

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ferji.inspecciones.data.database.AppDatabase
import com.ferji.inspecciones.data.repository.InspeccionRepository
import com.ferji.inspecciones.ui.components.FerjiEmptyState
import com.ferji.inspecciones.ui.components.FerjiGradientDivider
import com.ferji.inspecciones.ui.components.FerjiInfoRow
import com.ferji.inspecciones.ui.components.FerjiStatCard
import com.ferji.inspecciones.ui.components.FerjiStatusBadge
import com.ferji.inspecciones.ui.components.FerjiTitleBar
import com.ferji.inspecciones.ui.components.FerjiBrandBanner
import com.ferji.inspecciones.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*



class ListaInspeccionesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = AppDatabase.getDatabase(this)
        val repository = InspeccionRepository(database.inspeccionDao())

        setContent {
            FerjiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PantallaListaInspecciones(repository, onBack = { finish() })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaListaInspecciones(
    repository: InspeccionRepository,
    onBack: () -> Unit = {}
) {
    var inspecciones by remember { mutableStateOf(emptyList<com.ferji.inspecciones.data.model.InspeccionEntity>()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        launch(Dispatchers.IO) {
            try {
                repository.getAllInspecciones().collect { lista ->
                    inspecciones = lista
                    isLoading = false
                }
            } catch (e: Exception) {
                error = e.message
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    FerjiTitleBar(
                        subtitle = if (inspecciones.isNotEmpty()) "Inspecciones · ${inspecciones.size}" else "Inspecciones",
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
    ) { innerPadding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        strokeWidth = 3.dp
                    )
                }
            }
            error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.padding(Spacing.xl),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(Spacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            Icon(
                                Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                "Error: $error",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
            inspecciones.isEmpty() -> {
                FerjiEmptyState(
                    icon = "📭",
                    title = "No hay inspecciones guardadas",
                    subtitle = "Crea una nueva inspección para comenzar",
                    modifier = Modifier.padding(innerPadding)
                )
            }
            else -> {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    // Gradiente decorativo de fondo
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = Spacing.base, vertical = Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        // Resumen de inspecciones con identidad visual
                        item {
                            val pendientes = inspecciones.count { it.estado == "PENDIENTE" }
                            val completadas = inspecciones.count { it.estado == "COMPLETADA" }

                            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                // Banner con identidad
                                FerjiBrandBanner(
                                    title = "Inspecciones",
                                    subtitle = "${inspecciones.size} registro${if (inspecciones.size != 1) "s" else ""} encontrado${if (inspecciones.size != 1) "s" else ""}"
                                )

                                // Estadísticas con color
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                                ) {
                                    FerjiStatCard(
                                        value = "${inspecciones.size}",
                                        label = "Total",
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (pendientes > 0) {
                                        FerjiStatCard(
                                            value = "$pendientes",
                                            label = "Pendientes",
                                            color = FerjiOrange,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (completadas > 0) {
                                        FerjiStatCard(
                                            value = "$completadas",
                                            label = "Completadas",
                                            color = FerjiGreen,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                FerjiGradientDivider()
                            }
                        }

                        items(inspecciones) { inspeccion ->
                            TarjetaInspeccion(inspeccion = inspeccion)
                        }
                        item { Spacer(modifier = Modifier.height(Spacing.lg)) }
                    }
                }
            }
        }
    }
}

@Composable
fun TarjetaInspeccion(inspeccion: com.ferji.inspecciones.data.model.InspeccionEntity) {
    val statusColor = when (inspeccion.estado) {
        "PENDIENTE" -> FerjiOrange
        "COMPLETADA" -> FerjiGreen
        else -> MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.level1)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Franja de color lateral indicando estado
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                    .background(statusColor)
            )

            Column(modifier = Modifier.weight(1f).padding(Spacing.md)) {
                // Header con ID y Estado
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Outlined.Assignment,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = "Inspección #${inspeccion.id}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    FerjiStatusBadge(
                        text = inspeccion.estado,
                        color = statusColor
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.sm))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(Spacing.sm))

                // Datos
                FerjiInfoRow(label = "RUT", value = inspeccion.rut)
                FerjiInfoRow(label = "Siniestro", value = inspeccion.siniestro)
                FerjiInfoRow(label = "Dirección", value = inspeccion.direccion)
                FerjiInfoRow(label = "Inspector", value = inspeccion.rutInspector)
                FerjiInfoRow(label = "Email", value = inspeccion.mail)

                Spacer(modifier = Modifier.height(Spacing.xs))

                // Fecha
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Icon(
                        Icons.Outlined.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.width(Spacing.xxs))
                    Text(
                        text = formatearFecha(inspeccion.fechaCreacion),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

fun formatearFecha(date: Date): String {
    val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return format.format(date)
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PantallaListaInspeccionesPreview() {
    FerjiTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.lg)
        ) {
            Text(
                "Inspecciones Guardadas",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Vista previa - Los datos reales se cargan desde la BD",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview
@Composable
fun TarjetaInspeccionPreview() {
    FerjiTheme {
        TarjetaInspeccion(
            inspeccion = com.ferji.inspecciones.data.model.InspeccionEntity(
                id = 1,
                rut = "12.345.678-9",
                siniestro = "SIS-2024-001",
                direccion = "Av. Principal 123",
                rutInspector = "98.765.432-1",
                mail = "inspector@ferji.cl",
                fechaCreacion = Date(),
                estado = "PENDIENTE"
            )
        )
    }
}

