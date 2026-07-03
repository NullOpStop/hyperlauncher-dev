package net.kdt.pojavlaunch.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import androidx.core.graphics.ColorUtils

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    secondary = Color(0xFFCCC2DC),
    tertiary = Color(0xFFEFB8C8)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6750A4),
    secondary = Color(0xFF625B71),
    tertiary = Color(0xFF7D5260)
)

private fun colorSchemeFromSeed(seed: Int, darkTheme: Boolean): ColorScheme {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(seed, hsl)
    val hue = hsl[0]

    return if (darkTheme) {
        val primary = Color(ColorUtils.HSLToColor(floatArrayOf(hue, 0.4f, 0.7f)))
        val onPrimary = Color(ColorUtils.HSLToColor(floatArrayOf(hue, 0.1f, 0.1f)))
        val primaryContainer = Color(ColorUtils.HSLToColor(floatArrayOf(hue, 0.3f, 0.2f)))
        val onPrimaryContainer = Color(ColorUtils.HSLToColor(floatArrayOf(hue, 0.4f, 0.8f)))
        val secondary = Color(ColorUtils.HSLToColor(floatArrayOf(hue, 0.1f, 0.6f)))
        val onSecondary = Color(ColorUtils.HSLToColor(floatArrayOf(hue, 0.1f, 0.1f)))
        val surface = Color(ColorUtils.HSLToColor(floatArrayOf(hue, 0.05f, 0.08f)))
        val onSurface = Color(ColorUtils.HSLToColor(floatArrayOf(hue, 0.05f, 0.9f)))
        val onSurfaceVariant = Color(ColorUtils.HSLToColor(floatArrayOf(hue, 0.05f, 0.7f)))
        val outline = Color(ColorUtils.HSLToColor(floatArrayOf(hue, 0.05f, 0.5f)))
        darkColorScheme(
            primary = primary, onPrimary = onPrimary, primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
            secondary = secondary, onSecondary = onSecondary, surface = surface, onSurface = onSurface, background = surface,
            onBackground = onSurface, surfaceVariant = Color(ColorUtils.HSLToColor(floatArrayOf(hue, 0.05f, 0.12f))),
            onSurfaceVariant = onSurfaceVariant, outline = outline
        )
    } else {
        val primary = Color(ColorUtils.HSLToColor(floatArrayOf(hue, 0.6f, 0.4f)))
        val onPrimary = Color.White
        val primaryContainer = Color(ColorUtils.HSLToColor(floatArrayOf(hue, 0.4f, 0.9f)))
        val onPrimaryContainer = Color(ColorUtils.HSLToColor(floatArrayOf(hue, 0.6f, 0.1f)))
        val secondary = Color(ColorUtils.HSLToColor(floatArrayOf(hue, 0.2f, 0.4f)))
        val onSecondary = Color.White
        val surface = Color(ColorUtils.HSLToColor(floatArrayOf(hue, 0.05f, 0.98f)))
        val onSurface = Color(ColorUtils.HSLToColor(floatArrayOf(hue, 0.05f, 0.1f)))
        val onSurfaceVariant = Color(ColorUtils.HSLToColor(floatArrayOf(hue, 0.05f, 0.3f)))
        val outline = Color(ColorUtils.HSLToColor(floatArrayOf(hue, 0.05f, 0.5f)))
        lightColorScheme(
            primary = primary, onPrimary = onPrimary, primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
            secondary = secondary, onSecondary = onSecondary, surface = surface, onSurface = onSurface, background = surface,
            onBackground = onSurface, surfaceVariant = Color(ColorUtils.HSLToColor(floatArrayOf(hue, 0.05f, 0.92f))),
            onSurfaceVariant = onSurfaceVariant, outline = outline
        )
    }
}

private fun getPresetColor(theme: ColorTheme, preset: String): Color {
    return when (preset) {
        ColorThemeType.EMBERMIRE -> theme.embermire
        ColorThemeType.VELVET_ROSE -> theme.velvetRose
        ColorThemeType.MISTWAVE -> theme.mistwave
        ColorThemeType.GLACIER -> theme.glacier
        ColorThemeType.VERDANTFIELD -> theme.verdantField
        ColorThemeType.URBAN_ASH -> theme.urbanAsh
        ColorThemeType.VERDANT_DAWN -> theme.verdantDawn
        else -> theme.embermire
    }
}

