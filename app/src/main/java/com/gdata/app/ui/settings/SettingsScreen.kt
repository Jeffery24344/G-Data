package com.gdata.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Rule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gdata.app.ui.theme.Primary
import com.gdata.app.util.DataFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    var editing by remember { mutableStateOf(false) }
    var totalGb by remember { mutableStateOf("20") }
    var remainingGb by remember { mutableStateOf("5.6") }
    var daysLeft by remember { mutableStateOf("12") }
    var savedTotal by remember { mutableStateOf(20_000_000_000L) }
    var savedRemaining by remember { mutableStateOf(5_600_000_000L) }
    var savedDays by remember { mutableStateOf(12) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.DataUsage, null, tint = Primary)
                            Spacer(Modifier.width(12.dp))
                            Text("Data Bundle", fontWeight = FontWeight.SemiBold)
                        }
                        if (!editing) TextButton(onClick = { editing = true }) { Text("Edit") }
                    }
                    Spacer(Modifier.height(16.dp))
                    if (editing) {
                        OutlinedTextField(totalGb, { totalGb = it }, label = { Text("Total (GB)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(remainingGb, { remainingGb = it }, label = { Text("Remaining (GB)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(daysLeft, { daysLeft = it }, label = { Text("Days left") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Spacer(Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { editing = false }) { Text("Cancel") }
                            Spacer(Modifier.width(8.dp))
                            Button(onClick = {
                                savedTotal = ((totalGb.toDoubleOrNull() ?: 0.0) * 1_000_000_000L).toLong()
                                savedRemaining = ((remainingGb.toDoubleOrNull() ?: 0.0) * 1_000_000_000L).toLong()
                                savedDays = daysLeft.toIntOrNull() ?: 0
                                editing = false
                            }) { Text("Save") }
                        }
                    } else {
                        Text("Remaining", style = MaterialTheme.typography.bodySmall)
                        Text(DataFormat.formatBytes(savedRemaining), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Primary)
                        Text("Total: ${DataFormat.formatBytes(savedTotal)}")
                        if (savedDays > 0) {
                            val daily = savedRemaining / savedDays
                            Text("$savedDays days left • Recommended ${DataFormat.formatBytes(daily)}/day",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            SettingsGroup("Preferences") {
                SettingsItem(Icons.Outlined.Notifications, "Notifications & Alerts", "Daily limits, high usage warnings")
                SettingsItem(Icons.Outlined.Rule, "Optimization Rules", "Automatic mode switching")
                SettingsItem(Icons.Outlined.PrivacyTip, "Privacy", "What we monitor and what we don’t")
            }
            SettingsGroup("About") {
                SettingsItem(Icons.Outlined.Info, "About G Data", "Version 1.0.0 • Big Big Dream")
            }
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Surface(onClick = { }, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Primary)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
