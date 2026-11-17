package io.github.gopalsuryak.galaxywatch.mobile

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import io.github.gopalsuryak.galaxywatch.shared.Protocol
import io.github.gopalsuryak.galaxywatch.shared.ProtocolException

/**
 * Receives sample batches from the watch, saves them, then deletes the data item so it
 * doesn't stay in the Data Layer forever. Runs on a background thread, so blocking is fine.
 */
class WatchDataListenerService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        val store = SampleStore.get(this)
        val dataClient = Wearable.getDataClient(this)

        for (event in dataEvents) {
            if (event.type != DataEvent.TYPE_CHANGED) continue
            val item = event.dataItem
            if (item.uri.path?.startsWith(Protocol.SAMPLES_PATH_PREFIX) != true) continue

            try {
                val samples = Protocol.decode(item.data ?: continue)
                store.insert(samples)
                Log.d(TAG, "saved ${samples.size} samples from ${item.uri.host}")
            } catch (e: ProtocolException) {
                // A batch we can't read will never become readable, so drop it instead of
                // letting it block the queue. Usually means the watch app is newer.
                Log.w(TAG, "dropping unreadable batch ${item.uri}: ${e.message}")
            }
            runCatching { Tasks.await(dataClient.deleteDataItems(item.uri)) }
                .onFailure { Log.w(TAG, "could not delete ${item.uri}", it) }
        }
    }

    private companion object {
        const val TAG = "WatchDataListener"
    }
}
