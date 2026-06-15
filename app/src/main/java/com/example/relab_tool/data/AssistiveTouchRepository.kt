package com.example.relab_tool.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.relab_tool.di.AssistiveTouchDataStore
import com.example.relab_tool.model.AssistiveTouchConfig
import com.example.relab_tool.model.ButtonSize
import com.example.relab_tool.model.MenuAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssistiveTouchRepository @Inject constructor(
    @AssistiveTouchDataStore private val dataStore: DataStore<Preferences>
) {
    private object Keys {
        val ENABLED = booleanPreferencesKey("at_enabled")
        val MENU_ACTIONS = stringPreferencesKey("at_menu_actions")
        val MENU_ITEM_COUNT = intPreferencesKey("at_menu_item_count")
        val BUTTON_SIZE = stringPreferencesKey("at_button_size")
        val BUTTON_COLOR = longPreferencesKey("at_button_color")
        val BUTTON_POS_X = intPreferencesKey("at_button_pos_x")
        val BUTTON_POS_Y = intPreferencesKey("at_button_pos_y")
        val CUSTOM_APP_PACKAGE = stringPreferencesKey("at_custom_app_package")
    }

    val config: Flow<AssistiveTouchConfig> = dataStore.data.map { prefs ->
        val actionIds = prefs[Keys.MENU_ACTIONS]
        val menuActions = if (actionIds != null) {
            actionIds.split(",")
                .mapNotNull { MenuAction.fromId(it.trim()) }
                .ifEmpty { AssistiveTouchConfig.DEFAULT_MENU_ACTIONS }
        } else {
            AssistiveTouchConfig.DEFAULT_MENU_ACTIONS
        }

        val buttonSize = prefs[Keys.BUTTON_SIZE]?.let { name ->
            try { ButtonSize.valueOf(name) } catch (_: IllegalArgumentException) { ButtonSize.MEDIUM }
        } ?: ButtonSize.MEDIUM

        AssistiveTouchConfig(
            isEnabled = prefs[Keys.ENABLED] ?: false,
            menuActions = menuActions,
            menuItemCount = prefs[Keys.MENU_ITEM_COUNT] ?: 6,
            buttonSize = buttonSize,
            buttonColor = prefs[Keys.BUTTON_COLOR] ?: AssistiveTouchConfig.DEFAULT_BUTTON_COLOR,
            buttonPositionX = prefs[Keys.BUTTON_POS_X] ?: -1,
            buttonPositionY = prefs[Keys.BUTTON_POS_Y] ?: 200,
            customAppPackage = prefs[Keys.CUSTOM_APP_PACKAGE]
        )
    }

    suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.ENABLED] = enabled }
    }

    suspend fun setMenuActions(actions: List<MenuAction>) {
        dataStore.edit { prefs ->
            prefs[Keys.MENU_ACTIONS] = actions.joinToString(",") { it.id }
        }
    }

    suspend fun setMenuItemCount(count: Int) {
        val clamped = count.coerceIn(4, 8)
        dataStore.edit { it[Keys.MENU_ITEM_COUNT] = clamped }
    }

    suspend fun setButtonSize(size: ButtonSize) {
        dataStore.edit { it[Keys.BUTTON_SIZE] = size.name }
    }

    suspend fun setButtonColor(color: Long) {
        dataStore.edit { it[Keys.BUTTON_COLOR] = color }
    }

    suspend fun setButtonPosition(x: Int, y: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.BUTTON_POS_X] = x
            prefs[Keys.BUTTON_POS_Y] = y
        }
    }

    suspend fun setCustomAppPackage(packageName: String?) {
        dataStore.edit { prefs ->
            if (packageName != null) {
                prefs[Keys.CUSTOM_APP_PACKAGE] = packageName
            } else {
                prefs.remove(Keys.CUSTOM_APP_PACKAGE)
            }
        }
    }
}
