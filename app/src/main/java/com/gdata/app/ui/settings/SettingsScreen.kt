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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gdata.app.ui.theme.Primary
import com.gdata.app.util.DataFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onPrivacy: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val bundle by viewModel.bundle.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf(false) }
    var totalGb by remember { mutableStateOf("") }
    var remainingGb by remember { mutableStateOf("") }
    var daysLeft by remember { mutableStateOf("") }

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
                        if (!editing) {
                            TextButton(onClick = {
                                totalGb = if (bundle.totalBytes > 0)
                                    (bundle.totalBytes / 1_000_000_000.0).toString() else ""
                                remainingGb = if (bundle.remainingBytes > 0)
                                    (bundle.remainingBytes / 1_000_000_000.0).toString() else ""
                                daysLeft = if (bundle.daysLeft > 0) bundle.daysLeft.toString() else ""
                                editing = true
                            }) { Text("Edit") }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    if (editing) {
                        OutlinedTextField(
                            totalGb, { totalGb = it },
                            label = { Text("Total (GB)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            remainingGb, { remainingGb = it },
                            label = { Text("Remaining (GB)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            daysLeft, { daysLeft = it },
                            label = { Text("Days left") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { editing = false }) { Text("Cancel") }
                            Spacer(Modifier.width(8.dp))
                            Button(onClick = {
                                viewModel.saveBundle(totalGb, remainingGb, daysLeft)
                                editing = false
                            }) { Text("Save") }
                        }
                    } else if (bundle.isValid) {
                        Text("Remaining", style = MaterialTheme.typography.bodySmall)
                        Text(
                            DataFormat.formatBytes(bundle.remainingBytes),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Primary
                        )
                        Text("Total: ${DataFormat.formatBytes(bundle.totalBytes)}")
                        if (bundle.daysLeft > 0) {
                            Text(
                                "${bundle.daysLeft} days left • Recommended ${DataFormat.formatBytes(bundle.recommendedDailyBytes)}/day",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        Text(
                            "No bundle saved yet. Tap Edit to enter your package.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column {
                    Surface(onClick = onPrivacy, modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.PrivacyTip, null, tint = Primary)
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text("Privacy", fontWeight = FontWeight.Medium)
                                Text("What we monitor and what we don\'t", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Surface(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Info, null, tint = Primary)
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text("About G Data", fontWeight = FontWeight.Medium)
                                Text("Version 1.0.0 • Big Big Dream", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = { viewModel.testNotification() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Outlined.Notifications, null)
                Spacer(Modifier.width(8.dp))
                Text("Send test notification")
            }
        }
    }
}
