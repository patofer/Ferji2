// En: com/ferji/inspecciones/ui/mantenedor/MantenedorDanosScreen.kt
package com.ferji.inspecciones.ui.mantenedor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ferji.inspecciones.utils.DanoConstants


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MantenedorDanosScreen(
    onBack: () -> Unit,
    onDanoSelected: (clave: String, descripcion: String) -> Unit,
    onNavigateToPartidaSelection: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mantenedor de Partidas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Text(
                text = "Seleccione un tipo de daño para gestionar sus partidas:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
            LazyColumn {
                // ... dentro de la LazyColumn
                items(DanoConstants.opcionesDanosConClave) { (clave, descripcion) ->
                    // Excluimos "Otro" ya que no tiene partidas predefinidas
                    if (clave != DanoConstants.CLAVE_OTRO_DANO) {
                        ListItem(
                            // CORRECCIÓN: Renombrar 'headlineText' a 'headlineContent'
                            headlineContent = { Text(descripcion) },
                            modifier = Modifier.clickable { onDanoSelected(clave, descripcion) }
                        )
                        Divider()
                    }
                }
// ...

            }
        }
    }
}
