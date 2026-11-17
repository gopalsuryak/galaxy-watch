package io.github.gopalsuryak.galaxywatch.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.gopalsuryak.galaxywatch.shared.HealthSample
import io.github.gopalsuryak.galaxywatch.shared.SampleType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = SampleStore.get(this)
        setContent { MaterialTheme { Dashboard(store) } }
    }

    private data class Snapshot(
        val latest: Map<SampleType, HealthSample?> = emptyMap(),
        val recent: List<HealthSample> = emptyList(),
    )

    @Composable
    private fun Dashboard(store: SampleStore) {
        val version by store.version.collectAsStateWithLifecycle()
        val snapshot by produceState(Snapshot(), version) {
            value = withContext(Dispatchers.IO) {
                Snapshot(
                    latest = SampleType.entries.associateWith { store.latest(it) },
                    recent = store.recent(50),
                )
            }
        }

        Scaffold { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Galaxy Watch Companion", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Not a medical device. Readings are for personal tracking only.",
                    style = MaterialTheme.typography.bodySmall,
                )

                SampleType.entries.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { type ->
                            MetricCard(type, snapshot.latest[type], Modifier.weight(1f))
                        }
                    }
                }

                Button(
                    onClick = { startActivity(CsvExporter.shareIntent(this@MainActivity, store.all())) },
                    enabled = snapshot.recent.isNotEmpty(),
                ) { Text("Export CSV") }

                Text("Recent readings", style = MaterialTheme.typography.titleMedium)
                if (snapshot.recent.isEmpty()) {
                    Text("Nothing yet. Open the app on your watch and tap Sync.")
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(snapshot.recent) { s ->
                        Text("${time(s.endMs)}  ${label(s.type)}: ${format(s)}")
                    }
                }
            }
        }
    }

    @Composable
    private fun MetricCard(type: SampleType, sample: HealthSample?, modifier: Modifier) {
        Card(modifier = modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text(label(type), style = MaterialTheme.typography.labelMedium)
                Text(sample?.let { format(it) } ?: "--", style = MaterialTheme.typography.titleLarge)
                sample?.let { Text("at ${time(it.endMs)}", style = MaterialTheme.typography.bodySmall) }
            }
        }
    }

    private fun label(type: SampleType) = when (type) {
        SampleType.HEART_RATE_BPM -> "Heart rate"
        SampleType.STEPS_DAILY -> "Steps today"
        SampleType.CALORIES_DAILY -> "Calories today"
        SampleType.DISTANCE_DAILY -> "Distance today"
    }

    private fun format(s: HealthSample) = when (s.type) {
        SampleType.HEART_RATE_BPM -> "${s.value.toInt()} bpm"
        SampleType.STEPS_DAILY -> "${s.value.toLong()}"
        SampleType.CALORIES_DAILY -> "${s.value.toInt()} kcal"
        SampleType.DISTANCE_DAILY -> "%.2f km".format(s.value / 1000)
    }

    private fun time(ms: Long): String = TIME.format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()))

    private companion object {
        val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d HH:mm")
    }
}
