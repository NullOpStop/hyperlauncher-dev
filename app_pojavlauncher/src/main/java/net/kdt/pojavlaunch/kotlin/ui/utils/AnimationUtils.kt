package net.kdt.pojavlaunch.ui.utils

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import net.kdt.pojavlaunch.prefs.LauncherPreferences

object AnimationUtils {
    @Composable
    fun getTransitionSpec(): AnimatedContentTransitionScope<*>.() -> ContentTransform {
        val animPreset = LauncherPreferences.PREF_TRANSITION_ANIMATION_STATE.value
        val animDuration = LauncherPreferences.PREF_TRANSITION_DURATION_STATE.intValue
        val animIntensity = LauncherPreferences.PREF_TRANSITION_INTENSITY_STATE.value

        return remember(animPreset, animDuration, animIntensity) {
            {
                when (animPreset) {
                    "fade" -> {
                        fadeIn(animationSpec = tween(animDuration)) togetherWith fadeOut(animationSpec = tween(animDuration))
                    }
                    "bounce" -> {
                        val enter = slideInVertically(
                            initialOffsetY = { h -> -(h * 0.12f * animIntensity).toInt() },
                            animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)
                        ) + fadeIn(tween(animDuration))
                        val exit = slideOutVertically(
                            targetOffsetY = { h -> (h * 0.12f * animIntensity).toInt() },
                            animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)
                        ) + fadeOut(tween(animDuration / 2))
                        enter togetherWith exit using SizeTransform(clip = false)
                    }
                    else -> {
                        fadeIn(animationSpec = tween(animDuration)) togetherWith fadeOut(animationSpec = tween(animDuration))
                    }
                }
            }
        }
    }
}
