package com.example.relab_tool.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.relab_tool.data.ThemeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val repository: ThemeRepository
) : ViewModel() {

    val themeMode: StateFlow<DarkModeOption> = repository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DarkModeOption.FOLLOW_SYSTEM)

    val useDynamicColor: StateFlow<Boolean> = repository.useDynamicColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val seedColor: StateFlow<Long> = repository.seedColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0xFF6750A4)

    fun setThemeMode(mode: DarkModeOption) {
        viewModelScope.launch {
            repository.setThemeMode(mode)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            repository.setDynamicColor(enabled)
        }
    }

    fun setSeedColor(color: Long) {
        viewModelScope.launch {
            repository.setSeedColor(color)
        }
    }
}
