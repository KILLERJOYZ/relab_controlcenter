package com.example.relab_tool.ui

import androidx.lifecycle.ViewModel
import com.example.relab_tool.data.PerformanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PerformanceViewModel @Inject constructor(
    val performanceRepository: PerformanceRepository,
    val towerInfoProvider: com.example.relab_tool.data.TowerInfoProvider
) : ViewModel()
