# Fix Bluetooth Connected Devices Count Bug

The Bluetooth connected devices count is always shown as 0 because `BluetoothManager.getConnectedDevices(profile)` is documented to return an empty list for A2DP and Headset profiles. These profiles instead require a Profile Proxy to retrieve connected devices, which is an asynchronous and heavy operation for a synchronous polling loop.

This plan proposes a more robust way to count connected devices by combining `getConnectedDevices(GATT)` with reflection-based checks on `bondedDevices`.

## Proposed Changes

### [DeviceInfoViewModel.kt](file:///C:/Users/luken/StudioProjects/relab_controlcenter/app/src/main/java/com/example/relab_tool/ui/DeviceInfoViewModel.kt)

#### Imports
- Add `import android.bluetooth.BluetoothAdapter`
- Add `import android.bluetooth.BluetoothDevice`

#### [DeviceInfoViewModel.kt](file:///C:/Users/luken/StudioProjects/relab_controlcenter/app/src/main/java/com/example/relab_tool/ui/DeviceInfoViewModel.kt#L740-L760)
- Rewrite `getBluetoothConnectedDevicesCount()` to use multiple detection methods.

```kotlin
    private fun getBluetoothConnectedDevicesCount(): Int {
        val context = getApplication<Application>()
        val hasConnectPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        if (!hasConnectPermission) return 0

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager ?: return 0
        val adapter = bluetoothManager.adapter ?: return 0

        if (!adapter.isEnabled) return 0

        return try {
            val connectedAddresses = mutableSetOf<String>()

            // 1. Method: getConnectedDevices for GATT (reliable for many LE devices)
            try {
                bluetoothManager.getConnectedDevices(BluetoothProfile.GATT).forEach {
                    connectedAddresses.add(it.address)
                }
            } catch (e: Exception) {}

            // 2. Method: Hidden isConnected() via reflection
            // This is the most reliable way to check connection state for bonded devices
            // (A2DP, HFP, HID) without needing asynchronous profile proxies.
            try {
                adapter.bondedDevices.forEach { device ->
                    try {
                        val isConnectedMethod = device.javaClass.getMethod("isConnected")
                        if (isConnectedMethod.invoke(device) as Boolean) {
                            connectedAddresses.add(device.address)
                        }
                    } catch (e: Exception) {}
                }
            } catch (e: Exception) {}

            // 3. Method: Fallback to profile check for common profiles
            // Even though docs say some return empty, OEMs sometimes implement them.
            val profiles = intArrayOf(
                BluetoothProfile.A2DP,
                BluetoothProfile.HEADSET,
                4, // HID_HOST (API 11)
                22 // LE_AUDIO (API 31)
            )
            for (profile in profiles) {
                try {
                    bluetoothManager.getConnectedDevices(profile).forEach {
                        connectedAddresses.add(it.address)
                    }
                } catch (e: Exception) {}
            }

            connectedAddresses.size
        } catch (e: Exception) {
            0
        }
    }
```

## Verification Plan

### Automated Tests
- Not applicable for hardware-dependent Bluetooth features in this environment.

### Manual Verification
- Check the code for syntax errors using `analyze_file`.
- Verify the imports are correctly added.
- Verify the logic correctly handles permissions and potential exceptions.
- Since I cannot physically connect a Bluetooth device to the emulator/device, I will rely on the robustness of the implementation which follows industry standards for such apps.
