package com.ferji.inspecciones

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ferji.inspecciones.data.model.InspeccionEntity
import com.ferji.inspecciones.ui.components.FerjiEmptyState
import com.ferji.inspecciones.ui.components.FerjiInfoRow
import com.ferji.inspecciones.ui.components.FerjiTitleBar
import com.ferji.inspecciones.ui.theme.*
import com.ferji.inspecciones.viewmodels.ReenvioInspeccionesViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Locale

@AndroidEntryPoint
class ReenvioInspeccionesActivity : ComponentActivity() {

    private val viewModel: ReenvioInspeccionesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FerjiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PantallaReenvioInspecciones(
                        viewModel = viewModel,
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaReenvioInspecciones(
    viewModel: ReenvioInspeccionesViewModel,
    onBack: () -> Unit
) {
    val inspecciones by viewModel.inspeccionesFiltradas.collectAsState()
    val textoBusqueda by viewModel.textoBusqueda.collectAsState()
    val reenvioState by viewModel.reenvioState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(reenvioState.mensaje) {
        reenvioState.mensaje?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.limpiarMensaje()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    FerjiTitleBar(
                        subtitle = "Reenviar Inspección",
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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Barra de búsqueda mejorada
            OutlinedTextField(
                value = textoBusqueda,
                onValueChange = { viewModel.onBusquedaChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.base, vertical = Spacing.md),
                placeholder = { Text("Buscar por siniestro, RUT, dirección...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Buscar",
                        tint = MaterialTheme.colorScheme.primary)
                },
                trailingIcon = {
                    if (textoBusqueda.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onBusquedaChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Contador con estilo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.base, vertical = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "${inspecciones.size} inspección(es) encontrada(s)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            if (inspecciones.isEmpty()) {
                FerjiEmptyState(
                    icon = "📭",
                    title = if (textoBusqueda.isNotEmpty()) "No se encontraron resultados"
                            else "No hay inspecciones registradas",
                    subtitle = if (textoBusqueda.isNotEmpty()) "Intenta con otro término de búsqueda"
                               else "Las inspecciones completadas aparecerán aquí"
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = Spacing.base, vertical = Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    items(inspecciones, key = { it.id }) { inspeccion ->
                        TarjetaInspeccionReenvio(
                            inspeccion = inspeccion,
                            isLoading = reenvioState.isLoading && reenvioState.inspeccionIdEnProceso == inspeccion.id,
                            isOtroEnviando = reenvioState.isLoading && reenvioState.inspeccionIdEnProceso != inspeccion.id,
                            onReenviar = { viewModel.reenviarInspeccion(inspeccion) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TarjetaInspeccionReenvio(
    inspeccion: InspeccionEntity,
    isLoading: Boolean,
    isOtroEnviando: Boolean,
    onReenviar: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.level1)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Franja de color lateral
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                    .background(FerjiOrange)
            )

            Column(modifier = Modifier.weight(1f).padding(Spacing.md)) {
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
                                .background(FerjiOrangeLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Assignment,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = FerjiOrange
                            )
                        }
                        Text(
                            text = inspeccion.siniestro,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    Text(
                        text = dateFormat.format(inspeccion.fechaCreacion),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.sm))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(Spacing.sm))

                FerjiInfoRow(label = "RUT", value = inspeccion.rut)
                FerjiInfoRow(label = "Inspector", value = inspeccion.rutInspector)
                FerjiInfoRow(label = "Dirección", value = inspeccion.direccion)
                FerjiInfoRow(label = "Email", value = inspeccion.mail)

                Spacer(modifier = Modifier.height(Spacing.md))

                Button(
                    onClick = onReenviar,
                    modifier = Modifier.fillMaxWidth().height(ComponentSize.buttonHeightSmall),
                    enabled = !isLoading && !isOtroEnviando,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FerjiOrange
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Text("Enviando...",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold)
                    } else {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Enviar",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Text("Enviar PDF + Presupuesto",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DatoInspeccion(label: String, valor: String) {
    FerjiInfoRow(label = label, value = valor)
}

