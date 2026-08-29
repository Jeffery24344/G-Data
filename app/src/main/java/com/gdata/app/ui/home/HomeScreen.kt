package com.gdata.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gdata.app.domain.model.OptimizationMode
import com.gdata.app.ui.theme.Primary
import com.gdata.app.util.DataFormat

@Composable
fun HomeScreen() {
    var optimizationOn by remember { mutableStateOf(true) }
    var mode by remember { mutableStateOf(OptimizationMode.BALANCED) }

    val todayBytes = 1_240_000_000L
    val monthBytes = 18_700_000_000L
    val savedBytes = 680_000_000L
    val remainingBytes = 5_600_000_000L

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = mode.displayName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = mode.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                UsageRow(label = "Today", value = DataFormat.formatBytes(todayBytes))
                UsageRow(label = "This Month", value = DataFormat.formatBytes(monthBytes))
                HorizontalDivider()
                UsageRow(
                    label = "Estimated Savings",
                    value = DataFormat.formatBytes(savedBytes),
                    highlight = true
                )
                Text(
                    text = "Estimates only • real stats after Usage Access",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Remaining Data",
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = DataFormat.formatBytes(remainingBytes),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
                Text(
                    text = "Set your bundle in Settings for accurate daily targets",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "DATA OPTIMIZATION",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (optimizationOn) "Active" else "Paused",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (optimizationOn) Primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = optimizationOn,
                    onCheckedChange = { optimizationOn = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = Primary)
                )
            }
        }

        Text(
            text = "Optimization Mode",
            style = MaterialTheme.typography.titleMedium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ModeChip(
                label = "Performance",
                selected = mode == OptimizationMode.PERFORMANCE,
                onClick = { mode = OptimizationMode.PERFORMANCE },
                modifier = Modifier.weight(1f)
            )
            ModeChip(
                label = "Balanced",
                selected = mode == OptimizationMode.BALANCED,
                onClick = { mode = OptimizationMode.BALANCED },
                modifier = Modifier.weight(1f)
            )
            ModeChip(
                label = "Extreme",
                selected = mode == OptimizationMode.EXTREME,
                onClick = { mode = OptimizationMode.EXTREME },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun ModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = label) },
        modifier = modifier
    )
}

@Composable
private fun UsageRow(
    label: String,
    value: String,
    highlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (highlight) Primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
