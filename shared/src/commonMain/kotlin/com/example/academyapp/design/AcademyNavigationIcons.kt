package com.example.academyapp.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Shared 24dp vector icons for Academy navigation and curriculum controls. */
object AcademyNavigationIcons {
    val Module: ImageVector = ImageVector.Builder(
        name = "AcademyModule",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round,
            strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round,
        ) {
            moveTo(10f, 6f)
            lineTo(20f, 6f)
            moveTo(10f, 12f)
            lineTo(20f, 12f)
            moveTo(10f, 18f)
            lineTo(20f, 18f)
            moveTo(4f, 6f)
            lineTo(5f, 6f)
            moveTo(4f, 12f)
            lineTo(5f, 12f)
            moveTo(4f, 18f)
            lineTo(5f, 18f)
        }
    }.build()

    val Search: ImageVector = ImageVector.Builder(
        name = "AcademySearch",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round,
            strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round,
        ) {
            moveTo(10.5f, 10.5f)
            curveTo(12.9853f, 10.5f, 15f, 8.4853f, 15f, 6f)
            curveTo(15f, 3.5147f, 12.9853f, 1.5f, 10.5f, 1.5f)
            curveTo(8.0147f, 1.5f, 6f, 3.5147f, 6f, 6f)
            curveTo(6f, 8.4853f, 8.0147f, 10.5f, 10.5f, 10.5f)
            moveTo(14f, 9.5f)
            lineTo(20.5f, 16f)
        }
    }.build()

    val QuizPencil: ImageVector = ImageVector.Builder(
        name = "AcademyQuizPencil",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(3f, 17.25f)
            lineTo(3f, 21f)
            lineTo(6.75f, 21f)
            lineTo(17.81f, 9.94f)
            lineTo(14.06f, 6.19f)
            close()
            moveTo(20.71f, 7.04f)
            curveTo(21.1f, 6.65f, 21.1f, 6.02f, 20.71f, 5.63f)
            lineTo(18.37f, 3.29f)
            curveTo(17.98f, 2.9f, 17.35f, 2.9f, 16.96f, 3.29f)
            lineTo(15.13f, 5.12f)
            lineTo(18.88f, 8.87f)
            lineTo(20.71f, 7.04f)
            close()
        }
    }.build()

    val ChevronUp: ImageVector = ImageVector.Builder(
        name = "AcademyChevronUp",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round,
            strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round,
        ) {
            moveTo(6f, 15f)
            lineTo(12f, 9f)
            lineTo(18f, 15f)
        }
    }.build()

    val ChevronRight: ImageVector = ImageVector.Builder(
        name = "AcademyChevronRight",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round,
            strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round,
        ) {
            moveTo(9f, 6f)
            lineTo(15f, 12f)
            lineTo(9f, 18f)
        }
    }.build()

    val Home: ImageVector = ImageVector.Builder(
        name = "AcademyHome",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 3f)
            lineTo(2f, 11f)
            lineTo(5f, 11f)
            lineTo(5f, 21f)
            lineTo(10f, 21f)
            lineTo(10f, 14f)
            lineTo(14f, 14f)
            lineTo(14f, 21f)
            lineTo(19f, 21f)
            lineTo(19f, 11f)
            lineTo(22f, 11f)
            close()
        }
    }.build()

    val MyLearning: ImageVector = ImageVector.Builder(
        name = "AcademyMyLearning",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 3f)
            lineTo(1f, 8.5f)
            lineTo(12f, 14f)
            lineTo(20f, 10f)
            lineTo(20f, 16f)
            lineTo(22f, 16f)
            lineTo(22f, 9f)
            close()
        }
        path(fill = SolidColor(Color.Black)) {
            moveTo(5f, 11f)
            lineTo(5f, 17f)
            lineTo(12f, 21f)
            lineTo(19f, 17f)
            lineTo(19f, 11f)
            lineTo(12f, 14.5f)
            close()
        }
    }.build()

    val Profile: ImageVector = ImageVector.Builder(
        name = "AcademyProfile",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 12f)
            curveTo(14.21f, 12f, 16f, 10.21f, 16f, 8f)
            curveTo(16f, 5.79f, 14.21f, 4f, 12f, 4f)
            curveTo(9.79f, 4f, 8f, 5.79f, 8f, 8f)
            curveTo(8f, 10.21f, 9.79f, 12f, 12f, 12f)
            close()
            moveTo(12f, 14f)
            curveTo(7.58f, 14f, 4f, 16.24f, 4f, 19f)
            lineTo(4f, 21f)
            lineTo(20f, 21f)
            lineTo(20f, 19f)
            curveTo(20f, 16.24f, 16.42f, 14f, 12f, 14f)
            close()
        }
    }.build()
}
