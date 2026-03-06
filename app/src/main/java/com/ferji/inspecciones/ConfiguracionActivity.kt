package com.ferji.inspecciones

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Rule
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.ferji.inspecciones.ui.components.FerjiColoredSectionHeader
import com.ferji.inspecciones.ui.components.FerjiGradientDivider
import com.ferji.inspecciones.ui.components.FerjiTitleBar
import com.ferji.inspecciones.ui.theme.FerjiTheme
import com.ferji.inspecciones.ui.theme.Spacing
import com.ferji.inspecciones.viewmodels.ConfiguracionViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ConfiguracionActivity : ComponentActivity() {

    private val viewModel: ConfiguracionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            viewModel.events.collectLatest { event ->
                when (event) {
                    is ConfiguracionViewModel.ConfiguracionEvent.ShowMessage -> {
                        Toast.makeText(this@ConfiguracionActivity, event.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        setContent {
            FerjiTheme {
                PantallaConfiguracion(
                    viewModel = viewModel,
                    onVolver = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaConfiguracion(
    viewModel: ConfiguracionViewModel,
    onVolver: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    FerjiTitleBar(subtitle = "Configuración", compact = true)
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                // ═══ Sección: Correos de Notificación ═══
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        FerjiColoredSectionHeader(
                            title = "Correos de Notificación",
                            backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            icon = {
                                Icon(
                                    Icons.Outlined.Email,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        )

                        OutlinedTextField(
                            value = state.emailAdmin,
                            onValueChange = viewModel::onEmailAdminChange,
                            label = { Text("Email Administrador *") },
                            leadingIcon = {
                                Icon(Icons.Outlined.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(20.dp))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            supportingText = {
                                Text("Correo principal que recibe todas las notificaciones")
                            }
                        )

                        OutlinedTextField(
                            value = state.emailCc,
                            onValueChange = viewModel::onEmailCcChange,
                            label = { Text("Email en Copia (CC)") },
                            leadingIcon = {
                                Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(20.dp))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            supportingText = {
                                Text("Correo adicional opcional para copias")
                            }
                        )
                    }
                }

                // ═══ Sección: Reglas de Envío ═══
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FerjiColoredSectionHeader(
                            title = "Reglas de Envío",
                            backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            icon = {
                                Icon(
                                    Icons.Outlined.Rule,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        )

                        // Toggle: Enviar copia de Inspección al Inspector
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Enviar Inspección al Inspector",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "El inspector recibirá una copia del PDF",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = state.enviarInspeccionAlInspector,
                                onCheckedChange = viewModel::onEnviarInspeccionAlInspectorChange
                            )
                        }

                        HorizontalDivider()

                        // Toggle: Enviar copia de Presupuesto al Inspector
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Enviar Presupuesto al Inspector",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "El inspector recibirá una copia del Excel",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = state.enviarPresupuestoAlInspector,
                                onCheckedChange = viewModel::onEnviarPresupuestoAlInspectorChange
                            )
                        }
                    }
                }

                // ═══ Resumen visual ═══
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "Resumen del flujo de correos",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        val adminText = if (state.emailAdmin.isNotBlank()) state.emailAdmin else "(sin configurar)"
                        Text("Admin: $adminText", style = MaterialTheme.typography.bodySmall)

                        if (state.emailCc.isNotBlank()) {
                            Text("CC: ${state.emailCc}", style = MaterialTheme.typography.bodySmall)
                        }

                        val inspeccionStatus = if (state.enviarInspeccionAlInspector) "Sí" else "No"
                        Text(
                            "Inspección al inspector: $inspeccionStatus",
                            style = MaterialTheme.typography.bodySmall
                        )

                        val presupuestoStatus = if (state.enviarPresupuestoAlInspector) "Sí" else "No"
                        Text(
                            "Presupuesto al inspector: $presupuestoStatus",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.sm))
                FerjiGradientDivider()
                Spacer(modifier = Modifier.height(Spacing.sm))

                // ═══ Botón Guardar ═══
                Button(
                    onClick = { viewModel.guardarConfiguracion() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = !state.isSaving,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(Spacing.sm))
                        Text(
                            "Guardar Configuración",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.md))
            }
        }
    }
}

