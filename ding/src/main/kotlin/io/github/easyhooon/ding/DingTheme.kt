package io.github.easyhooon.ding

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat

private val DingLightColorScheme = lightColorScheme(
    primary = Color(0xFF006D77),
)

private val DingDarkColorScheme = darkColorScheme(
    primary = Color(0xFF83D5DE),
)

@Composable
internal fun DingTheme(
    darkMode: Boolean?,
    content: @Composable () -> Unit,
) {
    val isDark = darkMode ?: isSystemInDarkTheme()
    val colorScheme = if (isDark) DingDarkColorScheme else DingLightColorScheme

    MaterialTheme(colorScheme = colorScheme) {
        ApplyDingSystemBars(isDark = isDark, background = colorScheme.surface)
        content()
    }
}

@Composable
private fun ApplyDingSystemBars(isDark: Boolean, background: Color) {
    val activity = LocalContext.current.findComponentActivity() ?: return
    DisposableEffect(isDark, background) {
        val barColor = background.toArgb()
        val statusBarStyle = if (isDark) {
            SystemBarStyle.dark(barColor)
        } else {
            SystemBarStyle.light(scrim = barColor, darkScrim = barColor)
        }
        val navigationBarStyle = if (isDark) {
            SystemBarStyle.dark(barColor)
        } else {
            SystemBarStyle.light(scrim = barColor, darkScrim = barColor)
        }
        activity.enableEdgeToEdge(
            statusBarStyle = statusBarStyle,
            navigationBarStyle = navigationBarStyle,
        )
        activity.window.isNavigationBarContrastEnforced = false
        WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }
        onDispose { }
    }
}

private tailrec fun Context.findComponentActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findComponentActivity()
    else -> null
}
