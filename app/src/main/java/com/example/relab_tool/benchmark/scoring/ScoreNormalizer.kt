package com.example.relab_tool.benchmark.scoring

object ScoreNormalizer {
    fun normalize(rawValue: Double, baseline: Double, cap: Double, isInverted: Boolean): Int {
        if (isInverted) {
            // Lower is better. If value <= cap (best), return 1000.
            // Let's define the worst-case ceiling. Let's make it baseline + 2.5 * (baseline - cap)
            val diff = (baseline - cap).coerceAtLeast(0.1)
            val worst = baseline + 2.5 * diff
            if (rawValue <= cap) return 1000
            if (rawValue >= worst) return 0
            
            return if (rawValue <= baseline) {
                // Between cap and baseline (maps to 1000 down to 500)
                val fraction = (rawValue - cap) / diff
                (1000 - fraction * 500).toInt()
            } else {
                // Between baseline and worst (maps to 500 down to 0)
                val fraction = (rawValue - baseline) / (worst - baseline)
                (500 - fraction * 500).toInt()
            }.coerceIn(0, 1000)
        } else {
            // Higher is better.
            if (rawValue >= cap) return 1000
            if (rawValue <= 0.0) return 0
            
            return if (rawValue <= baseline) {
                // Between 0 and baseline (maps to 0 to 500)
                val fraction = rawValue / baseline
                (fraction * 500).toInt()
            } else {
                // Between baseline and cap (maps to 500 to 1000)
                val fraction = (rawValue - baseline) / (cap - baseline).coerceAtLeast(0.1)
                (500 + fraction * 500).toInt()
            }.coerceIn(0, 1000)
        }
    }
}
