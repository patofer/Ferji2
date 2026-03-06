package com.ferji.inspecciones.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ferji.inspecciones.ui.theme.Primary40
import com.ferji.inspecciones.ui.theme.Primary30
import com.ferji.inspecciones.ui.theme.Primary80
import com.ferji.inspecciones.ui.theme.FerjiOrange

// ═══════════════════════════════════════════════════════════════
//  FERJI IDENTITY COMPONENTS
//  Logo de 4 puntos + Tipografía corporativa
// ═══════════════════════════════════════════════════════════════

/**
 * Logo de Ferji: 4 puntos dispuestos en cuadrado (como el número 4 en un dado).
 * Diseño cuadrado/moderno alineado con la identidad de marca.
 *
 * @param size Tamaño total del contenedor del logo
 * @param dotColor Color de los puntos
 * @param animated Si los puntos deben animarse al aparecer
 */
@Composable
fun FerjiDotLogo(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    dotColor: Color = Primary40,
    animated: Boolean = false
) {
    val animProgress = if (animated) {
        val infiniteTransition = rememberInfiniteTransition(label = "dotPulse")
        infiniteTransition.animateFloat(
            initialValue = 0.85f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = EaseInOutCubic),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        ).value
    } else 1f

    Canvas(
        modifier = modifier.size(size)
    ) {
        val dotRadius = size.toPx() * 0.16f * animProgress
        val padding = size.toPx() * 0.28f
        val right = size.toPx() - padding
        val top = padding
        val bottom = size.toPx() - padding

        // 4 puntos en disposición cuadrada
        // Superior izquierdo
        drawCircle(color = dotColor, radius = dotRadius, center = Offset(padding, top))
        // Superior derecho
        drawCircle(color = dotColor, radius = dotRadius, center = Offset(right, top))
        // Inferior izquierdo
        drawCircle(color = dotColor, radius = dotRadius, center = Offset(padding, bottom))
        // Inferior derecho
        drawCircle(color = dotColor, radius = dotRadius, center = Offset(right, bottom))
    }
}

/**
 * Título corporativo "FERJI INSPECCIONES" con diseño cuadrado/moderno.
 * Se usa como TopBar personalizado en todas las pantallas.
 *
 * @param subtitle Texto secundario debajo del título (ej: nombre de la pantalla)
 * @param showDots Mostrar los 4 puntos del logo
 * @param compact Modo compacto para barras de navegación
 */
@Composable
fun FerjiTitleBar(
    modifier: Modifier = Modifier,
    subtitle: String = "",
    showDots: Boolean = true,
    compact: Boolean = false
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)
    ) {
        if (showDots) {
            Box(
                modifier = Modifier
                    .size(if (compact) 36.dp else 44.dp)
                    .clip(RoundedCornerShape(if (compact) 8.dp else 10.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Primary40, Primary30)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                FerjiDotLogo(
                    size = if (compact) 24.dp else 30.dp,
                    dotColor = Color.White
                )
            }
        }

        Column {
            Text(
                text = "FERJI",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (subtitle.isNotEmpty()) subtitle else "INSPECCIONES",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Barra de sección con color de fondo para las pantallas de inspección.
 * Añade jerarquía visual y color a los formularios.
 */
@Composable
fun FerjiColoredSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    icon: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        icon?.invoke()
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            ),
            color = contentColor
        )
    }
}

/**
 * Divider decorativo con gradiente para separar secciones.
 */
@Composable
fun FerjiGradientDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        color.copy(alpha = 0.3f),
                        color.copy(alpha = 0.5f),
                        color.copy(alpha = 0.3f),
                        Color.Transparent
                    )
                )
            )
    )
}

/**
 * Banner superior con identidad visual FERJI para formularios.
 * Degradado sutil con logo y texto motivacional.
 */
@Composable
fun FerjiBrandBanner(
    title: String,
    subtitle: String = "",
    modifier: Modifier = Modifier,
    gradientColors: List<Color> = listOf(Primary40, Primary30)
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.horizontalGradient(colors = gradientColors)
            )
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Logo en fondo blanco semi-transparente
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                FerjiDotLogo(
                    size = 28.dp,
                    dotColor = Color.White
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

/**
 * Card decorativa para mostrar estadísticas o resúmenes con color.
 */
@Composable
fun FerjiStatCard(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.ExtraBold
            ),
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color.copy(alpha = 0.8f)
        )
    }
}

/**
 * Sección de fotos con diseño mejorado y contador.
 */
@Composable
fun FerjiPhotoSectionHeader(
    photoCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        FerjiOrange.copy(alpha = 0.12f),
                        FerjiOrange.copy(alpha = 0.04f)
                    )
                )
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(FerjiOrange.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text("📸", style = MaterialTheme.typography.labelMedium)
            }
            Text(
                text = "EVIDENCIA FOTOGRÁFICA",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                ),
                color = FerjiOrange
            )
        }
        if (photoCount > 0) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(FerjiOrange)
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "$photoCount",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
            }
        }
    }
}

