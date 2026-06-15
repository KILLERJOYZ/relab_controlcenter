package com.example.relab_tool.ui

import android.os.Build
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.relab_tool.R
import com.example.relab_tool.ui.theme.DarkModeOption
import com.example.relab_tool.ui.theme.ThemeViewModel
import com.example.relab_tool.ui.theme.ShapeLarge
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    viewModel: ThemeViewModel,
    onBack: () -> Unit
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val useDynamicColor by viewModel.useDynamicColor.collectAsStateWithLifecycle()
    val seedColor by viewModel.seedColor.collectAsStateWithLifecycle()

    var showCustomPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_theme)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Theme Mode Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.theme_dark_mode),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    DarkModeOption.entries.forEachIndexed { index, option ->
                        val label = when (option) {
                            DarkModeOption.FOLLOW_SYSTEM -> stringResource(R.string.theme_follow_system)
                            DarkModeOption.LIGHT -> stringResource(R.string.theme_light)
                            DarkModeOption.DARK -> stringResource(R.string.theme_dark)
                        }
                        SegmentedButton(
                            selected = themeMode == option,
                            onClick = { viewModel.setThemeMode(option) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = DarkModeOption.entries.size),
                            label = { Text(label) }
                        )
                    }
                }
            }

            // Dynamic Color Toggle
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                InfoGroupCard(
                    title = stringResource(R.string.theme_dynamic_color),
                    icon = Icons.Default.Add, // Using Add as a placeholder for dynamic
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.palette_dynamic_desc),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                stringResource(R.string.theme_follow_system_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = useDynamicColor,
                            onCheckedChange = { viewModel.setDynamicColor(it) }
                        )
                    }
                }
            }

            // Color Palette Section
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.theme_color_palette),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                val palettes = listOf(
                    PaletteItem(R.string.palette_ocean, 0xFF0061A4),
                    PaletteItem(R.string.palette_forest, 0xFF006D3B),
                    PaletteItem(R.string.palette_sunset, 0xFF9C4146),
                    PaletteItem(R.string.palette_lavender, 0xFF715573),
                    PaletteItem(R.string.palette_mono, 0xFF605D62)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(end = 16.dp)
                ) {
                    items(palettes) { palette ->
                        val isSelected = !useDynamicColor && seedColor == palette.color
                        SwatchCard(
                            nameRes = palette.nameRes,
                            color = Color(palette.color),
                            isSelected = isSelected,
                            onClick = {
                                viewModel.setDynamicColor(false)
                                viewModel.setSeedColor(palette.color)
                            }
                        )
                    }
                    item {
                        // Custom Card
                        val isCustomSelected = !useDynamicColor && palettes.none { it.color == seedColor }
                        CustomSwatchCard(
                            isSelected = isCustomSelected,
                            currentColor = if (isCustomSelected) Color(seedColor) else null,
                            onClick = { showCustomPicker = true }
                        )
                    }
                }
            }
        }
    }

    if (showCustomPicker) {
        CustomColorBottomSheet(
            initialColor = Color(seedColor),
            onDismiss = { showCustomPicker = false },
            onApply = { color ->
                viewModel.setDynamicColor(false)
                viewModel.setSeedColor(color.toArgb().toLong())
                showCustomPicker = false
            }
        )
    }
}

data class PaletteItem(val nameRes: Int, val color: Long)

@Composable
fun SwatchCard(
    nameRes: Int,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(100.dp)
            .clickable(onClick = onClick),
        shape = ShapeLarge,
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                }
            }
            Text(
                text = stringResource(nameRes),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun CustomSwatchCard(
    isSelected: Boolean,
    currentColor: Color?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(100.dp)
            .clickable(onClick = onClick),
        shape = ShapeLarge,
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (currentColor != null) currentColor 
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (currentColor == null) {
                    Icon(Icons.Default.Add, contentDescription = null)
                } else if (isSelected) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                }
            }
            Text(
                text = stringResource(R.string.palette_custom),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomColorBottomSheet(
    initialColor: Color,
    onDismiss: () -> Unit,
    onApply: (Color) -> Unit
) {
    var red by remember { mutableFloatStateOf(initialColor.red) }
    var green by remember { mutableFloatStateOf(initialColor.green) }
    var blue by remember { mutableFloatStateOf(initialColor.blue) }

    val currentColor = Color(red, green, blue)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.select_custom_color), style = MaterialTheme.typography.headlineSmall)
            
            // Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(ShapeLarge)
                    .background(currentColor)
            )

            // Sliders
            ColorSlider(stringResource(R.string.color_red), red) { red = it }
            ColorSlider(stringResource(R.string.color_green), green) { green = it }
            ColorSlider(stringResource(R.string.color_blue), blue) { blue = it }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.cancel))
                }
                Button(
                    onClick = { onApply(currentColor) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.apply))
                }
            }
        }
    }
}

@Composable
fun ColorSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Column {
        Text("$label: ${(value * 255).toInt()}", style = MaterialTheme.typography.labelMedium)
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
