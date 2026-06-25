package com.example.relab_tool.benchmark.scoring

import com.example.relab_tool.benchmark.domain.model.ScoreTier

object TierClassifier {
    fun classify(score: Double): ScoreTier {
        return ScoreTier.fromScore((score * 1000.0).toInt())
    }
}
