package com.example.relab_tool.benchmark.domain

import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.util.Log
import com.example.relab_tool.benchmark.domain.engine.BenchmarkEngine
import com.example.relab_tool.benchmark.domain.model.*
import com.example.relab_tool.benchmark.scoring.TierClassifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlin.math.roundToInt

class BenchmarkOrchestrator(
    private val context: Context,
    private val engines: List<BenchmarkEngine>
) {
    companion object {
        private const val TAG = "BenchmarkOrchestrator"
    }

    fun runBenchmark(
        isQuickTest: Boolean,
        includeNetwork: Boolean
    ): Flow<BenchmarkOrchestratorState> = flow {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        
        val pillarsToRun = if (isQuickTest) {
            listOf(BenchmarkPillar.CPU_SINGLE_CORE, BenchmarkPillar.CPU_MULTI_CORE)
        } else {
            BenchmarkPillar.entries.filter { pillar ->
                if (!includeNetwork) {
                    pillar != BenchmarkPillar.WIFI &&
                    pillar != BenchmarkPillar.CELLULAR &&
                    pillar != BenchmarkPillar.BROWSER_WEB
                } else {
                    true
                }
            }
        }
        
        val completedScores = mutableListOf<PillarScore>()
        
        emit(BenchmarkOrchestratorState.Running(
            currentPillar = pillarsToRun[0],
            currentSubTestLabel = "Starting...",
            pillarProgress = 0f,
            overallProgress = 0f,
            completedPillarScores = emptyList(),
            thermalStatus = 0,
            thermalHeadroom = 0.3f,
            estimatedRemainingSeconds = pillarsToRun.size * 15,
            isThermalPaused = false,
            runningHardwareScore = 0
        ))
        
        val startTime = System.currentTimeMillis()
        
        for ((index, pillar) in pillarsToRun.withIndex()) {
            val engine = engines.find { it.pillar == pillar }
            
            var isThermalPaused = false
            while (isThermalThrottled(powerManager)) {
                isThermalPaused = true
                val curStatus = getThermalStatus(powerManager)
                val curHeadroom = getThermalHeadroom(powerManager)
                emit(BenchmarkOrchestratorState.Running(
                    currentPillar = pillar,
                    currentSubTestLabel = "Thermal Limit Reached. Cooling down device...",
                    pillarProgress = 0f,
                    overallProgress = index.toFloat() / pillarsToRun.size.toFloat(),
                    completedPillarScores = completedScores.toList(),
                    thermalStatus = curStatus,
                    thermalHeadroom = curHeadroom,
                    estimatedRemainingSeconds = (pillarsToRun.size - index) * 15,
                    isThermalPaused = true,
                    runningHardwareScore = calculateRunningScore(completedScores, pillarsToRun)
                ))
                delay(3000L)
            }
            
            val curStatus = getThermalStatus(powerManager)
            val curHeadroom = getThermalHeadroom(powerManager)
            emit(BenchmarkOrchestratorState.Running(
                currentPillar = pillar,
                currentSubTestLabel = "Running ${pillar.name}...",
                pillarProgress = 0f,
                overallProgress = index.toFloat() / pillarsToRun.size.toFloat(),
                completedPillarScores = completedScores.toList(),
                thermalStatus = curStatus,
                thermalHeadroom = curHeadroom,
                estimatedRemainingSeconds = (pillarsToRun.size - index) * 15,
                isThermalPaused = false,
                runningHardwareScore = calculateRunningScore(completedScores, pillarsToRun)
            ))
            
            val subScores = if (engine != null && engine.isAvailable()) {
                try {
                    engine.run { progress ->
                        // we update sub progress when called
                    }
                } catch (e: Throwable) {
                    Log.e(TAG, "Engine failed for $pillar", e)
                    emptyList()
                }
            } else {
                emptyList()
            }
            
            val isSkipped = subScores.isEmpty()
            val pillarAvgScore = if (subScores.isNotEmpty()) subScores.map { it.score }.average().roundToInt() else 0
            val pScore = PillarScore(pillar, pillarAvgScore, subScores, isSkipped)
            completedScores.add(pScore)
            
            val finalStatus = getThermalStatus(powerManager)
            val finalHeadroom = getThermalHeadroom(powerManager)
            emit(BenchmarkOrchestratorState.Running(
                currentPillar = pillar,
                currentSubTestLabel = "Completed ${pillar.name}",
                pillarProgress = 1.0f,
                overallProgress = (index + 1).toFloat() / pillarsToRun.size.toFloat(),
                completedPillarScores = completedScores.toList(),
                thermalStatus = finalStatus,
                thermalHeadroom = finalHeadroom,
                estimatedRemainingSeconds = (pillarsToRun.size - index - 1) * 15,
                isThermalPaused = false,
                runningHardwareScore = calculateRunningScore(completedScores, pillarsToRun)
            ))
            delay(200L)
        }
        
        val finalResult = compileFinalResult(completedScores, isQuickTest)
        emit(BenchmarkOrchestratorState.Complete(finalResult))
    }.flowOn(Dispatchers.Default)

    private fun isThermalThrottled(pm: PowerManager?): Boolean {
        if (pm == null) return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            pm.currentThermalStatus >= PowerManager.THERMAL_STATUS_SEVERE
        } else {
            false
        }
    }

    private fun getThermalStatus(pm: PowerManager?): Int {
        if (pm == null) return 0
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            pm.currentThermalStatus
        } else {
            0
        }
    }

    private fun getThermalHeadroom(pm: PowerManager?): Float {
        if (pm == null) return 0.3f
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try { pm.getThermalHeadroom(0) } catch (e: Exception) { 0.3f }
        } else {
            0.3f
        }
    }

    private fun calculateRunningScore(completed: List<PillarScore>, pillarsToRun: List<BenchmarkPillar>): Int {
        if (completed.isEmpty()) return 0
        var totalWeight = 0.0f
        var weightedSum = 0.0f
        for (pScore in completed) {
            if (!pScore.isSkipped) {
                weightedSum += pScore.score * pScore.pillar.weight
                totalWeight += pScore.pillar.weight
            }
        }
        if (totalWeight == 0f) return 0
        return (weightedSum / totalWeight * 100.0).roundToInt()
    }

    private fun compileFinalResult(scores: List<PillarScore>, isQuickTest: Boolean): BenchmarkResult {
        var hardwareScoreSum = 0.0f
        var hardwareWeightSum = 0.0f
        var connectivityScoreSum = 0.0f
        var connectivityWeightSum = 0.0f
        
        for (pScore in scores) {
            if (pScore.isSkipped) continue
            val p = pScore.pillar
            val isConnectivity = p == BenchmarkPillar.WIFI || p == BenchmarkPillar.CELLULAR || p == BenchmarkPillar.BROWSER_WEB
            
            if (isConnectivity) {
                connectivityScoreSum += pScore.score * p.weight
                connectivityWeightSum += p.weight
            } else {
                hardwareScoreSum += pScore.score * p.weight
                hardwareWeightSum += p.weight
            }
        }
        
        val hardwareScore = if (hardwareWeightSum > 0) {
            (hardwareScoreSum / hardwareWeightSum * 88.0).roundToInt()
        } else 0
        
        val connectivityScore = if (connectivityWeightSum > 0) {
            (connectivityScoreSum / connectivityWeightSum * 12.0).roundToInt()
        } else 0
        
        val totalScore = hardwareScore + connectivityScore
        
        return BenchmarkResult(
            timestamp = System.currentTimeMillis(),
            deviceModel = Build.MODEL ?: "Unknown Device",
            deviceSoc = Build.HARDWARE ?: "Unknown SoC",
            hardwareScore = hardwareScore,
            connectivityScore = connectivityScore,
            totalScore = totalScore,
            tier = TierClassifier.classify(totalScore),
            pillarScores = scores,
            isQuickTest = isQuickTest
        )
    }
}

sealed interface BenchmarkOrchestratorState {
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
    ) : BenchmarkOrchestratorState
    data class Complete(val result: BenchmarkResult) : BenchmarkOrchestratorState
}
