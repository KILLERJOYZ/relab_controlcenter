package com.example.relab_tool.ui.cit.tests

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.ui.res.stringResource
import com.example.relab_tool.R
import com.example.relab_tool.ui.cit.CITTestResult

@Composable
fun FrontCameraTest(onResult: (CITTestResult) -> Unit) {
    CameraPreviewScreen(stringResource(R.string.front_camera_test), CameraSelector.LENS_FACING_FRONT, onResult)
}

@Composable
fun RearCameraTest(onResult: (CITTestResult) -> Unit) {
    CameraPreviewScreen(stringResource(R.string.rear_camera_test), CameraSelector.LENS_FACING_BACK, onResult)
}

@Composable
fun CameraPreviewScreen(title: String, lensFacing: Int, onResult: (CITTestResult) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by remember { 
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) 
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
    }

    DisposableEffect(Unit) {
        if (!hasPermission) launcher.launch(Manifest.permission.CAMERA)
        onDispose { }
    }

    if (!hasPermission) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.camera_permission_required_msg))
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(16.dp))
        
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
                        
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                        } catch (e: Exception) {
                            // Hardware might be missing
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { onResult(CITTestResult.FAIL) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(stringResource(R.string.cit_fail)) }
            Button(onClick = { onResult(CITTestResult.PASS) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { Text(stringResource(R.string.cit_pass)) }
        }
    }
}

@Composable
fun FlashlightTest(onResult: (CITTestResult) -> Unit) {
    val context = LocalContext.current
    val cameraManager = remember { context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager }
    var isOn by remember { mutableStateOf(false) }
    var cameraId by remember { mutableStateOf<String?>(null) }
    
    DisposableEffect(Unit) {
        try {
            val list = cameraManager?.cameraIdList ?: emptyArray()
            for (id in list) {
                val characteristics = cameraManager?.getCameraCharacteristics(id) ?: continue
                val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraId = id
                    break
                }
            }
        } catch (_: Throwable) { }

        onDispose {
            cameraId?.let { id ->
                try {
                    cameraManager?.setTorchMode(id, false)
                } catch (_: Throwable) {}
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.flashlight_test_title), style = MaterialTheme.typography.headlineMedium)
            
            Button(
                onClick = {
                    cameraId?.let { id ->
                        try {
                            isOn = !isOn
                            cameraManager?.setTorchMode(id, isOn)
                        } catch (_: Throwable) {}
                    }
                }
            ) {
                Text(if (isOn) stringResource(R.string.disabled).uppercase() else stringResource(R.string.enabled).uppercase())
            }

            Spacer(modifier = Modifier.height(32.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = { onResult(CITTestResult.FAIL) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(stringResource(R.string.cit_fail)) }
                Button(onClick = { onResult(CITTestResult.PASS) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { Text(stringResource(R.string.cit_pass)) }
            }
        }
    }
}
