package com.aifactory.appmessagecapture.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = PrimaryOrange,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryOrangeLight,
    onPrimaryContainer = PrimaryOrangeDark,
    secondary = SecondaryPurple,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryPurpleLight,
    onSecondaryContainer = SecondaryPurpleDark,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SearchBackground,
    onSurfaceVariant = OnSurfaceVariantLight,
    error = ErrorRed,
    onError = OnErrorLight,
    outline = OutlineLight
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryOrange,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryOrangeDark,
    onPrimaryContainer = PrimaryOrangeLight,
    secondary = SecondaryPurpleLight,
    onSecondary = SecondaryPurpleDark,
    secondaryContainer = SecondaryPurpleDark,
    onSecondaryContainer = SecondaryPurpleLight,
    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFBDBDBD),
    error = Color(0xFFEF5350),
    onError = Color(0xFF000000),
    outline = Color(0xFF616161)
)

@Composable
fun AppMessageCaptureTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
