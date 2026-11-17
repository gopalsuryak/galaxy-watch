package io.github.gopalsuryak.galaxywatch.shared

/**
 * One reading coming off the watch.
 *
 * Point-in-time readings (heart rate) use the same value for [startMs] and [endMs].
 * Daily totals (steps, calories, distance) cover the interval from the start of the day
 * up to [endMs], so the latest one for a day is that day's running total.
 */
data class HealthSample(
    val type: SampleType,
    val value: Double,
    val startMs: Long,
    val endMs: Long,
) {
    init {
        require(endMs >= startMs) { "endMs ($endMs) is before startMs ($startMs)" }
        require(!value.isNaN() && !value.isInfinite()) { "value must be a finite number" }
    }
}

enum class SampleType(val unit: String) {
    HEART_RATE_BPM("bpm"),
    STEPS_DAILY("steps"),
    CALORIES_DAILY("kcal"),
    DISTANCE_DAILY("m"),
}
