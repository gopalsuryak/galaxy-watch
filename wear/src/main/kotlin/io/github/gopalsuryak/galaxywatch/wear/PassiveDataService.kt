package io.github.gopalsuryak.galaxywatch.wear

import android.util.Log
import androidx.health.services.client.PassiveListenerService
import androidx.health.services.client.data.DataPointContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Health Services wakes this service up with batched background data, even when the
 * app UI is closed. We forward everything to the phone straight away.
 */
class PassiveDataService : PassiveListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewDataPointsReceived(dataPoints: DataPointContainer) {
        val samples = SampleMapper.toSamples(dataPoints)
        scope.launch {
            runCatching { SampleSender(this@PassiveDataService).send(samples) }
                .onFailure { Log.w(TAG, "could not queue samples for the phone", it) }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val TAG = "PassiveDataService"
    }
}
