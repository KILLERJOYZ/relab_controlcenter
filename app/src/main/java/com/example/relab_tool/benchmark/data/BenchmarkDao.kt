package com.example.relab_tool.benchmark.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BenchmarkDao {
    @Query("SELECT * FROM benchmark_results ORDER BY timestamp DESC")
    fun getAllResultsFlow(): Flow<List<BenchmarkResultEntity>>

    @Query("SELECT * FROM benchmark_results")
    fun getAllResultsSync(): List<BenchmarkResultEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertResult(result: BenchmarkResultEntity): Long

    @Delete
    fun deleteResult(result: BenchmarkResultEntity): Int

    @Query("DELETE FROM benchmark_results")
    fun deleteAllResults(): Int
}
