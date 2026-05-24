package com.example.relab_tool.di

import android.content.Context
import com.example.relab_tool.data.PerformanceRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PerformanceModule {

    @Provides
    @Singleton
    fun providePerformanceRepository(@ApplicationContext context: Context): PerformanceRepository {
        return PerformanceRepository(context)
    }

    @Provides
    @Singleton
    fun provideTowerInfoProvider(@ApplicationContext context: Context): com.example.relab_tool.data.TowerInfoProvider {
        return com.example.relab_tool.data.TowerInfoProvider(context)
    }
}
