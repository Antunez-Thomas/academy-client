package com.example.academyapp.design

import kotlin.test.Test
import kotlin.test.assertEquals
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

class AcademyColorsTest {
    @Test
    fun brandColorsMatchApprovedAcademyPalette() {
        assertEquals(Color(0xFF3056C4), AcademyColors.BrandBlue)
        assertEquals(Color(0xFF1E2D6B), AcademyColors.BrandNavy)
        assertEquals(Color(0xFFFF6A00), AcademyColors.BrandOrange)
    }

    @Test
    fun primaryNavigationUsesSharedVectorAssets() {
        assertEquals("AcademyHome", AcademyNavigationIcons.Home.name)
        assertEquals("AcademyMyLearning", AcademyNavigationIcons.MyLearning.name)
        assertEquals("AcademyProfile", AcademyNavigationIcons.Profile.name)
        assertEquals(24.dp, AcademyNavigationIcons.Home.defaultWidth)
        assertEquals(24.dp, AcademyNavigationIcons.MyLearning.defaultWidth)
        assertEquals(24.dp, AcademyNavigationIcons.Profile.defaultWidth)
    }
}