private fun getPresetColorScheme(preset: String, darkTheme: Boolean): ColorScheme {
    return if (darkTheme) {
        darkColorScheme(
            primary = getPresetColor(primaryDark, preset),
            onPrimary = getPresetColor(onPrimaryDark, preset),
            primaryContainer = getPresetColor(primaryContainerDark, preset),
            onPrimaryContainer = getPresetColor(onPrimaryContainerDark, preset),
            secondary = getPresetColor(secondaryDark, preset),
            onSecondary = getPresetColor(onSecondaryDark, preset),
            secondaryContainer = getPresetColor(secondaryContainerDark, preset),
            onSecondaryContainer = getPresetColor(onSecondaryContainerDark, preset),
            tertiary = getPresetColor(tertiaryDark, preset),
            onTertiary = getPresetColor(onTertiaryDark, preset),
            tertiaryContainer = getPresetColor(tertiaryContainerDark, preset),
            onTertiaryContainer = getPresetColor(onTertiaryContainerDark, preset),
            error = getPresetColor(errorDark, preset),
            onError = getPresetColor(onErrorDark, preset),
            errorContainer = getPresetColor(errorContainerDark, preset),
            onErrorContainer = getPresetColor(onErrorContainerDark, preset),
            background = getPresetColor(backgroundDark, preset),
            onBackground = getPresetColor(onBackgroundDark, preset),
            surface = getPresetColor(surfaceDark, preset),
            onSurface = getPresetColor(onSurfaceDark, preset),
            surfaceVariant = getPresetColor(surfaceVariantDark, preset),
            onSurfaceVariant = getPresetColor(onSurfaceVariantDark, preset),
            outline = getPresetColor(outlineDark, preset),
            outlineVariant = getPresetColor(outlineVariantDark, preset),
            scrim = getPresetColor(scrimDark, preset),
            inverseSurface = getPresetColor(inverseSurfaceDark, preset),
            inverseOnSurface = getPresetColor(inverseOnSurfaceDark, preset),
            inversePrimary = getPresetColor(inversePrimaryDark, preset),
            surfaceDim = getPresetColor(surfaceDimDark, preset),
            surfaceBright = getPresetColor(surfaceBrightDark, preset),
            surfaceContainerLowest = getPresetColor(surfaceContainerLowestDark, preset),
            surfaceContainerLow = getPresetColor(surfaceContainerLowDark, preset),
            surfaceContainer = getPresetColor(surfaceContainerDark, preset),
            surfaceContainerHigh = getPresetColor(surfaceContainerHighDark, preset),
            surfaceContainerHighest = getPresetColor(surfaceContainerHighestDark, preset),
        )
    } else {
        lightColorScheme(
            primary = getPresetColor(primaryLight, preset),
            onPrimary = getPresetColor(onPrimaryLight, preset),
            primaryContainer = getPresetColor(primaryContainerLight, preset),
            onPrimaryContainer = getPresetColor(onPrimaryContainerLight, preset),
            secondary = getPresetColor(secondaryLight, preset),
            onSecondary = getPresetColor(onSecondaryLight, preset),
            secondaryContainer = getPresetColor(secondaryContainerLight, preset),
            onSecondaryContainer = getPresetColor(onSecondaryContainerLight, preset),
            tertiary = getPresetColor(tertiaryLight, preset),
            onTertiary = getPresetColor(onTertiaryLight, preset),
            tertiaryContainer = getPresetColor(tertiaryContainerLight, preset),
            onTertiaryContainer = getPresetColor(onTertiaryContainerLight, preset),
            error = getPresetColor(errorLight, preset),
            onError = getPresetColor(onErrorLight, preset),
            errorContainer = getPresetColor(errorContainerLight, preset),
            onErrorContainer = getPresetColor(onErrorContainerLight, preset),
            background = getPresetColor(backgroundLight, preset),
            onBackground = getPresetColor(onBackgroundLight, preset),
            surface = getPresetColor(surfaceLight, preset),
            onSurface = getPresetColor(onSurfaceLight, preset),
            surfaceVariant = getPresetColor(surfaceVariantLight, preset),
            onSurfaceVariant = getPresetColor(onSurfaceVariantLight, preset),
            outline = getPresetColor(outlineLight, preset),
            outlineVariant = getPresetColor(outlineVariantLight, preset),
            scrim = getPresetColor(scrimLight, preset),
            inverseSurface = getPresetColor(inverseSurfaceLight, preset),
            inverseOnSurface = getPresetColor(inverseOnSurfaceLight, preset),
            inversePrimary = getPresetColor(inversePrimaryLight, preset),
            surfaceDim = getPresetColor(surfaceDimLight, preset),
            surfaceBright = getPresetColor(surfaceBrightLight, preset),
            surfaceContainerLowest = getPresetColor(surfaceContainerLowestLight, preset),
            surfaceContainerLow = getPresetColor(surfaceContainerLowLight, preset),
            surfaceContainer = getPresetColor(surfaceContainerLight, preset),
            surfaceContainerHigh = getPresetColor(surfaceContainerHighLight, preset),
            surfaceContainerHighest = getPresetColor(surfaceContainerHighestLight, preset),
        )
    }
}

