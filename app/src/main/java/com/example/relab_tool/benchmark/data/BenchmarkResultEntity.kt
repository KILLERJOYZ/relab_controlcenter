package com.example.relab_tool.benchmark.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.relab_tool.benchmark.domain.model.BenchmarkResult
import com.example.relab_tool.benchmark.domain.model.PillarScore
import com.example.relab_tool.benchmark.domain.model.ScoreTier

@Entity(tableName = "benchmark_results")
data class BenchmarkResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val timestamp: Long,
    val deviceModel: String,
    val deviceSoc: String,
    val hardwareScore: Int,
    val connectivityScore: Int,
    val totalScore: Int,
    val tier: ScoreTier,
    val pillarScores: List<PillarScore>,
    val isQuickTest: Boolean
) {
    fun toDomain(): BenchmarkResult = BenchmarkResult(
        id = id,
        timestamp = timestamp,
        deviceModel = deviceModel,
        deviceSoc = deviceSoc,
        hardwareScore = hardwareScore,
        connectivityScore = connectivityScore,
        totalScore = totalScore,
        tier = tier,
        pillarScores = pillarScores,
        isQuickTest = isQuickTest
    )

    companion object {
        fun fromDomain(domain: BenchmarkResult): BenchmarkResultEntity = BenchmarkResultEntity(
            id = domain.id,
            timestamp = domain.timestamp,
            deviceModel = domain.deviceModel,
            deviceSoc = domain.deviceSoc,
            hardwareScore = domain.hardwareScore,
            connectivityScore = domain.connectivityScore,
            totalScore = domain.totalScore,
            tier = domain.tier,
            pillarScores = domain.pillarScores,
            isQuickTest = domain.isQuickTest
        )
    }
}
