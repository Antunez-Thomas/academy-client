package com.example.academyapp.design.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.academyapp.design.AcademyDimensions
import com.example.academyapp.design.AcademyNavigationIcons
import com.example.academyapp.design.AcademySpacing
import com.example.academyapp.design.AcademyTheme
import com.example.academyapp.generated.resources.Res
import com.example.academyapp.generated.resources.nav_home
import com.example.academyapp.generated.resources.nav_my_learning
import com.example.academyapp.generated.resources.nav_profile
import org.jetbrains.compose.resources.stringResource
import com.example.academyapp.navigation.AcademyDestination
import com.example.academyapp.navigation.AcademyHeightClass
import com.example.academyapp.navigation.AcademyNavigationPlacement
import com.example.academyapp.navigation.AcademyWindowClass
import com.example.academyapp.navigation.adaptiveContentWidthDp
import com.example.academyapp.navigation.adaptiveLayoutFor

@Composable
fun AcademyAppScaffold(
    destination: AcademyDestination,
    showNavigation: Boolean,
    onDestinationSelected: (AcademyDestination) -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    val currentContent by rememberUpdatedState(content)
    val movableContent = remember {
        movableContentOf<PaddingValues> { padding -> currentContent(padding) }
    }
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
        val adaptiveLayout = adaptiveLayoutFor(maxWidth.value, maxHeight.value)
        val windowClass = adaptiveLayout.windowClass
        when {
            !showNavigation -> Scaffold { padding ->
                AcademyContentFrame(windowClass, adaptiveLayout.heightClass, padding, movableContent)
            }
            adaptiveLayout.navigationPlacement == AcademyNavigationPlacement.BottomBar -> Scaffold(
                bottomBar = { AcademyDestinationBar(destination, onDestinationSelected) },
            ) { padding -> AcademyContentFrame(windowClass, adaptiveLayout.heightClass, padding, movableContent) }
            else -> Row(Modifier.fillMaxSize()) {
                AcademyDestinationRail(destination, onDestinationSelected)
                Scaffold(Modifier.weight(1f)) { padding ->
                    AcademyContentFrame(windowClass, adaptiveLayout.heightClass, padding, movableContent)
                }
            }
        }
    }
}

@Composable
private fun AcademyContentFrame(
    windowClass: AcademyWindowClass,
    heightClass: AcademyHeightClass,
    padding: PaddingValues,
    content: @Composable (PaddingValues) -> Unit,
) {
    val horizontalPadding = when (windowClass) {
        AcademyWindowClass.Compact -> AcademyDimensions.CompactHorizontalPadding
        AcademyWindowClass.Medium -> AcademyDimensions.MediumHorizontalPadding
        AcademyWindowClass.Expanded -> AcademyDimensions.ExpandedHorizontalPadding
    }
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentAlignment = Alignment.TopCenter,
    ) {
        val contentWidthDp = adaptiveContentWidthDp(
            windowClass = windowClass,
            availableWidthDp = maxWidth.value,
            readingMaxWidthDp = AcademyDimensions.ReadingMaxWidth.value,
        )
        Box(
            modifier = Modifier.widthIn(max = contentWidthDp.dp)
                .fillMaxSize()
                .padding(
                    horizontal = horizontalPadding,
                    vertical = if (heightClass == AcademyHeightClass.Compact) {
                        AcademyDimensions.ShortViewportVerticalPadding
                    } else {
                        AcademySpacing.Medium
                    },
                ),
        ) {
            content(PaddingValues())
        }
    }
}

@Composable
private fun AcademyDestinationBar(
    selected: AcademyDestination,
    onSelected: (AcademyDestination) -> Unit,
) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        AcademyDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = selected == destination,
                onClick = { onSelected(destination) },
                icon = { DestinationGlyph(destination, selected == destination) },
                label = { DestinationLabel(destination) },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                modifier = Modifier.defaultMinSize(
                    minWidth = AcademyDimensions.MinimumTouchTarget,
                    minHeight = AcademyDimensions.MinimumTouchTarget,
                ),
            )
        }
    }
}

