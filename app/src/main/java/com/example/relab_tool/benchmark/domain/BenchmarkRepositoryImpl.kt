package com.example.relab_tool.benchmark.domain

import com.example.relab_tool.benchmark.data.BenchmarkDao
import com.example.relab_tool.benchmark.data.BenchmarkResultEntity
import com.example.relab_tool.benchmark.domain.model.BenchmarkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class BenchmarkRepositoryImpl(
    private val dao: BenchmarkDao
) : BenchmarkRepository {
    override fun getHistoryFlow(): Flow<List<BenchmarkResult>> {
        return dao.getAllResultsFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun saveResult(result: BenchmarkResult) {
        withContext(Dispatchers.IO) {
            dao.insertResult(BenchmarkResultEntity.fromDomain(result))
        }
    }

    override suspend fun deleteResult(result: BenchmarkResult) {
        withContext(Dispatchers.IO) {
            dao.deleteResult(BenchmarkResultEntity.fromDomain(result))
        }
    }

    override suspend fun clearHistory() {
        withContext(Dispatchers.IO) {
            dao.deleteAllResults()
        }
    }
}
