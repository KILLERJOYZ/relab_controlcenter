package com.example.relab_tool.benchmark.domain.engine

import com.example.relab_tool.benchmark.domain.model.BenchmarkPillar
import com.example.relab_tool.benchmark.domain.model.SubScore

interface BenchmarkEngine {
    val pillar: BenchmarkPillar
    suspend fun run(onProgress: (Float) -> Unit): List<SubScore>
    fun isAvailable(): Boolean = true
}
