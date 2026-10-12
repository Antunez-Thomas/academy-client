@file:OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)
package com.example.academyapp.features.account.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.academyapp.DeletionRequestState
import com.example.academyapp.PlatformBackHandler
import com.example.academyapp.design.*
import com.example.academyapp.design.components.*
import com.example.academyapp.generated.resources.*
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.stringResource

@Composable
fun AccountRoute(accountId: Long, viewModel: AccountViewModel, isLoggingOut: Boolean, logoutError: String?,
    onLogout: () -> Unit, deletionState: DeletionRequestState, onRequestDeletion: () -> Unit, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(accountId) { viewModel.open(accountId) }
    val picker = rememberAccountPhotoPicker(viewModel::selected)
    PlatformBackHandler(state.dirty || state.busy) { viewModel.requestLeave(onBack) }
    AccountScreen(state, isLoggingOut, logoutError, onLogout, deletionState, onRequestDeletion,
        viewModel::edit, viewModel::save, viewModel::reload, picker, viewModel::removePhoto, viewModel::discard)
    if (state.confirmDiscard) AlertDialog(
        onDismissRequest = viewModel::cancelLeave,
        title = { Text(stringResource(Res.string.account_unsaved_title)) },
        text = { Text(stringResource(Res.string.account_unsaved_body)) },
        confirmButton = { TextButton(onClick = viewModel::confirmLeave) { Text(stringResource(Res.string.account_discard)) } },
        dismissButton = { TextButton(onClick = viewModel::cancelLeave) { Text(stringResource(Res.string.account_keep_editing)) } },
    )
}

