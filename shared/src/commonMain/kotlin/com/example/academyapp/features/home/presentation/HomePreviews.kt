package com.example.academyapp.features.home.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.academyapp.design.AcademySpacing
import com.example.academyapp.design.AcademyTheme

@Preview(widthDp = 390, heightDp = 844, name = "Inicio · compact")
@Composable
private fun AcademyHomeCompactPreview() {
    AcademyTheme {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().padding(AcademySpacing.Medium), verticalArrangement = Arrangement.spacedBy(AcademySpacing.Large)) {
                HomeScreen(homeSummary("Ana García", listOf(
                    com.example.academyapp.api.model.Course(1, "Neuroeducación y aprendizaje", "Aprende estrategias para el aula.", null, 42, listOf("Neuroeducación")),
                    com.example.academyapp.api.model.Course(2, "Desarrollo socioemocional", "", null, 0, emptyList()),
                )), {}, {})
            }
        }
    }
}

@Preview(widthDp = 914, heightDp = 411, name = "Inicio · phone landscape")
@Composable
private fun AcademyHomeLandscapePreview() {
    AcademyTheme {
        Surface(Modifier.fillMaxSize().padding(AcademySpacing.Medium), color = MaterialTheme.colorScheme.background) {
            HomeScreen(
                homeSummary("Ana García",
                    listOf(
                        com.example.academyapp.api.model.Course(1, "Neuroeducación y aprendizaje", "Aprende estrategias para el aula.", null, 42, listOf("Neuroeducación")),
                        com.example.academyapp.api.model.Course(2, "Desarrollo socioemocional", "", null, 0, emptyList()),
                    ),
                ),
                {},
                {},
            )
        }
    }
}

@Preview(widthDp = 720, heightDp = 960, fontScale = 1.3f, name = "Inicio · wider · 130% font")
@Composable
private fun AcademyHomeLargeFontPreview() {
    AcademyHomeCompactPreview()
}
