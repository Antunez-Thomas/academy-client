package com.example.academyapp.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Semantic colors from the approved Academy mobile redesign. */
object AcademyColors {
    val BrandBlue = Color(0xFF3056C4)
    val BrandNavy = Color(0xFF1E2D6B)
    val BrandOrange = Color(0xFFFF6A00)
    val BrandOrangeDark = Color(0xFFB84A00)
    val White = Color(0xFFFFFFFF)
    val Black = Color(0xFF000000)

    internal val BlueContainer = Color(0xFFDFE7FB)
    internal val OnBlueContainer = BrandNavy
    internal val OrangeContainer = Color(0xFFFFE7D6)
    internal val OnOrangeContainer = Color(0xFF663000)
    internal val LightBackground = Color(0xFFF4F6FC)
    internal val LightSurface = White
    internal val LightSurfaceVariant = Color(0xFFE8EDFB)
    internal val LightOutline = Color(0xFFC5D0F0)
    internal val ProgressTrack = Color(0xFFE3E6F2)
    internal val SecondaryText = Color(0xFF55608A)
    internal val Error = Color(0xFFB3261E)
    internal val OnError = White
    internal val Success = Color(0xFF216B3A)
    internal val OnSuccess = White
    internal val Warning = Color(0xFF795900)
    internal val OnWarning = White
    internal val AuthInputForeground = BrandNavy
    internal val AuthInputForegroundVariant = SecondaryText
    internal val AuthInputDisabledForeground = Color(0xFF8B92A8)
    internal val AuthInputBackground = White
    internal val AuthInputDisabledBackground = Color(0xFFF5F6FA)
    internal val AuthInputOutline = LightOutline
    internal val AuthDisabledAction = Color(0xFFB7C1DB)
}

object AcademyShapes {
    val Card = RoundedCornerShape(20.dp)
    val FeaturedCard = RoundedCornerShape(24.dp)
    val ListRow = RoundedCornerShape(16.dp)
    val Field = RoundedCornerShape(14.dp)
    val Pill = RoundedCornerShape(percent = 50)
    val Sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
}

object AcademySpacing {
    val XSmall: Dp = 4.dp
    val Small: Dp = 8.dp
    val MediumSmall: Dp = 12.dp
    val Medium: Dp = 16.dp
    val Large: Dp = 24.dp
    val XLarge: Dp = 32.dp
    val XXLarge: Dp = 40.dp
    val Huge: Dp = 48.dp
}

object AcademyDimensions {
    val CompactHorizontalPadding: Dp = 20.dp
    val MediumHorizontalPadding: Dp = 24.dp
    val ExpandedHorizontalPadding: Dp = 32.dp
    val ReadingMaxWidth: Dp = 840.dp
    val SidePaneMaxWidth: Dp = 360.dp
    val NavigationRailWidth: Dp = 112.dp
    val FoldSafePaneGutter: Dp = 32.dp
    val AuthFormMaxWidth: Dp = 460.dp
    val CardCornerRadius: Dp = 20.dp
    val ControlCornerRadius: Dp = 14.dp
    val LargeCornerRadius: Dp = 24.dp
    val AccessHeaderHeight: Dp = 164.dp
    val AccessHeaderTopPadding: Dp = 40.dp
    val AccessHorizontalPadding: Dp = 24.dp
    val AccessFormPadding: Dp = 20.dp
    val ShortViewportVerticalPadding: Dp = 8.dp
    val FieldMinimumHeight: Dp = 52.dp
    val ButtonMinimumHeight: Dp = 52.dp
    val ProgressBarHeight: Dp = 6.dp
    val AvatarSize: Dp = 56.dp
    val MinimumTouchTarget: Dp = 48.dp
    val InlineIndicatorSize: Dp = 20.dp
    val InlineIndicatorStrokeWidth: Dp = 2.dp
}
