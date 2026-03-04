package com.ferji.inspecciones.ui.mantenedor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ferji.inspecciones.viewmodels.PartidaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleDanoScreen(
    idPadre: Long,
    descripcionPadre: String,
    viewModel: PartidaViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onNavigateToCrearPartidaHija: (Long) -> Unit
) {
    // --- INICIO DE LA CORRECCIÓN CLAVE ---
    // Llamamos a la función PÚBLICA del ViewModel, respetando el encapsulamiento.
    LaunchedEffect(key1 = idPadre) {
        viewModel.cargarPartidasDe(idPadre)
    }
    // --- FIN DE LA CORRECCIÓN CLAVE ---

    val partidasHijas by viewModel.partidasDePrincipal.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(descripcionPadre, style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onNavigateToCrearPartidaHija(idPadre) }) {
                Icon(Icons.Default.Add, contentDescription = "Añadir Partida Hija")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Text(
                text = "Partidas Hijas:",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            if (partidasHijas.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Text("Aún no hay partidas hijas. Presiona el botón '+' para añadir.")
                }
            } else {
                LazyColumn {
                    items(partidasHijas) { partida ->
                        ListItem(
                            headlineContent = { Text(partida.descripcion) },
                            supportingContent = { Text("Precio: $${partida.precioUnitario} / ${partida.unidad}") },
                            trailingContent = {
                                IconButton(onClick = { viewModel.eliminarPartida(partida) }) {
                                    Icon(Icons.Default.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        )
                        Divider()
                    }
                }
            }
        }
    }
}
