package com.ferji.inspecciones.ui.mantenedor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.ferji.inspecciones.data.model.PartidaPrincipalEntity
import com.ferji.inspecciones.viewmodels.PartidaPrincipalViewModel

/**
 * Pantalla que muestra la lista de Partidas Principales (categorías maestras).
 * Al hacer clic en una, navega a la pantalla de detalles.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartidaPrincipalListScreen(
    viewModel: PartidaPrincipalViewModel = hiltViewModel(),
    onBack: () -> Unit,
    // --- INICIO DE LA CORRECCIÓN 1 ---
    // El callback ahora acepta el ID (Long) y el Nombre (String)
    onPartidaPrincipalClick: (id: Long, nombre: String) -> Unit
    // --- FIN DE LA CORRECCIÓN 1 ---
) {
    val partidasPrincipales by viewModel.partidasPrincipales.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seleccionar Categoría") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            items(partidasPrincipales) { partidaPrincipal ->
                ListItem(
                    headlineContent = { Text(partidaPrincipal.nombre) },
                    trailingContent = {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Ver detalles")
                    },
                    modifier = Modifier.clickable {
                        // --- INICIO DE LA CORRECCIÓN 2 ---
                        // Pasamos AMBOS valores al callback: el ID y el Nombre
                        onPartidaPrincipalClick(partidaPrincipal.id, partidaPrincipal.nombre)
                        // --- FIN DE LA CORRECCIÓN 2 ---
                    }
                )
                Divider()
            }
        }
    }
}
