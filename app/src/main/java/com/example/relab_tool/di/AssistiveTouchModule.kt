package com.example.relab_tool.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Qualifier to distinguish the AssistiveTouch DataStore from the existing
 * theme_settings DataStore provided by [DataStoreModule].
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AssistiveTouchDataStore

@Module
@InstallIn(SingletonComponent::class)
object AssistiveTouchModule {

    @Provides
    @Singleton
    @AssistiveTouchDataStore
    fun provideAssistiveTouchDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("assistive_touch_settings") }
        )
    }
}