@Composable
fun PojavTheme(
    darkTheme: Boolean = when(LauncherPreferences.prefAppThemeState.value) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    },
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val isThemeTypeEnabled = LauncherPreferences.PREF_THEME_TYPE_ENABLED_STATE.value
    val themeTypeMode = LauncherPreferences.PREF_THEME_TYPE_MODE_STATE.value

    val colorScheme = when {
        isThemeTypeEnabled -> {
            if (themeTypeMode == ColorThemeType.MONOCHROME) {
                darkColorScheme(
                    primary = Color.White,
                    onPrimary = Color.Black,
                    primaryContainer = Color.DarkGray,
                    onPrimaryContainer = Color.White,
                    background = Color.Black,
                    surface = Color.Black,
                    onSurface = Color.White,
                    surfaceVariant = Color(0xFF1A1A1A),
                    onSurfaceVariant = Color.LightGray
                )
            } else {
                getPresetColorScheme(themeTypeMode, darkTheme)
            }
        }
        LauncherPreferences.PREF_THEME_COLOR_ENABLED_STATE.value -> {
            colorSchemeFromSeed(LauncherPreferences.PREF_THEME_SEED_COLOR_STATE.intValue, darkTheme)
        }
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val animatedColorScheme = animateColorScheme(colorScheme)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity
            activity?.window?.let { window ->
                window.statusBarColor = animatedColorScheme.surface.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme && !isThemeTypeEnabled
            }
        }
    }

    MaterialTheme(
        colorScheme = animatedColorScheme,
        content = content
    )
}

@Composable
private fun animateColorScheme(targetColorScheme: ColorScheme): ColorScheme {
    val animationSpec = tween<Color>(durationMillis = 500)

    @Composable
    fun animateColor(color: Color) = animateColorAsState(targetValue = color, animationSpec = animationSpec, label = "colorAnimation").value

    return targetColorScheme.copy(
        primary = animateColor(targetColorScheme.primary),
        onPrimary = animateColor(targetColorScheme.onPrimary),
        primaryContainer = animateColor(targetColorScheme.primaryContainer),
        onPrimaryContainer = animateColor(targetColorScheme.onPrimaryContainer),
        inversePrimary = animateColor(targetColorScheme.inversePrimary),
        secondary = animateColor(targetColorScheme.secondary),
        onSecondary = animateColor(targetColorScheme.onSecondary),
        secondaryContainer = animateColor(targetColorScheme.secondaryContainer),
        onSecondaryContainer = animateColor(targetColorScheme.onSecondaryContainer),
        tertiary = animateColor(targetColorScheme.tertiary),
        onTertiary = animateColor(targetColorScheme.onTertiary),
        tertiaryContainer = animateColor(targetColorScheme.tertiaryContainer),
        onTertiaryContainer = animateColor(targetColorScheme.onTertiaryContainer),
        background = animateColor(targetColorScheme.background),
        onBackground = animateColor(targetColorScheme.onBackground),
        surface = animateColor(targetColorScheme.surface),
        onSurface = animateColor(targetColorScheme.onSurface),
        surfaceVariant = animateColor(targetColorScheme.surfaceVariant),
        onSurfaceVariant = animateColor(targetColorScheme.onSurfaceVariant),
        surfaceTint = animateColor(targetColorScheme.surfaceTint),
        inverseSurface = animateColor(targetColorScheme.inverseSurface),
        inverseOnSurface = animateColor(targetColorScheme.inverseOnSurface),
        error = animateColor(targetColorScheme.error),
        onError = animateColor(targetColorScheme.onError),
        errorContainer = animateColor(targetColorScheme.errorContainer),
        onErrorContainer = animateColor(targetColorScheme.onErrorContainer),
        outline = animateColor(targetColorScheme.outline),
        outlineVariant = animateColor(targetColorScheme.outlineVariant),
        scrim = animateColor(targetColorScheme.scrim)
    )
}
