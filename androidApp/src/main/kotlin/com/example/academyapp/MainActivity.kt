package com.example.academyapp

import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val isDebugBuild = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        val environment = androidAcademyEnvironment(
            isDebugBuild = isDebugBuild,
            useStaging = intent.getBooleanExtra(EXTRA_USE_STAGING, false),
        )
        if (isDebugBuild) {
            Log.i(DEBUG_LOG_TAG, "API environment=$environment")
        }

        setContent {
            val component = remember(environment) {
                createAndroidAcademyAppComponent(
                    applicationContext,
                    environment,
                )
            }
            val appViewModel = viewModel { component.createSessionViewModel() }
            val courseListViewModel = viewModel { component.createCourseListViewModel() }
            App(appViewModel, courseListViewModel, component.quizWebSessionController, viewModel { component.createAccountViewModel() })
        }
    }
}

private const val EXTRA_USE_STAGING = "com.example.academyapp.USE_STAGING_API"
private const val DEBUG_LOG_TAG = "AcademyStartup"

@Preview
@Composable
fun AppAndroidPreview() {
    AcademyAppPreview()
}
