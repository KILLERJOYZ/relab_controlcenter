package com.example.relab_tool.ui.assistivetouch

import android.app.Application
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.relab_tool.data.AssistiveTouchRepository
import com.example.relab_tool.model.AssistiveTouchConfig
import com.example.relab_tool.model.ButtonSize
import com.example.relab_tool.model.MenuAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AssistiveTouchViewModel @Inject constructor(
    application: Application,
    private val repository: AssistiveTouchRepository
) : AndroidViewModel(application) {

    val config: StateFlow<AssistiveTouchConfig> = repository.config
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AssistiveTouchConfig())

    private val _isAccessibilityEnabled = MutableStateFlow(false)
    val isAccessibilityEnabled: StateFlow<Boolean> = _isAccessibilityEnabled.asStateFlow()

    private val _isOverlayPermissionGranted = MutableStateFlow(false)
    val isOverlayPermissionGranted: StateFlow<Boolean> = _isOverlayPermissionGranted.asStateFlow()

    init {
        refreshPermissionState()
    }

    fun refreshPermissionState() {
        val context = getApplication<Application>()
        _isOverlayPermissionGranted.value = Settings.canDrawOverlays(context)
        _isAccessibilityEnabled.value = isAccessibilityServiceEnabled(context)
    }

    fun toggleEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setEnabled(enabled)
        }
    }

    fun setMenuActions(actions: List<MenuAction>) {
        viewModelScope.launch {
            repository.setMenuActions(actions)
        }
    }

    fun reorderMenuActions(actions: List<MenuAction>) {
        viewModelScope.launch {
            repository.setMenuActions(actions)
        }
    }

    fun setMenuItemCount(count: Int) {
        viewModelScope.launch {
            repository.setMenuItemCount(count)
            // Trim menu actions if needed
            val current = config.value.menuActions
            if (current.size > count) {
                repository.setMenuActions(current.take(count))
            }
        }
    }

    fun setButtonSize(size: ButtonSize) {
        viewModelScope.launch {
            repository.setButtonSize(size)
        }
    }

    fun setButtonColor(color: Long) {
        viewModelScope.launch {
            repository.setButtonColor(color)
        }
    }

    fun setCustomAppPackage(packageName: String?) {
        viewModelScope.launch {
            repository.setCustomAppPackage(packageName)
        }
    }

    fun addMenuAction(action: MenuAction) {
        viewModelScope.launch {
            val current = config.value.menuActions.toMutableList()
            if (action !in current && current.size < config.value.menuItemCount) {
                current.add(action)
                repository.setMenuActions(current)
            }
        }
    }

    fun removeMenuAction(action: MenuAction) {
        viewModelScope.launch {
            val current = config.value.menuActions.toMutableList()
            current.remove(action)
            repository.setMenuActions(current)
        }
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            ?: return false
        val enabledServices = am.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )
        val target = ComponentName(context, com.example.relab_tool.worker.AssistiveTouchService::class.java)
        return enabledServices.any {
            ComponentName.unflattenFromString(it.id) == target
        }
    }
}
