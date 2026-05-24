package com.example.relab_tool.ui.theme

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

/**
 * Wraps any high-motion surface (scrollable list, carousel, gesture surface,
 * animated transition) and requests the highest available frame rate category
 * from the system while this composable is in the composition.
 *
 * On API 35+ (Android 15): uses View.frameRateCategory = FRAME_RATE_CATEGORY_HIGH,
 * which is the declarative, LTPO-aware approach — the platform picks the optimal
 * rate (up to 120 Hz) and drops back down automatically when idle.
 *
 * On API 31–34: no-op — MainActivity.requestHighRefreshRate() already sets the
 * preferred display mode at the window level via Display.getSupportedModes(), which
 * covers those versions adequately.
 *
 * On API < 31: no-op.
 *
 * Usage:
 *   HighRefreshRateScope {
 *       LazyColumn { ... }
 *   }
 */
@Composable
fun HighRefreshRateScope(content: @Composable () -> Unit) {
    if (Build.VERSION.SDK_INT >= 35) {
        val view = LocalView.current
        DisposableEffect(view) {
            // Using reflection to call setFrameRateCategory to avoid compilation issues
            // (Unresolved reference) on environments where API 35 is not fully recognized.
            val setFrameRateCategory = runCatching {
                view.javaClass.getMethod("setFrameRateCategory", Int::class.javaPrimitiveType)
            }.getOrNull()

            setFrameRateCategory?.invoke(view, 2) // 2 = View.FRAME_RATE_CATEGORY_HIGH

            onDispose {
                setFrameRateCategory?.invoke(view, 1) // 1 = View.FRAME_RATE_CATEGORY_NORMAL
            }
        }
    }
    content()
}
