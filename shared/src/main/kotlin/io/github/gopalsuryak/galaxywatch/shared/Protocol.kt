package io.github.gopalsuryak.galaxywatch.shared

/**
 * Wire format between the watch and the phone, sent over the Wear OS Data Layer.
 *
 * Kept as plain text on purpose: it's easy to inspect, has no dependencies, and the
 * shared module stays a pure JVM library that can be unit tested without a device.
 *
 *   v1
 *   HEART_RATE_BPM|72.0|1731830400000|1731830400000
 *   STEPS_DAILY|4210.0|1731801600000|1731830400000
 */
object Protocol {
    /** Each batch from the watch is written as its own data item under this prefix. */
    const val SAMPLES_PATH_PREFIX = "/samples"

    const val VERSION = 1
    private const val HEADER = "v$VERSION"
    private const val SEP = '|'

    /** Data Layer items are capped at 100 KB; this keeps batches well under that. */
    const val MAX_SAMPLES_PER_BATCH = 1_000

    fun samplesPath(batchId: String): String = "$SAMPLES_PATH_PREFIX/$batchId"

    fun encode(samples: List<HealthSample>): ByteArray {
        require(samples.size <= MAX_SAMPLES_PER_BATCH) {
            "batch of ${samples.size} exceeds $MAX_SAMPLES_PER_BATCH, split it first"
        }
        val text = buildString {
            append(HEADER).append('\n')
            for (s in samples) {
                append(s.type.name).append(SEP)
                    .append(s.value).append(SEP)
                    .append(s.startMs).append(SEP)
                    .append(s.endMs).append('\n')
            }
        }
        return text.toByteArray(Charsets.UTF_8)
    }

    /**
     * Unknown sample types are skipped rather than failing the whole batch, so an older
     * phone app keeps working when a newer watch app starts sending new types.
     */
    fun decode(bytes: ByteArray): List<HealthSample> {
        val lines = bytes.toString(Charsets.UTF_8).lineSequence().filter { it.isNotBlank() }.toList()
        if (lines.isEmpty()) throw ProtocolException("empty payload")
        if (lines.first() != HEADER) throw ProtocolException("unsupported header '${lines.first()}'")

        return lines.drop(1).mapNotNull { line ->
            val parts = line.split(SEP)
            if (parts.size != 4) throw ProtocolException("malformed line '$line'")
            val type = SampleType.entries.firstOrNull { it.name == parts[0] } ?: return@mapNotNull null
            try {
                HealthSample(type, parts[1].toDouble(), parts[2].toLong(), parts[3].toLong())
            } catch (e: IllegalArgumentException) {
                throw ProtocolException("bad values in line '$line': ${e.message}")
            }
        }
    }
}

class ProtocolException(message: String) : Exception(message)
