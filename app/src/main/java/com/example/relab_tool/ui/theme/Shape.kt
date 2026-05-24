package com.example.relab_tool.ui.theme

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * M3 Expressive shape tokens.
 *
 * Performance rules:
 * - Every shape is a top-level val/object, allocated ONCE at classload.
 * - NEVER create a shape inside a composable body — it allocates a new object
 *   on every recomposition, creating GC pressure.
 * - Use graphicsLayer { shape = ... } when the shape must animate (see Task 5).
 */

// ── Canonical M3 Expressive shape scale ───────────────────────────────────────

/** ExtraSmall — 4 dp; used for chips, badges */
val ShapeExtraSmall = RoundedCornerShape(4.dp)

/** Small — 8 dp; used for small buttons, snackbars */
val ShapeSmall = RoundedCornerShape(8.dp)

/** Medium — 12 dp; used for cards, dialogs secondary surface */
val ShapeMedium = RoundedCornerShape(12.dp)

/** Large — 16 dp; used for bottom sheets, navigation drawers */
val ShapeLarge = RoundedCornerShape(16.dp)

/** ExtraLarge — 28 dp; used for dialogs, large cards */
val ShapeExtraLarge = RoundedCornerShape(28.dp)

/** Full — pill/circle; used for FAB, extended FABs, nav-bar indicator */
val ShapeFull = RoundedCornerShape(percent = 100)

/** Card shape — matches the app's card rounding */
val ShapeCard = RoundedCornerShape(24.dp)

// ── Expressive hero shapes (non-rectangular moments) ──────────────────────────

/** Cut-corner accent — used for primary action accents and hero moments */
val ShapeAccentCut = CutCornerShape(topStart = 16.dp, bottomEnd = 16.dp)

/** Asymmetric pill — subtle expressiveness for chips and badges */
val ShapeAsymmetricPill = RoundedCornerShape(
    topStart = 100.dp,
    topEnd = 100.dp,
    bottomStart = 100.dp,
    bottomEnd = 16.dp
)

// ── MaterialTheme.shapes wiring ───────────────────────────────────────────────

/**
 * AppShapes wires the canonical M3 scale into MaterialTheme.
 * Reference these via MaterialTheme.shapes.* or the top-level vals above.
 */
val AppShapes = Shapes(
    extraSmall = ShapeExtraSmall,
    small       = ShapeSmall,
    medium      = ShapeMedium,
    large       = ShapeLarge,
    extraLarge  = ShapeExtraLarge
)
