package com.example.spamshield.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.spamshield.dataclasses.StatisticsResponse
import com.example.spamshield.dataclasses.WeeklyDistributionItem
import com.example.spamshield.ui.viewmodel.SpamShieldViewModel
import com.example.spamshield.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(viewModel: SpamShieldViewModel) {
    val statisticsState by viewModel.statisticsState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadStatistics()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Statistics") }) }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (val state = statisticsState) {
                is UiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is UiState.Success -> {
                    StatisticsContent(stats = state.data)
                }
                is UiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                        Button(
                            onClick = { viewModel.loadStatistics() },
                            modifier = Modifier.padding(top = 8.dp)
                        ) { Text("Retry") }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun StatisticsContent(stats: StatisticsResponse) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatisticCard(modifier = Modifier.weight(1f), label = "Total Messages", value = stats.totalMessages?.toString() ?: "—")
                StatisticCard(modifier = Modifier.weight(1f), label = "Spam Count", value = stats.spamCount?.toString() ?: "—")
                StatisticCard(modifier = Modifier.weight(1f), label = "Ham Count", value = stats.hamCount?.toString() ?: "—")
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatisticCard(modifier = Modifier.weight(1f), label = "Today Spam", value = stats.todaySpamCount?.toString() ?: "—")
                StatisticCard(modifier = Modifier.weight(1f), label = "Week Spam", value = stats.weekSpamCount?.toString() ?: "—")
                StatisticCard(modifier = Modifier.weight(1f), label = "Month Spam", value = stats.monthSpamCount?.toString() ?: "—")
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatisticCard(
                    modifier = Modifier.weight(1f),
                    label = "Spam %",
                    value = stats.spamPercentage?.let { "%.1f%%".format(it) } ?: "—"
                )
                StatisticCard(
                    modifier = Modifier.weight(1f),
                    label = "Avg Confidence",
                    value = stats.averageConfidenceAll?.let { "${(it * 100).toInt()}%" } ?: "—"
                )
                StatisticCard(
                    modifier = Modifier.weight(1f),
                    label = "Spam Confidence",
                    value = stats.averageConfidenceSpam?.let { "${(it * 100).toInt()}%" } ?: "—"
                )
            }
        }

        val distribution = stats.weeklySpamDistribution
        if (!distribution.isNullOrEmpty()) {
            item {
                Text(
                    text = "Weekly Spam Distribution",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                WeeklyBarChart(items = distribution)
            }
        }
    }
}

@Composable
private fun StatisticCard(modifier: Modifier = Modifier, label: String, value: String) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = value, style = MaterialTheme.typography.titleLarge)
            Text(text = label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun WeeklyBarChart(items: List<WeeklyDistributionItem>) {
    val maxCount = items.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
    val barColor = Color(0xFFF44336)

    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                val barWidth = size.width / items.size
                val maxBarHeight = size.height * 0.8f

                items.forEachIndexed { index, item ->
                    val barHeight = (item.count.toFloat() / maxCount) * maxBarHeight
                    val x = index * barWidth + barWidth * 0.1f
                    val y = size.height - barHeight

                    drawRect(
                        color = barColor,
                        topLeft = Offset(x, y),
                        size = Size(barWidth * 0.8f, barHeight)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                items.forEach { item ->
                    Text(
                        text = item.date.takeLast(5),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}
