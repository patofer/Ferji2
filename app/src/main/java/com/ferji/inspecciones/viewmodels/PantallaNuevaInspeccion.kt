package com.ferji.inspecciones

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ferji.inspecciones.ui.components.FerjiColoredSectionHeader
import com.ferji.inspecciones.ui.components.FerjiGradientDivider
import com.ferji.inspecciones.ui.components.FerjiDotLogo
import com.ferji.inspecciones.ui.components.FerjiBrandBanner
import com.ferji.inspecciones.ui.theme.ComponentSize
import com.ferji.inspecciones.ui.theme.Primary30
import com.ferji.inspecciones.ui.theme.Primary40
import com.ferji.inspecciones.ui.theme.Secondary30
import com.ferji.inspecciones.ui.theme.Secondary40
import com.ferji.inspecciones.ui.theme.Tertiary30
import com.ferji.inspecciones.ui.theme.Tertiary40
import com.ferji.inspecciones.ui.theme.Spacing
import com.ferji.inspecciones.viewmodels.NuevaInspeccionViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaNuevaInspeccion(viewModel: NuevaInspeccionViewModel, modifier: Modifier = Modifier, isLoading: Boolean) {
    Log.d("PantallaNuevaInsp_UI", "--- PantallaNuevaInspeccion RECOMPONIENDO --- rutInsp: ${viewModel.rutInspector}, isValid: ${viewModel.isRutInspectorValid}")
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        // Gradiente decorativo de fondo sutil
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.md, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // ═══ Banner corporativo ═══
            FerjiBrandBanner(
                title = "Nueva Inspección",
                subtitle = "Complete los datos del siniestro"
            )

            // ═══ Sección: Datos del Asegurado ═══
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FerjiColoredSectionHeader(
                        title = "Datos del Asegurado",
                        backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        icon = {
                            Icon(
                                Icons.Outlined.Person,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    )

                    OutlinedTextField(
                        value = viewModel.rut,
                        onValueChange = { viewModel.onRutChange(it) },
                        label = { Text("RUT *") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Badge, contentDescription = null, modifier = Modifier.size(20.dp))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        isError = viewModel.rut.isNotBlank() && !viewModel.isRutValid,
                        supportingText = {
                            if (viewModel.rut.isNotBlank() && !viewModel.isRutValid) {
                                Text("RUT chileno inválido")
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
                    )
                }
            }

            // ═══ Sección: Datos del Siniestro ═══
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FerjiColoredSectionHeader(
                        title = "Datos del Siniestro",
                        backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        icon = {
                            Icon(
                                Icons.Outlined.Assignment,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    )

                    OutlinedTextField(
                        value = viewModel.siniestro,
                        onValueChange = { newValue -> viewModel.onSiniestroChange(newValue) },
                        label = { Text("Siniestro *") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Assignment, contentDescription = null, modifier = Modifier.size(20.dp))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = viewModel.direccion,
                        onValueChange = { newValue -> viewModel.onDireccionChange(newValue) },
                        label = { Text("Dirección *") },
                        leadingIcon = {
                            Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(20.dp))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // ═══ Sección: Inspector ═══
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FerjiColoredSectionHeader(
                        title = "Inspector",
                        backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        icon = {
                            Icon(
                                Icons.Outlined.Engineering,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    )

        OutlinedTextField(
            value = viewModel.rutInspector,
            onValueChange = { },
            label = { Text("RUT Inspector *") },
            leadingIcon = {
                Icon(Icons.Outlined.Engineering, contentDescription = null, modifier = Modifier.size(20.dp))
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            readOnly = true,
            enabled = false,
            isError = false,
            supportingText = {
                Text("Asignado automáticamente desde su sesión")
            }
        )

                    OutlinedTextField(
                        value = viewModel.mail,
                        onValueChange = { },
                        label = { Text("Mail Inspector *") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Email, contentDescription = null, modifier = Modifier.size(20.dp))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        readOnly = true,
                        enabled = false,
                        isError = false,
                        supportingText = {
                            Text("Asignado automáticamente desde su sesión")
                        }
                    )
                }
            }

        Spacer(modifier = Modifier.height(Spacing.sm))

        FerjiGradientDivider()

        Spacer(modifier = Modifier.height(Spacing.sm))

        // ═══ Mensaje de Resultado ═══
        uiState.mensajeGlobalUi?.let { mensajeDelEstado ->
            if (mensajeDelEstado.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (mensajeDelEstado.contains("✅"))
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = mensajeDelEstado,
                        color = if (mensajeDelEstado.contains("✅"))
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(Spacing.md),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.sm))

        Log.d("NUEVA_INSPECCION_UI", "Navegando a NuevaHabitacion con ID ")

        // ═══ Botón Guardar con identidad FERJI ═══
        Button(
            onClick = { viewModel.guardarInspeccion() },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = viewModel.todosCamposLlenos,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = if (viewModel.todosCamposLlenos) Brush.horizontalGradient(
                            colors = listOf(Primary40, Primary30)
                        ) else Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                            )
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    // Mini logo Ferji en el botón
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        FerjiDotLogo(
                            size = 18.dp,
                            dotColor = Color.White
                        )
                    }
                    Text(
                        text = "Guardar Inspección",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Icon(
                        Icons.Outlined.Save,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.md))
        }
    }
}
