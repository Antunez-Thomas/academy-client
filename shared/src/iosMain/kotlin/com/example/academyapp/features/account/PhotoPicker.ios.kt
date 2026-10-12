@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.example.academyapp.features.account.presentation

import com.example.academyapp.features.account.domain.MAX_PHOTO_BYTES

import androidx.compose.runtime.*
import kotlinx.cinterop.*
import platform.Foundation.*
import platform.PhotosUI.*
import platform.UIKit.*
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy

@Composable
actual fun rememberAccountPhotoPicker(onResult: (PhotoSelection) -> Unit): () -> Unit {
    val latest by rememberUpdatedState(onResult)
    var active by remember { mutableStateOf(true) }
    val delegate = remember {
        object : NSObject(), PHPickerViewControllerDelegateProtocol {
            override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
                picker.dismissViewControllerAnimated(true, null)
                val result = didFinishPicking.firstOrNull() as? PHPickerResult
                if (result == null) { if (active) latest(PhotoSelection.Cancelled); return }
                val provider = result.itemProvider
                val type = when {
                    provider.hasItemConformingToTypeIdentifier("public.jpeg") -> "public.jpeg"
                    provider.hasItemConformingToTypeIdentifier("public.png") -> "public.png"
                    else -> { if (active) latest(PhotoSelection.Failed); return }
                }
                provider.loadDataRepresentationForTypeIdentifier(type) { data, error ->
                    val selection = if (error != null || data == null || data.length == 0uL || data.length > MAX_PHOTO_BYTES.toULong()) PhotoSelection.Failed
                    else {
                        val bytes = ByteArray(data.length.toInt())
                        bytes.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
                        PhotoSelection.Selected(bytes, if (type == "public.jpeg") "image/jpeg" else "image/png")
                    }
                    dispatch_async(dispatch_get_main_queue()) { if (active) latest(selection) }
                }
            }
        }
    }
    DisposableEffect(Unit) { active = true; onDispose { active = false } }
    return remember(delegate) {
        {
            val configuration = PHPickerConfiguration().apply { selectionLimit = 1; filter = PHPickerFilter.imagesFilter }
            val picker = PHPickerViewController(configuration).apply { this.delegate = delegate }
            val window = UIApplication.sharedApplication.windows.firstOrNull { (it as? UIWindow)?.isKeyWindow() == true } as? UIWindow
            var presenter = window?.rootViewController
            while (presenter?.presentedViewController != null) presenter = presenter?.presentedViewController
            if (presenter == null) latest(PhotoSelection.Failed) else presenter.presentViewController(picker, true, null)
        }
    }
}
