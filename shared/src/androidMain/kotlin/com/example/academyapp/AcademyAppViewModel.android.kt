package com.example.academyapp

import android.content.Context
import com.example.academyapp.api.AcademyEnvironment
import com.example.academyapp.api.AcademyApiEnvironments
import com.example.academyapp.course.AndroidQuizWebSessionController
import com.example.academyapp.session.AndroidKeystoreTokenStorage

fun androidAcademyEnvironment(
    isDebugBuild: Boolean,
    useStaging: Boolean,
): AcademyEnvironment =
    if (isDebugBuild && useStaging) AcademyEnvironment.STAGING else AcademyEnvironment.LOCAL

fun createAndroidAcademyAppComponent(
    context: Context,
    environment: AcademyEnvironment = AcademyEnvironment.LOCAL,
): AcademyAppComponent = createAcademyAppComponent(
    storage = AndroidKeystoreTokenStorage(context),
    quizWebSessionController = AndroidQuizWebSessionController(
        context,
        AcademyApiEnvironments.configurationFor(environment).requireBaseUrl(),
    ),
    environment = environment,
)
