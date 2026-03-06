package com.ferji.inspecciones.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ═══════════════════════════════════════════════════════════════
//  SHAPES — Material Design 3 Shape Scale
// ═══════════════════════════════════════════════════════════════

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

// ═══════════════════════════════════════════════════════════════
//  SPACING TOKENS (dp)
// ═══════════════════════════════════════════════════════════════
object Spacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val base = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 40.dp
    val huge = 48.dp
    val massive = 64.dp
}

// ═══════════════════════════════════════════════════════════════
//  ELEVATION TOKENS (dp)
// ═══════════════════════════════════════════════════════════════
object Elevation {
    val none = 0.dp
    val level1 = 1.dp
    val level2 = 3.dp
    val level3 = 6.dp
    val level4 = 8.dp
    val level5 = 12.dp
}

// ═══════════════════════════════════════════════════════════════
//  COMPONENT SIZE TOKENS
// ═══════════════════════════════════════════════════════════════
object ComponentSize {
    val buttonHeight = 52.dp
    val buttonHeightSmall = 40.dp
    val iconButtonSize = 48.dp
    val cardMinHeight = 72.dp
    val topBarHeight = 64.dp
    val searchBarHeight = 56.dp
    val avatarSizeLarge = 48.dp
    val avatarSizeMedium = 40.dp
    val avatarSizeSmall = 32.dp
    val iconSizeLarge = 28.dp
    val iconSizeMedium = 24.dp
    val iconSizeSmall = 20.dp
}

