package com.aifactory.appmessagecapture.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = BrandTeal,
    onPrimary = OnBrandTeal,
    primaryContainer = BrandTealLight,
    onPrimaryContainer = BrandTealDark,
    secondary = MistBlue,
    onSecondary = OnMistBlue,
    secondaryContainer = MistBlueLight,
    onSecondaryContainer = MistBlueDark,
    tertiary = SandGold,
    onTertiary = OnBrandTeal,
    tertiaryContainer = SandGoldLight,
    onTertiaryContainer = SandGoldDark,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    error = ErrorRed,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    outline = OutlineLight
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandTeal,
    onPrimary = OnBrandTeal,
    primaryContainer = Color(0xFF14524A),
    onPrimaryContainer = Color(0xFFA8F0E4),
    secondary = Color(0xFF9FBFE4),
    onSecondary = Color(0xFF1B3447),
    secondaryContainer = Color(0xFF2E4659),
    onSecondaryContainer = Color(0xFFC9DDF4),
    tertiary = Color(0xFFD9B98C),
    onTertiary = Color(0xFF3A2A15),
    tertiaryContainer = Color(0xFF4A3B26),
    onTertiaryContainer = Color(0xFFF7E5C8),
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    error = ErrorRedDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    outline = OutlineDark
)

// Soft UI shape scale: 8 / 16 / 24 / 28 dp
val SoftShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

/** No-op indication: components show no ripple / press shadow on click. */
private object NoIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        object : Modifier.Node() {}

    override fun equals(other: Any?): Boolean = this === other

    override fun hashCode(): Int = System.identityHashCode(this)
}

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
        shapes = SoftShapes
    ) {
        // Must be INSIDE MaterialTheme: material3's MaterialTheme provides
        // its own ripple as LocalIndication, which would shadow an outer
        // provider. This inner override wins, so every default-indication
        // clickable (cards, rows, tabs) shows no ripple / press shadow.
        CompositionLocalProvider(LocalIndication provides NoIndication) {
            content()
        }
    }
}
