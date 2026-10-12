package com.example.academyapp.api

enum class AcademyEnvironment {
    LOCAL,
    STAGING,
    PRODUCTION,
}

class AcademyApiConfiguration internal constructor(
    val environment: AcademyEnvironment,
    val baseUrl: String?,
    val isEnabled: Boolean,
) {
    internal fun requireBaseUrl(): String {
        if (!isEnabled || baseUrl == null) {
            throw AcademyEnvironmentDisabledException(environment)
        }
        return baseUrl
    }
}

class AcademyEnvironmentDisabledException(
    val environment: AcademyEnvironment,
) : IllegalStateException("$environment API environment is disabled.")

object AcademyApiEnvironments {
    private const val STAGING_BASE_URL =
        "https://api.example.com/wp-json/academy-mobile/v1/"

    fun configurationFor(environment: AcademyEnvironment): AcademyApiConfiguration = when (environment) {
        AcademyEnvironment.LOCAL -> enabled(environment, platformLocalApiBaseUrl())
        AcademyEnvironment.STAGING -> enabled(environment, STAGING_BASE_URL)
        AcademyEnvironment.PRODUCTION -> AcademyApiConfiguration(
            environment = environment,
            baseUrl = null,
            isEnabled = false,
        )
    }

    private fun enabled(environment: AcademyEnvironment, baseUrl: String): AcademyApiConfiguration {
        require(baseUrl.startsWith("http://") || baseUrl.startsWith("https://")) {
            "Academy API base URL must use HTTP or HTTPS."
        }
        return AcademyApiConfiguration(
            environment = environment,
            baseUrl = baseUrl.trimEnd('/') + '/',
            isEnabled = true,
        )
    }
}

internal expect fun platformLocalApiBaseUrl(): String
