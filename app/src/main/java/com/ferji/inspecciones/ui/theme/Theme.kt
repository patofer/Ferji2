package com.ferji.inspecciones.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val TenpoLightColorScheme = lightColorScheme(
    primary = Purple20,
    onPrimary = White,
    primaryContainer = Purple90,
    onPrimaryContainer = Purple10,

    secondary = Purple30,
    onSecondary = White,
    secondaryContainer = Purple90,
    onSecondaryContainer = Purple10,

    background = Grey90,
    onBackground = Grey10,

    surface = White,
    onSurface = Grey10,

    error = Color(0xFFBA1A1A),
    onError = White
)

// Esquema de colores para modo oscuro
val TenpoDarkColorScheme = darkColorScheme(
    primary = Purple80,
    onPrimary = Purple20,
    primaryContainer = Purple30,
    onPrimaryContainer = White,

    secondary = Purple80,
    onSecondary = Purple20,
    secondaryContainer = Purple30,
    onSecondaryContainer = White,

    background = Grey10,
    onBackground = Grey90,

    surface = Grey20,
    onSurface = Grey90,

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)


@Composable
fun FerjiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        TenpoDarkColorScheme
    } else {
        TenpoLightColorScheme
    }

    MaterialTheme(
        colorScheme = colors, // Define tu tipografía si es necesario
        content = content
    )
}