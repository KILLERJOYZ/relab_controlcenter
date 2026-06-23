package com.example.relab_tool.benchmark.domain.model

import com.example.relab_tool.R

enum class BenchmarkPillar(
    val weight: Float,
    val displayNameRes: Int
) {
    CPU_SINGLE_CORE(0.13f, R.string.pillar_cpu_single),
    CPU_MULTI_CORE(0.14f, R.string.pillar_cpu_multi),
    GPU_RENDERING(0.16f, R.string.pillar_gpu),
    GAMING_SIMULATION(0.10f, R.string.pillar_gaming_simulation),
    MEMORY(0.10f, R.string.pillar_memory),
    STORAGE_IO(0.06f, R.string.pillar_storage),
    AI_ML(0.06f, R.string.pillar_ai_ml),
    UX_SMOOTHNESS(0.05f, R.string.pillar_ux),
    CODEC_MEDIA(0.05f, R.string.pillar_codec_media),
    THERMAL_EFFICIENCY(0.06f, R.string.pillar_thermal),
    WIFI(0.03f, R.string.pillar_wifi),
    CELLULAR(0.03f, R.string.pillar_cellular),
    BROWSER_WEB(0.03f, R.string.pillar_browser_web)
}
