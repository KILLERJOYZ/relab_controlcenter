package com.example.relab_tool.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.relab_tool.R
import com.example.relab_tool.model.AppInfo
import com.example.relab_tool.model.InstallationStatus
import com.example.relab_tool.model.SearchResult
import kotlinx.coroutines.launch
import java.util.Locale
import android.content.Context
import java.text.Normalizer

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(viewModel: AppInstallerViewModel, windowSizeClass: WindowSizeClass, onLaunchCIT: () -> Unit = {}) {
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val isAppsLoaded by viewModel.isLoaded.collectAsStateWithLifecycle()
    val deviceInfoViewModel: DeviceInfoViewModel = hiltViewModel()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, deviceInfoViewModel) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                deviceInfoViewModel.setAppInForeground(true)
            } else if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE) {
                deviceInfoViewModel.setAppInForeground(false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
            deviceInfoViewModel.setAppInForeground(true)
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            deviceInfoViewModel.setAppInForeground(false)
        }
    }
    val performanceViewModel: PerformanceViewModel = hiltViewModel()
    val performanceRepository = performanceViewModel.performanceRepository
    val towerInfoProvider = performanceViewModel.towerInfoProvider
    val pagerState = rememberPagerState(pageCount = { 5 })
    val coroutineScope = rememberCoroutineScope()

    // Listen for Dashboard card taps that want to deep-link into DeviceInfoScreen
    val requestedInfoTab by deviceInfoViewModel.requestedInfoTab.collectAsStateWithLifecycle()
    LaunchedEffect(requestedInfoTab) {
        val tab = requestedInfoTab ?: return@LaunchedEffect
        // Smoother transition to Device Info page
        pagerState.animateScrollToPage(
            page = 3,
            animationSpec = tween(
                durationMillis = 350,
                easing = FastOutSlowInEasing
            )
        )
    }

    val useNavigationRail = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

    // Scroll-aware behavior for navigation bars
    var navBarVisible by remember { mutableStateOf(true) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showSearch by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val navHeight = 64.dp
    val systemNavigationPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomPadding = 24.dp + systemNavigationPadding
    val totalHeight = navHeight + bottomPadding

    val targetOffset = if (navBarVisible) 0.dp else totalHeight
    val animatedOffset by animateDpAsState(
        targetValue = targetOffset,
        animationSpec = if (navBarVisible) {
            spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy)
        } else {
            spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioNoBouncy)
        },
        label = "nav_offset"
    )

    val animatedScale by animateFloatAsState(
        targetValue = if (navBarVisible) 1f else 0.85f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "nav_scale"
    )

    val animatedAlpha by animateFloatAsState(
        targetValue = if (navBarVisible) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "nav_alpha"
    )

    val nestedScrollConnection = remember(scrollBehavior) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: NestedScrollSource): androidx.compose.ui.geometry.Offset {
                // Scrolling down (swipe up)
                if (available.y < -15f) {
                    navBarVisible = false
                }
                // Scrolling up (swipe down)
                else if (available.y > 15f) {
                    navBarVisible = true
                }
                return scrollBehavior.nestedScrollConnection.onPreScroll(available, source)
            }

            override fun onPostScroll(
                consumed: androidx.compose.ui.geometry.Offset,
                available: androidx.compose.ui.geometry.Offset,
                source: NestedScrollSource
            ): androidx.compose.ui.geometry.Offset {
                return scrollBehavior.nestedScrollConnection.onPostScroll(consumed, available, source)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                return scrollBehavior.nestedScrollConnection.onPreFling(available)
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (available.y > 1000f) {
                    navBarVisible = true
                } else if (available.y < -1000f) {
                    navBarVisible = false
                }
                return scrollBehavior.nestedScrollConnection.onPostFling(consumed, available)
            }
        }
    }

    val navItems = listOf(
        NavItem(stringResource(R.string.nav_home), Icons.Default.Home, 0),
        NavItem(stringResource(R.string.nav_installer), Icons.Default.Apps, 1),
        NavItem(stringResource(R.string.nav_benchmarks), Icons.Default.Speed, 2),
        NavItem(stringResource(R.string.nav_device_info), Icons.Default.Info, 3),
        NavItem(stringResource(R.string.nav_settings), Icons.Default.Settings, 4)
    )

    Row(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
    ) {
        if (useNavigationRail) {
            FloatingNavigationRail(
                items = navItems,
                selectedIndex = pagerState.currentPage,
                onItemSelected = { index ->
                    coroutineScope.launch { pagerState.animateScrollToPage(index) }
                }
            )
        }

        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
            Scaffold(
                modifier = Modifier.nestedScroll(nestedScrollConnection),
                topBar = {
                    val isCompassActive by deviceInfoViewModel.isSatelliteCompassActive.collectAsStateWithLifecycle()
                    if (!isCompassActive) {
                        val collapsedFraction = scrollBehavior.state.collapsedFraction
                        val titleAlpha = 1f - collapsedFraction
                        LargeTopAppBar(
                            modifier = Modifier.graphicsLayer {
                                if (pagerState.currentPage == 3) {
                                    alpha = titleAlpha
                                }
                            },
                            title = {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .graphicsLayer { alpha = titleAlpha }
                                        .padding(start = 4.dp, end = 20.dp, bottom = 16.dp)
                                ) {
                                    Text(
                                        text = when(pagerState.currentPage) {
                                            0 -> stringResource(R.string.nav_home)
                                            1 -> stringResource(R.string.nav_installer)
                                            2 -> stringResource(R.string.nav_benchmarks)
                                            3 -> stringResource(R.string.nav_device_info)
                                            else -> stringResource(R.string.nav_settings)
                                        },
                                        style = MaterialTheme.typography.displaySmall,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = (-1.5).sp
                                    )
                                }
                            },
                            scrollBehavior = scrollBehavior,
                            colors = TopAppBarDefaults.largeTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background,
                                scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                                titleContentColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            ) { padding ->
                val isCompassActive by deviceInfoViewModel.isSatelliteCompassActive.collectAsStateWithLifecycle()
                val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                val minTopPadding = if (pagerState.currentPage == 3) {
                    statusBarPadding
                } else {
                    56.dp + statusBarPadding
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .padding(
                                top = if (isCompassActive) 0.dp else minTopPadding,
                                bottom = if (isCompassActive) 0.dp else padding.calculateBottomPadding()
                            )
                            .offset {
                                if (isCompassActive) {
                                    IntOffset.Zero
                                } else {
                                    val minTopPaddingPx = minTopPadding.toPx()
                                    val topPaddingPx = padding.calculateTopPadding().toPx()
                                    val heightOffsetPx = scrollBehavior.state.heightOffset
                                    val yCurrentPx = (topPaddingPx + heightOffsetPx).coerceAtLeast(minTopPaddingPx)
                                    IntOffset(0, (yCurrentPx - minTopPaddingPx).roundToInt())
                                }
                            }
                            .fillMaxSize(),
                        userScrollEnabled = !isCompassActive,
                        beyondViewportPageCount = 1
                    ) { page ->
                        when (page) {
                            0 -> DashboardTab(
                                viewModel = deviceInfoViewModel,
                                windowSizeClass = windowSizeClass,
                                onNavigateToInfoTab = { tabIndex ->
                                    deviceInfoViewModel.requestInfoTab(tabIndex)
                                }
                            )
                            1 -> {
                                // Trigger lazy load the first time the installer tab is visited
                                LaunchedEffect(Unit) { viewModel.ensureLoaded() }
                                AppInstallerContent(apps = apps, viewModel = viewModel, windowSizeClass = windowSizeClass, isLoaded = isAppsLoaded)
                            }
                            2 -> BenchmarksScreen(viewModel = performanceViewModel, windowSizeClass = windowSizeClass)
                            3 -> DeviceInfoScreen(viewModel = deviceInfoViewModel, windowSizeClass = windowSizeClass)
                            4 -> SettingsScreen(onLaunchCIT = onLaunchCIT)
                        }
                    }

                    if (pagerState.currentPage == 3 && !isCompassActive) {
                        val collapsedFraction = scrollBehavior.state.collapsedFraction
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(statusBarPadding)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = collapsedFraction))
                        )
                    }
                }
            }

            if (!useNavigationRail) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = bottomPadding)
                        .graphicsLayer {
                            translationY = animatedOffset.toPx()
                            scaleX = animatedScale
                            scaleY = animatedScale
                            alpha = animatedAlpha
                            rotationX = (animatedOffset.value / totalHeight.value) * -35f
                            rotationZ = (animatedOffset.value / totalHeight.value) * 5f
                            cameraDistance = 12 * density.density
                            shadowElevation = if (navBarVisible) 16f else 0f
                        },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .widthIn(max = 600.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FloatingNavigationBar(
                            modifier = Modifier.weight(1f, fill = false),
                            items = navItems,
                            selectedIndex = pagerState.currentPage,
                            onItemSelected = { index ->
                                coroutineScope.launch { pagerState.animateScrollToPage(index) }
                            }
                        )

                        FloatingSearchButton(
                            onClick = { showSearch = true }
                        )
                    }
                }
            }

            // Tablet/rail mode: floating search FAB at bottom-end
            if (useNavigationRail) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            end = 24.dp,
                            bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                        ),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    FloatingSearchButton(onClick = { showSearch = true })
                }
            }

            if (showSearch) {
                SearchOverlay(
                    viewModel = deviceInfoViewModel,
                    onDismiss = { showSearch = false }
                )
            }
        }
    }
}

