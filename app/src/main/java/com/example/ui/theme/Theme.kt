package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = KunjachayaDarkPrimary,
    onPrimary = androidx.compose.ui.graphics.Color(0xFF133800),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF235000),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFB7F38B),
    secondary = androidx.compose.ui.graphics.Color(0xFFBDCBAF),
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFF3E4A35),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFFDDE6D3),
    background = KunjachayaDarkBackground,
    surface = KunjachayaDarkSurface,
    onSurface = androidx.compose.ui.graphics.Color(0xFFE2E3DC),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFC4C8BB),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF2B2E28),
    outline = androidx.compose.ui.graphics.Color(0xFF44483E),
    errorContainer = androidx.compose.ui.graphics.Color(0xFF93000A),
    onErrorContainer = androidx.compose.ui.graphics.Color(0xFFFFDAD6)
)

private val LightColorScheme = lightColorScheme(
    primary = KunjachayaPrimary,
    onPrimary = KunjachayaOnPrimary,
    primaryContainer = KunjachayaPrimaryContainer,
    onPrimaryContainer = KunjachayaOnPrimaryContainer,
    secondary = KunjachayaSecondary,
    secondaryContainer = KunjachayaSecondaryContainer,
    onSecondaryContainer = KunjachayaOnSecondaryContainer,
    background = KunjachayaBackground,
    surface = KunjachayaSurface,
    onSurface = KunjachayaOnSurface,
    onSurfaceVariant = KunjachayaOnSurfaceVariant,
    surfaceVariant = KunjachayaCardBackground,
    outline = KunjachayaOutline,
    errorContainer = KunjachayaErrorContainer,
    onErrorContainer = KunjachayaOnErrorContainer
)

@Composable
fun KunjachayaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set false to preserve Kunjachaya Club brand identity
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
