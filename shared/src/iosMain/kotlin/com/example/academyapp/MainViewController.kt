package com.example.academyapp

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.viewmodel.compose.viewModel

fun MainViewController() = ComposeUIViewController {
    val component = remember { createIosAcademyAppComponent() }
    val appViewModel = viewModel { component.createSessionViewModel() }
    val courseListViewModel = viewModel { component.createCourseListViewModel() }
    App(appViewModel, courseListViewModel, component.quizWebSessionController, viewModel { component.createAccountViewModel() })
}
