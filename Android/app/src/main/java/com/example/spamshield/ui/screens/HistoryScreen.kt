package com.example.spamshield.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.spamshield.data.local.MessageEntity
import com.example.spamshield.ui.viewmodel.SpamShieldViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: SpamShieldViewModel) {
    val allMessages by viewModel.allMessages.collectAsStateWithLifecycle()
    val spamMessages by viewModel.spamMessages.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("All", "Spam", "Ham")

    val displayedMessages = when (selectedTab) {
        1 -> spamMessages
        2 -> allMessages.filter { it.classification == "ham" }
        else -> allMessages
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Message History") }) }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            if (displayedMessages.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No messages yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn {
                    items(displayedMessages) { message ->
                        HistoryMessageRow(message = message)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryMessageRow(message: MessageEntity) {
    val color = classificationColor(message.classification, message.confidence)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = message.sender, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = message.timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                color = color.copy(alpha = 0.15f),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = "${message.classification.uppercase()} ${(message.confidence * 100).toInt()}%",
                    color = color,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            if (message.feedbackGiven) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Text(
                        text = "Feedback",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }
        }
        Text(
            text = message.messageText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
