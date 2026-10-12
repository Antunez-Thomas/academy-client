package com.example.academyapp.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.academyapp.api.model.Course
import com.example.academyapp.design.AcademyColors
import com.example.academyapp.design.AcademyDimensions
import com.example.academyapp.design.AcademyShapes
import com.example.academyapp.design.AcademySpacing
import com.example.academyapp.design.AcademyTheme
import com.example.academyapp.design.AcademyNavigationIcons
import com.example.academyapp.generated.resources.Res
import com.example.academyapp.generated.resources.curriculum_next_topic
import com.example.academyapp.generated.resources.section_collapsed
import com.example.academyapp.generated.resources.section_expanded
import org.jetbrains.compose.resources.stringResource

@Composable
fun AcademyTextField(value: String, onValueChange: (String) -> Unit, label: String?,
    modifier: Modifier = Modifier, placeholder: String? = null, enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Next, keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None, trailingIcon: @Composable (() -> Unit)? = null) {
    OutlinedTextField(value, onValueChange, modifier.fillMaxWidth(), enabled = enabled,
        label = label?.let { { Text(it) } }, placeholder = { placeholder?.let { Text(it) } }, singleLine = true,
        shape = AcademyShapes.Field, keyboardOptions = keyboardOptions.copy(imeAction = imeAction),
        visualTransformation = visualTransformation, trailingIcon = trailingIcon,
        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = AcademyColors.LightBackground,
            unfocusedContainerColor = AcademyColors.LightBackground, focusedBorderColor = AcademyColors.BrandBlue,
            unfocusedBorderColor = AcademyColors.LightOutline))
}

@Composable
fun AcademyButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(onClick, modifier.heightIn(min = AcademyDimensions.ButtonMinimumHeight), enabled = enabled,
        shape = AcademyShapes.Pill,
        colors = ButtonDefaults.buttonColors(containerColor = AcademyColors.BrandOrange, contentColor = Color.White)) {
        Text(text)
    }
}

@Composable
fun AcademyCourseCard(course: Course, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(onClick, modifier.fillMaxWidth(), shape = AcademyShapes.Card,
        colors = CardDefaults.cardColors(containerColor = AcademyColors.BrandBlue, contentColor = Color.White)) {
        Column(Modifier.padding(AcademySpacing.Medium), verticalArrangement = Arrangement.spacedBy(AcademySpacing.Small)) {
            Text(course.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (course.categories.isNotEmpty()) Text(course.categories.joinToString(" · "),
                style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = .9f), maxLines = 1)
            AcademyProgressIndicator(course.progressPercent, onHero = true)
        }
    }
}

