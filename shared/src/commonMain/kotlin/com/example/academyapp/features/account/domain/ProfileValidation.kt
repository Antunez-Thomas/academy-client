package com.example.academyapp.features.account.domain

import com.example.academyapp.api.model.ProfileUpdateRequest

/** Shared input rules aligned with the additive profile API contract. */
const val MAX_PHOTO_BYTES = 5 * 1024 * 1024
fun normalizeName(value: String) = value.trim().map { if (it.isWhitespace()) ' ' else it }.joinToString("").replace(Regex(" +"), " ")
fun normalizePhone(value: String) = value.trim().replace(Regex("[ ().-]"), "")
fun validateProfile(value: ProfileUpdateRequest): Map<String, String> = buildMap {
    for ((field, name) in listOf("firstName" to value.firstName, "surname" to value.surname)) {
        if (name.isBlank()) put(field, "required")
        else if (name.codePointsCount() > 100 || name.any { it == '<' || it == '>' || it.code < 32 || it.code == 127 }) put(field, "invalid_name")
    }
    if (value.phone.isBlank()) put("phone", "required")
    else if (!Regex("^\\+?[0-9]{7,15}$").matches(value.phone)) put("phone", "invalid_phone")
}
private fun String.codePointsCount(): Int = count { !it.isLowSurrogate() }
