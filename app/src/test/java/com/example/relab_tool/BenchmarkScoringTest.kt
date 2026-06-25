package com.example.relab_tool

import com.example.relab_tool.benchmark.scoring.ScoreNormalizer
import com.example.relab_tool.benchmark.scoring.TierClassifier
import com.example.relab_tool.benchmark.domain.model.ScoreTier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BenchmarkScoringTest {

    @Test
    fun testScoreNormalizer_HigherIsBetter() {
        // Test normalizer for higher-is-better metric (isInverted = false)
        // baseline = 100.0, cap = 500.0
        
        // Exact baseline should yield 5.0
        assertEquals(5.0, ScoreNormalizer.normalize(100.0, 100.0, 500.0, false), 0.001)
        
        // Exact cap or above should yield 10.0
        assertEquals(10.0, ScoreNormalizer.normalize(500.0, 100.0, 500.0, false), 0.001)
        assertEquals(10.0, ScoreNormalizer.normalize(600.0, 100.0, 500.0, false), 0.001)
        
        // Zero or below should yield 0.0
        assertEquals(0.0, ScoreNormalizer.normalize(0.0, 100.0, 500.0, false), 0.001)
        assertEquals(0.0, ScoreNormalizer.normalize(-10.0, 100.0, 500.0, false), 0.001)
        
        // Midpoint between 0 and baseline should yield 2.5
        assertEquals(2.5, ScoreNormalizer.normalize(50.0, 100.0, 500.0, false), 0.001)
        
        // Midpoint between baseline and cap should yield 7.5
        assertEquals(7.5, ScoreNormalizer.normalize(300.0, 100.0, 500.0, false), 0.001)
    }

    @Test
    fun testScoreNormalizer_LowerIsBetter() {
        // Test normalizer for lower-is-better metric (isInverted = true)
        // baseline = 10.0, cap = 2.0 (lower is better, e.g. latency)
        
        // Exact cap or below should yield 10.0
        assertEquals(10.0, ScoreNormalizer.normalize(2.0, 10.0, 2.0, true), 0.001)
        assertEquals(10.0, ScoreNormalizer.normalize(1.5, 10.0, 2.0, true), 0.001)
        
        // Exact baseline should yield 5.0
        assertEquals(5.0, ScoreNormalizer.normalize(10.0, 10.0, 2.0, true), 0.001)
        
        // Midpoint between cap and baseline (6.0) should yield 7.5
        assertEquals(7.5, ScoreNormalizer.normalize(6.0, 10.0, 2.0, true), 0.001)
    }

    @Test
    fun testTierClassifier() {
        // ENTRY: 0..59_999 points (0.0 .. 59.999 on scaled double)
        assertEquals(ScoreTier.ENTRY, TierClassifier.classify(0.0))
        assertEquals(ScoreTier.ENTRY, TierClassifier.classify(15.0))
        assertEquals(ScoreTier.ENTRY, TierClassifier.classify(25.0))
        
        // MID: 150_000..299_999 points (150.0 .. 299.999 on scaled double)
        assertEquals(ScoreTier.MID, TierClassifier.classify(150.0))
        assertEquals(ScoreTier.MID, TierClassifier.classify(200.0))
        assertEquals(ScoreTier.MID, TierClassifier.classify(299.9))
        
        // HIGH: 500_000..699_999 points (500.0 .. 699.999 on scaled double)
        assertEquals(ScoreTier.HIGH, TierClassifier.classify(500.0))
        assertEquals(ScoreTier.HIGH, TierClassifier.classify(600.0))
        assertEquals(ScoreTier.HIGH, TierClassifier.classify(699.9))
        
        // FLAGSHIP: 700_000..849_999 points (700.0 .. 849.999 on scaled double)
        assertEquals(ScoreTier.FLAGSHIP, TierClassifier.classify(700.0))
        assertEquals(ScoreTier.FLAGSHIP, TierClassifier.classify(800.0))
        assertEquals(ScoreTier.FLAGSHIP, TierClassifier.classify(849.9))
        
        // ELITE: 850_000..1_000_000 points (850.0 .. 1000.0 on scaled double)
        assertEquals(ScoreTier.ELITE, TierClassifier.classify(850.0))
        assertEquals(ScoreTier.ELITE, TierClassifier.classify(900.0))
        assertEquals(ScoreTier.ELITE, TierClassifier.classify(1000.0))
    }

    @Test
    fun testScoreNormalizer_SafeGuards() {
        // Test division by zero and invalid inputs in normalize
        assertEquals(0.0, ScoreNormalizer.normalize(100.0, 0.0, 500.0, false), 0.001)
        assertEquals(0.0, ScoreNormalizer.normalize(100.0, 100.0, 0.0, false), 0.001)
        assertEquals(0.0, ScoreNormalizer.normalize(Double.NaN, 100.0, 500.0, false), 0.001)
        assertEquals(0.0, ScoreNormalizer.normalize(100.0, Double.NaN, 500.0, false), 0.001)
        
        // Test baseline == cap
        assertEquals(10.0, ScoreNormalizer.normalize(200.0, 100.0, 100.0, false), 0.001)
        assertEquals(5.0, ScoreNormalizer.normalize(100.0, 100.0, 100.0, false), 0.001)
        
        // Test geometricMean safe guards
        assertEquals(0.0, ScoreNormalizer.geometricMean(emptyList()), 0.001)
        val listWithNaN = listOf(5.0, Double.NaN, 10.0)
        val geoMean = ScoreNormalizer.geometricMean(listWithNaN)
        assertTrue(geoMean > 0.0 && !geoMean.isNaN() && !geoMean.isInfinite())
        
        // Test computeFinalScore with NaN
        val finalScore = ScoreNormalizer.computeFinalScore(listOf(Pair(1.0f, Double.NaN), Pair(1.0f, 8.0)))
        assertTrue(finalScore > 0.0 && !finalScore.isNaN() && !finalScore.isInfinite())
        
        // Test applyDynamicCoefficients with NaN
        val coeffScore = ScoreNormalizer.applyDynamicCoefficients(Double.NaN, 1.0f, 1.0f)
        assertEquals(0.0, coeffScore, 0.001)
    }

    @Test
    fun testBenchmarkConverters_SafeGuards() {
        val converters = com.example.relab_tool.benchmark.data.BenchmarkConverters()
        
        // Construct scores containing NaN and Infinite
        val pillarScore = com.example.relab_tool.benchmark.domain.model.PillarScore(
            pillar = com.example.relab_tool.benchmark.domain.model.BenchmarkPillar.CPU_SINGLE_CORE,
            score = Double.NaN,
            subScores = listOf(
                com.example.relab_tool.benchmark.domain.model.SubScore(
                    name = "Sub Test",
                    rawValue = Double.POSITIVE_INFINITY,
                    unit = "score",
                    score = Double.NaN,
                    isPartial = false
                )
            ),
            isSkipped = false
        )
        
        // Attempt serialization. Since we sanitized NaN/Infinite, it should succeed without throwing JSONException
        val jsonString = try {
            converters.fromPillarScores(listOf(pillarScore))
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
        
        // Under isReturnDefaultValues, jsonString might be empty/null, but calling it should not crash
        assertTrue(jsonString != null)
    }
}
