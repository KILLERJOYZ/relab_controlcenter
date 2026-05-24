package com.example.relab_tool.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.example.relab_tool.R
import com.example.relab_tool.model.AppInfo

@Preview(showBackground = true)
@Composable
fun AppListItemUpToDatePreview() {
    AppListItem(
        app = AppInfo(
            name = "Antutu Benchmark",
            packageName = "com.antutu.ABenchMark",
            category = "Benchmark",
            installedVersion = "10.0.1",
            latestVersion = "10.0.1",
            hasUpdate = false
        )
    )
}

@Preview(showBackground = true)
@Composable
fun AppListItemNotInstalledPreview() {
    AppListItem(
        app = AppInfo(
            name = "3DMark",
            packageName = "com.futuremark.dmandroid.application",
            category = "Benchmark",
            installedVersion = null,
            latestVersion = "2.0.0",
            hasUpdate = false
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppHubScreen(
    onBack: () -> Unit,
    viewModel: AppHubViewModel = viewModel()
) {
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val updateCount by viewModel.updateCount.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_hub_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.nav_back))
                    }
                },
                actions = {
                    if (updateCount > 0) {
                        BadgedBox(badge = { Badge { Text("$updateCount") } }) {
                            Icon(Icons.Default.SystemUpdate, contentDescription = stringResource(R.string.updates))
                        }
                    }
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh))
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (isRefreshing && apps.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(apps, key = { it.packageName }) { app ->
                        AppListItem(app)
                    }
                }
            }
        }
    }
}

@Composable
fun AppListItem(app: AppInfo) {
    ListItem(
        headlineContent = { Text(app.name, fontWeight = FontWeight.Bold) },
        supportingContent = {
            Column {
                Text(app.packageName, style = MaterialTheme.typography.bodySmall)
                if (app.hasUpdate) {
                    Text(
                        "${app.installedVersion} → ${app.latestVersion}",
                        color = Color(0xFF4CAF50),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        leadingContent = {
            AsyncImage(
                model = app.iconUrl,
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
        },
        trailingContent = {
            when {
                app.hasUpdate -> {
                    Button(
                        onClick = { /* Handle Update */ },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.update))
                    }
                }
                app.installedVersion != null -> {
                    Icon(Icons.Default.Check, contentDescription = stringResource(R.string.up_to_date), tint = Color(0xFF4CAF50))
                }
                else -> {
                    Button(onClick = { /* Handle Download */ }) {
                        Text(stringResource(R.string.download))
                    }
                }
            }
        }
    )
}