@Composable
fun AcademyHeroCourseCard(course: Course, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(onClick, modifier.fillMaxWidth(), shape = AcademyShapes.FeaturedCard,
        colors = CardDefaults.cardColors(containerColor = AcademyColors.BrandNavy, contentColor = Color.White)) {
        Column(Modifier.padding(AcademySpacing.Large), verticalArrangement = Arrangement.spacedBy(AcademySpacing.Medium)) {
            Text("Continúa donde lo dejaste", style = MaterialTheme.typography.titleLarge)
            Text(course.title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            AcademyProgressIndicator(course.progressPercent, onHero = true)
            Text("Abrir curso →", style = MaterialTheme.typography.labelLarge, color = AcademyColors.BrandOrange)
        }
    }
}

@Composable
fun AcademyLessonSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    expanded: Boolean? = null,
    showNext: Boolean = false,
    onToggle: (() -> Unit)? = null,
) {
    val toggleStateDescription = expanded?.let {
        stringResource(if (it) Res.string.section_expanded else Res.string.section_collapsed)
    }
    val interactionModifier = if (expanded != null && onToggle != null) {
        Modifier
            .toggleable(value = expanded, role = Role.Button, onValueChange = { onToggle() })
            .semantics {
                stateDescription = toggleStateDescription.orEmpty()
            }
    } else Modifier
    Row(
        modifier = modifier.fillMaxWidth().clip(AcademyShapes.ListRow)
            .background(AcademyColors.LightSurface)
            .border(
                width = 1.dp,
                color = if (expanded == true) AcademyColors.LightOutline else Color.Transparent,
                shape = AcademyShapes.ListRow,
            )
            .then(interactionModifier)
            .defaultMinSize(minHeight = AcademyDimensions.MinimumTouchTarget)
            .padding(horizontal = AcademySpacing.Small, vertical = AcademySpacing.XSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AcademySpacing.MediumSmall),
    ) {
        Box(
            modifier = Modifier.size(28.dp).clip(AcademyShapes.Field)
                .background(if (expanded == true) AcademyColors.BrandBlue else AcademyColors.BlueContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = AcademyNavigationIcons.Module,
                contentDescription = null,
                tint = if (expanded == true) AcademyColors.White else AcademyColors.BrandBlue,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = AcademyColors.BrandNavy,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (showNext) {
            Text(
                stringResource(Res.string.curriculum_next_topic),
                modifier = Modifier.clip(AcademyShapes.Pill).background(AcademyColors.OrangeContainer)
                    .padding(horizontal = AcademySpacing.Small, vertical = AcademySpacing.XSmall),
                color = AcademyColors.OnOrangeContainer,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        if (expanded != null && onToggle != null) {
            val chevronRotation by animateFloatAsState(
                targetValue = if (expanded) -90f else 0f,
                animationSpec = tween(durationMillis = 180),
                label = "module-chevron-rotation",
            )
            Icon(
                imageVector = AcademyNavigationIcons.ChevronRight,
                contentDescription = null,
                tint = AcademyColors.BrandBlue,
                modifier = Modifier.size(24.dp).graphicsLayer { rotationZ = chevronRotation },
            )
        }
    }
}

@Composable
fun AcademyLessonRow(
    title: String,
    subtitle: String,
    completed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hasVideo: Boolean = true,
) {
    Row(modifier.fillMaxWidth().clip(AcademyShapes.ListRow).background(MaterialTheme.colorScheme.surface)
        .clickable(onClick = onClick).defaultMinSize(minHeight = AcademyDimensions.MinimumTouchTarget)
        .padding(horizontal = AcademySpacing.MediumSmall, vertical = AcademySpacing.Small), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AcademySpacing.MediumSmall)) {
        Box(
            Modifier.size(32.dp).clip(CircleShape)
                .background(if (!completed && !hasVideo) AcademyColors.OrangeContainer else AcademyColors.BlueContainer),
            contentAlignment = Alignment.Center,
        ) {
            when {
                completed -> Text("✓", color = AcademyColors.BrandBlue, style = MaterialTheme.typography.labelLarge)
                hasVideo -> Text("▶", color = AcademyColors.BrandBlue, style = MaterialTheme.typography.labelLarge)
                else -> Icon(
                    imageVector = AcademyNavigationIcons.Search,
                    contentDescription = null,
                    tint = AcademyColors.BrandOrange,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AcademySpacing.XSmall)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            imageVector = AcademyNavigationIcons.ChevronRight,
            contentDescription = null,
            tint = AcademyColors.BrandBlue,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
fun AcademyTopicRow(
    title: String,
    subtitle: String,
    completed: Boolean,
    isNext: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().clip(AcademyShapes.ListRow)
            .background(AcademyColors.LightBackground)
            .clickable(onClick = onClick)
            .defaultMinSize(minHeight = AcademyDimensions.MinimumTouchTarget)
            .padding(horizontal = AcademySpacing.Small, vertical = AcademySpacing.XSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AcademySpacing.Small),
    ) {
        Box(
            modifier = Modifier.size(24.dp).clip(CircleShape)
                .background(if (isNext) AcademyColors.OrangeContainer else AcademyColors.BlueContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (completed) "✓" else "▶",
                color = if (isNext) AcademyColors.BrandOrange else AcademyColors.BrandBlue,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AcademySpacing.XSmall)) {
            Text(title, style = MaterialTheme.typography.labelMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (isNext) {
            Text(
                stringResource(Res.string.curriculum_next_topic),
                modifier = Modifier.clip(AcademyShapes.Pill).background(AcademyColors.OrangeContainer)
                    .padding(horizontal = AcademySpacing.Small, vertical = AcademySpacing.XSmall),
                color = AcademyColors.OnOrangeContainer,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
fun AcademyAvatar(initial: String, modifier: Modifier = Modifier) {
    Box(modifier.size(AcademyDimensions.AvatarSize).clip(CircleShape).background(AcademyColors.BrandOrange), contentAlignment = Alignment.Center) {
        Text(initial.take(1).uppercase(), style = MaterialTheme.typography.titleLarge, color = Color.White)
    }
}

@Preview(widthDp = 390, heightDp = 844, name = "Academy course components")
@Composable
private fun AcademyCourseComponentsPreview() {
    AcademyTheme {
        Column(Modifier.fillMaxSize().background(AcademyColors.LightBackground).padding(AcademySpacing.Medium),
            verticalArrangement = Arrangement.spacedBy(AcademySpacing.Medium)) {
            val course = Course(1, "Neuroeducación y aprendizaje", "Aprende estrategias para el aula.", null, 42, listOf("Neuroeducación"))
            AcademyHeroCourseCard(course, {})
            AcademyCourseCard(course, {})
            AcademyLessonSectionHeader("Neurofisiología", expanded = false, onToggle = {})
            AcademyLessonSectionHeader("Evaluaciones", expanded = false, showNext = true, onToggle = {})
            AcademyLessonSectionHeader("Neuromitos", expanded = true, onToggle = {})
            AcademyLessonRow("Neuromitos en educación", "Tema · Video", false, {})
            AcademyTopicRow("Neuromitos 1 – ¿Cómo nace el mito?", "Tema · Video", false, true, {})
            AcademyButton("Continuar", {})
            AcademyAvatar("Ana García")
        }
    }
}
