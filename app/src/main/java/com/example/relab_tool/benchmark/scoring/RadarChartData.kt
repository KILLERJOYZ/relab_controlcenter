package com.example.relab_tool.benchmark.scoring

import com.example.relab_tool.benchmark.domain.model.BenchmarkPillar
import com.example.relab_tool.benchmark.domain.model.PillarScore

data class RadarChartEntry(
    val pillar: BenchmarkPillar,
    val score: Int,
    val referenceScore: Int = 500
)

object RadarChartData {
    fun fromPillarScores(scores: List<PillarScore>): List<RadarChartEntry> {
        return BenchmarkPillar.entries.map { pillar ->
            val scoreObj = scores.find { it.pillar == pillar }
            val scoreVal = if (scoreObj == null || scoreObj.isSkipped) 0 else scoreObj.score
            RadarChartEntry(pillar, scoreVal)
        }
    }
}
