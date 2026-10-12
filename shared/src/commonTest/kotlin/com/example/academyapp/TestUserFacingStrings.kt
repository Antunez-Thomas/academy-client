package com.example.academyapp

import com.example.academyapp.generated.resources.error_forbidden
import com.example.academyapp.generated.resources.error_server_unavailable
import com.example.academyapp.generated.resources.error_invalid_response
import com.example.academyapp.generated.resources.Res
import com.example.academyapp.generated.resources.error_close_quiz_session
import com.example.academyapp.generated.resources.error_mark_lesson_complete
import com.example.academyapp.generated.resources.error_open_quiz
import com.example.academyapp.generated.resources.error_refresh_course_progress
import com.example.academyapp.generated.resources.error_unexpected_failure
import com.example.academyapp.generated.resources.error_video_authorization
import com.example.academyapp.generated.resources.error_no_connection
import com.example.academyapp.generated.resources.error_unauthorized
import com.example.academyapp.generated.resources.auth_action_failed
import com.example.academyapp.generated.resources.auth_accept_legal_agreement
import com.example.academyapp.generated.resources.auth_check_email
import com.example.academyapp.generated.resources.auth_invalid_email
import com.example.academyapp.generated.resources.auth_invalid_name
import com.example.academyapp.generated.resources.auth_invalid_password
import com.example.academyapp.generated.resources.auth_password_mismatch
import com.example.academyapp.generated.resources.auth_registration_unavailable
import org.jetbrains.compose.resources.StringResource

internal val testUserFacingStrings = UserFacingStringResolver { resource: StringResource ->
    when (resource) {
        Res.string.error_close_quiz_session ->
            "No pudimos cerrar la sesión de la evaluación. Revisa tu conexión e intenta cerrar sesión nuevamente."
        Res.string.error_mark_lesson_complete ->
            "No pudimos marcar esta lección como completada. Inténtalo de nuevo."
        Res.string.error_open_quiz -> "No pudimos abrir esta evaluación. Inténtalo de nuevo."
        Res.string.error_refresh_course_progress ->
            "Se guardó la finalización, pero no pudimos actualizar el progreso del curso. Inténtalo de nuevo."
        Res.string.error_unexpected_failure -> "Academy no pudo completar la solicitud. Inténtalo de nuevo."
        Res.string.error_video_authorization -> "No pudimos autorizar este video. Inténtalo de nuevo."
        Res.string.error_no_connection ->
            "No se pudo conectar con Academy. Revisa tu conexión e inténtalo de nuevo."
        Res.string.error_server_unavailable -> "Academy no está disponible temporalmente. Inténtalo de nuevo."
        Res.string.error_invalid_response -> "Academy envió una respuesta inesperada. Inténtalo de nuevo."
        Res.string.error_forbidden -> "Tu cuenta no tiene acceso a este contenido."
        Res.string.error_unauthorized -> "El correo electrónico o la contraseña son incorrectos."
        Res.string.auth_action_failed -> "No pudimos completar la solicitud. Revisa tus datos e inténtalo de nuevo."
        Res.string.auth_accept_legal_agreement -> "Debes aceptar los términos y la política de privacidad para crear una cuenta."
        Res.string.auth_check_email -> "Si existe una cuenta asociada, recibirás instrucciones para restablecer tu contraseña."
        Res.string.auth_invalid_email -> "Ingresa un correo electrónico válido."
        Res.string.auth_invalid_name -> "Ingresa tu nombre completo."
        Res.string.auth_invalid_password -> "La contraseña debe tener al menos 8 caracteres."
        Res.string.auth_password_mismatch -> "Las contraseñas no coinciden."
        Res.string.auth_registration_unavailable -> "La creación de cuentas no está disponible por el momento."
        else -> error("Missing test translation for $resource")
    }
}
