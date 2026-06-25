package com.example.relab_tool.benchmark.scoring

import com.example.relab_tool.benchmark.domain.model.SubScore
import java.util.Locale
import kotlin.math.exp
import kotlin.math.ln

/**
 * ScoreNormalizer — converts raw benchmark measurements to normalized sub-scores.
 *
 * Each sub-test produces a score in the range [0.0, 10.0].
 * The final pillar score is the geometric mean of its 20 sub-scores.
 * The overall benchmark score is the weighted geometric mean of all 7 pillar scores,
 * scaled to the [0.0, 1000.0] range.
 */
object ScoreNormalizer {

    /**
     * Corrected piecewise normalization calculation.
     * Maps baseline performance to exactly 5.0 pts, and cap performance to 10.0 pts.
     */
    fun normalize(raw: Double, baseline: Double, cap: Double, inverted: Boolean): Double {
        if (raw.isNaN() || raw < 0.0) return 0.0
        if (baseline.isNaN() || baseline <= 0.0) return 0.0
        if (cap.isNaN() || cap <= 0.0) return 0.0

        val result = if (inverted) {
            // Lower values are better (e.g., latency, execution time)
            if (raw >= baseline) {
                // Performance is worse than or equal to baseline (Scale 0.0 to 5.0)
                val score = (2.0 - (raw / baseline)) * 5.0
                if (score.isNaN()) 0.0 else score.coerceIn(0.0, 5.0)
            } else {
                // Performance is better than baseline (Scale 5.0 to 10.0)
                if (raw <= cap) 10.0
                else if (baseline == cap) 5.0
                else {
                    val score = 5.0 + 5.0 * ((baseline - raw) / (baseline - cap))
                    if (score.isNaN()) 5.0 else score
                }
            }
        } else {
            // Higher values are better (e.g., throughput, FPS, GFLOPS)
            if (raw <= baseline) {
                // Performance is worse than or equal to baseline (Scale 0.0 to 5.0)
                val score = (raw / baseline) * 5.0
                if (score.isNaN()) 0.0 else score
            } else {
                // Performance is better than baseline (Scale 5.0 to 10.0)
                if (raw >= cap) 10.0
                else if (cap == baseline) 5.0
                else {
                    val score = 5.0 + 5.0 * ((raw - baseline) / (cap - baseline))
                    if (score.isNaN()) 5.0 else score
                }
            }
        }
        return if (result.isNaN() || result.isInfinite()) 0.0 else result.coerceIn(0.0, 10.0)
    }

    /**
     * Formats scores using Locale.US to prevent formatting errors across regions.
     * This avoids crude string replacements like.replace("0.", "") that strip leading zeros.
     */
    fun createSubScore(
        name: String,
        raw: Double,
        unit: String,
        baseline: Double,
        cap: Double,
        inverted: Boolean,
        hasError: Boolean
    ): SubScore {
        val normalized = normalize(raw, baseline, cap, inverted)
        // Ensure decimal values under 1.0 are printed correctly as "0.XXX"
        val formattedValue = String.format(Locale.US, "%.3f", normalized)
        val scoreVal = formattedValue.toDoubleOrNull() ?: normalized

        return SubScore(
            name = name,
            rawValue = raw,
            unit = unit,
            score = if (scoreVal.isNaN() || scoreVal.isInfinite()) 0.0 else scoreVal,
            isPartial = hasError || raw == 0.0 || raw.isNaN() || raw.isInfinite()
        )
    }

    /**
     * Compute the geometric mean of a list of Double sub-scores.
     * Returns 0.0 if the list is empty.
     * Scores near 0 are coerced to a very small decimal instead of 1.0 to prevent ln(0) -> -Infinity.
     */
    fun geometricMean(scores: List<Double>): Double {
        val safeScores = scores.map {
            if (it.isNaN() || it.isInfinite()) 0.001
            else it.coerceAtLeast(0.001)
        }
        if (safeScores.isEmpty()) return 0.0
        val logSum = safeScores.sumOf { ln(it) }
        val result = exp(logSum / safeScores.size)
        return if (result.isNaN() || result.isInfinite()) 0.0 else result
    }

    /**
     * Aggregate all 7 pillar geometric-mean scores into the final 1,000-point total.
     *
     * @param pillarGeoMeans Map of (weight, geometricMeanScore) per pillar.
     * @return Final score in [0.0, 1000.0].
     */
    fun computeFinalScore(pillarGeoMeans: List<Pair<Float, Double>>): Double {
        if (pillarGeoMeans.isEmpty()) return 0.0
        var totalWeight = 0.0
        var weightedLogSum = 0.0
        for ((weight, geoMean) in pillarGeoMeans) {
            val safeWeight = if (weight.isNaN() || weight.isInfinite()) 0.0 else weight.toDouble()
            val safeGeoMean = if (geoMean.isNaN() || geoMean.isInfinite()) 0.001 else geoMean.coerceAtLeast(0.001)
            if (safeWeight > 0.0 && safeGeoMean > 0.0) {
                weightedLogSum += safeWeight * ln(safeGeoMean)
                totalWeight += safeWeight
            }
        }
        if (totalWeight == 0.0) return 0.0
        val compositeGeoMean = exp(weightedLogSum / totalWeight)
        if (compositeGeoMean.isNaN() || compositeGeoMean.isInfinite()) return 0.0
        // compositeGeoMean is in [0.0, 10.0] range. Scale to [0.0, 1000.0] by multiplying by 100.
        return (compositeGeoMean * 100.0).coerceIn(0.0, 1000.0)
    }

    /**
     * Apply 3-factor scoring:
     *   finalScore = rawScore × thermalCoeff × energyCoeff
     */
    fun applyDynamicCoefficients(rawScore: Double, thermalCoeff: Float, energyCoeff: Float): Double {
        val safeRaw = if (rawScore.isNaN() || rawScore.isInfinite()) 0.0 else rawScore
        val safeThermal = if (thermalCoeff.isNaN() || thermalCoeff.isInfinite()) 1.0f else thermalCoeff
        val safeEnergy = if (energyCoeff.isNaN() || energyCoeff.isInfinite()) 1.0f else energyCoeff
        val adjusted = safeRaw * safeThermal * safeEnergy
        return if (adjusted.isNaN() || adjusted.isInfinite()) 0.0 else adjusted.coerceIn(0.0, 1000.0)
    }
}
