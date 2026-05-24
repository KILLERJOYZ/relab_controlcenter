package com.example.relab_tool.ui.cit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class CITViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _testResults = MutableStateFlow<Map<CITTestRoute, CITTestResult>>(
        savedStateHandle.get<Map<String, String>>("test_results")?.mapKeys { 
            CITTestRoute.valueOf(it.key) 
        }?.mapValues { 
            CITTestResult.valueOf(it.value) 
        } ?: CITTestRoute.entries.filter { it != CITTestRoute.DASHBOARD }.associateWith { 
            CITTestResult.NOT_TESTED 
        }
    )
    val testResults: StateFlow<Map<CITTestRoute, CITTestResult>> = _testResults.asStateFlow()

    fun updateTestResult(route: CITTestRoute, result: CITTestResult) {
        val current = _testResults.value.toMutableMap()
        current[route] = result
        _testResults.value = current
        
        // Persist to SavedStateHandle
        savedStateHandle["test_results"] = current.mapKeys { it.key.name }.mapValues { it.value.name }
    }

    fun resetTests() {
        val reset = CITTestRoute.entries.filter { it != CITTestRoute.DASHBOARD }.associateWith { 
            CITTestResult.NOT_TESTED 
        }
        _testResults.value = reset
        savedStateHandle["test_results"] = reset.mapKeys { it.key.name }.mapValues { it.value.name }
    }
}