data class NavItem(val title: String, val icon: ImageVector, val index: Int)

@Composable
fun FloatingNavigationBar(
    items: List<NavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val navHeight = 64.dp
    
    Surface(
        modifier = modifier
            .height(navHeight)
            .widthIn(min = 280.dp),
        shape = RoundedCornerShape(navHeight / 2),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 16.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        val callbacks = remember(items, onItemSelected) {
            items.map { item -> { onItemSelected(item.index) } }
        }
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            items.forEachIndexed { idx, item ->
                FloatingNavItem(
                    item = item,
                    isSelected = selectedIndex == item.index,
                    onClick = callbacks[idx]
                )
            }
        }
    }
}

@Composable
fun FloatingSearchButton(onClick: () -> Unit) {
    val navHeight = 64.dp
    Surface(
        modifier = Modifier
            .size(navHeight)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick
            ),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primary,
        tonalElevation = 12.dp,
        shadowElevation = 24.dp,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = stringResource(id = R.string.search_device_info),
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchOverlay(
    viewModel: DeviceInfoViewModel,
    onDismiss: () -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Text(
                stringResource(id = R.string.search_device_info),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1.5).sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(id = R.string.search_placeholder)) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(id = R.string.cancel))
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            if (searchQuery.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(bottom = 100.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Info, 
                            null, 
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            stringResource(id = R.string.search_instruction),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                SearchResultsList(viewModel = viewModel)
            }
        }
    }
}


