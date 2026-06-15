package com.example.relab_tool.ui.theme

import android.os.Build
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

/**
 * Wraps any composable subtree and requests the highest available frame rate
 * from the system while this composable is in the composition.
 *
 * **API 34+ (Android 14):** uses `View.setRequestedFrameRate()` with the
 * device's maximum refresh rate — the most direct, per-view signal to the platform.
 *
 * **API 35+ (Android 15):** also sets `frameRateCategory = FRAME_RATE_CATEGORY_HIGH`
 * which is the declarative, LTPO-aware approach. The platform picks the optimal
 * rate (up to 120/144 Hz) and drops back automatically when idle.
 *
 * On API < 34: no-op — `MainActivity.requestHighRefreshRate()` sets the preferred
 * display mode at the window level which is sufficient.
 *
 * All API calls use reflection to avoid compilation errors when minSdk < 34.
 *
 * Usage:
 *   HighRefreshRateScope {
 *       LazyColumn { ... }
 *   }
 */
@Composable
fun HighRefreshRateScope(content: @Composable () -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        val view = LocalView.current
        DisposableEffect(view) {
            val display = try {
                view.display ?: view.context.display
            } catch (_: Exception) {
                null
            }
            val maxFps = display?.supportedModes
                ?.maxByOrNull { it.refreshRate }?.refreshRate ?: 120f

            // Layer 1: View.setRequestedFrameRate (API 34+)
            try {
                val method = view.javaClass.getMethod(
                    "setRequestedFrameRate", Float::class.javaPrimitiveType
                )
                method.invoke(view, maxFps)
            } catch (e: Exception) {
                Log.w("HighRefreshRate", "setRequestedFrameRate failed", e)
            }

            // Layer 2: View.setFrameRateCategory (API 35+)
            if (Build.VERSION.SDK_INT >= 35) {
                try {
                    val setCategory = view.javaClass.getMethod(
                        "setFrameRateCategory", Int::class.javaPrimitiveType
                    )
                    // 4 = FRAME_RATE_CATEGORY_HIGH_HINT, fallback to 2 = FRAME_RATE_CATEGORY_HIGH
                    try {
                        setCategory.invoke(view, 4)
                    } catch (_: Exception) {
                        setCategory.invoke(view, 2)
                    }
                } catch (e: Exception) {
                    Log.w("HighRefreshRate", "setFrameRateCategory failed", e)
                }
            }

            onDispose {
                // Reset to system default on dispose
                try {
                    val method = view.javaClass.getMethod(
                        "setRequestedFrameRate", Float::class.javaPrimitiveType
                    )
                    method.invoke(view, 0f) // 0 = system default
                } catch (_: Exception) {}

                if (Build.VERSION.SDK_INT >= 35) {
                    try {
                        val setCategory = view.javaClass.getMethod(
                            "setFrameRateCategory", Int::class.javaPrimitiveType
                        )
                        setCategory.invoke(view, 0) // 0 = FRAME_RATE_CATEGORY_DEFAULT
                    } catch (_: Exception) {}
                }
            }
        }
    }
    content()
}
