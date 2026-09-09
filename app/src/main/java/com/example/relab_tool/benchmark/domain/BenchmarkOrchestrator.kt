package com.example.relab_tool.benchmark.domain

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import com.example.relab_tool.benchmark.domain.engine.BenchmarkEngine
import com.example.relab_tool.benchmark.domain.model.*
import com.example.relab_tool.benchmark.scoring.ScoreNormalizer
import com.example.relab_tool.benchmark.scoring.TierClassifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
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
    ): Flow<BenchmarkOrchestratorState> = channelFlow {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager

        // RC-7: 3s stabilization delay — allows governor to reach base clock
        // and ART JIT to quiesce before any measurements begin
        delay(3000L)

        val pillarsToRun = if (isQuickTest) {
            listOf(BenchmarkPillar.CPU_SINGLE_CORE, BenchmarkPillar.CPU_MULTI_CORE)
        } else if (!includeNetwork) {
            BenchmarkPillar.entries.filter { it.weight > 0f && it != BenchmarkPillar.NETWORK_IPC }
        } else {
            BenchmarkPillar.entries.filter { it.weight > 0f }
        }
        
        val completedScores = mutableListOf<PillarScore>()
        
        send(BenchmarkOrchestratorState.Running(
            currentPillar = pillarsToRun[0],
            currentSubTestLabel = "Starting...",
            pillarProgress = 0f,
            overallProgress = 0f,
            completedPillarScores = emptyList(),
            thermalStatus = 0,
            thermalHeadroom = 0.3f,
            estimatedRemainingSeconds = calculateRemainingSeconds(pillarsToRun, 0, 0f),
            isThermalPaused = false,
            runningHardwareScore = 0
        ))
        
        for ((index, pillar) in pillarsToRun.withIndex()) {
            val engine = engines.find { it.pillar == pillar }
            
            var isThermalPaused = false
            while (isThermalThrottled(powerManager)) {
                isThermalPaused = true
                val curStatus = getThermalStatus(powerManager)
                val curHeadroom = getThermalHeadroom(powerManager)
                send(BenchmarkOrchestratorState.Running(
                    currentPillar = pillar,
                    currentSubTestLabel = "Thermal Limit Reached. Cooling down device...",
                    pillarProgress = 0f,
                    overallProgress = index.toFloat() / pillarsToRun.size.toFloat(),
                    completedPillarScores = completedScores.toList(),
                    thermalStatus = curStatus,
                    thermalHeadroom = curHeadroom,
                    estimatedRemainingSeconds = calculateRemainingSeconds(pillarsToRun, index, 0f),
                    isThermalPaused = true,
                    runningHardwareScore = calculateRunningScore(completedScores, pillarsToRun)
                ))
                delay(3000L)
            }
            
            val curStatus = getThermalStatus(powerManager)
            val curHeadroom = getThermalHeadroom(powerManager)
            send(BenchmarkOrchestratorState.Running(
                currentPillar = pillar,
                currentSubTestLabel = "Running ${pillar.name}...",
                pillarProgress = 0f,
                overallProgress = index.toFloat() / pillarsToRun.size.toFloat(),
                completedPillarScores = completedScores.toList(),
                thermalStatus = curStatus,
                thermalHeadroom = curHeadroom,
                estimatedRemainingSeconds = calculateRemainingSeconds(pillarsToRun, index, 0f),
                isThermalPaused = false,
                runningHardwareScore = calculateRunningScore(completedScores, pillarsToRun)
            ))
            
            val subScores = if (engine != null && engine.isAvailable()) {
                try {
                    engine.run { progress ->
                        val currentStatus = getThermalStatus(powerManager)
                        val currentHeadroom = getThermalHeadroom(powerManager)
                        send(BenchmarkOrchestratorState.Running(
                            currentPillar = pillar,
                            currentSubTestLabel = "Running ${pillar.name}...",
                            pillarProgress = progress,
                            overallProgress = (index.toFloat() + progress) / pillarsToRun.size.toFloat(),
                            completedPillarScores = completedScores.toList(),
                            thermalStatus = currentStatus,
                            thermalHeadroom = currentHeadroom,
                            estimatedRemainingSeconds = calculateRemainingSeconds(pillarsToRun, index, progress),
                            isThermalPaused = false,
                            runningHardwareScore = calculateRunningScore(completedScores, pillarsToRun)
                        ))
                    }
                } catch (e: Throwable) {
                    Log.e(TAG, "Engine failed for $pillar", e)
                    emptyList()
                }
            } else {
                emptyList()
            }
            
            val isSkipped = subScores.isEmpty()
            // Fix-B: exclude zero-score (failed/partial) sub-scores from geo mean
            // to avoid artificially elevating the pillar via coerceAtLeast(1) trick
            val validSubs = subScores.filter { it.score > 0 && !it.isPartial }
            val pillarGeoScore = when {
                validSubs.isNotEmpty() -> ScoreNormalizer.geometricMean(validSubs.map { it.score }).roundToInt()
                subScores.isNotEmpty() -> ScoreNormalizer.geometricMean(subScores.map { it.score.coerceAtLeast(1) }).roundToInt()
                else -> 0
            }
            val pScore = PillarScore(pillar, pillarGeoScore, subScores, isSkipped)
            completedScores.add(pScore)
            
            val finalStatus = getThermalStatus(powerManager)
            val finalHeadroom = getThermalHeadroom(powerManager)
            send(BenchmarkOrchestratorState.Running(
                currentPillar = pillar,
                currentSubTestLabel = "Completed ${pillar.name}",
                pillarProgress = 1.0f,
                overallProgress = (index + 1).toFloat() / pillarsToRun.size.toFloat(),
                completedPillarScores = completedScores.toList(),
                thermalStatus = finalStatus,
                thermalHeadroom = finalHeadroom,
                estimatedRemainingSeconds = calculateRemainingSeconds(pillarsToRun, index, 1.0f),
                isThermalPaused = false,
                runningHardwareScore = calculateRunningScore(completedScores, pillarsToRun)
            ))
            delay(200L)
        }
        
        val finalResult = compileFinalResult(completedScores, isQuickTest)
        send(BenchmarkOrchestratorState.Complete(finalResult))
    }.flowOn(Dispatchers.Default)

    fun runSinglePillar(pillar: BenchmarkPillar): Flow<BenchmarkOrchestratorState> = channelFlow {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val completedScores = mutableListOf<PillarScore>()
        
        send(BenchmarkOrchestratorState.Running(
            currentPillar = pillar,
            currentSubTestLabel = "Starting...",
            pillarProgress = 0f,
            overallProgress = 0f,
            completedPillarScores = emptyList(),
            thermalStatus = 0,
            thermalHeadroom = 0.3f,
            estimatedRemainingSeconds = getPillarEstimatedDuration(pillar),
            isThermalPaused = false,
            runningHardwareScore = 0
        ))
        
        val engine = engines.find { it.pillar == pillar }
        
        var isThermalPaused = false
        while (isThermalThrottled(powerManager)) {
            isThermalPaused = true
            val curStatus = getThermalStatus(powerManager)
            val curHeadroom = getThermalHeadroom(powerManager)
            send(BenchmarkOrchestratorState.Running(
                currentPillar = pillar,
                currentSubTestLabel = "Thermal Limit Reached. Cooling down device...",
                pillarProgress = 0f,
                overallProgress = 0f,
                completedPillarScores = completedScores.toList(),
                thermalStatus = curStatus,
                thermalHeadroom = curHeadroom,
                estimatedRemainingSeconds = getPillarEstimatedDuration(pillar),
                isThermalPaused = true,
                runningHardwareScore = 0
            ))
            delay(3000L)
        }
        
        val curStatus = getThermalStatus(powerManager)
        val curHeadroom = getThermalHeadroom(powerManager)
        send(BenchmarkOrchestratorState.Running(
            currentPillar = pillar,
            currentSubTestLabel = "Running ${pillar.name}...",
            pillarProgress = 0f,
            overallProgress = 0f,
            completedPillarScores = completedScores.toList(),
            thermalStatus = curStatus,
            thermalHeadroom = curHeadroom,
            estimatedRemainingSeconds = getPillarEstimatedDuration(pillar),
            isThermalPaused = false,
            runningHardwareScore = 0
        ))
        
        val subScores = if (engine != null && engine.isAvailable()) {
            try {
                engine.run { progress ->
                    val cStatus = getThermalStatus(powerManager)
                    val cHeadroom = getThermalHeadroom(powerManager)
                    send(BenchmarkOrchestratorState.Running(
                        currentPillar = pillar,
                        currentSubTestLabel = "Running ${pillar.name}...",
                        pillarProgress = progress,
                        overallProgress = progress,
                        completedPillarScores = completedScores.toList(),
                        thermalStatus = cStatus,
                        thermalHeadroom = cHeadroom,
                        estimatedRemainingSeconds = ((1f - progress) * getPillarEstimatedDuration(pillar)).roundToInt().coerceAtLeast(0),
                        isThermalPaused = false,
                        runningHardwareScore = 0
                    ))
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Engine failed for $pillar", e)
                emptyList()
            }
        } else {
            emptyList()
        }
        
        val isSkipped = subScores.isEmpty()
        // Fix-B: exclude zero-score (failed/partial) sub-scores from geo mean
        // (same logic as runBenchmark path)
        val validSubs = subScores.filter { it.score > 0 && !it.isPartial }
        val pillarGeoScore = when {
            validSubs.isNotEmpty() -> ScoreNormalizer.geometricMean(validSubs.map { it.score }).roundToInt()
            subScores.isNotEmpty() -> ScoreNormalizer.geometricMean(subScores.map { it.score.coerceAtLeast(1) }).roundToInt()
            else -> 0
        }
        val pScore = PillarScore(pillar, pillarGeoScore, subScores, isSkipped)
        completedScores.add(pScore)
        
        val finalStatus = getThermalStatus(powerManager)
        val finalHeadroom = getThermalHeadroom(powerManager)
        send(BenchmarkOrchestratorState.Running(
            currentPillar = pillar,
            currentSubTestLabel = "Completed ${pillar.name}",
            pillarProgress = 1.0f,
            overallProgress = 1.0f,
            completedPillarScores = completedScores.toList(),
            thermalStatus = finalStatus,
            thermalHeadroom = finalHeadroom,
            estimatedRemainingSeconds = 0,
            isThermalPaused = false,
            runningHardwareScore = 0
        ))
        delay(200L)
        
        val finalResult = compileFinalResult(completedScores, isQuickTest = false)
        send(BenchmarkOrchestratorState.Complete(finalResult))
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

    /**
     * Rate-limited thermal headroom reading.
     * Android rate-limits getThermalHeadroom() — calling too frequently returns NaN.
     * We cache the result and only re-read every 10 seconds (PR-03 fix).
     */
    private var lastHeadroomReadTimeMs = 0L
    private var cachedHeadroom = 0.3f

    private fun getThermalHeadroom(pm: PowerManager?): Float {
        if (pm == null) return 0.3f
        val now = System.currentTimeMillis()
        if (now - lastHeadroomReadTimeMs < 10_000L) return cachedHeadroom
        lastHeadroomReadTimeMs = now
        cachedHeadroom = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val h = pm.getThermalHeadroom(10) // 10s forecast (PR-03)
                if (h.isNaN() || h <= 0f) 0.3f else h
            } catch (e: Exception) { 0.3f }
        } else {
            0.3f
        }
        return cachedHeadroom
    }

    /**
     * Running score uses weighted geometric mean of completed pillars.
     * Returns the current estimated total score on a [0, 1_000_000] scale.
     */
    private fun calculateRunningScore(completed: List<PillarScore>, pillarsToRun: List<BenchmarkPillar>): Int {
        if (completed.isEmpty()) return 0
        val pillarGeoMeans = completed
            .filter { !it.isSkipped }
            .map { pScore ->
                // Fix-B: Match compileFinalResult — exclude zero-score and partial sub-scores
                val validSubs = pScore.subScores.filter { it.score > 0 && !it.isPartial }
                val geoMean = if (validSubs.isNotEmpty()) {
                    ScoreNormalizer.geometricMean(validSubs.map { it.score })
                } else {
                    ScoreNormalizer.geometricMean(pScore.subScores.map { it.score.coerceAtLeast(1) })
                }
                Pair(pScore.pillar.weight, geoMean)
            }
        return ScoreNormalizer.computeFinalScore(pillarGeoMeans)
    }

    /**
     * Compile final benchmark result using:
     *   1. Geometric mean of sub-scores within each pillar (already done at PillarScore creation)
     *   2. Weighted geometric mean across all 7 pillars via ScoreNormalizer.computeFinalScore()
     *   3. Thermal/energy penalty via ScoreNormalizer.applyDynamicCoefficients()
     */
    private fun compileFinalResult(scores: List<PillarScore>, isQuickTest: Boolean): BenchmarkResult {
        // Weighted geometric mean across all active pillars (Fix-B: skip zero-score subs)
        val pillarGeoMeans = scores
            .filter { !it.isSkipped }
            .map { pScore ->
                val validSubs = pScore.subScores.filter { it.score > 0 && !it.isPartial }
                val geoMean = if (validSubs.isNotEmpty()) {
                    ScoreNormalizer.geometricMean(validSubs.map { it.score })
                } else {
                    ScoreNormalizer.geometricMean(pScore.subScores.map { it.score.coerceAtLeast(1) })
                }
                Pair(pScore.pillar.weight, geoMean)
            }
        val rawScore = ScoreNormalizer.computeFinalScore(pillarGeoMeans)

        // RC-2: Apply thermal coefficient — narrowed to [0.85, 1.0], max 15% penalty
        val thermalCoeff = computeThermalCoefficient()
        val energyCoeff = computeEnergyCoefficient()
        val thermalAdjusted = ScoreNormalizer.applyDynamicCoefficients(rawScore, thermalCoeff, energyCoeff)
        // RC-2: Floor totalScore at 85% of rawScore — thermal can't invert silicon rankings
        val totalScore = thermalAdjusted.coerceAtLeast((rawScore * 0.85).toInt())

        // RC-11: Determine run scope for display badge
        val runScope = when {
            isQuickTest -> "CPU Only"
            scores.none { it.pillar == BenchmarkPillar.NETWORK_IPC && !it.isSkipped } -> "Full (No Network)"
            else -> "Full"
        }

        return BenchmarkResult(
            timestamp = System.currentTimeMillis(),
            deviceModel = Build.MODEL ?: "Unknown Device",
            deviceSoc = Build.HARDWARE ?: "Unknown SoC",
            hardwareScore = rawScore,
            connectivityScore = 0,
            totalScore = totalScore,
            tier = TierClassifier.classify(totalScore),
            pillarScores = scores,
            isQuickTest = isQuickTest,
            runScope = runScope
        )
    }

    /**
     * Convert thermal headroom to a penalty coefficient [0.4, 1.0].
     * headroom near 0.0 → device is severely throttled → heavy penalty.
     * headroom > 0.8 → device is cool → no penalty.
     */
    private fun computeThermalCoefficient(): Float {
        val h = cachedHeadroom.coerceIn(0f, 1f)
        // RC-2: Narrowed band — max 15% penalty prevents tablet/phone inversion
        // h=1.0 (cool)  → 1.00 (no penalty)
        // h=0.0 (severe throttle) → 0.85 (−15% max)
        return (0.85f + 0.15f * h).coerceIn(0.85f, 1.0f)
    }

    /**
     * Read battery energy consumption if available.
     * Returns coefficient in [0.5, 1.5].
     * Default 1.0 when data is unavailable.
     */
    private fun computeEnergyCoefficient(): Float {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return 1.0f
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val energyUwh = bm?.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER)
            // If energy data is unavailable or unsupported, return neutral coefficient
            if (energyUwh == null || energyUwh <= 0L) 1.0f else 1.0f
            // Note: Full energy efficiency scoring requires measuring delta energy
            // across the entire benchmark run. For now, return neutral.
        } catch (_: Exception) { 1.0f }
    }

    private fun getPillarEstimatedDuration(pillar: BenchmarkPillar): Int {
        return when (pillar) {
            BenchmarkPillar.CPU_SINGLE_CORE -> 120
            BenchmarkPillar.CPU_MULTI_CORE  -> 120
            BenchmarkPillar.GPU_OPENGL      -> 180
            BenchmarkPillar.GPU_VULKAN      -> 180
            BenchmarkPillar.STORAGE_IO      -> 150
            BenchmarkPillar.VIDEO_CODEC     -> 120
            BenchmarkPillar.NETWORK_IPC     -> 90
            else -> 0 // Legacy stub pillars — not registered in DI
        }
    }

    private fun calculateRemainingSeconds(
        pillars: List<BenchmarkPillar>,
        currentIndex: Int,
        currentProgress: Float
    ): Int {
        var seconds = 0.0f
        for (i in currentIndex until pillars.size) {
            val duration = getPillarEstimatedDuration(pillars[i]).toFloat()
            if (i == currentIndex) {
                seconds += (1.0f - currentProgress) * duration
            } else {
                seconds += duration
            }
        }
        return seconds.roundToInt().coerceAtLeast(0)
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
