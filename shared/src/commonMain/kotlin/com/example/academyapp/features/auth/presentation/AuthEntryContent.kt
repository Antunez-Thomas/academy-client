package com.example.academyapp.features.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.academyapp.AuthEntryScreen
import com.example.academyapp.api.model.AuthOptions
import com.example.academyapp.design.AcademyDimensions
import com.example.academyapp.design.AcademySpacing
import com.example.academyapp.design.components.AcademyAdaptivePane
import com.example.academyapp.design.components.AcademyScreenTransition
import com.example.academyapp.design.components.AcademyTextField
import com.example.academyapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
private fun LoginScreen(
    isSubmitting: Boolean,
    errorMessage: String?,
    onLogin: (String, String) -> Unit,
    noticeMessage: String? = null,
    onForgotPassword: () -> Unit = {},
    registrationAvailable: Boolean = false,
    onRegister: () -> Unit = {},
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.widthIn(max = AcademyDimensions.AuthFormMaxWidth).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AcademySpacing.Large),
    ) {
        if (registrationAvailable) {
            Row(Modifier.fillMaxWidth().clip(com.example.academyapp.design.AcademyShapes.Pill)
                .background(com.example.academyapp.design.AcademyColors.LightSurfaceVariant).padding(4.dp)) {
                Button(onClick = {}, modifier = Modifier.weight(1f), enabled = false, shape = com.example.academyapp.design.AcademyShapes.Pill,
                    colors = ButtonDefaults.buttonColors(disabledContainerColor = com.example.academyapp.design.AcademyColors.BrandBlue,
                        disabledContentColor = com.example.academyapp.design.AcademyColors.White)) {
                    Text(stringResource(Res.string.sign_in))
                }
                TextButton(onClick = onRegister, modifier = Modifier.weight(1f), enabled = !isSubmitting) {
                    Text(stringResource(Res.string.auth_create_account))
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(AcademySpacing.Medium)) {
            noticeMessage?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            AuthEmailField(email, { email = it }, enabled = !isSubmitting, imeAction = ImeAction.Next)
            AuthPasswordField(password, { password = it }, enabled = !isSubmitting)
            TextButton(onClick = onForgotPassword, enabled = !isSubmitting,
                modifier = Modifier.align(Alignment.End).heightIn(min = AcademyDimensions.MinimumTouchTarget)) {
                Text(stringResource(Res.string.auth_forgot_password), color = com.example.academyapp.design.AcademyColors.BrandOrangeDark)
            }
            Button(
                onClick = { onLogin(email, password) },
                modifier = Modifier.fillMaxWidth().heightIn(min = AcademyDimensions.MinimumTouchTarget),
                enabled = !isSubmitting && email.isNotBlank() && password.isNotBlank(),
                colors = authPrimaryButtonColors(),
            ) {
                if (isSubmitting) androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(AcademyDimensions.InlineIndicatorSize),
                    strokeWidth = AcademyDimensions.InlineIndicatorStrokeWidth,
                    color = MaterialTheme.colorScheme.onPrimary,
                ) else Text(stringResource(Res.string.sign_in))
            }
        }
    }
}
@Composable
private fun AuthBrandHeader(subtitle: String, showBack: Boolean, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AcademySpacing.XSmall),
    ) {
        if (showBack) {
            TextButton(onClick = onBack, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                Text(stringResource(Res.string.auth_back_to_sign_in), color = androidx.compose.ui.graphics.Color.White)
            }
        }
        Text(
            text = stringResource(Res.string.app_name),
            color = androidx.compose.ui.graphics.Color.White,
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            subtitle,
            color = androidx.compose.ui.graphics.Color.White.copy(alpha = .9f),
            style = MaterialTheme.typography.bodySmall,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun AuthEmailField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    imeAction: ImeAction,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AcademySpacing.XSmall)) {
        Text(stringResource(Res.string.email), style = MaterialTheme.typography.labelMedium,
            color = com.example.academyapp.design.AcademyColors.BrandNavy)
        AcademyTextField(value, onValueChange, null, placeholder = stringResource(Res.string.auth_email_placeholder), enabled = enabled,
            imeAction = imeAction, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
    }
}

