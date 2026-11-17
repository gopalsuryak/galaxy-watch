package io.github.gopalsuryak.galaxywatch.wear

import android.content.Context
import androidx.concurrent.futures.await
import androidx.health.services.client.HealthServices
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DeltaDataType
import androidx.health.services.client.data.PassiveListenerConfig
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.runBlocking

class HealthRepository(context: Context) {

    private val client = HealthServices.getClient(context.applicationContext)

    /** Which of our passive types this particular watch actually supports. */
    suspend fun supportedPassiveTypes(): Set<DataType<*, *>> {
        val caps = client.passiveMonitoringClient.getCapabilitiesAsync().await()
        return SampleMapper.passiveTypes.keys
            .filter { it in caps.supportedDataTypesPassiveMonitoring }
            .toSet()
    }

    /** Registers [PassiveDataService]; survives app restarts until cleared. */
    suspend fun startBackgroundSync(): Set<DataType<*, *>> {
        val types = supportedPassiveTypes()
        if (types.isEmpty()) return types
        val config = PassiveListenerConfig.builder().setDataTypes(types).build()
        client.passiveMonitoringClient
            .setPassiveListenerServiceAsync(PassiveDataService::class.java, config)
            .await()
        return types
    }

    suspend fun stopBackgroundSync() {
        client.passiveMonitoringClient.clearPassiveListenerServiceAsync().await()
    }

    /** Live heart rate for the on-watch screen, emitted while collected. */
    fun liveHeartRate(): Flow<Double> = callbackFlow {
        val callback = object : MeasureCallback {
            override fun onAvailabilityChanged(dataType: DeltaDataType<*, *>, availability: Availability) = Unit

            override fun onDataReceived(data: DataPointContainer) {
                data.getData(DataType.HEART_RATE_BPM).lastOrNull()?.let { trySend(it.value) }
            }
        }
        client.measureClient.registerMeasureCallback(DataType.HEART_RATE_BPM, callback)
        awaitClose {
            runBlocking {
                runCatching {
                    client.measureClient.unregisterMeasureCallbackAsync(DataType.HEART_RATE_BPM, callback).await()
                }
            }
        }
    }
}
