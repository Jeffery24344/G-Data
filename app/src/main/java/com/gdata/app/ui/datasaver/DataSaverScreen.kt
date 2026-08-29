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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Data Saver", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            ModePolicy.activeSummary(state.mode, state.optimizationEnabled, state.gamingMode),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

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
                        color = if (state.optimizationEnabled) Primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = state.optimizationEnabled,
                    onCheckedChange = viewModel::setOptimizationEnabled,
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
            Row(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.SportsEsports, null,
                    tint = if (state.gamingMode) Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (state.gamingMode) "Gaming Mode Active 🎮" else "Gaming Mode",
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Forces Performance priority for responsiveness.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = state.gamingMode,
                    onCheckedChange = viewModel::setGamingMode,
                    colors = SwitchDefaults.colors(checkedTrackColor = Primary)
                )
            }
        }

        Text("Optimization Mode", fontWeight = FontWeight.SemiBold)

        ModeOptionCard(
            OptimizationMode.PERFORMANCE, Icons.Outlined.Speed,
            selected = state.mode == OptimizationMode.PERFORMANCE && !state.gamingMode,
            enabled = state.optimizationEnabled && !state.gamingMode,
            onClick = { viewModel.setMode(OptimizationMode.PERFORMANCE) }
        )
        ModeOptionCard(
            OptimizationMode.BALANCED, Icons.Outlined.Balance,
            selected = state.mode == OptimizationMode.BALANCED && !state.gamingMode,
            enabled = state.optimizationEnabled && !state.gamingMode,
            onClick = { viewModel.setMode(OptimizationMode.BALANCED) }
        )
        ModeOptionCard(
            OptimizationMode.EXTREME, Icons.Outlined.DataSaverOff,
            selected = state.mode == OptimizationMode.EXTREME && !state.gamingMode,
            enabled = state.optimizationEnabled && !state.gamingMode,
            onClick = { viewModel.setMode(OptimizationMode.EXTREME) }
        )

        // Active policy tips
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("What this mode does", fontWeight = FontWeight.SemiBold)
                ModePolicy.actionHints(
                    if (state.gamingMode) OptimizationMode.PERFORMANCE else state.mode
                ).forEach { tip ->
                    Text("• $tip", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        if (state.mode == OptimizationMode.EXTREME && state.optimizationEnabled && !state.gamingMode) {
            Button(
                onClick = {
                    try {
                        context.startActivity(
                            Intent(Settings.ACTION_DATA_SAVER_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                        )
                    } catch (_: Exception) {
                        context.startActivity(
                            Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Open system Data Saver")
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.Info, null, tint = Primary)
                Text(
                    "Android does not allow apps to silently rewrite all traffic without a VPN. " +
                        "G Data applies real policies: mode-based estimates, automatic rules " +
                        "(battery / remaining data / high usage), and shortcuts to system Data Saver. " +
                        "It never invents free data or fake speed boosts.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = if (selected) Primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(mode.displayName, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(mode.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
