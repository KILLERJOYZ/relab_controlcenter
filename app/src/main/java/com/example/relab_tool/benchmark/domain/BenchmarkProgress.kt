package com.example.relab_tool.benchmark.domain

import com.example.relab_tool.benchmark.domain.model.BenchmarkPillar
import com.example.relab_tool.benchmark.domain.model.PillarScore

data class BenchmarkProgress(
    val currentPillar: BenchmarkPillar,
    val currentSubTestLabel: String,
    val pillarProgress: Float,
    val overallProgress: Float,
    val completedPillarScores: List<PillarScore>,
    val runningHardwareScore: Int
)
