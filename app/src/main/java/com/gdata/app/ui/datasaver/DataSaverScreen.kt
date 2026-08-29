package com.gdata.app.ui.datasaver

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gdata.app.domain.model.OptimizationMode
import com.gdata.app.ui.theme.Primary

@Composable
fun DataSaverScreen() {
    var optimizationOn by remember { mutableStateOf(true) }
    var gamingMode by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf(OptimizationMode.BALANCED) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Data Saver",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Choose how aggressively G Data optimizes your mobile data.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Master toggle
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
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (optimizationOn) "Active" else "Paused",
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

        // Gaming Mode
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            border = if (gamingMode) BorderStroke(2.dp, Primary) else null,
            colors = CardDefaults.cardColors(
                containerColor = if (gamingMode) Primary.copy(alpha = 0.08f)
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
                    tint = if (gamingMode) Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (gamingMode) "Gaming Mode Active 🎮" else "Gaming Mode",
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (gamingMode) {
                            "Network optimization is prioritizing responsiveness."
                        } else {
                            "Prioritizes low latency and reduces background interference."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = gamingMode,
                    onCheckedChange = { enabled ->
                        gamingMode = enabled
                        if (enabled) {
                            mode = OptimizationMode.PERFORMANCE
                        }
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = Primary)
                )
            }
        }

        Text(
            text = "Optimization Mode",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        ModeOptionCard(
            mode = OptimizationMode.PERFORMANCE,
            icon = Icons.Outlined.Speed,
            selected = mode == OptimizationMode.PERFORMANCE && !gamingMode,
            enabled = optimizationOn && !gamingMode,
            onClick = { mode = OptimizationMode.PERFORMANCE }
        )
        ModeOptionCard(
            mode = OptimizationMode.BALANCED,
            icon = Icons.Outlined.Balance,
            selected = mode == OptimizationMode.BALANCED && !gamingMode,
            enabled = optimizationOn && !gamingMode,
            onClick = { mode = OptimizationMode.BALANCED }
        )
        ModeOptionCard(
            mode = OptimizationMode.EXTREME,
            icon = Icons.Outlined.DataSaverOff,
            selected = mode == OptimizationMode.EXTREME && !gamingMode,
            enabled = optimizationOn && !gamingMode,
            onClick = { mode = OptimizationMode.EXTREME }
        )

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
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = Primary
                )
                Text(
                    text = "G Data never invents free data or artificially increases speed. " +
                            "It only reduces unnecessary consumption where Android allows it.",
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
            .then(
                if (enabled) Modifier.clickable(onClick = onClick)
                else Modifier
            ),
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
                Text(
                    text = mode.displayName,
                    fontWeight = FontWeight.SemiBold
                )
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
