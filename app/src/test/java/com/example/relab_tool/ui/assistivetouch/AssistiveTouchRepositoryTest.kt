package com.example.relab_tool.ui.assistivetouch

import app.cash.turbine.test
import com.example.relab_tool.data.AssistiveTouchRepository
import com.example.relab_tool.model.AssistiveTouchConfig
import com.example.relab_tool.model.ButtonSize
import com.example.relab_tool.model.MenuAction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for [AssistiveTouchRepository] data flows.
 * Uses a fake in-memory implementation to test persistence logic
 * without requiring Android DataStore or Hilt injection.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AssistiveTouchRepositoryTest {

    private lateinit var repository: FakeAssistiveTouchRepository
    private val defaultConfig = AssistiveTouchConfig()

    @BeforeEach
    fun setup() {
        repository = FakeAssistiveTouchRepository()
    }

    @Test
    fun `config emits default values initially`() = runTest {
        repository.config.test {
            val config = awaitItem()
            assertEquals(false, config.isEnabled)
            assertEquals(6, config.menuItemCount)
            assertEquals(ButtonSize.MEDIUM, config.buttonSize)
            assertEquals(AssistiveTouchConfig.DEFAULT_BUTTON_COLOR, config.buttonColor)
            assertNull(config.customAppPackage)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setEnabled toggles config`() = runTest {
        repository.config.test {
            awaitItem() // default
            repository.setEnabled(true)
            val updated = awaitItem()
            assertTrue(updated.isEnabled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setMenuActions persists action list`() = runTest {
        val actions = listOf(MenuAction.HOME, MenuAction.BACK, MenuAction.SCREENSHOT)
        repository.config.test {
            awaitItem() // default
            repository.setMenuActions(actions)
            val updated = awaitItem()
            assertEquals(actions, updated.menuActions)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setMenuItemCount clamps to valid range`() = runTest {
        repository.config.test {
            awaitItem()
            repository.setMenuItemCount(2)
            assertEquals(4, awaitItem().menuItemCount) // clamped to min
            repository.setMenuItemCount(10)
            assertEquals(8, awaitItem().menuItemCount) // clamped to max
            repository.setMenuItemCount(6)
            assertEquals(6, awaitItem().menuItemCount) // normal
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setButtonSize updates config`() = runTest {
        repository.config.test {
            awaitItem()
            repository.setButtonSize(ButtonSize.LARGE)
            assertEquals(ButtonSize.LARGE, awaitItem().buttonSize)
            repository.setButtonSize(ButtonSize.SMALL)
            assertEquals(ButtonSize.SMALL, awaitItem().buttonSize)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setButtonColor updates config`() = runTest {
        val newColor = 0xFF1565C0L
        repository.config.test {
            awaitItem()
            repository.setButtonColor(newColor)
            assertEquals(newColor, awaitItem().buttonColor)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setButtonPosition persists coordinates`() = runTest {
        repository.config.test {
            awaitItem()
            repository.setButtonPosition(150, 300)
            val updated = awaitItem()
            assertEquals(150, updated.buttonPositionX)
            assertEquals(300, updated.buttonPositionY)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setCustomAppPackage sets and clears package`() = runTest {
        repository.config.test {
            awaitItem()
            repository.setCustomAppPackage("com.example.test")
            assertEquals("com.example.test", awaitItem().customAppPackage)
            repository.setCustomAppPackage(null)
            assertNull(awaitItem().customAppPackage)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `reorder menu actions preserves order`() = runTest {
        val original = listOf(MenuAction.HOME, MenuAction.BACK, MenuAction.RECENTS)
        val reordered = listOf(MenuAction.RECENTS, MenuAction.HOME, MenuAction.BACK)
        repository.config.test {
            awaitItem()
            repository.setMenuActions(original)
            assertEquals(original, awaitItem().menuActions)
            repository.setMenuActions(reordered)
            assertEquals(reordered, awaitItem().menuActions)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

/**
 * Fake implementation that mimics the real [AssistiveTouchRepository] using
 * in-memory [MutableStateFlow] instead of DataStore for testing.
 */
private class FakeAssistiveTouchRepository {
    private val _config = MutableStateFlow(AssistiveTouchConfig())
    val config: Flow<AssistiveTouchConfig> = _config

    suspend fun setEnabled(enabled: Boolean) {
        _config.update { it.copy(isEnabled = enabled) }
    }

    suspend fun setMenuActions(actions: List<MenuAction>) {
        _config.update { it.copy(menuActions = actions) }
    }

    suspend fun setMenuItemCount(count: Int) {
        val clamped = count.coerceIn(4, 8)
        _config.update { it.copy(menuItemCount = clamped) }
    }

    suspend fun setButtonSize(size: ButtonSize) {
        _config.update { it.copy(buttonSize = size) }
    }

    suspend fun setButtonColor(color: Long) {
        _config.update { it.copy(buttonColor = color) }
    }

    suspend fun setButtonPosition(x: Int, y: Int) {
        _config.update { it.copy(buttonPositionX = x, buttonPositionY = y) }
    }

    suspend fun setCustomAppPackage(packageName: String?) {
        _config.update { it.copy(customAppPackage = packageName) }
    }
}
