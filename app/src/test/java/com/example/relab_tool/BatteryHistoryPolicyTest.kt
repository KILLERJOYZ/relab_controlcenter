package com.example.relab_tool

import com.example.relab_tool.data.BatteryHistoryPolicy
import org.junit.Assert.*
import org.junit.Test

class BatteryHistoryPolicyTest {
    @Test fun historyCrossesMidnightWithoutResetting() {
        val midnight = 2 * BatteryHistoryPolicy.RETENTION_MS
        val history = listOf(Triple(midnight - 60_000, 80, false), Triple(midnight + 60_000, 79, false))
        assertEquals(history, BatteryHistoryPolicy.retained(history, midnight + 60_000))
    }

    @Test fun removesExpiredInvalidAndFutureSamples() {
        val now = BatteryHistoryPolicy.RETENTION_MS * 3
        val good = Triple(now - 100, 50, false)
        assertEquals(listOf(good), BatteryHistoryPolicy.retained(listOf(
            Triple(now - BatteryHistoryPolicy.RETENTION_MS - 1, 80, false),
            Triple(now, -1, false), Triple(now + 1, 50, false), good
        ), now))
    }

    @Test fun dischargeDoesNotBridgeChargingSessions() {
        val history = listOf(
            Triple(0L, 100, false), Triple(3_600_000L, 50, false),
            Triple(7_200_000L, 90, true), Triple(10_800_000L, 90, false),
            Triple(14_400_000L, 80, false)
        )
        assertEquals(10f, BatteryHistoryPolicy.dischargeRate(history), 0.001f)
        assertEquals(0f, BatteryHistoryPolicy.dischargeRate(history + Triple(15_000_000L, 85, true)), 0f)
    }
}
