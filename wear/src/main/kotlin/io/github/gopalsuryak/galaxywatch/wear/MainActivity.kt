package io.github.gopalsuryak.galaxywatch.wear

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val repo by lazy { HealthRepository(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { WatchScreen(repo) } }
    }

    private companion object {
        val PERMISSIONS = arrayOf(Manifest.permission.BODY_SENSORS, Manifest.permission.ACTIVITY_RECOGNITION)
    }

    @Composable
    private fun WatchScreen(repo: HealthRepository) {
        val scope = rememberCoroutineScope()
        var granted by remember { mutableStateOf(hasPermissions()) }
        var status by remember { mutableStateOf("Not syncing") }

        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            granted = hasPermissions()
        }
        LaunchedEffect(Unit) { if (!granted) launcher.launch(PERMISSIONS) }

        val bpm by remember(granted) { if (granted) repo.liveHeartRate() else emptyFlow() }
            .collectAsState(initial = null)

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = bpm?.let { "${it.toInt()} bpm" } ?: "-- bpm",
                style = MaterialTheme.typography.display2,
            )
            Text(text = status, textAlign = TextAlign.Center, style = MaterialTheme.typography.caption2)
            Button(
                modifier = Modifier.padding(top = 8.dp),
                enabled = granted,
                onClick = {
                    scope.launch {
                        status = runCatching { repo.startBackgroundSync() }.fold(
                            onSuccess = { types -> if (types.isEmpty()) "No supported sensors" else "Syncing ${types.size} metrics" },
                            onFailure = { "Sync failed: ${it.message}" },
                        )
                    }
                },
            ) { Text("Sync") }
        }
    }

    private fun hasPermissions() = PERMISSIONS.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }
}
