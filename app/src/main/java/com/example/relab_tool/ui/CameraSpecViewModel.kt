package com.example.relab_tool.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.relab_tool.data.CameraSpecRepository
import com.example.relab_tool.model.CameraSpec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CameraSpecViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CameraSpecRepository(application)

    private val _cameraSpecs = MutableStateFlow<List<CameraSpec>>(emptyList())
    val cameraSpecs: StateFlow<List<CameraSpec>> = _cameraSpecs.asStateFlow()

    private val _selectedCameraIndex = MutableStateFlow(0)
    val selectedCameraIndex: StateFlow<Int> = _selectedCameraIndex.asStateFlow()

    init {
        loadCameraSpecs()
    }

    private fun loadCameraSpecs() {
        viewModelScope.launch {
            val specs = repository.getCameraSpecs()
            _cameraSpecs.value = specs
        }
    }

    fun selectCamera(index: Int) {
        if (index in _cameraSpecs.value.indices) {
            _selectedCameraIndex.value = index
        }
    }
}
