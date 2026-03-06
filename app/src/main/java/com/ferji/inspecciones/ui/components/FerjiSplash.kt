package com.ferji.inspecciones.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ferji.inspecciones.ui.theme.Primary30
import com.ferji.inspecciones.ui.theme.Primary40
import com.ferji.inspecciones.ui.theme.Primary80
import com.ferji.inspecciones.ui.theme.Spacing

// ═══════════════════════════════════════════════════════════════
//  SPLASH SCREEN ANIMADO — Identidad FERJI
//  Logo de 4 puntos con animación secuencial + texto corporativo
// ═══════════════════════════════════════════════════════════════

/**
 * Splash screen animado con identidad de marca FERJI.
 * Animación: Los 4 puntos aparecen secuencialmente, luego
 * el texto "FERJI" e "INSPECCIONES" se desvanecen suavemente.
 *
 * Duración total: ~1500ms (profesional, no demasiado largo)
 */
@Composable
fun FerjiAnimatedSplash(modifier: Modifier = Modifier) {
    // ── Animaciones de los 4 puntos (aparecen secuencialmente) ──
    val dotAnimations = (0..3).map { index ->
        val delay = index * 120 // 120ms entre cada punto

        val animatable = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(delay.toLong())
            animatable.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 400,
                    easing = EaseOutBack
                )
            )
        }
        animatable.value
    }

    // ── Animación del logo container (escala) ──
    val logoScale = remember { Animatable(0.8f) }
    LaunchedEffect(Unit) {
        logoScale.animateTo(
            targetValue = 1f,
            animationSpec = tween(600, easing = EaseOutCubic)
        )
    }

    // ── Animación del texto FERJI ──
    val textAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(500)
        textAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(400, easing = EaseInOutCubic)
        )
    }

    // ── Animación del subtítulo INSPECCIONES ──
    val subtitleAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(700)
        subtitleAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(400, easing = EaseInOutCubic)
        )
    }

    // ── Animación del loader inferior ──
    val loaderAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(900)
        loaderAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(300, easing = EaseInOutCubic)
        )
    }

    // Pulse infinito sutil para los dots una vez aparecen
    val infiniteTransition = rememberInfiniteTransition(label = "splashPulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Gradiente decorativo de fondo
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                                Color.Transparent
                            ),
                            radius = 800f
                        )
                    )
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // ── Logo cuadrado con 4 puntos animados ──
                Box(
                    modifier = Modifier
                        .scale(logoScale.value * pulse)
                        .size(120.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Primary40, Primary30)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Canvas con 4 puntos animados
                    Canvas(modifier = Modifier.size(72.dp)) {
                        val dotRadius = size.width * 0.14f
                        val padding = size.width * 0.25f
                        val right = size.width - padding
                        val top = padding
                        val bottom = size.height - padding

                        val positions = listOf(
                            Offset(padding, top),      // Superior izquierdo
                            Offset(right, top),        // Superior derecho
                            Offset(padding, bottom),   // Inferior izquierdo
                            Offset(right, bottom)      // Inferior derecho
                        )

                        positions.forEachIndexed { index, offset ->
                            val anim = dotAnimations[index]
                            drawCircle(
                                color = Color.White.copy(alpha = anim),
                                radius = dotRadius * anim,
                                center = offset
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.xxl))

                // ── Texto FERJI ──
                Text(
                    text = "FERJI",
                    modifier = Modifier.alpha(textAlpha.value),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 6.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(Spacing.xs))

                // ── Texto INSPECCIONES ──
                Text(
                    text = "INSPECCIONES",
                    modifier = Modifier.alpha(subtitleAlpha.value),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 4.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(Spacing.huge))

                // ── Indicador de carga animado (3 puntos) ──
                Row(
                    modifier = Modifier.alpha(loaderAlpha.value),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(3) { index ->
                        val dotAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.3f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(600, delayMillis = index * 200),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "loaderDot$index"
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = dotAlpha)
                                )
                        )
                    }
                }
            }
        }
    }
}

