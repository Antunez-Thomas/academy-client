package com.example.academyapp.features.account.presentation

import androidx.compose.runtime.Composable

sealed interface PhotoSelection {
    class Selected(val bytes: ByteArray, val mimeType: String) : PhotoSelection
    data object Cancelled : PhotoSelection
    data object Failed : PhotoSelection
}
/** Android Photo Picker / iOS PHPicker: selected image only, no broad library permission. */
@Composable
expect fun rememberAccountPhotoPicker(onResult: (PhotoSelection) -> Unit): () -> Unit
