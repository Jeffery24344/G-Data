package com.gdata.app.ui.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gdata.app.ui.theme.Primary
import com.gdata.app.util.DataFormat

data class DemoApp(val name: String, val bytes: Long, val percent: Float)

private val demoApps = listOf(
    DemoApp("YouTube", 4_800_000_000L, 28f),
    DemoApp("TikTok", 3_200_000_000L, 19f),
    DemoApp("Instagram", 1_700_000_000L, 10f),
    DemoApp("Chrome", 1_100_000_000L, 6.5f),
    DemoApp("WhatsApp", 850_000_000L, 5f),
    DemoApp("Google Photos", 620_000_000L, 3.7f),
    DemoApp("Play Store", 410_000_000L, 2.4f)
)

@Composable
fun AppsScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("This Month • Mobile Data", style = MaterialTheme.typography.labelLarge)
                    Text(
                        DataFormat.formatBytes(demoApps.sumOf { it.bytes }),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Demo list — grant Usage Access for real per-app data",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        itemsIndexed(demoApps) { index, app ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${index + 1}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(28.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(app.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(DataFormat.formatBytes(app.bytes), color = Primary, style = MaterialTheme.typography.bodyLarge)
                        }
                        Text("${"%.1f".format(app.percent)}%", style = MaterialTheme.typography.titleSmall)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        when {
                            app.bytes > 2_000_000_000L -> "Very high data usage detected."
                            app.bytes > 800_000_000L -> "High data consumption."
                            else -> "Moderate usage."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Text("Optimize")
                    }
                }
            }
        }
    }
}
