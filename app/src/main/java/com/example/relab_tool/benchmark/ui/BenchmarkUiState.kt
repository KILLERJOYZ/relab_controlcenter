package com.example.relab_tool.benchmark.ui

import com.example.relab_tool.benchmark.domain.model.BenchmarkPillar
import com.example.relab_tool.benchmark.domain.model.BenchmarkResult
import com.example.relab_tool.benchmark.domain.model.PillarScore

sealed interface BenchmarkUiState {
    data object Idle : BenchmarkUiState
    data object AwaitingConsent : BenchmarkUiState
    data class Running(
        val currentPillar: BenchmarkPillar,
        val currentSubTestLabel: String,
        val pillarProgress: Float,
        val overallProgress: Float,
        val completedPillarScores: List<PillarScore>,
        val thermalStatus: Int,
        val thermalHeadroom: Float,
        val estimatedRemainingSeconds: Int,
        val isThermalPaused: Boolean,
        val runningHardwareScore: Int
    ) : BenchmarkUiState
    data class Complete(
        val result: BenchmarkResult
    ) : BenchmarkUiState
    data class Error(val message: String, val isRecoverable: Boolean = true) : BenchmarkUiState
}
