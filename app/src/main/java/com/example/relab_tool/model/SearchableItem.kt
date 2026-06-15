package com.example.relab_tool.model

import androidx.annotation.StringRes

/**
 * A single searchable entry in the unified search index.
 * Every device info field is flattened into one of these.
 *
 * @param labelResId   String resource for the field label (e.g., R.string.screen_resolution)
 * @param value        The resolved runtime value (e.g., "1080 x 2400")
 * @param categoryResId String resource for the category (e.g., R.string.tab_display)
 */
data class SearchableItem(
    @StringRes val labelResId: Int,
    val value: String,
    @StringRes val categoryResId: Int
)

/**
 * A search result ready for UI rendering, with all strings resolved.
 */
data class SearchResult(
    val label: String,
    val value: String,
    val category: String,
    @StringRes val categoryResId: Int
)
