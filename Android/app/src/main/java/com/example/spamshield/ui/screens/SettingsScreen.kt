package com.example.spamshield.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.spamshield.token.TokenManager
import com.example.spamshield.ui.theme.DarkBackground
import com.example.spamshield.ui.theme.DarkSurface
import com.example.spamshield.ui.theme.HamGreen
import com.example.spamshield.ui.theme.SpamRed
import com.example.spamshield.ui.theme.TextSecondary
import com.example.spamshield.ui.viewmodel.SpamShieldViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(viewModel: SpamShieldViewModel) {
    val context = LocalContext.current
    val optedIn by viewModel.optedIn.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadOptedInState(context)
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            scope.launch { snackbarHostState.showSnackbar(it) }
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(DarkBackground)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 4.dp)
            )

            // Device ID
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "Device ID", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Text(
                        text = TokenManager.getDeviceId(context) ?: "Not registered",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Consent toggle
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "Share spam messages for training",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Only confirmed spam messages will be sent to improve the model.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = optedIn,
                        onCheckedChange = { viewModel.setConsent(it) },
                        enabled = !isLoading,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = HamGreen,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = Color(0xFF2A2A42)
                        )
                    )
                }
            }

            // Delete button
            Button(
                onClick = { showDeleteDialog = true },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = SpamRed),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(
                    text = "Delete All Stored Spam",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Privacy notice
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Privacy Notice",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = "Message text is stored locally only. The server only receives classification metadata unless you opt in to sharing spam messages for model improvement.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = DarkSurface,
            title = { Text("Delete Stored Spam?", color = Color.White) },
            text = { Text("This will delete all spam messages stored on the server. This cannot be undone.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteStoredSpam()
                    showDeleteDialog = false
                }) { Text("Delete", color = SpamRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}
