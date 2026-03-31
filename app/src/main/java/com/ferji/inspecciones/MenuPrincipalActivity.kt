package com.ferji.inspecciones

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startActivity
import com.ferji.inspecciones.ui.actividades.MaestroPartidasActivity
import com.ferji.inspecciones.ui.components.FerjiMenuCard
import com.ferji.inspecciones.ui.components.FerjiSectionHeader
import com.ferji.inspecciones.ui.components.FerjiTitleBar
import com.ferji.inspecciones.ui.theme.*
import com.ferji.inspecciones.viewmodels.MenuPrincipalViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MenuPrincipalActivity : ComponentActivity() {

    private val viewModel: MenuPrincipalViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FerjiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val esAdmin by viewModel.esAdministrador.collectAsState()
                    PantallaMenuPrincipal(esAdministrador = esAdmin)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaMenuPrincipal(esAdministrador: Boolean) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    FerjiTitleBar(subtitle = "Menú Principal")
                },
                actions = {
                    if (esAdministrador) {
                        IconButton(onClick = {
                            val intent = Intent(context, ConfiguracionActivity::class.java)
                            context.startActivity(intent)
                        }) {
                            Icon(
                                Icons.Outlined.Settings,
                                contentDescription = "Configuración",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // ═══ SECCIÓN: Inspecciones ═══
            FerjiSectionHeader(title = "Inspecciones")

            FerjiMenuCard(
                title = "Nueva Inspección",
                subtitle = "Crear una inspección de siniestro",
                icon = Icons.Filled.AddCircle,
                gradientColors = listOf(Primary40, Primary30),
                onClick = {
                    val intent = Intent(context, NuevaInspeccionActivity::class.java)
                    startActivity(context, intent, null)
                }
            )

            FerjiMenuCard(
                title = "Listado de Inspecciones",
                subtitle = "Ver, retomar o eliminar inspecciones",
                icon = Icons.Outlined.Assignment,
                gradientColors = listOf(Secondary40, Secondary30),
                onClick = {
                    val intent = Intent(context, ListaInspeccionesActivity::class.java)
                    context.startActivity(intent)
                }
            )

            FerjiMenuCard(
                title = "Enviar Inspección",
                subtitle = "Reenviar Inspeccíon PDF ",
                icon = Icons.AutoMirrored.Filled.Send,
                gradientColors = listOf(FerjiOrange, Color(0xFFD35400)),
                onClick = {
                    val intent = Intent(context, ReenvioInspeccionesActivity::class.java)
                    context.startActivity(intent)
                }
            )

            // ═══ SECCIÓN: Administración (solo admin) ═══
            if (esAdministrador) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                FerjiSectionHeader(title = "Administración")

                FerjiMenuCard(
                    title = "Maestro de Partidas",
                    subtitle = "Categorías y tipos de daño",
                    icon = Icons.Outlined.Category,
                    gradientColors = listOf(Color(0xFF7D5260), Color(0xFF6B4450)),
                    onClick = {
                        val intent = Intent(context, MaestroPartidasActivity::class.java)
                        context.startActivity(intent)
                    }
                )

                FerjiMenuCard(
                    title = "Mantenedor de Precios",
                    subtitle = "Gestionar precios unitarios",
                    icon = Icons.Outlined.PriceChange,
                    gradientColors = listOf(Color(0xFF625B71), Color(0xFF514A5F)),
                    onClick = {
                        val intent = Intent(context, MantenedorPreciosActivity::class.java)
                        context.startActivity(intent)
                    }
                )

                FerjiMenuCard(
                    title = "Configuración de Correos",
                    subtitle = "Destinatarios y reglas de envío",
                    icon = Icons.Outlined.Settings,
                    gradientColors = listOf(Color(0xFF4A6741), Color(0xFF3B5534)),
                    onClick = {
                        val intent = Intent(context, ConfiguracionActivity::class.java)
                        context.startActivity(intent)
                    }
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xl))
        }
    }
}

@Preview(showBackground = true, name = "Admin")
@Composable
fun PantallaMenuAdminPreview() {
    FerjiTheme { PantallaMenuPrincipal(esAdministrador = true) }
}

@Preview(showBackground = true, name = "Usuario")
@Composable
fun PantallaMenuUsuarioPreview() {
    FerjiTheme { PantallaMenuPrincipal(esAdministrador = false) }
}
