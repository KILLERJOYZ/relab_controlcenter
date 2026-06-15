package com.example.relab_tool.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.relab_tool.data.PerformanceRepository
import com.example.relab_tool.data.TowerInfoProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PerformanceViewModel @Inject constructor(
    val performanceRepository: PerformanceRepository,
    val towerInfoProvider: TowerInfoProvider
) : ViewModel() {

    private val _towers = MutableStateFlow<List<TowerInfoProvider.TowerInfo>>(emptyList())
    val towers = _towers.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _towers.value = towerInfoProvider.getTowerInfo()
        }
    }

    fun runCpuBenchmark() {
        viewModelScope.launch {
            performanceRepository.runCpuBenchmark()
        }
    }

    fun runRamBenchmark() {
        viewModelScope.launch {
            performanceRepository.runRamBenchmark()
        }
    }

    fun startStabilityTest() {
        performanceRepository.startStabilityTest(viewModelScope)
    }

    fun stopStabilityTest() {
        performanceRepository.stopStabilityTest()
    }
}
