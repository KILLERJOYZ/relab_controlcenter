package com.example.relab_tool.benchmark.domain.model

import com.example.relab_tool.R

enum class ScoreTier(
    val labelRes: Int,
    val descriptionRes: Int,
    val scoreRange: IntRange
) {
    ENTRY(R.string.tier_entry, R.string.tier_entry_desc, 0..25000),
    MID(R.string.tier_mid, R.string.tier_mid_desc, 25001..55000),
    HIGH(R.string.tier_high, R.string.tier_high_desc, 55001..80000),
    FLAGSHIP(R.string.tier_flagship, R.string.tier_flagship_desc, 80001..92000),
    ELITE(R.string.tier_elite, R.string.tier_elite_desc, 92001..100000);

    companion object {
        fun fromScore(score: Int): ScoreTier {
            return entries.find { score in it.scoreRange } ?: ENTRY
        }
    }
}
