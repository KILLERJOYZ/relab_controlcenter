package com.example.relab_tool.benchmark.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.relab_tool.benchmark.domain.BenchmarkOrchestrator
import com.example.relab_tool.benchmark.domain.BenchmarkOrchestratorState
import com.example.relab_tool.benchmark.domain.BenchmarkRepository
import com.example.relab_tool.benchmark.domain.model.BenchmarkResult
import com.example.relab_tool.benchmark.domain.model.BenchmarkPillar
import com.example.relab_tool.benchmark.scoring.PercentileCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BenchmarkViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val orchestrator: BenchmarkOrchestrator,
    private val repository: BenchmarkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<BenchmarkUiState>(BenchmarkUiState.Idle)
    val uiState: StateFlow<BenchmarkUiState> = _uiState.asStateFlow()

    val history: StateFlow<List<BenchmarkResult>> = repository.getHistoryFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private var activeJob: Job? = null
    private val percentileCalculator = PercentileCalculator(context)

    fun requestConsent() {
        _uiState.value = BenchmarkUiState.AwaitingConsent
    }

    fun resetToIdle() {
        _uiState.value = BenchmarkUiState.Idle
    }

    fun startFullBenchmark(includeNetwork: Boolean) {
        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            orchestrator.runBenchmark(isQuickTest = false, includeNetwork = includeNetwork).collect { state ->
                when (state) {
                    is BenchmarkOrchestratorState.Running -> {
                        _uiState.value = BenchmarkUiState.Running(
                            currentPillar = state.currentPillar,
                            currentSubTestLabel = state.currentSubTestLabel,
                            pillarProgress = state.pillarProgress,
                            overallProgress = state.overallProgress,
                            completedPillarScores = state.completedPillarScores,
                            thermalStatus = state.thermalStatus,
                            thermalHeadroom = state.thermalHeadroom,
                            estimatedRemainingSeconds = state.estimatedRemainingSeconds,
                            isThermalPaused = state.isThermalPaused,
                            runningHardwareScore = state.runningHardwareScore
                        )
                    }
                    is BenchmarkOrchestratorState.Complete -> {
                        repository.saveResult(state.result)
                        val percentile = percentileCalculator.calculatePercentile(state.result.totalScore)
                        val nearest = percentileCalculator.getNearestReferenceDevice(state.result.totalScore)
                        
                        _uiState.value = BenchmarkUiState.Complete(
                            result = state.result,
                            globalPercentile = percentile,
                            nearestReferenceDevice = nearest
                        )
                    }
                }
            }
        }
    }

    fun startQuickBenchmark() {
        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            orchestrator.runBenchmark(isQuickTest = true, includeNetwork = false).collect { state ->
                when (state) {
                    is BenchmarkOrchestratorState.Running -> {
                        _uiState.value = BenchmarkUiState.Running(
                            currentPillar = state.currentPillar,
                            currentSubTestLabel = state.currentSubTestLabel,
                            pillarProgress = state.pillarProgress,
                            overallProgress = state.overallProgress,
                            completedPillarScores = state.completedPillarScores,
                            thermalStatus = state.thermalStatus,
                            thermalHeadroom = state.thermalHeadroom,
                            estimatedRemainingSeconds = state.estimatedRemainingSeconds,
                            isThermalPaused = state.isThermalPaused,
                            runningHardwareScore = state.runningHardwareScore
                        )
                    }
                    is BenchmarkOrchestratorState.Complete -> {
                        repository.saveResult(state.result)
                        val percentile = percentileCalculator.calculatePercentile(state.result.totalScore)
                        val nearest = percentileCalculator.getNearestReferenceDevice(state.result.totalScore)
                        
                        _uiState.value = BenchmarkUiState.Complete(
                            result = state.result,
                            globalPercentile = percentile,
                            nearestReferenceDevice = nearest
                        )
                    }
                }
            }
        }
    }

    fun startSinglePillar(pillar: BenchmarkPillar) {
        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            orchestrator.runSinglePillar(pillar).collect { state ->
                when (state) {
                    is BenchmarkOrchestratorState.Running -> {
                        _uiState.value = BenchmarkUiState.Running(
                            currentPillar = state.currentPillar,
                            currentSubTestLabel = state.currentSubTestLabel,
                            pillarProgress = state.pillarProgress,
                            overallProgress = state.overallProgress,
                            completedPillarScores = state.completedPillarScores,
                            thermalStatus = state.thermalStatus,
                            thermalHeadroom = state.thermalHeadroom,
                            estimatedRemainingSeconds = state.estimatedRemainingSeconds,
                            isThermalPaused = state.isThermalPaused,
                            runningHardwareScore = state.runningHardwareScore
                        )
                    }
                    is BenchmarkOrchestratorState.Complete -> {
                        repository.saveResult(state.result)
                        val percentile = percentileCalculator.calculatePercentile(state.result.totalScore)
                        val nearest = percentileCalculator.getNearestReferenceDevice(state.result.totalScore)
                        
                        _uiState.value = BenchmarkUiState.Complete(
                            result = state.result,
                            globalPercentile = percentile,
                            nearestReferenceDevice = nearest
                        )
                    }
                }
            }
        }
    }

    fun cancelBenchmark() {
        activeJob?.cancel()
        activeJob = null
        _uiState.value = BenchmarkUiState.Idle
    }

    fun deleteResult(result: BenchmarkResult) {
        viewModelScope.launch {
            repository.deleteResult(result)
        }
    }

    fun selectHistoricalResult(result: BenchmarkResult) {
        val percentile = percentileCalculator.calculatePercentile(result.totalScore)
        val nearest = percentileCalculator.getNearestReferenceDevice(result.totalScore)
        _uiState.value = BenchmarkUiState.Complete(
            result = result,
            globalPercentile = percentile,
            nearestReferenceDevice = nearest
        )
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    override fun onCleared() {
        super.onCleared()
        activeJob?.cancel()
    }
}
