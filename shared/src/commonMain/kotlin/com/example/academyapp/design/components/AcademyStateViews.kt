package com.example.academyapp.design.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.example.academyapp.design.AcademyColors
import com.example.academyapp.design.AcademyTheme
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.LiveRegionMode
import com.example.academyapp.design.AcademySpacing
import com.example.academyapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun AcademyStateContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
fun AcademyLoadingState(message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AcademySpacing.Medium),
    ) {
        CircularProgressIndicator()
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun AcademyEmptyState(
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = Arrangement.spacedBy(AcademySpacing.Small),
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (actionLabel != null && onAction != null) {
            OutlinedButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun AcademyErrorState(
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(AcademySpacing.Large),
            verticalArrangement = Arrangement.spacedBy(AcademySpacing.Medium),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(message, color = MaterialTheme.colorScheme.error)
            Button(onClick = onAction) { Text(actionLabel ?: stringResource(Res.string.try_again)) }
        }
    }
}

@Composable
fun AcademySectionHeader(
    title: String,
    description: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(AcademySpacing.XSmall)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        description?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun AcademyProgressIndicator(
    percent: Int,
    modifier: Modifier = Modifier,
    onHero: Boolean = false,
) {
    val normalized = normalizeProgress(percent)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(AcademySpacing.XSmall)) {
        androidx.compose.material3.LinearProgressIndicator(
            progress = { normalized / 100f },
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).semantics { liveRegion = LiveRegionMode.Polite },
            color = AcademyColors.BrandOrange,
            trackColor = if (onHero) Color.White.copy(alpha = .25f) else AcademyColors.ProgressTrack,
            drawStopIndicator = {},
        )
        val label = when (normalized) {
            0 -> stringResource(Res.string.progress_not_started, normalized)
            100 -> stringResource(Res.string.progress_complete, normalized)
            else -> stringResource(Res.string.progress_in_progress, normalized)
        }
        Text(label, style = MaterialTheme.typography.labelLarge,
            color = if (onHero) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

internal fun normalizeProgress(percent: Int): Int = percent.coerceIn(0, 100)

@Preview(widthDp = 320, heightDp = 180, name = "Centered loading state")
@Composable
private fun AcademyLoadingStatePreview() {
    AcademyTheme {
        Surface {
            AcademyStateContainer {
                AcademyLoadingState("Cargando el contenido del curso…")
            }
        }
    }
}