@Composable
fun SearchResultsList(viewModel: DeviceInfoViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filteredResults by viewModel.searchResults.collectAsStateWithLifecycle()

    if (filteredResults.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(bottom = 100.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(id = R.string.no_results_found, searchQuery), style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        // Group results by category
        val grouped = remember(filteredResults) {
            filteredResults.groupBy { it.category }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp, start = 0.dp, end = 0.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            grouped.forEach { (category, results) ->
                // Category section header
                item(key = "header_$category") {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp, start = 4.dp)
                    )
                }
                items(results, key = { "${it.category}_${it.label}_${it.value}" }) { result ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = result.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = result.value,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            // Category badge
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text(
                                    text = result.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.FloatingNavItem(
    item: NavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else if (isSelected) 1.05f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "item_scale"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "content_color"
    )

    val indicatorScale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "indicator_scale"
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                onClick = onClick
            )
            .graphicsLayer {
                // Defer scale read into graphicsLayer — runs on RenderThread, zero recomposition
                scaleX = scale
                scaleY = scale
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Pill/Circle highlight bubble
            Box(
                modifier = Modifier
                    .size(width = 64.dp, height = 32.dp)
                    .graphicsLayer {
                        scaleX = indicatorScale
                        scaleY = indicatorScale
                        alpha = indicatorScale
                    }
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp))
            )
            
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else contentColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun FloatingNavigationRail(
    items: List<NavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isExpanded = configuration.screenWidthDp >= 840
    val railWidth = if (isExpanded) 240.dp else 72.dp
    val cardShape = if (isExpanded) RoundedCornerShape(28.dp) else RoundedCornerShape(36.dp)

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .padding(
                start = 16.dp,
                top = 24.dp,
                bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                end = 8.dp
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Surface(
            modifier = Modifier.width(railWidth).fillMaxHeight(),
            shape = cardShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 16.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalArrangement = if (isExpanded) Arrangement.spacedBy(8.dp, Alignment.Top) else Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = if (isExpanded) Alignment.Start else Alignment.CenterHorizontally
            ) {
                if (isExpanded) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                    )
                }

                val railCallbacks = remember(items, onItemSelected) {
                    items.map { item -> { onItemSelected(item.index) } }
                }
                items.forEachIndexed { idx, item ->
                    FloatingRailItem(
                        item = item,
                        isSelected = selectedIndex == item.index,
                        isExpanded = isExpanded,
                        onClick = railCallbacks[idx]
                    )
                }
            }
        }
    }
}