@Composable
private fun AuthPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    imeAction: ImeAction = ImeAction.Done,
    placeholder: String? = null,
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(AcademySpacing.XSmall)) {
        Text(stringResource(Res.string.password), style = MaterialTheme.typography.labelMedium,
            color = com.example.academyapp.design.AcademyColors.BrandNavy)
        AcademyTextField(value, onValueChange, null,
            placeholder = placeholder ?: stringResource(Res.string.auth_password_placeholder), enabled = enabled,
            imeAction = imeAction, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = if (visible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val description = stringResource(if (visible) Res.string.auth_hide_password else Res.string.auth_show_password)
                TextButton(onClick = { visible = !visible },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = AcademySpacing.Small),
                    modifier = Modifier.semantics { contentDescription = description }) {
                    Text(if (visible) "◉" else "◎", style = MaterialTheme.typography.titleMedium)
                }
            })
    }
}

@Composable
private fun authTextFieldColors() = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
    focusedTextColor = com.example.academyapp.design.AcademyColors.AuthInputForeground,
    unfocusedTextColor = com.example.academyapp.design.AcademyColors.AuthInputForeground,
    disabledTextColor = com.example.academyapp.design.AcademyColors.AuthInputDisabledForeground,
    focusedContainerColor = com.example.academyapp.design.AcademyColors.AuthInputBackground,
    unfocusedContainerColor = com.example.academyapp.design.AcademyColors.AuthInputBackground,
    disabledContainerColor = com.example.academyapp.design.AcademyColors.AuthInputDisabledBackground,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = com.example.academyapp.design.AcademyColors.AuthInputOutline,
    disabledBorderColor = com.example.academyapp.design.AcademyColors.AuthInputOutline,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = com.example.academyapp.design.AcademyColors.AuthInputForegroundVariant,
    disabledLabelColor = com.example.academyapp.design.AcademyColors.AuthInputDisabledForeground,
)

@Composable
private fun authPrimaryButtonColors() = ButtonDefaults.buttonColors(
    containerColor = com.example.academyapp.design.AcademyColors.BrandOrange,
    contentColor = MaterialTheme.colorScheme.onPrimary,
    disabledContainerColor = com.example.academyapp.design.AcademyColors.BrandOrange,
    disabledContentColor = MaterialTheme.colorScheme.onPrimary,
)

