package com.officeestimator.app.ui

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val Light = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD5EDE9),
    onPrimaryContainer = Color(0xFF07332F),
    secondary = Color(0xFFB7791F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF6E7C8),
    onSecondaryContainer = Color(0xFF3F2A06),
    background = Color(0xFFF3F6F5),
    onBackground = Color(0xFF16302D),
    surface = Color.White,
    onSurface = Color(0xFF16302D),
    surfaceVariant = Color(0xFFE7EEEC),
    onSurfaceVariant = Color(0xFF5D716E),
    outline = Color(0xFF9DB0AD),
    outlineVariant = Color(0xFFDCE5E3),
    errorContainer = Color(0xFFFDECEA),
    onErrorContainer = Color(0xFF7A1F16)
)

private val Dark = darkColorScheme(
    primary = Color(0xFF5FD0C3),
    onPrimary = Color(0xFF05302C),
    primaryContainer = Color(0xFF134E48),
    onPrimaryContainer = Color(0xFFD5EDE9),
    secondary = Color(0xFFE9B863),
    onSecondary = Color(0xFF3F2A06),
    secondaryContainer = Color(0xFF5A4012),
    onSecondaryContainer = Color(0xFFF6E7C8),
    background = Color(0xFF0E1716),
    onBackground = Color(0xFFE3ECEA),
    surface = Color(0xFF17221F),
    onSurface = Color(0xFFE3ECEA),
    surfaceVariant = Color(0xFF233330),
    onSurfaceVariant = Color(0xFFA5B8B4),
    outline = Color(0xFF5D716E),
    outlineVariant = Color(0xFF2C3D3A),
    errorContainer = Color(0xFF5C1E17),
    onErrorContainer = Color(0xFFFAD4CF)
)

@Composable
fun EstimatorTheme(dark: Boolean, content: @Composable () -> Unit) {
    val colors = if (dark) Dark else Light
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colors.background.toArgb()
                window.navigationBarColor = colors.surface.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !dark
                controller.isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(colorScheme = colors, content = content)
}
