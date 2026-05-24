package com.example.relab_tool.ui

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.relab_tool.R
import com.example.relab_tool.ui.theme.*
import com.example.relab_tool.worker.StatusNotificationService

// Top-level data class for palette picker entries
data class PaletteEntryData(
    val value: ColorPalette,
    val label: String,
    val color: Color,
    val desc: String
)

// ─────────────────────────────────────────────────────────────────────────────
// Settings entry point — routes between main list and sub-pages
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SettingsScreen(viewModel: DeviceInfoViewModel = hiltViewModel(), onLaunchCIT: () -> Unit = {}) {
    var showThemePage by remember { mutableStateOf(false) }

    AnimatedContent(
        targetState = showThemePage,
        transitionSpec = {
            if (targetState) {
                // Navigate into sub-page: slide in from right
                slideInHorizontally { it } + fadeIn() togetherWith
                        slideOutHorizontally { -it / 3 } + fadeOut()
            } else {
                // Navigate back: slide in from left
                slideInHorizontally { -it / 3 } + fadeIn() togetherWith
                        slideOutHorizontally { it } + fadeOut()
            }
        },
        label = "settings_nav"
    ) { onThemePage ->
        if (onThemePage) {
            ThemeSettingsPage(onBack = { showThemePage = false })
        } else {
            SettingsMainPage(
                viewModel     = viewModel,
                onOpenTheme   = { showThemePage = true },
                onLaunchCIT   = onLaunchCIT,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Main settings list — each section is its own card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SettingsMainPage(
    viewModel: DeviceInfoViewModel,
    onOpenTheme: () -> Unit,
    onLaunchCIT: () -> Unit,
) {
    val configuration     = LocalConfiguration.current
    val currentLocale     = configuration.locales[0].language
    val appLocale         = AppCompatDelegate.getApplicationLocales()[0]?.language
    val isWideScreen      = configuration.screenWidthDp > 600
    val context           = LocalContext.current
    val isNotifRunning    by StatusNotificationService.isRunning.collectAsState()

    // Data for report
    val summary by viewModel.deviceSummary.collectAsState()
    val system by viewModel.systemInfo.collectAsState()
    val battery by viewModel.batteryInfo.collectAsState()

    // Current theme state (read-only for display)
    val palette    by ThemeSettings.colorPalette.collectAsState()
    val darkMode   by ThemeSettings.darkMode.collectAsState()

    var showLangDialog by remember { mutableStateOf(false) }

    val langOptions = listOf(
        null to stringResource(R.string.lang_system),
        "en" to "English", "vi" to "Tiếng Việt", "es" to "Español",
        "fr" to "Français", "de" to "Deutsch", "zh-CN" to "中文 (Simplified)",
        "ja" to "日本語", "ko" to "한국어", "ru" to "Русский",
        "ar" to "العربية", "hi" to "हिन्दी", "pt" to "Português",
        "it" to "Italiano", "id" to "Bahasa Indonesia", "tr" to "Türkçe",
        "th" to "ไทย", "nl" to "Nederlands",
    )
    val currentLangLabel = langOptions.find { it.first == appLocale }?.second
        ?: stringResource(R.string.lang_system)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {

        // ── General ──────────────────────────────────────────────────────────
        SettingsCard(
            title = stringResource(R.string.settings_general),
            icon  = Icons.Outlined.Language
        ) {
            // Language
            SettingsRow(
                title    = stringResource(R.string.settings_language),
                subtitle = currentLangLabel,
                onClick  = { showLangDialog = true }
            )

            SettingsDivider()

            // Notifications
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        stringResource(R.string.status_notification),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        stringResource(R.string.status_notification_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isNotifRunning,
                    onCheckedChange = { checked ->
                        context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
                            .edit().putBoolean("status_notification_enabled", checked).apply()
                        val intent = Intent(context, StatusNotificationService::class.java)
                        if (checked) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                                context.startForegroundService(intent)
                            else context.startService(intent)
                        } else {
                            intent.action = "STOP_SERVICE"
                            context.startService(intent)
                        }
                    }
                )
            }
        }

        // ── Theme ─────────────────────────────────────────────────────────────
        SettingsCard(
            title = stringResource(R.string.settings_theme),
            icon  = Icons.Outlined.Palette
        ) {
            val paletteName = when (palette) {
                ColorPalette.FOLLOW_SYSTEM -> stringResource(R.string.palette_dynamic)
                ColorPalette.OCEAN         -> stringResource(R.string.palette_ocean)
                ColorPalette.FOREST        -> stringResource(R.string.palette_forest)
                ColorPalette.SUNSET        -> stringResource(R.string.palette_sunset)
                ColorPalette.LAVENDER      -> stringResource(R.string.palette_lavender)
                ColorPalette.MONO          -> stringResource(R.string.palette_mono)
                ColorPalette.CUSTOM        -> stringResource(R.string.palette_custom)
            }
            val darkModeName = when (darkMode) {
                DarkModeOption.FOLLOW_SYSTEM -> stringResource(R.string.theme_follow_system)
                DarkModeOption.LIGHT         -> stringResource(R.string.theme_light)
                DarkModeOption.DARK          -> stringResource(R.string.theme_dark)
            }

            SettingsRow(
                title    = stringResource(R.string.theme_style),
                subtitle = paletteName,
                trail    = stringResource(R.string.theme_dark_mode) + ": $darkModeName",
                onClick  = onOpenTheme
            )
        }

        // ── Reports ──────────────────────────────────────────────────────────
        SettingsCard(
            title = stringResource(R.string.settings_reports),
            icon  = Icons.Default.Description
        ) {
            SettingsRow(
                title    = stringResource(R.string.settings_hardware_report),
                subtitle = stringResource(R.string.settings_hardware_report_desc),
                onClick  = {
                    val file = com.example.relab_tool.utils.ReportGenerator.generateHardwareReport(context, summary, system, battery)
                    if (file != null) {
                        val uri = androidx.core.content.FileProvider.getUriForFile(context, "com.relab.controlcenter.fileprovider", file)
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "application/pdf")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, context.getString(R.string.settings_open_report)))
                    }
                }
            )
        }

        // ── Root Tools ────────────────────────────────────────────────────────
        val isRootAvailable = remember { com.example.relab_tool.utils.RootUtils.isRootAvailable() }
        if (isRootAvailable) {
            SettingsCard(
                title = stringResource(R.string.settings_root_tools),
                icon  = Icons.Default.Security
            ) {
                var chargeLimit by remember { mutableStateOf(80) }
                SettingsRow(
                    title    = stringResource(R.string.settings_charge_limiter),
                    subtitle = stringResource(R.string.settings_charge_limit_format, chargeLimit),
                    onClick  = { 
                        chargeLimit = if (chargeLimit == 80) 100 else 80
                        com.example.relab_tool.utils.RootUtils.setChargeLimit(chargeLimit)
                    }
                )
                SettingsDivider()
                SettingsRow(
                    title    = stringResource(R.string.settings_cpu_governor),
                    subtitle = stringResource(R.string.settings_cpu_governor_desc),
                    onClick  = { com.example.relab_tool.utils.RootUtils.setGovernor("performance") }
                )
            }
        }

        // ── Advanced ──────────────────────────────────────────────────────────
        SettingsCard(
            title = stringResource(R.string.settings_advanced),
            icon  = Icons.Default.Build
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onLaunchCIT)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.cit_title),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        stringResource(R.string.cit_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForwardIos,
                    null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        if (!isWideScreen) Spacer(Modifier.height(100.dp))
    }

    // ── Language dialog ───────────────────────────────────────────────────────
    if (showLangDialog) {
        AlertDialog(
            onDismissRequest = { showLangDialog = false },
            title = { Text(stringResource(R.string.settings_language)) },
            text = {
                Column(
                    Modifier
                        .selectableGroup()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    langOptions.forEach { (code, label) ->
                        val selected = if (code == null) appLocale == null else appLocale == code
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .selectable(selected, onClick = {
                                    val loc = if (code == null) LocaleListCompat.getEmptyLocaleList()
                                    else LocaleListCompat.forLanguageTags(code)
                                    AppCompatDelegate.setApplicationLocales(loc)
                                    showLangDialog = false
                                }, role = Role.RadioButton)
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected, null)
                            Column(Modifier.padding(start = 12.dp)) {
                                Text(label, style = MaterialTheme.typography.bodyLarge)
                                if (code == null)
                                    Text(
                                        currentLocale,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLangDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Theme sub-page
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ThemeSettingsPage(onBack: () -> Unit) {
    val palette      by ThemeSettings.colorPalette.collectAsState()
    val darkMode     by ThemeSettings.darkMode.collectAsState()
    val highContrast by ThemeSettings.highContrast.collectAsState()
    val accentIdx    by ThemeSettings.customAccentIndex.collectAsState()
    val customHex    by ThemeSettings.customColorHex.collectAsState()

    var showCustomPicker by remember { mutableStateOf(false) }

    val palettes = listOf(
        PaletteEntryData(ColorPalette.FOLLOW_SYSTEM, stringResource(R.string.palette_dynamic),  Color(0xFF1565C0), stringResource(R.string.palette_dynamic_desc)),
        PaletteEntryData(ColorPalette.OCEAN,         stringResource(R.string.palette_ocean),    Color(0xFF1565C0), stringResource(R.string.palette_ocean_desc)),
        PaletteEntryData(ColorPalette.FOREST,        stringResource(R.string.palette_forest),   Color(0xFF2E7D32), stringResource(R.string.palette_forest_desc)),
        PaletteEntryData(ColorPalette.SUNSET,        stringResource(R.string.palette_sunset),   Color(0xFFC62828), stringResource(R.string.palette_sunset_desc)),
        PaletteEntryData(ColorPalette.LAVENDER,      stringResource(R.string.palette_lavender), Color(0xFF6A1B9A), stringResource(R.string.palette_lavender_desc)),
        PaletteEntryData(ColorPalette.MONO,          stringResource(R.string.palette_mono),     Color(0xFF546E7A), stringResource(R.string.palette_mono_desc)),
        PaletteEntryData(ColorPalette.CUSTOM,        stringResource(R.string.palette_custom),   customHex?.let { Color(android.graphics.Color.parseColor(it)) } ?: Color.Transparent, stringResource(R.string.palette_custom_desc)),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // ── Back header ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.nav_back),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                stringResource(R.string.settings_theme),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Display Mode ─────────────────────────────────────────────────
            SettingsCard(
                title = stringResource(R.string.theme_dark_mode),
                icon  = Icons.Outlined.DarkMode
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DarkModeOption.entries.forEach { opt ->
                        ThemeOptionCard(
                            option = opt,
                            isSelected = opt == darkMode,
                            onClick = { ThemeSettings.setDarkMode(opt) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ── Color Palette ────────────────────────────────────────────────
            SettingsCard(
                title = stringResource(R.string.theme_color_palette),
                icon  = Icons.Outlined.Palette
            ) {
                // Grid — 3 per row on compact, 4 per row on wide
                val configuration = LocalConfiguration.current
                val cols = if (configuration.screenWidthDp > 600) 4 else 3

                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    palettes.chunked(cols).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { entry ->
                                PaletteChip(
                                    entry     = entry,
                                    selected  = palette == entry.value,
                                    onClick   = { ThemeSettings.setColorPalette(entry.value) },
                                    modifier  = Modifier.weight(1f)
                                )
                            }
                            // Fill empty slots so last row looks aligned
                            repeat(cols - row.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }

                // Custom accent swatches shown only when CUSTOM is selected
                AnimatedVisibility(visible = palette == ColorPalette.CUSTOM) {
                    Column {
                        SettingsDivider()
                        Text(
                            stringResource(R.string.theme_accent_color),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 8.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp)
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            accentColors.forEachIndexed { idx, color ->
                                val selected = idx == accentIdx && customHex == null
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .then(
                                            if (selected)
                                                Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                            else Modifier
                                        )
                                        .clickable { ThemeSettings.setCustomAccentIndex(idx) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selected)
                                        Icon(Icons.Default.Check, null, Modifier.size(16.dp), Color.White)
                                }
                            }
                        }

                        // Dedicated custom color picker button
                        OutlinedButton(
                            onClick = { showCustomPicker = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (customHex != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.ColorLens, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.palette_custom))
                            if (customHex != null) {
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(customHex?.let { Color(android.graphics.Color.parseColor(it)) } ?: Color.Transparent)
                                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), CircleShape)
                                )
                            }
                        }
                    }
                }
            }

            // ── Accessibility ─────────────────────────────────────────────────
            SettingsCard(
                title = stringResource(R.string.theme_accessibility),
                icon  = Icons.Outlined.Contrast
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f).padding(end = 16.dp)) {
                        Text(
                            stringResource(R.string.theme_high_contrast),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            stringResource(R.string.theme_high_contrast_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = highContrast,
                        onCheckedChange = { ThemeSettings.setHighContrast(it) }
                    )
                }
            }

            Spacer(Modifier.height(100.dp))
        }
    }

    // ── Custom Picker Dialog ──────────────────────────────────────────────────
    if (showCustomPicker) {
        var pickedColor by remember { 
            val initial = customHex?.let { Color(android.graphics.Color.parseColor(it)) } ?: Color.Blue
            mutableStateOf(initial) 
        }

        AlertDialog(
            onDismissRequest = { showCustomPicker = false },
            title = { Text(stringResource(R.string.palette_custom)) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ColorPickerWheel(
                        initialColor = pickedColor,
                        onColorChanged = { pickedColor = it }
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(pickedColor))
                        Spacer(Modifier.width(12.dp))
                        Text(String.format("#%08X", pickedColor.toArgb()), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { 
                    ThemeSettings.setCustomColor(pickedColor)
                    showCustomPicker = false 
                }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomPicker = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun ColorPickerWheel(
    initialColor: Color,
    onColorChanged: (Color) -> Unit
) {
    var hsv by remember { 
        val hsvOut = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor.toArgb(), hsvOut)
        mutableStateOf(Triple(hsvOut[0], hsvOut[1], hsvOut[2]))
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Hue Wheel
        Box(modifier = Modifier.size(280.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
                    val pos = change.position
                    val angle = Math.toDegrees(Math.atan2((pos.y - center.y).toDouble(), (pos.x - center.x).toDouble())).toFloat()
                    val hue = (angle + 360f) % 360f
                    hsv = hsv.copy(first = hue)
                    onColorChanged(Color(android.graphics.Color.HSVToColor(floatArrayOf(hsv.first, hsv.second, hsv.third))))
                }
            }) {
                val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension / 2 - 25.dp.toPx()
                for (i in 0..360) {
                    val color = Color.hsv(i.toFloat(), 1f, 1f)
                    drawArc(
                        color = color,
                        startAngle = i.toFloat(),
                        sweepAngle = 2f,
                        useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(center.x - radius, center.y - radius),
                        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 35.dp.toPx())
                    )
                }
                
                // Selector handle for Hue
                val angleRad = Math.toRadians(hsv.first.toDouble())
                val handleX = center.x + radius * Math.cos(angleRad).toFloat()
                val handleY = center.y + radius * Math.sin(angleRad).toFloat()
                drawCircle(Color.White, 14.dp.toPx(), androidx.compose.ui.geometry.Offset(handleX, handleY))
                drawCircle(Color.Black, 12.dp.toPx(), androidx.compose.ui.geometry.Offset(handleX, handleY), style = androidx.compose.ui.graphics.drawscope.Stroke(2.5.dp.toPx()))
            }

            // SV Square in middle
            Box(modifier = Modifier.size(120.dp)) {
                Canvas(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val pos = change.position
                        val s = (pos.x / size.width.toFloat()).coerceIn(0f, 1f)
                        val v = 1f - (pos.y / size.height.toFloat()).coerceIn(0f, 1f)
                        hsv = hsv.copy(second = s, third = v)
                        onColorChanged(Color(android.graphics.Color.HSVToColor(floatArrayOf(hsv.first, hsv.second, hsv.third))))
                    }
                }) {
                    val hueColor = Color.hsv(hsv.first, 1f, 1f)
                    // Simplified SV gradient
                    drawRect(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color.White, hueColor))
                    )
                    drawRect(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Color.Black))
                    )
                    
                    // Selector handle for SV
                    val handleX = hsv.second * size.width
                    val handleY = (1f - hsv.third) * size.height
                    drawCircle(Color.White, 10.dp.toPx(), androidx.compose.ui.geometry.Offset(handleX, handleY))
                    drawCircle(Color.Black, 8.dp.toPx(), androidx.compose.ui.geometry.Offset(handleX, handleY), style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared helpers
// ─────────────────────────────────────────────────────────────────────────────

/** A settings section card */
@Composable
fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(24.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(top = 16.dp, bottom = 6.dp)) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.5.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

/** A tappable row inside a SettingsCard */
@Composable
fun SettingsRow(
    title: String,
    subtitle: String,
    trail: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (trail != null)
                Text(trail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            Icons.AutoMirrored.Filled.ArrowForwardIos,
            null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(
        modifier  = Modifier.padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color     = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    )
}

/** Palette selection chip with colour circle */
@Composable
fun PaletteChip(
    entry: PaletteEntryData,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .then(
                if (selected)
                    Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                else Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            )
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .then(
                    if (entry.color == Color.Transparent)
                        Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    else Modifier.background(entry.color)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected)
                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
            else if (entry.color == Color.Transparent)
                Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
        Text(
            entry.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ThemeOptionCard(
    option: DarkModeOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val duration = 300
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        animationSpec = androidx.compose.animation.core.tween(duration), label = "bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = androidx.compose.animation.core.tween(duration), label = "content"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        animationSpec = androidx.compose.animation.core.tween(duration), label = "border"
    )

    Surface(
        onClick = onClick,
        modifier = modifier.height(84.dp),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = when(option) {
                    DarkModeOption.FOLLOW_SYSTEM -> Icons.Outlined.BrightnessAuto
                    DarkModeOption.LIGHT -> Icons.Outlined.LightMode
                    DarkModeOption.DARK -> Icons.Outlined.DarkMode
                },
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = when(option) {
                    DarkModeOption.FOLLOW_SYSTEM -> stringResource(R.string.theme_follow_system)
                    DarkModeOption.LIGHT -> stringResource(R.string.theme_light)
                    DarkModeOption.DARK -> stringResource(R.string.theme_dark)
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                color = contentColor,
                textAlign = TextAlign.Center,
                lineHeight = 12.sp
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    Relab_toolTheme {
        SettingsScreen()
    }
}