/** Auth UI boundary. The app shell supplies state and callbacks from AcademyAppViewModel. */
@Composable
internal fun AuthEntryContent(
    isSigningIn: Boolean,
    screen: AuthEntryScreen,
    loginError: String?,
    authOptions: AuthOptions,
    actionError: String?,
    actionMessage: String?,
    isActionInProgress: Boolean,
    onLogin: (String, String) -> Unit,
    onScreen: (AuthEntryScreen) -> Unit,
    onReset: (String) -> Unit,
    onRegister: (String, String, Boolean, String) -> Unit,
    noticeMessage: String? = null,
) {
    AcademyAdaptivePane(
        modifier = Modifier.fillMaxSize().background(com.example.academyapp.design.AcademyColors.BrandBlue),
        paneSpacing = 0.dp,
        primary = { twoPane ->
            Box(
                modifier = (if (twoPane) Modifier.fillMaxSize() else Modifier.fillMaxWidth().height(AcademyDimensions.AccessHeaderHeight))
                    .padding(
                        start = AcademyDimensions.AccessHorizontalPadding,
                        end = AcademyDimensions.AccessHorizontalPadding,
                        top = AcademyDimensions.AccessHeaderTopPadding,
                    ),
                contentAlignment = if (twoPane) Alignment.Center else Alignment.TopStart,
            ) {
                AcademyScreenTransition(
                    targetState = screen,
                    stateOrder = ::authScreenOrder,
                ) { headerScreen ->
                    AuthBrandHeader(
                        subtitle = stringResource(if (headerScreen == AuthEntryScreen.Registration) Res.string.auth_registration_tagline else Res.string.auth_brand_tagline),
                        showBack = headerScreen == AuthEntryScreen.PasswordReset,
                        onBack = { onScreen(AuthEntryScreen.SignIn) },
                    )
                }
            }
        },
        secondary = { twoPane ->
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = if (twoPane) com.example.academyapp.design.AcademyShapes.Card else com.example.academyapp.design.AcademyShapes.Sheet,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    Modifier.fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .padding(AcademyDimensions.AccessFormPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AcademyScreenTransition(
                        targetState = screen,
                        stateOrder = ::authScreenOrder,
                        modifier = Modifier.fillMaxWidth(),
                    ) { formScreen ->
                        when (formScreen) {
                            AuthEntryScreen.SignIn -> LoginScreen(
                                isSubmitting = isSigningIn,
                                errorMessage = loginError ?: actionError,
                                onLogin = onLogin,
                                noticeMessage = noticeMessage,
                                onForgotPassword = { onScreen(AuthEntryScreen.PasswordReset) },
                                registrationAvailable = authOptions.registrationAvailable,
                                onRegister = { onScreen(AuthEntryScreen.Registration) },
                            )
                            AuthEntryScreen.PasswordReset -> PasswordResetScreen(
                                isSubmitting = isActionInProgress,
                                errorMessage = actionError,
                                successMessage = actionMessage,
                                onSubmit = onReset,
                                onBack = { onScreen(AuthEntryScreen.SignIn) },
                            )
                            AuthEntryScreen.Registration -> RegistrationScreen(
                                isSubmitting = isSigningIn || isActionInProgress,
                                errorMessage = actionError,
                                authOptions = authOptions,
                                onSubmit = onRegister,
                                onBack = { onScreen(AuthEntryScreen.SignIn) },
                            )
                        }
                    }
                }
            }
        },
    )
}

private fun authScreenOrder(screen: AuthEntryScreen): Int = when (screen) {
    AuthEntryScreen.SignIn -> 0
    AuthEntryScreen.Registration -> 1
    AuthEntryScreen.PasswordReset -> 2
}

@Composable
private fun PasswordResetScreen(
    isSubmitting: Boolean,
    errorMessage: String?,
    successMessage: String?,
    onSubmit: (String) -> Unit,
    onBack: () -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("") }
    Column(
        modifier = Modifier.widthIn(max = AcademyDimensions.AuthFormMaxWidth).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AcademySpacing.Large),
    ) {
        Text(stringResource(Res.string.auth_reset_title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(Res.string.auth_reset_message), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(verticalArrangement = Arrangement.spacedBy(AcademySpacing.Medium)) {
            errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            successMessage?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            AuthEmailField(email, { email = it }, enabled = !isSubmitting, imeAction = ImeAction.Done)
            Button(onClick = { onSubmit(email) }, enabled = !isSubmitting && email.isNotBlank(), modifier = Modifier.fillMaxWidth().heightIn(min = AcademyDimensions.MinimumTouchTarget), colors = authPrimaryButtonColors()) {
                if (isSubmitting) androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(AcademyDimensions.InlineIndicatorSize), strokeWidth = AcademyDimensions.InlineIndicatorStrokeWidth, color = MaterialTheme.colorScheme.onPrimary) else Text(stringResource(Res.string.auth_send_reset))
            }
        }
        TextButton(onClick = onBack, enabled = !isSubmitting, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text(stringResource(Res.string.auth_back_to_sign_in)) }
    }
}

