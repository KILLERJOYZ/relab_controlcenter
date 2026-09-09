package com.example.relab_tool.benchmark.scoring

import com.example.relab_tool.benchmark.domain.model.BenchmarkPillar
import com.example.relab_tool.benchmark.domain.model.PillarScore

data class RadarChartEntry(
    val pillar: BenchmarkPillar,
    val score: Int
)

object RadarChartData {
    /**
     * Build radar chart entries from a list of pillar scores.
     *
     * Only includes pillars that:
     *  1. Are active (weight > 0) — i.e. part of the current 7-pillar architecture.
     *  2. Were actually run in this session (non-skipped PillarScore exists).
     *
     * This prevents axes for unrun/skipped pillars (e.g. Network when user
     * opted out, or CPU-only Quick Test) from appearing as zero-score spokes.
     */
    fun fromPillarScores(scores: List<PillarScore>): List<RadarChartEntry> {
        val runPillarSet = scores.filter { !it.isSkipped }.map { it.pillar }.toSet()
        return BenchmarkPillar.entries
            .filter { it.weight > 0f && it in runPillarSet }
            .map { pillar ->
                val scoreVal = scores.find { it.pillar == pillar }?.score ?: 0
                RadarChartEntry(pillar, scoreVal)
            }
    }
}
