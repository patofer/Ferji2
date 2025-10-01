// En DanoDropdown.kt
package com.ferji.inspecciones.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Definición de opciones (idealmente vendría del ViewModel o una capa de datos compartida)
// Si la mantienes aquí, asegúrate de que el ViewModel también use esta misma definición
// para consistencia, especialmente CLAVE_OTRO_DANO.
val opcionesDanosConClaveGlobal: List<Pair<String, String>> = listOf(
    "1" to "Daño muro",
    "2" to "Fisura en cielo",
    "3" to "Fisura en muro",
    "4" to "Fisura cornisas",
    "5" to "Daño pintura",
    "6" to "Otro"
)
const val CLAVE_OTRO_DANO_GLOBAL = "6" // Para referencia fácil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DanoDropdown(
    clavesDanosSeleccionados: List<String>, // Lista de CLAVES (String) seleccionadas
    onDanoToggled: (claveDano: String) -> Unit, // Función para seleccionar/deseleccionar una CLAVE
    textoOtro: String,
    onTextoOtroChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    // Opcional: pasar la lista de opciones desde fuera si quieres más flexibilidad
    // opcionesDanos: List<Pair<String, String>> = opcionesDanosConClaveGlobal,
    // claveOtro: String = CLAVE_OTRO_DANO_GLOBAL
) {
    var expanded by remember { mutableStateOf(false) }

    // Usar las opciones globales o las pasadas como parámetro
    val opcionesActuales = opcionesDanosConClaveGlobal // o `opcionesDanos` si lo pasas como param
    val claveActualOtro = CLAVE_OTRO_DANO_GLOBAL    // o `claveOtro` si lo pasas como param

    // Generar el texto a mostrar en el campo del dropdown
    val textoSeleccionadoDisplay = remember(clavesDanosSeleccionados, textoOtro) {
        if (clavesDanosSeleccionados.isEmpty()) {
            "Seleccionar tipo(s) de daño"
        } else {
            clavesDanosSeleccionados.mapNotNull { clave ->
                if (clave == claveActualOtro) {
                    if (textoOtro.isNotBlank()) "Otro: $textoOtro" else "Otro (especificar)"
                } else {
                    opcionesActuales.find { it.first == clave }?.second
                }
            }.joinToString(", ")
        }
    }

    Column(modifier = modifier) {
        Text(
            text = "Tipo de daño(s):",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = textoSeleccionadoDisplay,
                onValueChange = {},
                readOnly = true,
                label = { Text("Daños Seleccionados") }, // Etiqueta opcional para el campo
                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = "Abrir opciones de daño") },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth() // Asegura que el menú ocupe el ancho
            ) {
                opcionesActuales.forEach { (clave, descripcion) ->
                    val isChecked = clavesDanosSeleccionados.contains(clave)
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = null // El clic en el Row lo maneja
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(text = descripcion)
                            }
                        },
                        onClick = {
                            onDanoToggled(clave)
                            // El menú no se cierra automáticamente para permitir selecciones múltiples
                        },
                        // Importante para que el clic funcione en toda la fila
                        // y para que el estado del checkbox se actualice visualmente al hacer clic.
                        // No es estrictamente necesario añadir un interactionSource aquí para la funcionalidad básica.
                    )
                }
                // Opcional: Botón para cerrar el menú explícitamente si se prefiere
                // Divider(modifier = Modifier.padding(vertical = 4.dp))
                // DropdownMenuItem(
                //     text = { Text("Cerrar", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                //     onClick = { expanded = false }
                // )
            }
        }

        // Campo de texto para "otro" (solo si la CLAVE de "Otro" está seleccionada)
        if (clavesDanosSeleccionados.contains(claveActualOtro)) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = textoOtro,
                onValueChange = onTextoOtroChange,
                label = { Text("Especifique el tipo de daño 'Otro'") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Describa el tipo de daño") },
                singleLine = false, // Permitir múltiples líneas si la descripción puede ser larga
                maxLines = 3
            )
        }
    }
}