@Composable
fun FloatingRailItem(
    item: NavItem,
    isSelected: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else if (isSelected) 1.02f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "rail_item_scale"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "rail_content_color"
    )

    val containerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else Color.Transparent,
        label = "rail_container_color"
    )

    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (isExpanded) {
                    Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                } else {
                    Modifier.size(56.dp)
                }
            )
            .clip(shape)
            .background(containerColor)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                onClick = onClick
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        if (isExpanded) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else contentColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else contentColor
                )
            }
        } else {
            val indicatorScale by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0f,
                animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "rail_indicator_scale"
            )
            
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(48.dp)
                    .graphicsLayer {
                        scaleX = indicatorScale
                        scaleY = indicatorScale
                        alpha = indicatorScale
                    }
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
            )
            
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else contentColor,
                modifier = Modifier.size(26.dp).align(Alignment.Center)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppInstallerContent(apps: List<AppInfo>, viewModel: AppInstallerViewModel, windowSizeClass: WindowSizeClass, isLoaded: Boolean = true) {
    // Defer rendering until installation statuses are resolved to prevent first-frame flash
    if (!isLoaded) return

    var isGalleryView by remember { mutableStateOf(true) }
    val isWideScreen = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
    val columns = if (isWideScreen) (if (isGalleryView) 6 else 2) else (if (isGalleryView) 3 else 1)
    
    val benchmarkStr = stringResource(R.string.cat_benchmark)
    val gamesStr = stringResource(R.string.cat_games)
    val utilitiesStr = stringResource(R.string.cat_utilities)
    val systemStr = stringResource(R.string.cat_system)
    val socialStr = stringResource(R.string.cat_social_short)

    val categorizedApps = remember(apps, columns, benchmarkStr, gamesStr, utilitiesStr, systemStr, socialStr) {
        val categories = listOf(
            benchmarkStr to "Benchmark",
            gamesStr to "Games",
            utilitiesStr to "Utilities",
            systemStr to "System Services",
            socialStr to "Social"
        )
        categories.mapNotNull { (localizedName, categoryId) ->
            val categoryApps = apps.filter { it.category == categoryId }
            if (categoryApps.isNotEmpty()) {
                Triple(localizedName, categoryId, categoryApps.chunked(columns))
            } else null
        }
    }

    val context = LocalContext.current
    val bottomContentPadding = if (isWideScreen) 16.dp else 120.dp

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(
                onClick = { isGalleryView = !isGalleryView },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Icon(
                    imageVector = if (isGalleryView) Icons.AutoMirrored.Filled.List else Icons.Default.GridView,
                    contentDescription = if (isGalleryView) stringResource(R.string.view_list) else stringResource(R.string.view_gallery)
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomContentPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            categorizedApps.forEach { (localizedCategory, categoryId, chunks) ->
                item(key = categoryId) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Category Header inside the Card
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = when (categoryId) {
                                        "Benchmark" -> Icons.Default.Monitor
                                        "Games" -> Icons.Default.VideogameAsset
                                        "System Services" -> Icons.Default.Build
                                        "Social" -> Icons.AutoMirrored.Filled.Chat
                                        else -> Icons.Default.Settings
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = localizedCategory,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // App Grid / List
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                chunks.forEach { chunk ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        chunk.forEach { app ->
                                            Box(modifier = Modifier.weight(1f)) {
                                                if (isGalleryView) {
                                                    AppGalleryCard(app = app, onInstallClick = { viewModel.openAppListing(context, app) })
                                                } else {
                                                    AppCard(app = app, onInstallClick = { viewModel.openAppListing(context, app) })
                                                }
                                            }
                                        }
                                        if (chunk.size < columns) {
                                            repeat(columns - chunk.size) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppGalleryCard(app: AppInfo, onInstallClick: () -> Unit) {
    val context = LocalContext.current
    val appName = app.nameRes?.let { stringResource(it) } ?: app.name
    val imageModel = remember(app.iconUrl) {
        if (app.iconUrl?.startsWith("res:") == true) {
            val resName = app.iconUrl.substring(4)
            val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)
            if (resId != 0) resId else null
        } else {
            app.iconUrl
        }
    }

    // Load application icon asynchronously if installed on device
    val iconDrawable by produceState<android.graphics.drawable.Drawable?>(initialValue = null, keys = arrayOf(app.packageName)) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                context.packageManager.getApplicationIcon(app.packageName)
            } catch (e: Exception) {
                null
            }
        }
    }

    val placeholderIcon = when (app.category) {
        "Benchmark" -> Icons.Default.Monitor
        "Games" -> Icons.Default.VideogameAsset
        "System Services" -> Icons.Default.Build
        "Social" -> Icons.AutoMirrored.Filled.Chat
        else -> Icons.Default.Settings
    }

    val isInstalled = app.status == InstallationStatus.INSTALLED
    
    // Fix First Frame Flash by using Animation for color and border
    val animatedColor by animateColorAsState(
        targetValue = if (isInstalled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "card_bg_color"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isInstalled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "card_content_color"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clickable(
                onClick = onInstallClick
            ),
        shape = RoundedCornerShape(16.dp),
        color = animatedColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Icon with Badge
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isInstalled) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (iconDrawable != null) {
                        AsyncImage(
                            model = iconDrawable,
                            contentDescription = stringResource(id = R.string.app_name) + " " + appName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (imageModel != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(imageModel)
                                .crossfade(true)
                                .build(),
                            contentDescription = stringResource(id = R.string.app_name) + " " + appName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = placeholderIcon,
                            contentDescription = null,
                            tint = if (isInstalled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                
                if (isInstalled) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-6).dp)
                            .size(20.dp)
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                            .padding(1.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-6).dp)
                            .size(20.dp)
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                            .padding(2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = appName, 
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isInstalled) FontWeight.ExtraBold else FontWeight.Bold,
                color = contentColor,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun AppCard(app: AppInfo, onInstallClick: () -> Unit) {
    val context = LocalContext.current
    val appName = app.nameRes?.let { stringResource(it) } ?: app.name
    val imageModel = remember(app.iconUrl) {
        if (app.iconUrl?.startsWith("res:") == true) {
            val resName = app.iconUrl.substring(4)
            val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)
            if (resId != 0) resId else null
        } else {
            app.iconUrl
        }
    }

    // Load application icon asynchronously if installed on device
    val iconDrawable by produceState<android.graphics.drawable.Drawable?>(initialValue = null, keys = arrayOf(app.packageName)) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                context.packageManager.getApplicationIcon(app.packageName)
            } catch (e: Exception) {
                null
            }
        }
    }

    val placeholderIcon = when (app.category) {
        "Benchmark" -> Icons.Default.Monitor
        "Games" -> Icons.Default.VideogameAsset
        "System Services" -> Icons.Default.Build
        "Social" -> Icons.AutoMirrored.Filled.Chat
        else -> Icons.Default.Settings
    }

    val isInstalled = app.status == InstallationStatus.INSTALLED
    
    // Fix First Frame Flash by using Animation
    val animatedColor by animateColorAsState(
        targetValue = if (isInstalled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "list_card_bg"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isInstalled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "list_card_content"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = animatedColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // App Icon with Badge
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isInstalled) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (iconDrawable != null) {
                        AsyncImage(
                            model = iconDrawable,
                            contentDescription = stringResource(id = R.string.app_name) + " " + appName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (imageModel != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(imageModel)
                                .crossfade(true)
                                .build(),
                            contentDescription = stringResource(id = R.string.app_name) + " " + appName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = placeholderIcon,
                            contentDescription = null,
                            tint = if (isInstalled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                if (isInstalled) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                            .size(18.dp)
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                            .padding(1.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appName, 
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isInstalled) FontWeight.ExtraBold else FontWeight.Medium,
                    color = contentColor
                )
                Text(
                    text = app.packageName, 
                    style = MaterialTheme.typography.bodySmall, 
                    maxLines = 1,
                    color = contentColor.copy(alpha = 0.7f)
                )
            }

            IconButton(
                onClick = onInstallClick
            ) {
                if (isInstalled) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = stringResource(R.string.status_installed),
                        tint = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = stringResource(R.string.status_install),
                        tint = contentColor
                    )
                }
            }
        }
    }
}