@Composable
private fun AcademyDestinationRail(
    selected: AcademyDestination,
    onSelected: (AcademyDestination) -> Unit,
) {
    NavigationRail(
        modifier = Modifier.width(AcademyDimensions.NavigationRailWidth),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column {
            AcademyDestination.entries.forEach { destination ->
                NavigationRailItem(
                    selected = selected == destination,
                    onClick = { onSelected(destination) },
                    icon = { DestinationGlyph(destination, selected == destination) },
                    label = { DestinationLabel(destination) },
                    alwaysShowLabel = true,
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    modifier = Modifier.fillMaxWidth().defaultMinSize(
                        minWidth = AcademyDimensions.MinimumTouchTarget,
                        minHeight = AcademyDimensions.MinimumTouchTarget,
                    ),
                )
            }
        }
    }
}

@Composable
private fun DestinationLabel(destination: AcademyDestination) {
    // Keep system font scaling and let the item grow to fit the complete label.
    Text(destination.localizedLabel(), textAlign = TextAlign.Center)
}

@Composable
private fun AcademyDestination.localizedLabel(): String = stringResource(
    when (this) {
        AcademyDestination.Home -> Res.string.nav_home
        AcademyDestination.MyLearning -> Res.string.nav_my_learning
        AcademyDestination.Profile -> Res.string.nav_profile
    },
)

@Composable
private fun DestinationGlyph(destination: AcademyDestination, selected: Boolean) {
    val icon = when (destination) {
        AcademyDestination.Home -> AcademyNavigationIcons.Home
        AcademyDestination.MyLearning -> AcademyNavigationIcons.MyLearning
        AcademyDestination.Profile -> AcademyNavigationIcons.Profile
    }
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Preview(name = "Phone navigation · Home selected", widthDp = 390, heightDp = 844)
@Composable
fun AcademyPhoneHomeNavigationPreview() {
    AcademyNavigationPreview(destination = AcademyDestination.Home, widthClassLabel = "Compact")
}

@Preview(name = "Phone navigation · My Learning selected", widthDp = 390, heightDp = 844)
@Composable
fun AcademyPhoneMyLearningNavigationPreview() {
    AcademyNavigationPreview(destination = AcademyDestination.MyLearning, widthClassLabel = "Compact")
}

@Preview(name = "Tablet rail · Profile selected", widthDp = 1000, heightDp = 720)
@Composable
fun AcademyTabletProfileNavigationPreview() {
    AcademyNavigationPreview(destination = AcademyDestination.Profile, widthClassLabel = "Expanded")
}

@Preview(name = "Medium rail · My Learning selected", widthDp = 700, heightDp = 900)
@Composable
fun AcademyMediumMyLearningNavigationPreview() {
    AcademyNavigationPreview(destination = AcademyDestination.MyLearning, widthClassLabel = "Medium")
}

@Preview(name = "Phone navigation · 130% font", widthDp = 390, heightDp = 844, fontScale = 1.3f)
@Composable
fun AcademyPhoneLargeFontNavigationPreview() {
    AcademyNavigationPreview(destination = AcademyDestination.MyLearning, widthClassLabel = "Compact")
}

@Preview(name = "Rail navigation · 130% font", widthDp = 700, heightDp = 900, fontScale = 1.3f)
@Composable
fun AcademyRailLargeFontNavigationPreview() {
    AcademyNavigationPreview(destination = AcademyDestination.MyLearning, widthClassLabel = "Medium")
}

@Composable
private fun AcademyNavigationPreview(destination: AcademyDestination, widthClassLabel: String) {
    AcademyTheme {
        AcademyAppScaffold(
            destination = destination,
            showNavigation = true,
            onDestinationSelected = {},
        ) {
            Column {
                Text("$widthClassLabel navigation preview")
                Text("${destination.name} selected")
            }
        }
    }
}
