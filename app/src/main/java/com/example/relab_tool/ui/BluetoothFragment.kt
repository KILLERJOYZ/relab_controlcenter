package com.example.relab_tool.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.relab_tool.R
import com.example.relab_tool.databinding.FragmentBluetoothBinding
import kotlinx.coroutines.launch

class BluetoothFragment : Fragment() {

    private var _binding: FragmentBluetoothBinding? = null
    private val binding get() = _binding!!

    // Using activityViewModels since the ViewModel is shared across the Compose screens
    private val viewModel: DeviceInfoViewModel by activityViewModels()

    private lateinit var adapter: BluetoothFeaturesAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBluetoothBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = BluetoothFeaturesAdapter()
        binding.recyclerView.adapter = adapter

        setupClickListeners()
        checkPermissionsAndObserve()
    }

    private fun setupClickListeners() {
        binding.cardPairedDevices.setOnClickListener {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            startActivity(intent)
        }

        binding.cardNearbyDevices.setOnClickListener {
            // Placeholder for nearby devices scan
            android.widget.Toast.makeText(requireContext(), getString(R.string.bt_scanning), android.widget.Toast.LENGTH_SHORT).show()
        }

        binding.btnGrantPermission.setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", requireContext().packageName, null)
            }
            startActivity(intent)
        }
    }

    private fun checkPermissionsAndObserve() {
        val hasBluetoothPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        if (!hasBluetoothPermission) {
            binding.permissionRationaleContainer.visibility = View.VISIBLE
            binding.contentContainer.visibility = View.GONE
        } else {
            binding.permissionRationaleContainer.visibility = View.GONE
            binding.contentContainer.visibility = View.VISIBLE
            observeViewModel()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.bluetoothInfo.collect { info ->
                    info?.let {
                        adapter.submitData(it.featureGroups)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
