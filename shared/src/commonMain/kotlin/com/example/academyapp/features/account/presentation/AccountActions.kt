package com.example.academyapp.features.account.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.academyapp.DeletionRequestState
import com.example.academyapp.design.AcademySpacing
import com.example.academyapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/** Existing deletion/signout callbacks remain owned by the session composition root. */
@Composable
internal fun AccountActions(isLoggingOut: Boolean, logoutError: String?, onLogout: () -> Unit,
    deletionState: DeletionRequestState, onRequestDeletion: () -> Unit) {
    var showDeletionConfirmation by remember { mutableStateOf(false) }
    if (showDeletionConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeletionConfirmation = false },
            title = { Text(stringResource(Res.string.deletion_title)) },
            text = { Text(stringResource(Res.string.deletion_consequences), modifier = Modifier.verticalScroll(rememberScrollState())) },
            confirmButton = {
                TextButton(onClick = { showDeletionConfirmation = false; onRequestDeletion() }) {
                    Text(stringResource(Res.string.deletion_confirm_request))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeletionConfirmation = false }) { Text(stringResource(Res.string.deletion_cancel)) }
            },
        )
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AcademySpacing.Large)) {
        Text(stringResource(Res.string.deletion_overview), style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(
            onClick = { showDeletionConfirmation = true },
            enabled = !isLoggingOut && deletionState != DeletionRequestState.Sending,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            shape = com.example.academyapp.design.AcademyShapes.Pill,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Text(stringResource(if (deletionState == DeletionRequestState.Sending) Res.string.deletion_sending else Res.string.deletion_request))
        }
        if (deletionState == DeletionRequestState.AwaitingConfirmation) Text(stringResource(Res.string.deletion_email_sent))
        if (deletionState == DeletionRequestState.Failed) Text(stringResource(Res.string.deletion_failed), color = MaterialTheme.colorScheme.error)
        if (logoutError != null) {
            Text(logoutError, color = MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = onLogout,
            enabled = !isLoggingOut,
            modifier = Modifier.fillMaxWidth(),
            shape = com.example.academyapp.design.AcademyShapes.Pill,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        ) {
            Text(stringResource(if (isLoggingOut) Res.string.signing_out else Res.string.sign_out))
        }
    }
}
