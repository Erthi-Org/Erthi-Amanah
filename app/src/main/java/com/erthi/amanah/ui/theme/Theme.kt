package com.erthi.amanah.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ErthiDarkColorScheme = darkColorScheme(
    primary = RoyalGoldLight,
    onPrimary = NavyDeep,
    primaryContainer = NavyCard,
    onPrimaryContainer = SilverLight,
    secondary = PurpleEnamelLight,
    onSecondary = NavyDeep,
    secondaryContainer = NavyMid,
    onSecondaryContainer = SilverPale,
    tertiary = SuccessGreenLight,
    onTertiary = NavyDeep,
    background = NavyDeep,
    onBackground = TextPrimary,
    surface = NavyMid,
    onSurface = TextPrimary,
    surfaceVariant = NavyCard,
    onSurfaceVariant = TextSecondary,
    outline = DividerColor,
    outlineVariant = NavyLight,
    error = EmergencyRed,
    onError = Color.White,
    errorContainer = Color(0xFF4A0C0C),
    onErrorContainer = EmergencyRedLight,
    inverseSurface = SilverPale,
    inverseOnSurface = NavyDeep,
    inversePrimary = NavyCard,
    scrim = Color.Black.copy(alpha = 0.6f),
)

@Composable
fun ErthiAmanaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ErthiDarkColorScheme,
        typography = ErthiTypography,
        shapes = ErthiShapes,
        content = content,
    )
}
