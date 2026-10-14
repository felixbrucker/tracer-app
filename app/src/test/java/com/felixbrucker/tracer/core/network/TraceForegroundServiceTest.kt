package com.felixbrucker.tracer.core.network

import org.junit.Assert.assertEquals
import org.junit.Test

class TraceForegroundServiceTest {
    @Test
    fun testConstants() {
        assertEquals("tracer_service_channel", TraceForegroundService.CHANNEL_ID)
        assertEquals(1001, TraceForegroundService.ONGOING_NOTIFICATION_ID)
    }
}
