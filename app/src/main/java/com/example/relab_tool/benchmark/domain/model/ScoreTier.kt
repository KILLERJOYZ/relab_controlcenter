package com.example.relab_tool.benchmark.domain.model

import com.example.relab_tool.R

/**
 * Score tiers for the new 1,000,000-point benchmark scale.
 *
 * Calibrated reference points:
 *   ENTRY      Helio G85, SD 460               → 0 – 59,999
 *   ENTRY_MID  SD 680, SD 695, Dimensity 700    → 60k – 149,999
 *   MID        SD 778G, Dimensity 8100          → 150k – 299,999
 *   MID_HIGH   SD 7s Gen 3, Dimensity 9000      → 300k – 499,999
 *   HIGH       SD 8 Gen 1, Dimensity 9200       → 500k – 699,999
 *   FLAGSHIP   SD 8 Gen 3, Dimensity 9300       → 700k – 849,999
 *   ELITE      SD 8 Elite Gen 5, Dimensity 9500 → 850k – 1,000,000
 */
enum class ScoreTier(
    val labelRes: Int,
    val descriptionRes: Int,
    val scoreRange: IntRange
) {
    ENTRY(R.string.tier_entry, R.string.tier_entry_desc, 0..59_999),
    ENTRY_MID(R.string.tier_entry_mid, R.string.tier_entry_mid_desc, 60_000..149_999),
    MID(R.string.tier_mid, R.string.tier_mid_desc, 150_000..299_999),
    MID_HIGH(R.string.tier_mid_high, R.string.tier_mid_high_desc, 300_000..499_999),
    HIGH(R.string.tier_high, R.string.tier_high_desc, 500_000..699_999),
    FLAGSHIP(R.string.tier_flagship, R.string.tier_flagship_desc, 700_000..849_999),
    ELITE(R.string.tier_elite, R.string.tier_elite_desc, 850_000..1_000_000);

    companion object {
        fun fromScore(score: Int): ScoreTier {
            return entries.find { score in it.scoreRange } ?: ENTRY
        }
    }
}
