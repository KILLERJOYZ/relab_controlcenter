package com.example.relab_tool

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.hardware.camera2.CameraManager
import android.content.Context
import android.util.Log
import com.example.relab_tool.data.CameraSpecRepository
import com.example.relab_tool.utils.UltimateCameraDiagnostics
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CameraDiagInstrumentedTest {

    @Test
    fun runCameraDiagnostics() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraIds = cameraManager.cameraIdList
        
        Log.i("UltimateDiagTest", "=== ALL DETECTED CAMERA IDS: ${cameraIds.joinToString()} ===")
        
        for (id in cameraIds) {
            try {
                val profile = UltimateCameraDiagnostics.getTrueCameraSpecs(context, id)
                Log.i("UltimateDiagTest", "Camera ID: $id -> MP: ${profile.trueMegaPixels}, size: ${profile.width}x${profile.height}, method: ${profile.extractionMethod}, format: ${profile.opticalFormat}, sensor: ${profile.sensorModelName}")
            } catch (e: Exception) {
                Log.e("UltimateDiagTest", "Failed diagnosing Camera ID: $id", e)
            }
        }

        Log.i("UltimateDiagTest", "=== RUNNING CAMERASPECREPOSITORY TEST ===")
        val repo = CameraSpecRepository(context)
        val specs = repo.getCameraSpecs()
        Log.i("UltimateDiagTest", "=== RETURNED SPECS COUNT: ${specs.size} ===")
        for (spec in specs) {
            Log.i("UltimateDiagTest", "Returned Spec: ID=${spec.id}, facing=${spec.facing}, trueMp=${spec.physicalResolutionMp}, binnedSize=${spec.binnedResolutionSize}, sensor=${spec.sensorModel}")
        }
    }
}



