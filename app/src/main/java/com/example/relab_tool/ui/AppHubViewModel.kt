package com.example.relab_tool.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.relab_tool.data.AppRepository
import com.example.relab_tool.model.AppInfo
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AppHubViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(application)

    val apps: StateFlow<List<AppInfo>> = repository.apps

    val updateCount: StateFlow<Int> = repository.apps
        .map { list -> list.count { it.hasUpdate } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.refresh()
            _isRefreshing.value = false
        }
    }
}
