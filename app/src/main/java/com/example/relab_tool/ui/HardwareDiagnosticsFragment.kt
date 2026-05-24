package com.example.relab_tool.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.example.relab_tool.R
import com.example.relab_tool.databinding.FragmentHardwareDiagnosticsBinding
import com.example.relab_tool.ui.cit.*
import com.example.relab_tool.ui.cit.tests.*
import com.example.relab_tool.ui.theme.Relab_toolTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HardwareDiagnosticsFragment : Fragment() {

    private var _binding: FragmentHardwareDiagnosticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CITViewModel by viewModels()
    private lateinit var adapter: CITAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHardwareDiagnosticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupToolbar()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = CITAdapter { route ->
            launchTest(route)
        }
        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerView.adapter = adapter
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.testResults.collect { results ->
                    val items = createTestItems(results)
                    adapter.submitList(items)
                }
            }
        }
    }

    private fun createTestItems(results: Map<CITTestRoute, CITTestResult>): List<CITTestItem> {
        // This is a bridge between the ViewModel state and the Adapter's display items
        // In a real app, these names would be localized from strings.xml
        return results.map { (route, status) ->
            CITTestItem(
                id = route,
                name = getTestName(route),
                icon = androidx.compose.material.icons.Icons.Default.Info,
                status = status
            )
        }
    }

    private fun getTestName(route: CITTestRoute): String {
        return when (route) {
            CITTestRoute.LCD -> getString(R.string.cit_lcd_color_test)
            CITTestRoute.BRIGHTNESS -> getString(R.string.cit_brightness)
            CITTestRoute.TOUCH -> getString(R.string.cit_touchscreen)
            CITTestRoute.GESTURE -> getString(R.string.cit_gestures)
            CITTestRoute.BUTTONS -> getString(R.string.cit_hardware_buttons)
            CITTestRoute.EARPIECE -> getString(R.string.cit_earpiece)
            CITTestRoute.SPEAKER -> getString(R.string.cit_loudspeaker)
            CITTestRoute.MIC -> getString(R.string.cit_microphone)
            CITTestRoute.HEADPHONE -> getString(R.string.cit_headphone_jack)
            CITTestRoute.CAMERA_FRONT -> getString(R.string.front_camera_test)
            CITTestRoute.CAMERA_REAR -> getString(R.string.rear_camera_test)
            CITTestRoute.FLASHLIGHT -> getString(R.string.flashlight_test_title)
            // ... add others
            else -> route.name
        }
    }

    private fun launchTest(route: CITTestRoute) {
        // Here we can swap the content to a ComposeView that hosts the specific test
        val testView = ComposeView(requireContext()).apply {
            setContent {
                Relab_toolTheme {
                    when (route) {
                        CITTestRoute.LCD -> DisplayLCDTest(onResult = { viewModel.updateTestResult(route, it); closeTest() })
                        CITTestRoute.BRIGHTNESS -> BrightnessTest(onResult = { viewModel.updateTestResult(route, it); closeTest() })
                        CITTestRoute.TOUCH -> TouchscreenTest(onResult = { viewModel.updateTestResult(route, it); closeTest() })
                        CITTestRoute.GESTURE -> GestureTest(onResult = { viewModel.updateTestResult(route, it); closeTest() })
                        CITTestRoute.BUTTONS -> HardwareButtonTest(onResult = { viewModel.updateTestResult(route, it); closeTest() })
                        CITTestRoute.EARPIECE -> EarpieceTest(onResult = { viewModel.updateTestResult(route, it); closeTest() })
                        CITTestRoute.SPEAKER -> SpeakerTest(onResult = { viewModel.updateTestResult(route, it); closeTest() })
                        CITTestRoute.MIC -> MicrophoneTest(onResult = { viewModel.updateTestResult(route, it); closeTest() })
                        CITTestRoute.HEADPHONE -> HeadphoneTest(onResult = { viewModel.updateTestResult(route, it); closeTest() })
                        CITTestRoute.CAMERA_FRONT -> FrontCameraTest(onResult = { viewModel.updateTestResult(route, it); closeTest() })
                        CITTestRoute.CAMERA_REAR -> RearCameraTest(onResult = { viewModel.updateTestResult(route, it); closeTest() })
                        CITTestRoute.FLASHLIGHT -> FlashlightTest(onResult = { viewModel.updateTestResult(route, it); closeTest() })
                        CITTestRoute.ACCEL -> AccelerometerTest(onResult = { viewModel.updateTestResult(route, it); closeTest() })
                        CITTestRoute.GYRO -> GyroscopeTest(onResult = { viewModel.updateTestResult(route, it); closeTest() })
                        CITTestRoute.PROXIMITY -> ProximityTest(onResult = { viewModel.updateTestResult(route, it); closeTest() })
                        CITTestRoute.LIGHT -> LightSensorTest(onResult = { viewModel.updateTestResult(route, it); closeTest() })
                        CITTestRoute.COMPASS -> CompassTest(onResult = { viewModel.updateTestResult(route, it); closeTest() })
                        CITTestRoute.BAROMETER -> BarometerTest(onResult = { viewModel.updateTestResult(route, it); closeTest() })
                        CITTestRoute.WIFI -> WifiTest(onResult = { viewModel.updateTestResult(route, it); closeTest() })
                        CITTestRoute.BLUETOOTH -> BluetoothTest(onResult = { viewModel.updateTestResult(route, it); closeTest() })
                        CITTestRoute.GPS -> GPSTest(onResult = { viewModel.updateTestResult(route, it); closeTest() })
                        CITTestRoute.NFC -> NFCTest(onResult = { viewModel.updateTestResult(route, it); closeTest() })
                        CITTestRoute.VIBRATION -> VibrationTest(onResult = { viewModel.updateTestResult(route, it); closeTest() })
                        CITTestRoute.FINGERPRINT -> FingerprintTest(onResult = { viewModel.updateTestResult(route, it); closeTest() })
                        CITTestRoute.BATTERY_INFO -> BatteryInfoTest(onResult = { viewModel.updateTestResult(route, it); closeTest() })
                        CITTestRoute.DEVICE_INFO -> DeviceInfoSummaryTest(onResult = { viewModel.updateTestResult(route, it); closeTest() })
                        else -> {}
                    }
                }
            }
        }
        
        // For simplicity in this refactor, I'll just use the existing CITRootScreen logic 
        // but triggered from this Fragment if needed, or host the tests here.
        // Actually, the user wants the UI Consistency Refactor.
    }

    private fun closeTest() {
        // Logic to return to the dashboard
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
