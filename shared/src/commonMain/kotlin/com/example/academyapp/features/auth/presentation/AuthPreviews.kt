package com.example.academyapp.features.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.academyapp.AuthEntryScreen
import com.example.academyapp.PlatformStatusBarAppearance
import com.example.academyapp.api.model.AuthOptions
import com.example.academyapp.design.AcademySpacing
import com.example.academyapp.design.AcademyTheme

@Preview(widthDp = 390, heightDp = 844, name = "Sign in · compact")
@Composable
private fun AcademySignInCompactPreview() {
    AcademyTheme(authEntry = true) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize().padding(AcademySpacing.Medium), contentAlignment = Alignment.Center) {
                AuthEntryPreviewContent(AuthEntryScreen.SignIn)
            }
        }
    }
}
@Preview(widthDp = 960, heightDp = 1000, name = "Sign in · expanded")
@Composable
private fun AcademySignInExpandedPreview() {
    AcademyTheme(authEntry = true) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize().padding(AcademySpacing.XLarge), contentAlignment = Alignment.Center) {
                AuthEntryPreviewContent(AuthEntryScreen.SignIn)
            }
        }
    }
}

@Preview(widthDp = 914, heightDp = 411, name = "Sign in · phone landscape")
@Composable
private fun AcademySignInLandscapePreview() {
    AcademyTheme(authEntry = true) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            AuthEntryPreviewContent(AuthEntryScreen.SignIn)
        }
    }
}

@Preview(widthDp = 390, heightDp = 844, name = "Sign in · loading indicator")
@Composable
private fun AcademySignInLoadingPreview() {
    AcademyTheme(authEntry = true) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            AuthEntryPreviewContent(AuthEntryScreen.SignIn, isSigningIn = true)
        }
    }
}

@Preview(widthDp = 390, heightDp = 844, name = "Password reset · compact")
@Composable
private fun AcademyPasswordResetPreview() {
    AcademyTheme(authEntry = true) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize().padding(AcademySpacing.Medium), contentAlignment = Alignment.Center) {
                AuthEntryPreviewContent(AuthEntryScreen.PasswordReset)
            }
        }
    }
}

@Preview(widthDp = 390, heightDp = 844, name = "Registration · compact")
@Composable
private fun AcademyRegistrationPreview() {
    AcademyTheme(authEntry = true) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize().padding(AcademySpacing.Medium).verticalScroll(rememberScrollState()), contentAlignment = Alignment.Center) {
                AuthEntryPreviewContent(AuthEntryScreen.Registration)
            }
        }
    }
}

@Composable
private fun AuthEntryPreviewContent(
    screen: AuthEntryScreen,
    isSigningIn: Boolean = false,
    authOptions: AuthOptions = AuthOptions(true, true, "https://example.test/terms", "https://example.test/privacy"),
) {
    AuthEntryContent(
        isSigningIn = isSigningIn,
        screen = screen,
        loginError = null,
        authOptions = authOptions,
        actionError = null,
        actionMessage = null,
        isActionInProgress = false,
        onLogin = { _, _ -> },
        onScreen = {},
        onReset = {},
        onRegister = { _, _, _, _ -> },
    )
}

@Preview(widthDp = 360, heightDp = 360, name = "Sign in in a short viewport")
@Composable
private fun AcademySignInShortViewportPreview() {
    AcademyTheme(authEntry = true) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            PlatformStatusBarAppearance(lightIcons = true)
            BoxWithConstraints(
                Modifier.fillMaxSize()
                    .background(com.example.academyapp.design.AcademyColors.BrandBlue)
                    .statusBarsPadding()
                    .imePadding()
                    .padding(horizontal = 8.dp),
            ) {
                AuthEntryPreviewContent(AuthEntryScreen.SignIn, authOptions = AuthOptions(false))
            }
        }
    }
}
