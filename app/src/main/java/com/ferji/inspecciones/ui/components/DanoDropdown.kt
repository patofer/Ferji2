// Crea un archivo nuevo: ui/components/DanoDropdown.kt
package com.ferji.inspecciones.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DanoDropdown(
    danoSeleccionado: String,
    onDanoSeleccionado: (String) -> Unit,
    textoOtro: String,
    onTextoOtroChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val opcionesDanos = listOf(
        "daño muro",
        "fisura en cielo",
        "fisura en muro",
        "fisura cornisas",
        "daño pintura",
        "otro"
    )

    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        // Título
        Text(
            text = "Tipo de daño:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Dropdown Menu
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = if (danoSeleccionado.isEmpty()) "Seleccionar tipo de daño"
                else danoSeleccionado,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                opcionesDanos.forEach { dano ->
                    DropdownMenuItem(
                        text = { Text(dano) },
                        onClick = {
                            onDanoSeleccionado(dano)
                            expanded = false
                        }
                    )
                }
            }
        }

        // Campo de texto para "otro"
        if (danoSeleccionado == "otro") {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = textoOtro,
                onValueChange = onTextoOtroChange,
                label = { Text("Especifique el tipo de daño") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Describa el tipo de daño") }
            )
        }
    }
}