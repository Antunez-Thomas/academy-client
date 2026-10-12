package com.example.academyapp.features.account.presentation

import com.example.academyapp.features.account.domain.MAX_PHOTO_BYTES

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream

@Composable
actual fun rememberAccountPhotoPicker(onResult: (PhotoSelection) -> Unit): () -> Unit {
    val context = LocalContext.current
    val latest by rememberUpdatedState(onResult)
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) latest(PhotoSelection.Cancelled)
        else scope.launch {
            val selection = withContext(Dispatchers.IO) {
                try {
                    val mime = context.contentResolver.getType(uri)
                    if (mime !in listOf("image/jpeg", "image/png")) PhotoSelection.Failed
                    else context.contentResolver.openInputStream(uri)?.use { input ->
                        val output = ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        var count = input.read(buffer)
                        while (count != -1) {
                            ensureActive()
                            if (output.size() + count > MAX_PHOTO_BYTES) return@use PhotoSelection.Failed
                            output.write(buffer, 0, count)
                            count = input.read(buffer)
                        }
                        PhotoSelection.Selected(output.toByteArray(), mime!!)
                    } ?: PhotoSelection.Failed
                } catch (error: CancellationException) { throw error } catch (_: Exception) { PhotoSelection.Failed }
            }
            latest(selection)
        }
    }
    return remember(launcher) { { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) } }
}
