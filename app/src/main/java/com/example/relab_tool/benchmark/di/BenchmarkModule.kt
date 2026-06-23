package com.example.relab_tool.benchmark.di

import android.content.Context
import com.example.relab_tool.benchmark.data.AppDatabase
import com.example.relab_tool.benchmark.data.BenchmarkDao
import com.example.relab_tool.benchmark.domain.BenchmarkOrchestrator
import com.example.relab_tool.benchmark.domain.BenchmarkRepository
import com.example.relab_tool.benchmark.domain.BenchmarkRepositoryImpl
import com.example.relab_tool.benchmark.domain.engine.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BenchmarkModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideBenchmarkDao(database: AppDatabase): BenchmarkDao {
        return database.benchmarkDao()
    }

    @Provides
    @Singleton
    fun provideBenchmarkRepository(dao: BenchmarkDao): BenchmarkRepository {
        return BenchmarkRepositoryImpl(dao)
    }

    @Provides
    @Singleton
    fun provideBenchmarkEngines(@ApplicationContext context: Context): List<BenchmarkEngine> {
        return listOf(
            CpuSingleCoreBenchmark(),
            CpuMultiCoreBenchmark(),
            GpuBenchmark(context),
            GamingBenchmark(context),
            MemoryBenchmark(context),
            StorageBenchmark(context),
            AiBenchmark(context),
            UxBenchmark(context),
            CodecBenchmark(),
            ThermalBenchmark(context),
            WifiBenchmark(context),
            CellularBenchmark(context),
            BrowserBenchmark(context)
        )
    }

    @Provides
    @Singleton
    fun provideBenchmarkOrchestrator(
        @ApplicationContext context: Context,
        engines: List<@JvmSuppressWildcards BenchmarkEngine>
    ): BenchmarkOrchestrator {
        return BenchmarkOrchestrator(context, engines)
    }
}
