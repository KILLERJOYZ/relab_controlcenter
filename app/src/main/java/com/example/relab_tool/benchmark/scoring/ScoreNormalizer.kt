package com.example.relab_tool.benchmark.scoring

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow

/**
 * ScoreNormalizer — converts raw benchmark measurements to normalized sub-scores.
 *
 * Each sub-test produces a score in the range [0, 10_000].
 * The final pillar score is the geometric mean of its 20 sub-scores.
 * The overall benchmark score is the weighted geometric mean of all 7 pillar scores,
 * scaled to the [0, 1_000_000] range.
 *
 * Geometric mean is used because:
 *   1. It prevents a single outlier test from dominating the total.
 *   2. It penalises near-zero scores proportionally, reflecting real capability gaps.
 *   3. It is the de facto standard in Geekbench, SPECint, and similar suites.
 *
 * Score mapping:
 *   rawValue >= cap     → 10,000 (best possible for this class of hardware)
 *   rawValue == baseline→  5,000 (midpoint — matches SD 778G / Pixel 6 class)
 *   rawValue <= 0       →      0
 */
object ScoreNormalizer {

    private const val MAX_SCORE = 10_000
    private const val MID_SCORE = 5_000

    /**
     * Normalize a single sub-test result to [0, 10_000].
     *
     * @param rawValue   The measured metric (higher = better unless [isInverted]).
     * @param baseline   The reference value that maps to exactly 5,000 points
     *                   (represents a mid-range SoC like Snapdragon 778G / Dimensity 8100).
     * @param cap        The saturation value that maps to 10,000 points
     *                   (represents a high-end SoC like Snapdragon 8 Gen 3 or better).
     * @param isInverted True when lower rawValue = better (e.g. latency in ms, time in seconds).
     */
    fun normalize(rawValue: Double, baseline: Double, cap: Double, isInverted: Boolean): Int {
        if (isInverted) {
            // Lower is better.
            // Define a "floor" (worst case) = baseline + 2.5 × (baseline - cap)
            val diff = (baseline - cap).coerceAtLeast(0.001)
            val worst = baseline + 2.5 * diff
            if (rawValue <= cap) return MAX_SCORE
            if (rawValue >= worst) return 0
            return if (rawValue <= baseline) {
                val fraction = (rawValue - cap) / diff
                (MAX_SCORE - fraction * MID_SCORE).toInt()
            } else {
                val fraction = (rawValue - baseline) / (worst - baseline)
                (MID_SCORE - fraction * MID_SCORE).toInt()
            }.coerceIn(0, MAX_SCORE)
        } else {
            // Higher is better.
            if (rawValue >= cap) return MAX_SCORE
            if (rawValue <= 0.0) return 0
            return if (rawValue <= baseline) {
                val fraction = rawValue / baseline
                (fraction * MID_SCORE).toInt()
            } else {
                val fraction = (rawValue - baseline) / (cap - baseline).coerceAtLeast(0.001)
                (MID_SCORE + fraction * MID_SCORE).toInt()
            }.coerceIn(0, MAX_SCORE)
        }
    }

    /**
     * Compute the geometric mean of a list of integer sub-scores.
     * Returns 0.0 if the list is empty.
     * Scores of 0 are replaced with 1 to prevent ln(0) → -Infinity.
     */
    fun geometricMean(scores: List<Int>): Double {
        if (scores.isEmpty()) return 0.0
        val logSum = scores.sumOf { ln(it.toDouble().coerceAtLeast(1.0)) }
        return exp(logSum / scores.size)
    }

    /**
     * Aggregate all 7 pillar geometric-mean scores into the final 1,000,000-point total.
     *
     * @param pillarGeoMeans Map of (weight, geometricMeanScore) per pillar.
     * @return Final score in [0, 1_000_000].
     */
    fun computeFinalScore(pillarGeoMeans: List<Pair<Float, Double>>): Int {
        if (pillarGeoMeans.isEmpty()) return 0
        var totalWeight = 0.0
        var weightedLogSum = 0.0
        for ((weight, geoMean) in pillarGeoMeans) {
            if (weight > 0 && geoMean > 0) {
                weightedLogSum += weight * ln(geoMean.coerceAtLeast(1.0))
                totalWeight += weight
            }
        }
        if (totalWeight == 0.0) return 0
        val compositeGeoMean = exp(weightedLogSum / totalWeight)
        // compositeGeoMean is in [0, 10_000] range. Scale to [0, 1_000_000].
        return (compositeGeoMean / MAX_SCORE * 1_000_000).toInt().coerceIn(0, 1_000_000)
    }

    /**
     * Apply 3-factor scoring:
     *   finalScore = rawScore × thermalCoeff × energyCoeff
     *
     * @param rawScore         Score from [computeFinalScore].
     * @param thermalCoeff     From ThermalEnergyMonitor: [0.4, 1.0]. Default 1.0 if not measured.
     * @param energyCoeff      From ThermalEnergyMonitor: [0.5, 1.5]. Default 1.0 if not measured.
     */
    fun applyDynamicCoefficients(rawScore: Int, thermalCoeff: Float, energyCoeff: Float): Int {
        val adjusted = rawScore * thermalCoeff * energyCoeff
        return adjusted.toInt().coerceIn(0, 1_000_000)
    }
}
