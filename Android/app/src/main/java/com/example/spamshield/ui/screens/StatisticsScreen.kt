package com.example.spamshield.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.spamshield.dataclasses.StatisticsResponse
import com.example.spamshield.dataclasses.WeeklyDistributionItem
import com.example.spamshield.ui.theme.DarkBackground
import com.example.spamshield.ui.theme.DarkBorder
import com.example.spamshield.ui.theme.DarkSurface
import com.example.spamshield.ui.theme.DarkSurfaceVariant
import com.example.spamshield.ui.theme.SpamRed
import com.example.spamshield.ui.theme.TextSecondary
import com.example.spamshield.ui.viewmodel.SpamShieldViewModel
import com.example.spamshield.ui.viewmodel.UiState

@Composable
fun StatisticsScreen(viewModel: SpamShieldViewModel) {
    val statisticsState by viewModel.statisticsState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadStatistics()
    }

    Scaffold(containerColor = DarkBackground) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(DarkBackground)
        ) {
            Text(
                text = "Statistics",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp)
            )

            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = statisticsState) {
                    is UiState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = SpamRed
                        )
                    }
                    is UiState.Success -> {
                        StatisticsContent(stats = state.data)
                    }
                    is UiState.Error -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center).padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = state.message, color = SpamRed, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.loadStatistics() },
                                colors = ButtonDefaults.buttonColors(containerColor = SpamRed),
                                shape = RoundedCornerShape(10.dp)
                            ) { Text("Retry") }
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun StatisticsContent(stats: StatisticsResponse) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(DarkBackground),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, end = 16.dp, bottom = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                SpamRateArc(spamRate = (stats.spamPercentage ?: 0.0).toFloat())
            }
        }

        item {
            Text(
                text = "All time",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatsCard(modifier = Modifier.weight(1f), label = "Total", value = stats.totalMessages?.toString() ?: "—")
                StatsCard(modifier = Modifier.weight(1f), label = "Today's Spam", value = stats.todaySpamCount?.toString() ?: "—")
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatsCard(modifier = Modifier.weight(1f), label = "This Week", value = stats.weekSpamCount?.toString() ?: "—")
                StatsCard(
                    modifier = Modifier.weight(1f),
                    label = "Avg. Confidence",
                    value = stats.averageConfidenceAll?.let { "${(it * 100).toInt()}%" } ?: "—"
                )
            }
        }

        val distribution = stats.weeklySpamDistribution
        if (!distribution.isNullOrEmpty()) {
            item {
                Text(
                    text = "Spam Detected (This Week)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
                WeeklyBarChart(items = distribution)
            }
        }
    }
}

@Composable
private fun SpamRateArc(spamRate: Float) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(200.dp)
    ) {
        Canvas(modifier = Modifier.size(200.dp)) {
            val strokeWidth = 18.dp.toPx()
            val inset = strokeWidth / 2
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(inset, inset)
            val startAngle = -220f
            val totalSweep = 260f

            drawArc(
                color = DarkSurfaceVariant,
                startAngle = startAngle,
                sweepAngle = totalSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(strokeWidth, cap = StrokeCap.Round)
            )

            val clampedRate = spamRate.coerceIn(0f, 100f)
            if (clampedRate > 0f) {
                drawArc(
                    color = SpamRed,
                    startAngle = startAngle,
                    sweepAngle = totalSweep * (clampedRate / 100f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "%.1f%%".format(spamRate),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = SpamRed
            )
            Text(
                text = "Spam Rate",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun StatsCard(modifier: Modifier = Modifier, label: String, value: String) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}

@Composable
private fun WeeklyBarChart(items: List<WeeklyDistributionItem>) {
    val maxCount = items.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                val barWidth = size.width / items.size
                val maxBarHeight = size.height * 0.85f

                items.forEachIndexed { index, item ->
                    val barHeight = (item.count.toFloat() / maxCount) * maxBarHeight
                    val x = index * barWidth + barWidth * 0.15f
                    val y = size.height - barHeight

                    drawRect(
                        color = SpamRed,
                        topLeft = Offset(x, y),
                        size = Size(barWidth * 0.7f, barHeight)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                items.forEach { item ->
                    Text(
                        text = item.date.takeLast(5),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}
