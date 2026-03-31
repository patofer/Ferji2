package com.ferji.inspecciones

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ferji.inspecciones.data.repository.UserRoles
import com.ferji.inspecciones.ui.components.FerjiAnimatedSplash
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
//  PANTALLA BIENVENIDA — Identidad Ferji.cl
// ═══════════════════════════════════════════════════════════════
@Composable
fun PantallaBienvenida(
    userName: String,
    userRole: String?,
    onIngresarClick: () -> Unit,
    onCerrarSesionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // ── Animaciones de entrada ──
    val logoScale = remember { Animatable(0.6f) }
    val contentAlpha = remember { Animatable(0f) }
    val cardAlpha = remember { Animatable(0f) }
    val buttonAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        logoScale.animateTo(1f, animationSpec = tween(600, easing = EaseOutBack))
    }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(200)
        contentAlpha.animateTo(1f, animationSpec = tween(500, easing = EaseInOutCubic))
    }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(400)
        cardAlpha.animateTo(1f, animationSpec = tween(500, easing = EaseInOutCubic))
    }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(600)
        buttonAlpha.animateTo(1f, animationSpec = tween(500, easing = EaseInOutCubic))
    }

    val esAdmin = userRole == UserRoles.ADMIN
    val initials = userName.split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
        .joinToString("")
        .ifEmpty { "U" }

    Box(modifier = modifier.fillMaxSize()) {
        // ── Fondo blanco limpio ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        )

        // ── Franja verde sutil superior ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .align(Alignment.TopCenter)
                .background(FerjiVerde)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ── Logo de la empresa ──
            Image(
                painter = painterResource(id = R.drawable.logo_ferji),
                contentDescription = "Logo Ferji",
                modifier = Modifier
                    .scale(logoScale.value)
                    .size(130.dp)
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            // ── Marca FERJI ──
            Text(
                text = "FERJI",
                modifier = Modifier.alpha(contentAlpha.value),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 8.sp
                ),
                color = FerjiVerde
            )
            Text(
                text = "INSPECCIONES",
                modifier = Modifier.alpha(contentAlpha.value),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 6.sp
                ),
                color = Color(0xFF555555)
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            // ── Línea verde decorativa ──
            Box(
                modifier = Modifier
                    .alpha(contentAlpha.value)
                    .width(60.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(FerjiVerde)
            )

            Spacer(modifier = Modifier.height(Spacing.xl))

            // ── Tarjeta de usuario ──
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(cardAlpha.value),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF5F5F5)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar con iniciales
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(FerjiVerde),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.md))

                    Text(
                        text = "¡Bienvenido!",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF999999),
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(Spacing.xxs))

                    Text(
                        text = userName,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFF333333),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    // Badge de administrador
                    if (esAdmin) {
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = FerjiVerde.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                            ) {
                                Icon(
                                    Icons.Outlined.AdminPanelSettings,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = FerjiVerdeDark
                                )
                                Text(
                                    text = "ADMINISTRADOR",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = FerjiVerdeDark,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            // ── Botón Ingresar (verde corporativo Ferji) ──
            Button(
                onClick = onIngresarClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ComponentSize.buttonHeight)
                    .alpha(buttonAlpha.value),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FerjiVerde,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Login,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    "Ingresar al Sistema",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Botón cambiar usuario
            TextButton(
                onClick = onCerrarSesionClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(buttonAlpha.value)
            ) {
                Icon(
                    Icons.Outlined.SwapHoriz,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color(0xFF999999)
                )
                Spacer(Modifier.width(Spacing.xs))
                Text(
                    "Cambiar de usuario",
                    color = Color(0xFF999999)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.lg))
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  PANTALLA INICIAL (LoggedOut) — Identidad Ferji.cl
// ═══════════════════════════════════════════════════════════════
@Composable
fun PantallaInicial(modifier: Modifier = Modifier, onIngresarClick: () -> Unit) {
    // Animaciones
    val logoScale = remember { Animatable(0.5f) }
    val contentAlpha = remember { Animatable(0f) }
    val buttonAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        logoScale.animateTo(1f, animationSpec = tween(700, easing = EaseOutBack))
    }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(300)
        contentAlpha.animateTo(1f, animationSpec = tween(500, easing = EaseInOutCubic))
    }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(600)
        buttonAlpha.animateTo(1f, animationSpec = tween(500, easing = EaseInOutCubic))
    }

    // Pulse sutil del logo
    val infiniteTransition = rememberInfiniteTransition(label = "logoPulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // ── Fondo blanco limpio ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        )

        // ── Franja verde sutil superior ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .align(Alignment.TopCenter)
                .background(FerjiVerde)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo con animación
            Image(
                painter = painterResource(id = R.drawable.logo_ferji),
                contentDescription = "Logo Ferji",
                modifier = Modifier
                    .scale(logoScale.value * pulse)
                    .size(160.dp)
            )

            Spacer(modifier = Modifier.height(Spacing.xl))

            // Marca
            Text(
                text = "FERJI",
                modifier = Modifier.alpha(contentAlpha.value),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 8.sp
                ),
                color = FerjiVerde
            )
            Text(
                text = "INSPECCIONES",
                modifier = Modifier.alpha(contentAlpha.value),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 6.sp
                ),
                color = Color(0xFF555555)
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            // Línea verde decorativa
            Box(
                modifier = Modifier
                    .alpha(contentAlpha.value)
                    .width(50.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(FerjiVerde)
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            Text(
                text = "Gestión profesional de inspecciones\nde daños por siniestro",
                modifier = Modifier.alpha(contentAlpha.value),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF888888),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(Spacing.huge))

            // Botón verde corporativo Ferji
            Button(
                onClick = onIngresarClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ComponentSize.buttonHeight)
                    .alpha(buttonAlpha.value),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FerjiVerde,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
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
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(Spacing.huge))
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

