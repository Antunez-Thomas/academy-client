package com.example.academyapp.navigation

enum class AcademyDestination {
    Home,
    MyLearning,
    Profile,
}

fun initialAuthenticatedDestination(registrationJustCompleted: Boolean): AcademyDestination =
    if (registrationJustCompleted) AcademyDestination.MyLearning else AcademyDestination.Home

data class AcademyNavigationState(
    val selectedDestination: AcademyDestination = AcademyDestination.Home,
) {
    fun select(destination: AcademyDestination): AcademyNavigationState = copy(selectedDestination = destination)

    fun clearForSignedOutSession(): AcademyNavigationState = copy(selectedDestination = AcademyDestination.Home)

    /** Persist only a stable, Bundle-saveable enum name instead of this Kotlin data class. */
    fun saveableRoot(): String = selectedDestination.name

    companion object {
        /** A retained quiz child must be hosted by My Learning, including after shell recreation. */
        fun restore(savedRoot: String?, retainedQuizChildActive: Boolean): AcademyNavigationState {
            val restoredRoot = AcademyDestination.entries.firstOrNull { it.name == savedRoot }
                ?: AcademyDestination.Home
            return AcademyNavigationState(
                selectedDestination = if (retainedQuizChildActive) AcademyDestination.MyLearning else restoredRoot,
            )
        }
    }
}

/** Presentation decisions derived from saveable root state and retained feature state. */
data class AcademyShellState(
    val navigation: AcademyNavigationState,
    val showPrimaryNavigation: Boolean,
    val canCloseLearningChild: Boolean,
) {
    companion object {
        fun restore(
            savedRoot: String?,
            hasLearningChild: Boolean,
            retainedQuizChildActive: Boolean,
        ): AcademyShellState = AcademyShellState(
            navigation = AcademyNavigationState.restore(
                savedRoot = savedRoot,
                retainedQuizChildActive = retainedQuizChildActive,
            ),
            showPrimaryNavigation = !retainedQuizChildActive,
            canCloseLearningChild = hasLearningChild,
        )
    }
}

enum class AcademyWindowClass {
    Compact,
    Medium,
    Expanded,
    ;

    companion object {
        fun fromWidth(widthDp: Float): AcademyWindowClass = when {
            widthDp < 600f -> Compact
            widthDp < 840f -> Medium
            else -> Expanded
        }
    }
}

enum class AcademyHeightClass {
    Compact,
    Medium,
    Expanded,
    ;

    companion object {
        fun fromHeight(heightDp: Float): AcademyHeightClass = when {
            heightDp < 480f -> Compact
            heightDp < 900f -> Medium
            else -> Expanded
        }
    }
}

enum class AcademyNavigationPlacement {
    BottomBar,
    Rail,
}

data class AcademyAdaptiveLayout(
    val windowClass: AcademyWindowClass,
    val heightClass: AcademyHeightClass,
    val navigationPlacement: AcademyNavigationPlacement,
    val constrainsReadingWidth: Boolean,
    val supportsListDetail: Boolean,
    val isLandscape: Boolean,
)

fun adaptiveLayoutFor(windowClass: AcademyWindowClass): AcademyAdaptiveLayout = adaptiveLayoutFor(
    widthDp = when (windowClass) {
        AcademyWindowClass.Compact -> 599f
        AcademyWindowClass.Medium -> 700f
        AcademyWindowClass.Expanded -> 1_000f
    },
    heightDp = 900f,
)

fun adaptiveLayoutFor(widthDp: Float, heightDp: Float): AcademyAdaptiveLayout {
    val safeWidth = widthDp.coerceAtLeast(0f)
    val safeHeight = heightDp.coerceAtLeast(0f)
    val windowClass = AcademyWindowClass.fromWidth(safeWidth)
    val heightClass = AcademyHeightClass.fromHeight(safeHeight)
    val isLandscape = safeWidth > safeHeight
    val shortLandscape = isLandscape && heightClass == AcademyHeightClass.Compact
    // This function receives the usable content width as well as the outer window width. A 720dp
    // threshold keeps a book-fold portrait single-pane while allowing a reading-width-constrained
    // unfolded display (and a short landscape phone) to provide two useful panes.
    val supportsTwoPane = safeWidth >= 720f
    return AcademyAdaptiveLayout(
        windowClass = windowClass,
        heightClass = heightClass,
        navigationPlacement = if (windowClass == AcademyWindowClass.Compact && !shortLandscape) {
            AcademyNavigationPlacement.BottomBar
        } else {
            AcademyNavigationPlacement.Rail
        },
        constrainsReadingWidth = windowClass != AcademyWindowClass.Compact,
        supportsListDetail = supportsTwoPane,
        isLandscape = isLandscape,
    )
}

/** Returns the content width the adaptive shell should allocate before horizontal padding. */
fun adaptiveContentWidthDp(
    windowClass: AcademyWindowClass,
    availableWidthDp: Float,
    readingMaxWidthDp: Float,
): Float {
    val available = availableWidthDp.coerceAtLeast(0f)
    val readingMax = readingMaxWidthDp.coerceAtLeast(0f)
    return if (adaptiveLayoutFor(windowClass).constrainsReadingWidth) {
        minOf(available, readingMax)
    } else {
        available
    }
}
