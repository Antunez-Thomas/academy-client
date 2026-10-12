package com.example.academyapp.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AcademyLightColors = lightColorScheme(
    primary = AcademyColors.BrandBlue,
    onPrimary = AcademyColors.White,
    primaryContainer = AcademyColors.BlueContainer,
    onPrimaryContainer = AcademyColors.OnBlueContainer,
    secondary = AcademyColors.BrandOrange,
    onSecondary = AcademyColors.White,
    secondaryContainer = AcademyColors.OrangeContainer,
    onSecondaryContainer = AcademyColors.OnOrangeContainer,
    tertiary = AcademyColors.BrandOrangeDark,
    onTertiary = AcademyColors.White,
    background = AcademyColors.LightBackground,
    onBackground = AcademyColors.BrandNavy,
    surface = AcademyColors.LightSurface,
    onSurface = AcademyColors.BrandNavy,
    surfaceVariant = AcademyColors.LightSurfaceVariant,
    onSurfaceVariant = AcademyColors.SecondaryText,
    outline = AcademyColors.LightOutline,
    outlineVariant = AcademyColors.ProgressTrack,
    error = AcademyColors.Error,
    onError = AcademyColors.OnError,
)

@Composable
@Suppress("UNUSED_PARAMETER")
fun AcademyTheme(
    darkTheme: Boolean = false,
    authEntry: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = AcademyLightColors,
        typography = academyTypography(),
        shapes = Shapes(
            extraSmall = AcademyShapes.Field,
            small = AcademyShapes.Field,
            medium = AcademyShapes.Card,
            large = AcademyShapes.Sheet,
            extraLarge = AcademyShapes.FeaturedCard,
        ),
        content = content,
    )
}
