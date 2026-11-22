package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val NeonViceColorScheme = darkColorScheme(
    primary = NeonMagenta,
    onPrimary = TextPrimary,
    primaryContainer = NeonMagentaSoft,
    onPrimaryContainer = TextPrimary,
    secondary = ElectricCyan,
    onSecondary = ViceVoid,
    secondaryContainer = ElectricCyanSoft,
    onSecondaryContainer = TextPrimary,
    tertiary = ViceGold,
    onTertiary = ViceVoid,
    tertiaryContainer = ViceGoldSoft,
    onTertiaryContainer = ViceGold,
    background = ViceVoid,
    onBackground = TextPrimary,
    surface = ViceSurface,
    onSurface = TextPrimary,
    surfaceVariant = ViceSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainerHigh = ViceSurfaceHigh,
    outline = OutlineSubtle,
    error = DangerRed,
    onError = TextPrimary,
)

private val NeonViceShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
)

@Composable
fun NeonViceTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = NeonViceColorScheme,
        typography = NeonViceTypography,
        shapes = NeonViceShapes,
        content = content,
    )
}
