package com.example.academyapp.design.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.example.academyapp.design.AcademyDimensions
import com.example.academyapp.design.AcademySpacing
import com.example.academyapp.navigation.adaptiveLayoutFor

/**
 * Reflows one logical screen between a vertical stack and two stable panes.
 * Movable content preserves AndroidView/WebView identity across rotations. The center gutter keeps
 * critical controls away from the most common book-fold hinge area without requiring platform APIs.
 */
@Composable
fun AcademyAdaptivePane(
    modifier: Modifier = Modifier,
    primaryPaneMaxWidth: Dp = AcademyDimensions.SidePaneMaxWidth,
    paneSpacing: Dp = AcademyDimensions.FoldSafePaneGutter,
    primary: @Composable (twoPane: Boolean) -> Unit,
    secondary: @Composable (twoPane: Boolean) -> Unit,
) {
    val currentPrimary by rememberUpdatedState(primary)
    val currentSecondary by rememberUpdatedState(secondary)
    val movablePrimary = remember { movableContentOf<Boolean> { twoPane -> currentPrimary(twoPane) } }
    val movableSecondary = remember { movableContentOf<Boolean> { twoPane -> currentSecondary(twoPane) } }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val layout = adaptiveLayoutFor(maxWidth.value, maxHeight.value)
        if (layout.supportsListDetail) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(paneSpacing),
            ) {
                Box(Modifier.weight(0.38f).widthIn(max = primaryPaneMaxWidth)) {
                    movablePrimary(true)
                }
                Box(Modifier.weight(0.62f)) {
                    movableSecondary(true)
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(AcademySpacing.Medium),
            ) {
                movablePrimary(false)
                Box(Modifier.weight(1f)) {
                    movableSecondary(false)
                }
            }
        }
    }
}
