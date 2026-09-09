package com.example.relab_tool.data

/** Pure history calculations shared by persisted samples and regression tests. */
object BatteryHistoryPolicy {
    const val RETENTION_MS = 24L * 60 * 60 * 1000
    const val SAMPLE_INTERVAL_MS = 60_000L

    fun retained(history: List<Triple<Long, Int, Boolean>>, now: Long) =
        history.filter { it.first in (now - RETENTION_MS)..now && it.second in 0..100 }
            .sortedBy { it.first }.takeLast(2000)

    fun dischargeRate(history: List<Triple<Long, Int, Boolean>>): Float {
        val segment = history.asReversed().takeWhile { !it.third }.asReversed()
        if (segment.size < 2) return 0f
        val elapsed = segment.last().first - segment.first().first
        val decrease = segment.first().second - segment.last().second
        return if (elapsed > 0 && decrease > 0) decrease / (elapsed / 3_600_000f) else 0f
    }
}