@Composable
fun AccountScreen(state: AccountUiState, isLoggingOut: Boolean, logoutError: String?, onLogout: () -> Unit,
    deletionState: DeletionRequestState = DeletionRequestState.Idle, onRequestDeletion: () -> Unit = {},
    onEdit: (String, String) -> Unit = { _, _ -> }, onSave: () -> Unit = {}, onReload: () -> Unit = {},
    onPhoto: () -> Unit = {}, onRemovePhoto: () -> Unit = {}, onDiscard: () -> Unit = {}) {
    val enabled = !state.busy && !isLoggingOut
    AcademyAdaptivePane(
        primary = { twoPane ->
            Column(if (twoPane) Modifier.verticalScroll(rememberScrollState()) else Modifier,
                verticalArrangement = Arrangement.spacedBy(AcademySpacing.Medium)) {
                Text(stringResource(Res.string.nav_profile), style = MaterialTheme.typography.headlineLarge)
                state.user?.let { user ->
                    val profileName = listOf(user.firstName, user.surname).filter { it.isNotBlank() }.joinToString(" ").ifBlank { user.displayName }
                    Card(Modifier.fillMaxWidth(), shape = AcademyShapes.Card,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(Modifier.padding(AcademySpacing.Medium), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AcademySpacing.Medium)) {
                            val bitmap = remember(state.avatarBytes) { state.avatarBytes?.let { runCatching { it.decodeToImageBitmap() }.getOrNull() } }
                            if (bitmap != null) Image(bitmap, stringResource(Res.string.account_photo),
                                Modifier.size(AcademyDimensions.AvatarSize).clip(CircleShape), contentScale = ContentScale.Crop)
                            else Box(Modifier.size(AcademyDimensions.AvatarSize).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                                Text(profileName.take(1).uppercase(), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleLarge)
                            }
                            Column(Modifier.weight(1f)) {
                                Text(profileName, style = MaterialTheme.typography.titleMedium)
                                Text(user.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        secondary = {
            Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(AcademySpacing.Medium)) {
                if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (state.failed) {
                    Text(stringResource(Res.string.account_failed), color = MaterialTheme.colorScheme.error)
                    if (state.user == null) AcademyButton(stringResource(Res.string.account_retry), onReload, enabled = enabled)
                }
                state.user?.let { user ->
                    OutlinedButton(onPhoto, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = AcademyShapes.Pill) {
                        Text(stringResource(if (user.avatarUrl == null) Res.string.account_choose_photo else Res.string.account_replace_photo))
                    }
                    if (user.avatarUrl != null) OutlinedButton(onRemovePhoto, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = AcademyShapes.Pill) { Text(stringResource(Res.string.account_remove_photo)) }
                    Text(stringResource(Res.string.account_photo_hint), style = MaterialTheme.typography.bodyMedium)
                    if (state.photoError) Text(stringResource(Res.string.account_photo_failed), color = MaterialTheme.colorScheme.error)
                    Card(Modifier.fillMaxWidth(), shape = AcademyShapes.Card, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.padding(AcademySpacing.Medium), verticalArrangement = Arrangement.spacedBy(AcademySpacing.Medium)) {
                            AccountField(state.firstName, stringResource(Res.string.account_first_name), enabled, state.fieldErrors["firstName"]) { onEdit("firstName", it) }
                            AccountField(state.surname, stringResource(Res.string.account_surname), enabled, state.fieldErrors["surname"]) { onEdit("surname", it) }
                            AccountField(state.phone, stringResource(Res.string.account_phone), enabled, state.fieldErrors["phone"], KeyboardType.Phone) { onEdit("phone", it) }
                            OutlinedTextField(user.username, {}, readOnly = true, label = { Text(stringResource(Res.string.account_username)) }, modifier = Modifier.fillMaxWidth(), shape = AcademyShapes.Field)
                            OutlinedTextField(user.email, {}, readOnly = true, label = { Text(stringResource(Res.string.email)) }, modifier = Modifier.fillMaxWidth(), shape = AcademyShapes.Field)
                            Text(stringResource(Res.string.account_read_only), style = MaterialTheme.typography.bodyMedium)
                            AcademyButton(stringResource(Res.string.account_save), onSave, Modifier.fillMaxWidth(), enabled && state.dirty)
                            if (state.dirty) {
                                Text(stringResource(Res.string.account_unsaved_body), style = MaterialTheme.typography.bodyMedium)
                                TextButton(onDiscard, enabled = enabled, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(Res.string.account_discard)) }
                            }
                            if (state.saved) Text(stringResource(Res.string.account_saved))
                        }
                    }
                }
                AccountActions(isLoggingOut || state.busy, logoutError, onLogout, deletionState, onRequestDeletion)
            }
        },
    )
}

@Composable
private fun AccountField(value: String, label: String, enabled: Boolean, error: String?, keyboard: KeyboardType = KeyboardType.Text, onEdit: (String) -> Unit) {
    OutlinedTextField(value, onEdit, enabled = enabled, label = { Text(label) }, modifier = Modifier.fillMaxWidth(),
        shape = AcademyShapes.Field, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = keyboard), isError = error != null,
        supportingText = if (error == null) null else ({ Text(stringResource(when (error) {
            "required" -> Res.string.account_required
            "invalid_phone" -> Res.string.account_invalid_phone
            else -> Res.string.account_invalid_name
        })) }),
    )
}

@androidx.compose.ui.tooling.preview.Preview(widthDp = 390, heightDp = 844, name = "Cuenta · compacta")
@Composable
fun AccountCompactPreview() = AccountPreview()

@androidx.compose.ui.tooling.preview.Preview(widthDp = 720, heightDp = 900, name = "Cuenta · mediana")
@Composable
fun AccountMediumPreview() = AccountPreview()

@androidx.compose.ui.tooling.preview.Preview(widthDp = 1024, heightDp = 800, name = "Cuenta · expandida")
@Composable
fun AccountExpandedPreview() = AccountPreview()

@Composable
private fun AccountPreview() {
    AcademyTheme {
        Surface(Modifier.fillMaxSize().padding(AcademySpacing.Large), color = MaterialTheme.colorScheme.background) {
            AccountScreen(AccountUiState(
                user = com.example.academyapp.api.model.User(1, "learner@example.test", "Ana García", "ana", "Ana", "García", "+541155551234"),
                firstName = "Ana", surname = "García", phone = "+541155551234"), false, null, {})
        }
    }
}
