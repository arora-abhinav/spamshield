package com.example.spamshield.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.spamshield.data.local.MessageEntity
import com.example.spamshield.ui.theme.DarkBackground
import com.example.spamshield.ui.theme.DarkBorder
import com.example.spamshield.ui.theme.DarkSurface
import com.example.spamshield.ui.theme.DarkSurfaceVariant
import com.example.spamshield.ui.theme.HamGreen
import com.example.spamshield.ui.theme.SpamRed
import com.example.spamshield.ui.theme.TextSecondary
import com.example.spamshield.ui.theme.UncertainAmber
import com.example.spamshield.ui.viewmodel.SpamShieldViewModel
import kotlinx.coroutines.launch

fun classificationColor(classification: String, confidence: Double): Color = when {
    confidence >= 0.88 && classification == "spam" -> SpamRed
    confidence >= 0.88 && classification == "ham" -> HamGreen
    confidence >= 0.75 -> UncertainAmber
    else -> Color(0xFF888899)
}

fun classificationLabel(classification: String, confidence: Double): String = when {
    confidence >= 0.88 -> classification.uppercase()
    confidence >= 0.75 -> "UNCERTAIN"
    else -> classification.uppercase()
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
        containerColor = DarkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(DarkBackground)
        ) {
            Text(
                text = "SpamShield",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HomeStatCard(
                    modifier = Modifier.weight(1f),
                    label = "Total Spam",
                    value = spamMessages.size.toString(),
                    valueColor = SpamRed
                )
                HomeStatCard(
                    modifier = Modifier.weight(1f),
                    label = "Today's Spam",
                    value = todayMessages.count { it.classification == "spam" }.toString(),
                    valueColor = Color.White
                )
            }

            Text(
                text = "Recent Messages",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            LazyColumn {
                items(allMessages) { message ->
                    MessageRow(message = message, onClick = { selectedMessage = message })
                    HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                }
            }
        }

        if (selectedMessage != null) {
            ModalBottomSheet(
                onDismissRequest = { selectedMessage = null },
                sheetState = sheetState,
                containerColor = DarkSurface
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
private fun HomeStatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    valueColor: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

@Composable
private fun MessageRow(message: MessageEntity, onClick: () -> Unit) {
    val color = classificationColor(message.classification, message.confidence)
    val label = classificationLabel(message.classification, message.confidence)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(DarkBackground)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = message.sender,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = message.messageText.take(60),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = message.timestamp,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary.copy(alpha = 0.7f)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Surface(
            color = color.copy(alpha = 0.15f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "$label ${(message.confidence * 100).toInt()}%",
                color = color,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
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
    val color = classificationColor(message.classification, message.confidence)

    Column(
        modifier = Modifier
            .padding(24.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = message.sender,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = message.messageText,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = color.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "${classificationLabel(message.classification, message.confidence)} · ${(message.confidence * 100).toInt()}% confidence",
                    color = color,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        if (!message.feedbackGiven) {
            Text(
                text = "Is this classification wrong?",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { showConfirmDialog = "spam" },
                    colors = ButtonDefaults.buttonColors(containerColor = SpamRed),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Mark as Spam") }
                Button(
                    onClick = { showConfirmDialog = "ham" },
                    colors = ButtonDefaults.buttonColors(containerColor = HamGreen),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Mark as Ham") }
            }
        } else {
            Text(
                text = "Feedback given: ${message.userCorrection?.uppercase()}",
                style = MaterialTheme.typography.bodySmall,
                color = HamGreen
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }

    if (showConfirmDialog != null) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = null },
            containerColor = DarkSurface,
            title = { Text("Confirm Feedback", color = Color.White) },
            text = { Text("Mark this message as ${showConfirmDialog?.uppercase()}?", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    onFeedback(message.predictionId, showConfirmDialog!!)
                    showConfirmDialog = null
                }) { Text("Confirm", color = SpamRed) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}
