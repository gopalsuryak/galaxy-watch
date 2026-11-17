package io.github.gopalsuryak.galaxywatch.wear

import android.os.SystemClock
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import io.github.gopalsuryak.galaxywatch.shared.HealthSample
import io.github.gopalsuryak.galaxywatch.shared.SampleType
import java.time.Instant

/** Turns Health Services data points into our own wire model. */
object SampleMapper {

    /** Data types we ask Health Services for, and the matching wire type. */
    val passiveTypes = mapOf(
        DataType.HEART_RATE_BPM to SampleType.HEART_RATE_BPM,
        DataType.STEPS_DAILY to SampleType.STEPS_DAILY,
        DataType.CALORIES_DAILY to SampleType.CALORIES_DAILY,
        DataType.DISTANCE_DAILY to SampleType.DISTANCE_DAILY,
    )

    fun toSamples(container: DataPointContainer): List<HealthSample> {
        // Health Services timestamps are relative to boot; convert to wall clock time.
        val boot = Instant.ofEpochMilli(System.currentTimeMillis() - SystemClock.elapsedRealtime())
        val out = mutableListOf<HealthSample>()

        container.getData(DataType.HEART_RATE_BPM).forEach { p ->
            val t = p.getTimeInstant(boot).toEpochMilli()
            out += HealthSample(SampleType.HEART_RATE_BPM, p.value, t, t)
        }
        container.getData(DataType.STEPS_DAILY).forEach { p ->
            out += HealthSample(
                SampleType.STEPS_DAILY, p.value.toDouble(),
                p.getStartInstant(boot).toEpochMilli(), p.getEndInstant(boot).toEpochMilli(),
            )
        }
        container.getData(DataType.CALORIES_DAILY).forEach { p ->
            out += HealthSample(
                SampleType.CALORIES_DAILY, p.value,
                p.getStartInstant(boot).toEpochMilli(), p.getEndInstant(boot).toEpochMilli(),
            )
        }
        container.getData(DataType.DISTANCE_DAILY).forEach { p ->
            out += HealthSample(
                SampleType.DISTANCE_DAILY, p.value,
                p.getStartInstant(boot).toEpochMilli(), p.getEndInstant(boot).toEpochMilli(),
            )
        }
        // Health Services occasionally reports a zero-length or negative heart rate while
        // the sensor settles; those are noise, not readings.
        return out.filter { it.type != SampleType.HEART_RATE_BPM || it.value > 0 }
    }
}
