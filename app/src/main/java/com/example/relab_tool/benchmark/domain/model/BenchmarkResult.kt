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
    val isQuickTest: Boolean = false,
    val isWarmRun: Boolean = false,    // RC-6: true if ART JIT was pre-warmed by a prior run this session
    val runScope: String = "Full"      // RC-11: "CPU Only", "Full (No Network)", or "Full"
)
