package com.example.spamshield.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
import com.example.spamshield.ui.viewmodel.SpamShieldViewModel

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

    Scaffold(containerColor = DarkBackground) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(DarkBackground)
        ) {
            Text(
                text = "Message History",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp)
            )

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkSurface,
                contentColor = Color.White,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = SpamRed
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selectedTab == index) Color.White else TextSecondary
                            )
                        }
                    )
                }
            }

            if (displayedMessages.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No messages yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                }
            } else {
                LazyColumn {
                    items(displayedMessages) { message ->
                        HistoryMessageRow(message = message)
                        HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryMessageRow(message: MessageEntity) {
    val color = classificationColor(message.classification, message.confidence)
    val label = classificationLabel(message.classification, message.confidence)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkBackground)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                Text(
                    text = message.timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary.copy(alpha = 0.7f)
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                if (message.feedbackGiven) {
                    Surface(
                        color = HamGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Feedback",
                            style = MaterialTheme.typography.labelSmall,
                            color = HamGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = message.messageText,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}
