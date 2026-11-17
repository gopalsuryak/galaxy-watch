package io.github.gopalsuryak.galaxywatch.wear

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import io.github.gopalsuryak.galaxywatch.shared.HealthSample
import io.github.gopalsuryak.galaxywatch.shared.Protocol
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Sends samples to the phone as Data Layer items rather than messages.
 *
 * Messages are dropped when the phone is out of range; data items are stored and synced
 * by Google Play services once the connection comes back. The phone deletes each item
 * after saving it, so nothing piles up on the watch.
 */
class SampleSender(context: Context) {

    private val dataClient = Wearable.getDataClient(context.applicationContext)

    suspend fun send(samples: List<HealthSample>) {
        if (samples.isEmpty()) return
        samples.chunked(Protocol.MAX_SAMPLES_PER_BATCH).forEach { batch ->
            val request = PutDataRequest.create(Protocol.samplesPath(UUID.randomUUID().toString()))
                .setData(Protocol.encode(batch))
                .setUrgent()
            dataClient.putDataItem(request).await()
        }
        Log.d(TAG, "queued ${samples.size} samples for the phone")
    }

    private companion object {
        const val TAG = "SampleSender"
    }
}
