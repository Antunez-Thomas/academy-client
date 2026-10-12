package com.example.academyapp

import com.example.academyapp.api.AcademyEnvironment
import com.example.academyapp.course.IosQuizWebSessionController
import com.example.academyapp.session.IosKeychainTokenStorage

fun createIosAcademyAppComponent(
    environment: AcademyEnvironment = AcademyEnvironment.LOCAL,
): AcademyAppComponent = createAcademyAppComponent(
    storage = IosKeychainTokenStorage(),
    quizWebSessionController = IosQuizWebSessionController(),
    environment = environment,
)
