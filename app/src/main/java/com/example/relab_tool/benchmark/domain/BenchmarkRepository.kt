package com.example.relab_tool.benchmark.domain

import com.example.relab_tool.benchmark.domain.model.BenchmarkResult
import kotlinx.coroutines.flow.Flow

interface BenchmarkRepository {
    fun getHistoryFlow(): Flow<List<BenchmarkResult>>
    suspend fun saveResult(result: BenchmarkResult)
    suspend fun deleteResult(result: BenchmarkResult)
    suspend fun clearHistory()
}
