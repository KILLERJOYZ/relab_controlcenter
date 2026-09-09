package com.example.relab_tool.benchmark.domain.model

data class PillarScore(
    val pillar: BenchmarkPillar,
    val score: Int,
    val subScores: List<SubScore>,
    val isSkipped: Boolean = false
)
