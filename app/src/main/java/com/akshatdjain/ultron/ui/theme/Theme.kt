package com.akshatdjain.ultron.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val UltronDarkColorScheme = darkColorScheme(
    primary = UltronPrimary,
    onPrimary = UltronOnPrimary,
    secondary = UltronSecondary,
    tertiary = UltronTertiary,
    background = UltronBackground,
    onBackground = UltronOnBackground,
    surface = UltronSurface,
    onSurface = UltronOnBackground,
    surfaceVariant = UltronSurfaceVariant,
    onSurfaceVariant = UltronOnSurfaceVariant,
    outline = UltronOutline,
    error = UltronError
)

@Composable
fun UltronTheme(
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicDarkColorScheme(context)
    } else {
        UltronDarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = UltronTypography,
        content = content
    )
}
