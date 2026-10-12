package com.example.academyapp.design.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** A small directional transition shared by Academy's top-level and nested screens. */
@Composable
fun <T> AcademyScreenTransition(
    targetState: T,
    stateOrder: (T) -> Int,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedContentScope.(T) -> Unit,
) {
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = {
            val direction = if (stateOrder(targetState) >= stateOrder(initialState)) 1 else -1
            (fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it * direction / 14 }) togetherWith
                (fadeOut(tween(150)) + slideOutHorizontally(tween(150)) { -it * direction / 14 })
        },
        label = "academy-screen-transition",
        content = content,
    )
}
