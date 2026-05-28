package com.example.spamshield.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.spamshield.data.local.MessageEntity
import com.example.spamshield.ui.viewmodel.SpamShieldViewModel
import kotlinx.coroutines.launch

fun classificationColor(classification: String, confidence: Double): Color {
    return when {
        confidence >= 0.88 && classification == "spam" -> Color(0xFFF44336)
        confidence >= 0.88 && classification == "ham" -> Color(0xFF4CAF50)
        confidence >= 0.75 -> Color(0xFFFF9800)
        else -> Color.Gray
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: SpamShieldViewModel) {
    val allMessages by viewModel.allMessages.collectAsStateWithLifecycle()
    val spamMessages by viewModel.spamMessages.collectAsStateWithLifecycle()
    val todayMessages by viewModel.todayMessages.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* permissions result — app degrades gracefully if denied */ }

    LaunchedEffect(Unit) {
        viewModel.registerIfNeeded()
        smsPermissionLauncher.launch(
            arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
        )
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            scope.launch { snackbarHostState.showSnackbar(it) }
            viewModel.clearError()
        }
    }

    var selectedMessage by remember { mutableStateOf<MessageEntity?>(null) }
    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("SpamShield") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "Total Spam",
                    value = spamMessages.size.toString()
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "Today's Spam",
                    value = todayMessages.count { it.classification == "spam" }.toString()
                )
            }

            Text(
                text = "Recent Messages",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyColumn {
                items(allMessages) { message ->
                    MessageRow(
                        message = message,
                        onClick = { selectedMessage = message }
                    )
                    HorizontalDivider()
                }
            }
        }

        if (selectedMessage != null) {
            ModalBottomSheet(
                onDismissRequest = { selectedMessage = null },
                sheetState = sheetState
            ) {
                MessageDetailSheet(
                    message = selectedMessage!!,
                    onFeedback = { predictionId, actual ->
                        viewModel.submitFeedback(predictionId, actual, selectedMessage!!.messageText)
                        selectedMessage = null
                    },
                    onDismiss = { selectedMessage = null }
                )
            }
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier = Modifier, label: String, value: String) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = value, style = MaterialTheme.typography.headlineMedium)
            Text(text = label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun MessageRow(message: MessageEntity, onClick: () -> Unit) {
    val color = classificationColor(message.classification, message.confidence)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = message.sender, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = message.messageText.take(60),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = message.timestamp,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Surface(
            color = color.copy(alpha = 0.15f),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text(
                text = "${message.classification.uppercase()} ${(message.confidence * 100).toInt()}%",
                color = color,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun MessageDetailSheet(
    message: MessageEntity,
    onFeedback: (Int, String) -> Unit,
    onDismiss: () -> Unit
) {
    var showConfirmDialog by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.padding(24.dp).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "From: ${message.sender}", style = MaterialTheme.typography.titleMedium)
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = message.messageText,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Text(
            text = "Classified as: ${message.classification.uppercase()} (${(message.confidence * 100).toInt()}% confidence)",
            style = MaterialTheme.typography.bodySmall
        )

        if (!message.feedbackGiven) {
            Text(text = "Is this classification wrong?", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showConfirmDialog = "spam" }) { Text("Mark as Spam") }
                Button(onClick = { showConfirmDialog = "ham" }) { Text("Mark as Ham") }
            }
        } else {
            Text(
                text = "Feedback given: ${message.userCorrection?.uppercase()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

    if (showConfirmDialog != null) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = null },
            title = { Text("Confirm Feedback") },
            text = { Text("Mark this message as ${showConfirmDialog?.uppercase()}?") },
            confirmButton = {
                TextButton(onClick = {
                    onFeedback(message.predictionId, showConfirmDialog!!)
                    showConfirmDialog = null
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = null }) { Text("Cancel") }
            }
        )
    }
}
