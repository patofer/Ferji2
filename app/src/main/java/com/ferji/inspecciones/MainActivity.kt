package com.ferji.inspecciones

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ferji.inspecciones.data.repository.UserRoles
import com.ferji.inspecciones.ui.components.FerjiAnimatedSplash
import com.ferji.inspecciones.ui.components.FerjiDotLogo
import com.ferji.inspecciones.ui.components.FerjiTitleBar
import com.ferji.inspecciones.ui.theme.*
import com.ferji.inspecciones.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val loginLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FerjiTheme {
                MainRouter()
            }
        }
    }

    @Composable
    private fun MainRouter() {
        val viewModel: MainViewModel = hiltViewModel()
        val sessionState by viewModel.sessionState.collectAsState()

        when (val state = sessionState) {
            is MainViewModel.SessionState.LOADING -> {
                FerjiAnimatedSplash()
            }
            is MainViewModel.SessionState.LoggedIn -> {
                PantallaBienvenida(
                    userName = state.data.nombre ?: "Usuario",
                    userRole = state.data.role,
                    onIngresarClick = {
                        startActivity(Intent(this@MainActivity, MenuPrincipalActivity::class.java))
                    },
                    onCerrarSesionClick = {
                        viewModel.logout()
                    }
                )
            }
            is MainViewModel.SessionState.LoggedOut -> {
                PantallaInicial(
                    onIngresarClick = {
                        loginLauncher.launch(Intent(this@MainActivity, LoginActivity::class.java))
                    }
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  PANTALLA BIENVENIDA — Material Design 3
// ═══════════════════════════════════════════════════════════════
@Composable
fun PantallaBienvenida(
    userName: String,
    userRole: String?,
    onIngresarClick: () -> Unit,
    onCerrarSesionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Gradiente decorativo superior
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Logo con borde decorativo
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            2.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_ferji),
                        contentDescription = "Logo de Ferji",
                        modifier = Modifier.size(120.dp)
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.xxl))

                // Título FERJI con identidad
                FerjiTitleBar(compact = false)

                Spacer(modifier = Modifier.height(Spacing.xl))

                Text(
                    text = "¡Bienvenido!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(Spacing.xs))

                Text(
                    text = userName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                // Badge de administrador
                if (userRole == UserRoles.ADMIN) {
                    Spacer(modifier = Modifier.height(Spacing.md))
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                        ) {
                            Icon(
                                Icons.Outlined.AdminPanelSettings,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "ADMINISTRADOR",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.huge))

                // Botón Ingresar
                Button(
                    onClick = onIngresarClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ComponentSize.buttonHeight),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Login,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        "Ingresar",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.md))

                // Botón cambiar usuario
                TextButton(
                    onClick = onCerrarSesionClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Outlined.SwapHoriz,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    Text("Cambiar de usuario")
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  PANTALLA INICIAL — Material Design 3
// ═══════════════════════════════════════════════════════════════
@Composable
fun PantallaInicial(modifier: Modifier = Modifier, onIngresarClick: () -> Unit) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Gradiente decorativo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Logo corporativo cuadrado con 4 puntos
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Primary40, Primary30)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    FerjiDotLogo(
                        size = 84.dp,
                        dotColor = Color.White,
                        animated = true
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.xxl))

                Text(
                    text = "Inspecciones de\nDaños por Sismo",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                Text(
                    text = "Gestión profesional de inspecciones",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(Spacing.huge))

                Button(
                    onClick = onIngresarClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ComponentSize.buttonHeight),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Login,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        "Iniciar Sesión",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  SPLASH SCREEN (fallback simple)
// ═══════════════════════════════════════════════════════════════
@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    FerjiAnimatedSplash(modifier = modifier)
}

@Preview(showBackground = true, name = "Admin Logueado")
@Composable
fun PantallaBienvenidaAdminPreview() {
    FerjiTheme {
        PantallaBienvenida(
            userName = "Jorge Ferji",
            userRole = UserRoles.ADMIN,
            onIngresarClick = {},
            onCerrarSesionClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Pantalla Inicial")
@Composable
fun PantallaInicialPreview() {
    FerjiTheme {
        PantallaInicial(onIngresarClick = {})
    }
}

