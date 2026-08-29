package com.gdata.app.ui.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gdata.app.ui.theme.Primary
import com.gdata.app.util.DataFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen() {
    var period by remember { mutableIntStateOf(1) } // 0 Today, 1 Week, 2 Month
    val labels = listOf("Today", "Week", "Month")
    val totals = listOf(1_240_000_000L, 8_500_000_000L, 18_700_000_000L)
    val saved = listOf(280_000_000L, 1_900_000_000L, 4_200_000_000L)
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val dayBytes = listOf(0.9f, 1.2f, 0.7f, 1.5f, 1.1f, 1.8f, 1.3f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            labels.forEachIndexed { index, label ->
                SegmentedButton(
                    selected = period == index,
                    onClick = { period = index },
                    shape = SegmentedButtonDefaults.itemShape(index, labels.size)
                ) { Text(label) }
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(labels[period], style = MaterialTheme.typography.labelLarge)
                Text(
                    DataFormat.formatBytes(totals[period]),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Estimated saved: ${DataFormat.formatBytes(saved[period])}",
                    color = Primary,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "Demo data • grant Usage Access for real numbers",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (period == 1) {
            Text("Daily Usage", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp).height(180.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        dayBytes.forEach { f ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(f.coerceIn(0.15f, 1f))
                                    .background(Primary.copy(alpha = 0.85f), RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        days.forEach { d ->
                            Text(d, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
