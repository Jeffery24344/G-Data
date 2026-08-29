package com.gdata.app.ui.home

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gdata.app.domain.model.OptimizationMode
import com.gdata.app.ui.theme.Primary
import com.gdata.app.util.DataFormat

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = if (state.gamingMode) "Gaming Mode 🎮" else state.mode.displayName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = state.policySummary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (!state.hasUsagePermission) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.12f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Usage Access required", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Grant permission to show real mobile data usage.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Open Usage Access") }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            )
        ) {
            Column(Modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                UsageRow("Today", DataFormat.formatBytes(state.todayBytes))
                UsageRow("This Month", DataFormat.formatBytes(state.monthBytes))
                HorizontalDivider()
                UsageRow(
                    "Estimated Savings",
                    DataFormat.formatBytes(state.estimatedSavedBytes),
                    highlight = true
                )
                Text(
                    when {
                        !state.optimizationEnabled -> "Optimization off — no savings estimated"
                        state.gamingMode -> "Gaming Mode uses a lower savings estimate"
                        state.mode == OptimizationMode.EXTREME -> "Extreme mode — highest estimate (~28%)"
                        state.mode == OptimizationMode.BALANCED -> "Balanced mode estimate (~15%)"
                        else -> "Performance mode — light estimate (~5%)"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (state.bundle.isValid) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Remaining Data", style = MaterialTheme.typography.labelLarge)
                    Text(
                        DataFormat.formatBytes(state.bundle.remainingBytes),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                    if (state.bundle.daysLeft > 0) {
                        Text(
                            "${state.bundle.daysLeft} days left • ${DataFormat.formatBytes(state.bundle.recommendedDailyBytes)}/day",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("DATA OPTIMIZATION", fontWeight = FontWeight.Bold)
                    Text(
                        if (state.optimizationEnabled) "Active" else "Paused",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.optimizationEnabled) Primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = state.optimizationEnabled,
                    onCheckedChange = viewModel::setOptimizationEnabled,
                    colors = SwitchDefaults.colors(checkedTrackColor = Primary)
                )
            }
        }

        Text("Optimization Mode", style = MaterialTheme.typography.titleMedium)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModeChip("Performance", state.mode == OptimizationMode.PERFORMANCE && !state.gamingMode, {
                viewModel.setMode(OptimizationMode.PERFORMANCE)
            }, Modifier.weight(1f))
            ModeChip("Balanced", state.mode == OptimizationMode.BALANCED && !state.gamingMode, {
                viewModel.setMode(OptimizationMode.BALANCED)
            }, Modifier.weight(1f))
            ModeChip("Extreme", state.mode == OptimizationMode.EXTREME && !state.gamingMode, {
                viewModel.setMode(OptimizationMode.EXTREME)
            }, Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) }, modifier = modifier)
}

@Composable
private fun UsageRow(label: String, value: String, highlight: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (highlight) Primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
