package io.github.gopalsuryak.galaxywatch.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ProtocolTest {

    private val hr = HealthSample(SampleType.HEART_RATE_BPM, 72.0, 1_000L, 1_000L)
    private val steps = HealthSample(SampleType.STEPS_DAILY, 4210.0, 0L, 5_000L)

    @Test
    fun `round trip keeps every field`() {
        val decoded = Protocol.decode(Protocol.encode(listOf(hr, steps)))
        assertEquals(listOf(hr, steps), decoded)
    }

    @Test
    fun `empty batch round trips`() {
        assertEquals(emptyList(), Protocol.decode(Protocol.encode(emptyList())))
    }

    @Test
    fun `unknown sample types are skipped`() {
        val payload = "v1\nBLOOD_OXYGEN|98.0|1|1\nHEART_RATE_BPM|72.0|1000|1000\n".toByteArray()
        assertEquals(listOf(hr), Protocol.decode(payload))
    }

    @Test
    fun `wrong version is rejected`() {
        assertFailsWith<ProtocolException> { Protocol.decode("v2\n".toByteArray()) }
    }

    @Test
    fun `malformed line is rejected`() {
        assertFailsWith<ProtocolException> { Protocol.decode("v1\nHEART_RATE_BPM|72.0\n".toByteArray()) }
    }

    @Test
    fun `non numeric value is rejected`() {
        assertFailsWith<ProtocolException> { Protocol.decode("v1\nHEART_RATE_BPM|abc|1|1\n".toByteArray()) }
    }

    @Test
    fun `interval ending before it starts is rejected`() {
        assertFailsWith<ProtocolException> { Protocol.decode("v1\nSTEPS_DAILY|10.0|5|1\n".toByteArray()) }
    }

    @Test
    fun `oversized batch is refused on encode`() {
        val tooMany = List(Protocol.MAX_SAMPLES_PER_BATCH + 1) { hr }
        assertFailsWith<IllegalArgumentException> { Protocol.encode(tooMany) }
    }

    @Test
    fun `max batch fits in a data layer item`() {
        val worstCase = HealthSample(SampleType.CALORIES_DAILY, 123456.789, Long.MAX_VALUE - 1, Long.MAX_VALUE)
        val bytes = Protocol.encode(List(Protocol.MAX_SAMPLES_PER_BATCH) { worstCase })
        assert(bytes.size < 100 * 1024) { "batch is ${bytes.size} bytes" }
    }
}
