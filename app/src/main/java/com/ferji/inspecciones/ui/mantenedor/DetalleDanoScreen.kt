// En: com/ferji/inspecciones/ui/mantenedor/DetalleDanoScreen.kt
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
    claveDano: String,
    descripcionDano: String,
    viewModel: PartidaViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onNavigateToMaestro: (String) -> Unit // Pasamos claveDano para volver
) {
    // Cargar las partidas para este daño específico
    LaunchedEffect(key1 = claveDano) {
        viewModel.loadPartidasParaDano(claveDano)
    }

    val partidasAsociadas by viewModel.partidasAsociadas.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(descripcionDano, style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onNavigateToMaestro(claveDano) }) {
                Icon(Icons.Default.Add, contentDescription = "Asociar Partida")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Text(
                text = "Partidas asociadas:",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            LazyColumn {
                items(partidasAsociadas) { partida ->
                    ListItem(
                        // CORRECCIÓN 1: Renombrar 'headlineText' a 'headlineContent'
                        headlineContent = { Text(partida.descripcion) },

                        // CORRECCIÓN 2: Renombrar 'supportingText' a 'supportingContent'
                        supportingContent = { Text("Precio: $${partida.precioUnitario} / ${partida.unidad}") },

                        // 'trailingContent' ya es correcto
                        trailingContent = {
                            IconButton(onClick = { viewModel.desasociarPartidaDeDano(partida.id) }) {
                                Icon(Icons.Default.Delete, "Desasociar", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    )
                    Divider()
                }
            }

        }
    }
}
