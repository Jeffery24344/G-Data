package com.gdata.app.ui.datasaver

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.outlined.Balance
import androidx.compose.material.icons.outlined.DataSaverOff
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gdata.app.domain.model.ModePolicy
import com.gdata.app.domain.model.OptimizationMode
import com.gdata.app.ui.theme.Primary

@Composable
fun DataSaverScreen(
    viewModel: DataSaverViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Data Saver",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.15f))
            ) {
                Text(
                    text = "Active: ${if (state.gamingMode) "Gaming → Performance" else state.mode.displayName}" +
                        if (state.optimizationEnabled) " · ON" else " · OFF",
                    modifier = Modifier.padding(14.dp),
                    fontWeight = FontWeight.SemiBold,
                    color = Primary
                )
            }

            Text(
                text = state.statusLine.ifBlank {
                    ModePolicy.activeSummary(state.mode, state.optimizationEnabled, state.gamingMode)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

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
                        Text(text = "DATA OPTIMIZATION", fontWeight = FontWeight.Bold)
                        Text(
                            text = if (state.optimizationEnabled) "Active" else "Paused",
                            color = if (state.optimizationEnabled) Primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = state.optimizationEnabled,
                        onCheckedChange = { viewModel.setOptimizationEnabled(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = Primary)
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = if (state.gamingMode) BorderStroke(2.dp, Primary) else null,
                colors = CardDefaults.cardColors(
                    containerColor = if (state.gamingMode) Primary.copy(alpha = 0.08f)
                    else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsEsports,
                        contentDescription = null,
                        tint = if (state.gamingMode) Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (state.gamingMode) "Gaming Mode Active 🎮" else "Gaming Mode",
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Forces Performance priority for responsiveness.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = state.gamingMode,
                        onCheckedChange = { viewModel.setGamingMode(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = Primary)
                    )
                }
            }

            Text(text = "Optimization Mode", fontWeight = FontWeight.SemiBold)

            ModeOptionCard(
                mode = OptimizationMode.PERFORMANCE,
                icon = Icons.Outlined.Speed,
                selected = state.mode == OptimizationMode.PERFORMANCE && !state.gamingMode,
                enabled = state.optimizationEnabled && !state.gamingMode,
                onClick = { viewModel.setMode(OptimizationMode.PERFORMANCE) }
            )
            ModeOptionCard(
                mode = OptimizationMode.BALANCED,
                icon = Icons.Outlined.Balance,
                selected = state.mode == OptimizationMode.BALANCED && !state.gamingMode,
                enabled = state.optimizationEnabled && !state.gamingMode,
                onClick = { viewModel.setMode(OptimizationMode.BALANCED) }
            )
            ModeOptionCard(
                mode = OptimizationMode.EXTREME,
                icon = Icons.Outlined.DataSaverOff,
                selected = state.mode == OptimizationMode.EXTREME && !state.gamingMode,
                enabled = state.optimizationEnabled && !state.gamingMode,
                onClick = { viewModel.setMode(OptimizationMode.EXTREME) }
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "What this mode does", fontWeight = FontWeight.SemiBold)
                    val hintsMode = if (state.gamingMode) OptimizationMode.PERFORMANCE else state.mode
                    ModePolicy.actionHints(hintsMode).forEach { tip ->
                        Text(text = "• $tip", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (state.mode == OptimizationMode.EXTREME &&
                state.optimizationEnabled &&
                !state.gamingMode
            ) {
                Button(
                    onClick = {
                        val dataSaverIntent = Intent("android.settings.DATA_SAVER_SETTINGS")
                        dataSaverIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        try {
                            context.startActivity(dataSaverIntent)
                        } catch (_: Exception) {
                            val wireless = Intent(Settings.ACTION_WIRELESS_SETTINGS)
                            wireless.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(wireless)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Open system Data Saver")
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Outlined.Info, null, tint = Primary)
                    Text(
                        text = "Changing mode is saved immediately. You will see a confirmation message and a notification. " +
                            "Modes change G Data policy and estimates. For strongest real data cuts, use Extreme + system Data Saver.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeOptionCard(
    mode: OptimizationMode,
    icon: ImageVector,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val containerColor = when {
        !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        selected -> Primary.copy(alpha = 0.08f)
        else -> MaterialTheme.colorScheme.surface
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(16.dp),
        border = if (selected) BorderStroke(2.dp, Primary) else null,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = mode.displayName, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = mode.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RadioButton(
                selected = selected,
                onClick = if (enabled) onClick else null,
                enabled = enabled,
                colors = RadioButtonDefaults.colors(selectedColor = Primary)
            )
        }
    }
}
