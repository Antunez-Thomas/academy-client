package com.example.academyapp.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AcademyNavigationStateTest {
    @Test
    fun registrationOpensMyLearningWhileNormalAuthenticationOpensHome() {
        assertEquals(AcademyDestination.Home, initialAuthenticatedDestination(false))
        assertEquals(AcademyDestination.MyLearning, initialAuthenticatedDestination(true))
    }

    @Test
    fun selectingRootDestinationReplacesOnlySelectedDestination() {
        val initial = AcademyNavigationState()

        assertEquals(AcademyDestination.Home, initial.selectedDestination)
        assertEquals(
            AcademyNavigationState(AcademyDestination.Profile),
            initial.select(AcademyDestination.Profile),
        )
        assertEquals(AcademyDestination.Home, initial.selectedDestination)
    }

    @Test
    fun signedOutSessionReturnsNavigationToHome() {
        val state = AcademyNavigationState(AcademyDestination.Profile)

        assertEquals(AcademyNavigationState(), state.clearForSignedOutSession())
    }

    @Test
    fun selectedRootRestoresFromSaveableValueAfterRecreation() {
        val beforeRecreation = AcademyNavigationState(AcademyDestination.Profile)

        val restored = AcademyNavigationState.restore(
            savedRoot = beforeRecreation.saveableRoot(),
            retainedQuizChildActive = false,
        )

        assertEquals(AcademyDestination.Profile, restored.selectedDestination)
    }

    @Test
    fun liveQuizAfterConfigurationChangeStaysInsideClosableMyLearningShell() {
        val restored = AcademyShellState.restore(
            savedRoot = AcademyDestination.Profile.name,
            hasLearningChild = true,
            retainedQuizChildActive = true,
        )

        assertEquals(AcademyDestination.MyLearning, restored.navigation.selectedDestination)
        assertFalse(restored.showPrimaryNavigation)
        assertTrue(restored.canCloseLearningChild)
    }

    @Test
    fun processDeathDiscardsQuizAndRestoresVisibleClosableCurriculumShell() {
        val routes = listOf(
            com.example.academyapp.course.navigation.LearningRoute.Dashboard,
            com.example.academyapp.course.navigation.LearningRoute.Curriculum(42),
            com.example.academyapp.course.navigation.LearningRoute.QuizWeb(42, 9),
        )
        val restoredRoutes = com.example.academyapp.course.navigation.restoreLearningNavigation(
            1, com.example.academyapp.course.navigation.saveLearningNavigation(1, routes),
        )!!.routes
        val restored = AcademyShellState.restore(
            savedRoot = AcademyDestination.MyLearning.name,
            hasLearningChild = restoredRoutes.last() != com.example.academyapp.course.navigation.LearningRoute.Dashboard,
            retainedQuizChildActive = restoredRoutes.last() is com.example.academyapp.course.navigation.LearningRoute.QuizWeb,
        )
        assertEquals(AcademyDestination.MyLearning, restored.navigation.selectedDestination)
        assertTrue(restored.showPrimaryNavigation)
        assertTrue(restored.canCloseLearningChild)
    }

    @Test
    fun unknownSavedDestinationFallsBackToHome() {
        assertEquals(
            AcademyDestination.Home,
            AcademyNavigationState.restore("removed-destination", retainedQuizChildActive = false)
                .selectedDestination,
        )
    }

    @Test
    fun primaryDestinationsExposeStableSelection() {
        assertEquals(
            listOf(AcademyDestination.Home, AcademyDestination.MyLearning, AcademyDestination.Profile),
            AcademyDestination.entries,
        )
        assertEquals(
            AcademyDestination.MyLearning,
            AcademyNavigationState(AcademyDestination.MyLearning).selectedDestination,
        )
    }

    @Test
    fun windowClassesUseCompactMediumAndExpandedBreakpoints() {
        assertEquals(AcademyWindowClass.Compact, AcademyWindowClass.fromWidth(599f))
        assertEquals(AcademyWindowClass.Medium, AcademyWindowClass.fromWidth(600f))
        assertEquals(AcademyWindowClass.Medium, AcademyWindowClass.fromWidth(839f))
        assertEquals(AcademyWindowClass.Expanded, AcademyWindowClass.fromWidth(840f))
    }

    @Test
    fun heightClassesIdentifyShortLandscapeAndTallWindows() {
        assertEquals(AcademyHeightClass.Compact, AcademyHeightClass.fromHeight(479f))
        assertEquals(AcademyHeightClass.Medium, AcademyHeightClass.fromHeight(480f))
        assertEquals(AcademyHeightClass.Medium, AcademyHeightClass.fromHeight(899f))
        assertEquals(AcademyHeightClass.Expanded, AcademyHeightClass.fromHeight(900f))
    }

    @Test
    fun adaptiveShellUsesBottomBarOnlyForCompactWindows() {
        assertEquals(
            AcademyNavigationPlacement.BottomBar,
            adaptiveLayoutFor(AcademyWindowClass.Compact).navigationPlacement,
        )
        assertEquals(
            AcademyNavigationPlacement.Rail,
            adaptiveLayoutFor(AcademyWindowClass.Medium).navigationPlacement,
        )
        assertEquals(
            AcademyNavigationPlacement.Rail,
            adaptiveLayoutFor(AcademyWindowClass.Expanded).navigationPlacement,
        )
    }

    @Test
    fun mediumAndExpandedConstrainContentAndTwoPaneNeedsEnoughUsableWidth() {
        assertFalse(adaptiveLayoutFor(AcademyWindowClass.Compact).constrainsReadingWidth)
        assertTrue(adaptiveLayoutFor(AcademyWindowClass.Medium).constrainsReadingWidth)
        assertTrue(adaptiveLayoutFor(AcademyWindowClass.Expanded).constrainsReadingWidth)
        assertFalse(adaptiveLayoutFor(AcademyWindowClass.Medium).supportsListDetail)
        assertTrue(adaptiveLayoutFor(AcademyWindowClass.Expanded).supportsListDetail)
    }

    @Test
    fun shortLandscapeUsesRailAndTwoPanesWithoutTreatingTallFoldAsLandscape() {
        val phoneLandscape = adaptiveLayoutFor(widthDp = 914f, heightDp = 411f)
        assertEquals(AcademyNavigationPlacement.Rail, phoneLandscape.navigationPlacement)
        assertTrue(phoneLandscape.supportsListDetail)
        assertTrue(phoneLandscape.isLandscape)

        val narrowLandscape = adaptiveLayoutFor(widthDp = 680f, heightDp = 400f)
        assertEquals(AcademyNavigationPlacement.Rail, narrowLandscape.navigationPlacement)
        assertFalse(narrowLandscape.supportsListDetail)

        val bookFoldPortrait = adaptiveLayoutFor(widthDp = 673f, heightDp = 841f)
        assertEquals(AcademyNavigationPlacement.Rail, bookFoldPortrait.navigationPlacement)
        assertFalse(bookFoldPortrait.supportsListDetail)
        assertFalse(bookFoldPortrait.isLandscape)

        val unfoldedTablet = adaptiveLayoutFor(widthDp = 1_000f, heightDp = 700f)
        assertTrue(unfoldedTablet.supportsListDetail)

        assertFalse(adaptiveLayoutFor(widthDp = 719f, heightDp = 800f).supportsListDetail)
        assertTrue(adaptiveLayoutFor(widthDp = 720f, heightDp = 800f).supportsListDetail)
    }

    @Test
    fun adaptiveContentWidthCapsExpandedSurfaceAndKeepsSmallerLayoutsNatural() {
        assertEquals(400f, adaptiveContentWidthDp(AcademyWindowClass.Compact, 400f, 840f))
        assertEquals(800f, adaptiveContentWidthDp(AcademyWindowClass.Medium, 800f, 840f))
        assertEquals(840f, adaptiveContentWidthDp(AcademyWindowClass.Expanded, 1200f, 840f))
        assertEquals(700f, adaptiveContentWidthDp(AcademyWindowClass.Expanded, 700f, 840f))
    }

    @Test
    fun navigationContractKeepsDestinationsAndMinimumTouchTarget() {
        assertEquals(listOf(AcademyDestination.Home, AcademyDestination.MyLearning, AcademyDestination.Profile), AcademyDestination.entries)
        assertEquals(48f, com.example.academyapp.design.AcademyDimensions.MinimumTouchTarget.value)
    }
}
