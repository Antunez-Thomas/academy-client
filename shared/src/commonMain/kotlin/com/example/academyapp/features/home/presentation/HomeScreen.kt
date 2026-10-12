package com.example.academyapp.features.home.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.academyapp.design.AcademySpacing
import com.example.academyapp.design.components.AcademyAdaptivePane
import com.example.academyapp.design.components.AcademySectionHeader
import com.example.academyapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HomeScreen(
    summary: HomeSummary,
    onViewCourses: () -> Unit,
    onCourseSelected: (Long) -> Unit,
) {
    AcademyAdaptivePane(
        primary = { twoPane ->
            Column(
                modifier = if (twoPane) Modifier.fillMaxSize().verticalScroll(rememberScrollState()) else Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AcademySpacing.Large),
            ) {
                Text(stringResource(Res.string.nav_home), style = MaterialTheme.typography.headlineLarge)
                Text(
                    stringResource(Res.string.home_welcome, summary.greetingName),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AnimatedVisibility(
                    visible = summary.heroCourse != null,
                    enter = fadeIn(tween(240)) + slideInVertically(tween(240)) { it / 12 },
                    exit = fadeOut(tween(120)) + slideOutVertically(tween(120)) { -it / 16 },
                ) {
                    summary.heroCourse?.let { course ->
                        com.example.academyapp.design.components.AcademyHeroCourseCard(
                            course,
                            onClick = { onCourseSelected(course.id) },
                        )
                    }
                }
            }
        },
        secondary = {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AcademySpacing.Large),
            ) {
                AnimatedContent(
                    targetState = summary.hasCourseContent,
                    transitionSpec = {
                        (fadeIn(tween(240)) + slideInVertically(tween(240)) { it / 12 }) togetherWith
                            (fadeOut(tween(120)) + slideOutVertically(tween(120)) { -it / 16 })
                    },
                    label = "home-course-content-transition",
                ) { hasLoadedCourses ->
                    if (hasLoadedCourses) {
                        Column(verticalArrangement = Arrangement.spacedBy(AcademySpacing.Large)) {
                            AcademySectionHeader(stringResource(Res.string.home_my_learning_title))
                            summary.courses.forEach { course ->
                                com.example.academyapp.design.components.AcademyCourseCard(
                                    course,
                                    { onCourseSelected(course.id) },
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(AcademySpacing.Large)) {
                            AcademySectionHeader(
                                stringResource(Res.string.home_my_learning_title),
                                stringResource(Res.string.home_my_learning_description),
                            )
                            Button(
                                onClick = onViewCourses,
                                modifier = Modifier.fillMaxWidth(),
                                shape = com.example.academyapp.design.AcademyShapes.Pill,
                            ) {
                                Text(stringResource(Res.string.home_view_courses))
                            }
                        }
                    }
                }
            }
        },
    )
}