@Composable
private fun RegistrationScreen(
    isSubmitting: Boolean,
    errorMessage: String?,
    authOptions: AuthOptions,
    onSubmit: (String, String, Boolean, String) -> Unit,
    onBack: () -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var displayName by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var legalAgreementAccepted by rememberSaveable { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = Modifier.widthIn(max = AcademyDimensions.AuthFormMaxWidth).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AcademySpacing.Large),
    ) {
        Row(Modifier.fillMaxWidth().clip(com.example.academyapp.design.AcademyShapes.Pill)
            .background(com.example.academyapp.design.AcademyColors.LightSurfaceVariant).padding(4.dp)) {
            TextButton(onClick = onBack, modifier = Modifier.weight(1f), enabled = !isSubmitting) {
                Text(stringResource(Res.string.sign_in))
            }
            Button(onClick = {}, modifier = Modifier.weight(1f), enabled = false,
                shape = com.example.academyapp.design.AcademyShapes.Pill,
                colors = ButtonDefaults.buttonColors(disabledContainerColor = com.example.academyapp.design.AcademyColors.BrandBlue,
                    disabledContentColor = com.example.academyapp.design.AcademyColors.White)) {
                Text(stringResource(Res.string.auth_create_account))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(AcademySpacing.Medium)) {
            errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Column(verticalArrangement = Arrangement.spacedBy(AcademySpacing.XSmall)) {
                Text(stringResource(Res.string.auth_full_name), style = MaterialTheme.typography.labelMedium,
                    color = com.example.academyapp.design.AcademyColors.BrandNavy)
                AcademyTextField(value = displayName, onValueChange = { displayName = it }, label = null,
                    placeholder = stringResource(Res.string.auth_full_name_placeholder), enabled = !isSubmitting,
                    imeAction = ImeAction.Next)
            }
            AuthEmailField(email, { email = it }, enabled = !isSubmitting, imeAction = ImeAction.Next)
            AuthPasswordField(password, { password = it }, enabled = !isSubmitting, imeAction = ImeAction.Next,
                placeholder = stringResource(Res.string.auth_password_minimum))
            Column(verticalArrangement = Arrangement.spacedBy(AcademySpacing.Small)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = legalAgreementAccepted, onCheckedChange = { legalAgreementAccepted = it }, enabled = !isSubmitting)
                    Text(
                        stringResource(
                            when {
                                authOptions.termsUrl != null && authOptions.privacyUrl != null -> Res.string.auth_agree_terms_privacy
                                authOptions.privacyUrl != null -> Res.string.auth_agree_privacy
                                authOptions.termsUrl != null -> Res.string.auth_agree_terms
                                else -> Res.string.auth_agree_legal
                            },
                        ),
                        modifier = Modifier.clickable(enabled = !isSubmitting) { legalAgreementAccepted = !legalAgreementAccepted },
                    )
                }
                authOptions.termsUrl?.let { url ->
                    TextButton(onClick = { uriHandler.openUri(url) }, enabled = !isSubmitting) { Text(stringResource(Res.string.auth_terms_link)) }
                }
                authOptions.privacyUrl?.let { url ->
                    TextButton(onClick = { uriHandler.openUri(url) }, enabled = !isSubmitting) { Text(stringResource(Res.string.auth_privacy_link)) }
                }
            }
            Button(onClick = { onSubmit(email, password, legalAgreementAccepted, displayName) }, enabled = !isSubmitting && displayName.isNotBlank() && email.isNotBlank() && password.isNotBlank() && (!authOptions.legalAgreementRequired || legalAgreementAccepted), modifier = Modifier.fillMaxWidth().heightIn(min = AcademyDimensions.MinimumTouchTarget), colors = authPrimaryButtonColors()) {
                if (isSubmitting) androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(AcademyDimensions.InlineIndicatorSize), strokeWidth = AcademyDimensions.InlineIndicatorStrokeWidth, color = MaterialTheme.colorScheme.onPrimary) else Text(stringResource(Res.string.auth_register_submit))
            }
        }
        TextButton(onClick = onBack, enabled = !isSubmitting, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text(stringResource(Res.string.auth_back_to_sign_in)) }
    }
}
