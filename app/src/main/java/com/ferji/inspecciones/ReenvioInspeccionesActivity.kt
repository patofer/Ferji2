package com.ferji.inspecciones

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ferji.inspecciones.data.model.InspeccionEntity
import com.ferji.inspecciones.ui.theme.FerjiTheme
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

    // Mostrar Toast al recibir mensaje
    LaunchedEffect(reenvioState.mensaje) {
        reenvioState.mensaje?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.limpiarMensaje()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reenviar Inspección") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ── Campo de búsqueda ──
            OutlinedTextField(
                value = textoBusqueda,
                onValueChange = { viewModel.onBusquedaChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                placeholder = { Text("Buscar por siniestro, RUT, dirección...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Buscar")
                },
                trailingIcon = {
                    if (textoBusqueda.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onBusquedaChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                        }
                    }
                },
                singleLine = true
            )

            // ── Contador de resultados ──
            Text(
                text = "${inspecciones.size} inspección(es) encontrada(s)",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                fontSize = 13.sp,
                color = Color.Gray
            )

            HorizontalDivider()

            // ── Lista de inspecciones ──
            if (inspecciones.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📭", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            if (textoBusqueda.isNotEmpty()) "No se encontraron resultados"
                            else "No hay inspecciones registradas",
                            fontSize = 16.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Fila superior: Siniestro + Fecha
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Siniestro: ${inspeccion.siniestro}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = dateFormat.format(inspeccion.fechaCreacion),
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Datos de la inspección
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    DatoInspeccion(label = "RUT Cliente", valor = inspeccion.rut)
                    DatoInspeccion(label = "Inspector", valor = inspeccion.rutInspector)
                    DatoInspeccion(label = "Dirección", valor = inspeccion.direccion)
                    DatoInspeccion(label = "Email", valor = inspeccion.mail)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Botón de enviar
            Button(
                onClick = onReenviar,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && !isOtroEnviando,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE67E22)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enviando...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Enviar",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enviar PDF + Presupuesto", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DatoInspeccion(label: String, valor: String) {
    Row(modifier = Modifier.padding(vertical = 1.dp)) {
        Text(
            text = "$label: ",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Gray
        )
        Text(
            text = valor,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

