package com.example.relab_tool.benchmark.domain.model

data class BenchmarkResult(
    val id: Long = 0L,
    val timestamp: Long,
    val deviceModel: String,
    val deviceSoc: String,
    val hardwareScore: Int,
    val connectivityScore: Int,
    val totalScore: Int,
    val tier: ScoreTier,
    val pillarScores: List<PillarScore>,
    val isQuickTest: Boolean = false
)
