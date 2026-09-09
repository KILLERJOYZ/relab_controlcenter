package com.example.relab_tool.benchmark.scoring

import com.example.relab_tool.benchmark.domain.model.ScoreTier

object TierClassifier {
    fun classify(score: Int): ScoreTier {
        return ScoreTier.fromScore(score)
    }
}
