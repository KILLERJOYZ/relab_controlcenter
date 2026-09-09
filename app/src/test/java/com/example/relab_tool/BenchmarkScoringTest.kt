package com.example.relab_tool

import com.example.relab_tool.benchmark.scoring.ScoreNormalizer
import com.example.relab_tool.benchmark.scoring.TierClassifier
import com.example.relab_tool.benchmark.domain.model.ScoreTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BenchmarkScoringTest {

    @Test
    fun testScoreNormalizer_HigherIsBetter() {
        // Test normalizer for higher-is-better metric (isInverted = false)
        // baseline = 100.0, cap = 500.0
        
        // Exact baseline should yield 500
        assertEquals(500, ScoreNormalizer.normalize(100.0, 100.0, 500.0, false))
        
        // Exact cap or above should yield 1000
        assertEquals(1000, ScoreNormalizer.normalize(500.0, 100.0, 500.0, false))
        assertEquals(1000, ScoreNormalizer.normalize(600.0, 100.0, 500.0, false))
        
        // Zero or below should yield 0
        assertEquals(0, ScoreNormalizer.normalize(0.0, 100.0, 500.0, false))
        assertEquals(0, ScoreNormalizer.normalize(-10.0, 100.0, 500.0, false))
        
        // Midpoint between 0 and baseline should yield 250
        assertEquals(250, ScoreNormalizer.normalize(50.0, 100.0, 500.0, false))
        
        // Midpoint between baseline and cap should yield 750
        assertEquals(750, ScoreNormalizer.normalize(300.0, 100.0, 500.0, false))
    }

    @Test
    fun testScoreNormalizer_LowerIsBetter() {
        // Test normalizer for lower-is-better metric (isInverted = true)
        // baseline = 10.0, cap = 2.0 (lower is better, e.g. latency)
        
        // Exact cap or below should yield 1000
        assertEquals(1000, ScoreNormalizer.normalize(2.0, 10.0, 2.0, true))
        assertEquals(1000, ScoreNormalizer.normalize(1.5, 10.0, 2.0, true))
        
        // Exact baseline should yield 500
        assertEquals(500, ScoreNormalizer.normalize(10.0, 10.0, 2.0, true))
        
        // Midpoint between cap and baseline (6.0) should yield 750
        assertEquals(750, ScoreNormalizer.normalize(6.0, 10.0, 2.0, true))
    }

    @Test
    fun testTierClassifier() {
        // ENTRY: 0..25000
        assertEquals(ScoreTier.ENTRY, TierClassifier.classify(0))
        assertEquals(ScoreTier.ENTRY, TierClassifier.classify(15000))
        assertEquals(ScoreTier.ENTRY, TierClassifier.classify(25000))
        
        // MID: 25001..55000
        assertEquals(ScoreTier.MID, TierClassifier.classify(25001))
        assertEquals(ScoreTier.MID, TierClassifier.classify(40000))
        assertEquals(ScoreTier.MID, TierClassifier.classify(55000))
        
        // HIGH: 55001..80000
        assertEquals(ScoreTier.HIGH, TierClassifier.classify(55001))
        assertEquals(ScoreTier.HIGH, TierClassifier.classify(70000))
        assertEquals(ScoreTier.HIGH, TierClassifier.classify(80000))
        
        // FLAGSHIP: 80001..92000
        assertEquals(ScoreTier.FLAGSHIP, TierClassifier.classify(80001))
        assertEquals(ScoreTier.FLAGSHIP, TierClassifier.classify(90000))
        assertEquals(ScoreTier.FLAGSHIP, TierClassifier.classify(92000))
        
        // ELITE: 92001..100000
        assertEquals(ScoreTier.ELITE, TierClassifier.classify(92001))
        assertEquals(ScoreTier.ELITE, TierClassifier.classify(98000))
        assertEquals(ScoreTier.ELITE, TierClassifier.classify(100000))
    }
}
