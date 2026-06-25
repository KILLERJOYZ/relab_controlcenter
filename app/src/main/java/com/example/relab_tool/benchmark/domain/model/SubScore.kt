package com.example.relab_tool.benchmark.domain.model

data class SubScore(
    val name: String,
    val rawValue: Double,
    val unit: String,
    val score: Double,
    val isPartial: Boolean = false
)
