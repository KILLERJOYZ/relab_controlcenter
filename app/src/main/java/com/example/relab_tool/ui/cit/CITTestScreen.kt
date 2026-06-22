package com.example.relab_tool.ui.cit

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.relab_tool.R
import com.example.relab_tool.ui.theme.ShapeCard

enum class CITTestRoute {
    DASHBOARD,
    DISPLAY_TOUCH,
    SENSORS,
    AUDIO,
    CAMERA,
    CONNECTIVITY,
    PHYSICAL_BUTTONS,
    PERFORMANCE,
    PORTS_MISC
}

data class DashboardCategoryItem(
    val route: CITTestRoute,
    val name: String,
    val icon: ImageVector,
    val totalCount: Int,
    val completedCount: Int,
    val passedCount: Int,
    val failedCount: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CITRootScreen(onExit: () -> Unit, viewModel: CITViewModel = viewModel()) {
    var currentRoute by rememberSaveable { mutableStateOf(CITTestRoute.DASHBOARD.name) }
    val session by viewModel.session.collectAsStateWithLifecycle()
    var isBatchMode by rememberSaveable { mutableStateOf(false) }
    var showBatchCompleteDialog by remember { mutableStateOf(false) }

    val navigateTo: (CITTestRoute) -> Unit = { route ->
        currentRoute = route.name
    }

    val onCancelBatch: () -> Unit = {
        if (isBatchMode) {
            // Find next uncompleted category to continue the batch
            val nextCat = session.categories.firstOrNull { cat ->
                cat.tests.any { it.status == TestStatus.PENDING }
            }
            if (nextCat != null) {
                currentRoute = nextCat.categoryId
            } else {
                isBatchMode = false
                currentRoute = CITTestRoute.DASHBOARD.name
                showBatchCompleteDialog = true
            }
        } else {
            currentRoute = CITTestRoute.DASHBOARD.name
        }
    }

    val startBatchTest: () -> Unit = {
        isBatchMode = true
        val nextCat = session.categories.firstOrNull { cat ->
            cat.tests.any { it.status == TestStatus.PENDING }
        }
        if (nextCat != null) {
            currentRoute = nextCat.categoryId
        } else {
            isBatchMode = false
            showBatchCompleteDialog = true
        }
    }

    val routeEnum = try { CITTestRoute.valueOf(currentRoute) } catch (e: Exception) { CITTestRoute.DASHBOARD }

    if (showBatchCompleteDialog) {
        val allTests = session.categories.flatMap { it.tests }
        val passed = allTests.count { it.status == TestStatus.PASS }
        val failed = allTests.count { it.status == TestStatus.FAIL }
        val skipped = allTests.count { it.status == TestStatus.SKIPPED }
        
        AlertDialog(
            onDismissRequest = { showBatchCompleteDialog = false },
            title = { Text("Diagnostics Complete", fontWeight = FontWeight.Bold) },
            text = { Text("Guided wizard diagnostics finished!\n\nPassed: $passed\nFailed: $failed\nSkipped: $skipped") },
            confirmButton = {
                Button(onClick = { showBatchCompleteDialog = false }) {
                    Text("OK")
                }
            },
            shape = ShapeCard
        )
    }

    Crossfade(targetState = routeEnum, label = "CIT_Crossfade") { route ->
        when (route) {
            CITTestRoute.DASHBOARD -> CITDashboard(
                session = session,
                onNavigate = navigateTo,
                onExit = onExit,
                onReset = { viewModel.resetSession() },
                onStartBatch = startBatchTest
            )
            else -> CITCategoryWizardScreen(
                categoryId = route.name,
                viewModel = viewModel,
                onBack = onCancelBatch
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CITDashboard(
    session: DiagnosticSession,
    onNavigate: (CITTestRoute) -> Unit,
    onExit: () -> Unit,
    onReset: () -> Unit,
    onStartBatch: () -> Unit
) {
    val context = LocalContext.current
    
    val categoryItems = remember(session) {
        session.categories.map { cat ->
            val route = try { CITTestRoute.valueOf(cat.categoryId) } catch (e: Exception) { CITTestRoute.DASHBOARD }
            val icon = when (cat.categoryId) {
                "DISPLAY_TOUCH" -> Icons.Default.TouchApp
                "SENSORS" -> Icons.Default.ScreenRotation
                "AUDIO" -> Icons.Default.Mic
                "CAMERA" -> Icons.Default.CameraRear
                "CONNECTIVITY" -> Icons.Default.Wifi
                "PHYSICAL_BUTTONS" -> Icons.Default.AdsClick
                "PERFORMANCE" -> Icons.Default.BatteryFull
                else -> Icons.Default.Info
            }
            DashboardCategoryItem(
                route = route,
                name = cat.categoryName,
                icon = icon,
                totalCount = cat.tests.size,
                completedCount = cat.tests.count { it.status != TestStatus.PENDING },
                passedCount = cat.tests.count { it.status == TestStatus.PASS },
                failedCount = cat.tests.count { it.status == TestStatus.FAIL }
            )
        }
    }

    val totalSubTests = categoryItems.sumOf { it.totalCount }
    val completedSubTests = categoryItems.sumOf { it.completedCount }
    val passedSubTests = categoryItems.sumOf { it.passedCount }
    val failedSubTests = categoryItems.sumOf { it.failedCount }
    val progressFraction = if (totalSubTests > 0) completedSubTests.toFloat() / totalSubTests else 0f

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.hardware_diagnostics), fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(id = R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = ShapeCard,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Diagnostics Progress",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${(progressFraction * 100).toInt()}%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Passed: $passedSubTests",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                        Column {
                            Text(
                                text = "Failed: $failedSubTests",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Column {
                            Text(
                                text = "Pending: ${totalSubTests - completedSubTests}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val batchInteraction = remember { MutableInteractionSource() }
                        val batchPressed by batchInteraction.collectIsPressedAsState()
                        val batchScale by animateFloatAsState(
                            targetValue = if (batchPressed) 0.96f else 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "batch_scale"
                        )

                        Button(
                            onClick = onStartBatch,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .graphicsLayer { scaleX = batchScale; scaleY = batchScale },
                            shape = ShapeCard,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            interactionSource = batchInteraction
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (completedSubTests == 0) "Run All Tests" else "Resume Guided Flow",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                com.example.relab_tool.utils.CITReportPdfGenerator.generateAndShareReport(context, session)
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = ShapeCard,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Export PDF", 
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                com.example.relab_tool.utils.CITReportJsonExporter.exportAndShareJson(context, session)
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = ShapeCard,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Export JSON", 
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onReset,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = ShapeCard,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Reset All Progress", 
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(categoryItems) { item ->
                    CategoryCard(item = item, onClick = { onNavigate(item.route) })
                }
            }
        }
    }
}

@Composable
fun CategoryCard(item: DashboardCategoryItem, onClick: () -> Unit) {
    val completed = item.completedCount
    val total = item.totalCount
    val isDone = completed == total

    val cardColor = if (item.failedCount > 0) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
    } else if (isDone && item.passedCount > 0) {
        Color(0xFFE8F5E9).copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }

    val tintColor = if (item.failedCount > 0) {
        MaterialTheme.colorScheme.error
    } else if (isDone) {
        Color(0xFF2E7D32)
    } else {
        MaterialTheme.colorScheme.primary
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "card_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(125.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            ),
        shape = ShapeCard,
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = BorderStroke(
            width = 1.dp,
            color = if (item.failedCount > 0) MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                    else if (isDone) Color(0xFF2E7D32).copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = tintColor,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(tintColor.copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "$completed / $total COMPLETE",
                    style = MaterialTheme.typography.labelSmall,
                    color = tintColor,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
