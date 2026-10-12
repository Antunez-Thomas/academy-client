package com.example.academyapp

import platform.UIKit.UIDevice
import androidx.compose.runtime.Composable

class IOSPlatform : Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) = Unit

@Composable
actual fun PlatformStatusBarAppearance(lightIcons: Boolean) = Unit
