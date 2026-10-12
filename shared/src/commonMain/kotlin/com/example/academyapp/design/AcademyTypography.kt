package com.example.academyapp.design

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.academyapp.generated.resources.Res
import com.example.academyapp.generated.resources.montserrat_bold
import com.example.academyapp.generated.resources.montserrat_extrabold
import com.example.academyapp.generated.resources.montserrat_medium
import com.example.academyapp.generated.resources.montserrat_semibold
import org.jetbrains.compose.resources.Font

@Composable
fun academyFontFamily(): FontFamily = FontFamily(
    Font(Res.font.montserrat_medium, FontWeight.Medium),
    Font(Res.font.montserrat_semibold, FontWeight.SemiBold),
    Font(Res.font.montserrat_bold, FontWeight.Bold),
    Font(Res.font.montserrat_extrabold, FontWeight.ExtraBold),
)

@Composable
fun academyTypography(): Typography {
    val family = academyFontFamily()
    val defaults = Typography()
    fun TextStyle.withAcademyFont() = copy(fontFamily = family)
    return defaults.copy(
        displayLarge = defaults.displayLarge.withAcademyFont(),
        displayMedium = defaults.displayMedium.withAcademyFont(),
        displaySmall = defaults.displaySmall.withAcademyFont(),
        headlineLarge = defaults.headlineLarge.withAcademyFont(),
        headlineMedium = TextStyle(
            fontFamily = family,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.5).sp,
        ),
        headlineSmall = defaults.headlineSmall.withAcademyFont(),
        titleLarge = TextStyle(fontFamily = family, fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.ExtraBold),
        titleMedium = TextStyle(fontFamily = family, fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold),
        titleSmall = defaults.titleSmall.withAcademyFont(),
        bodyLarge = TextStyle(fontFamily = family, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
        bodyMedium = TextStyle(fontFamily = family, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
        bodySmall = defaults.bodySmall.withAcademyFont(),
        labelLarge = TextStyle(fontFamily = family, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold),
        labelMedium = TextStyle(fontFamily = family, fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold),
        labelSmall = defaults.labelSmall.withAcademyFont(),
    )
}
