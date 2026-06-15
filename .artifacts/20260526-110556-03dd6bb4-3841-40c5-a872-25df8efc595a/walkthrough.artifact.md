# Walkthrough: Fix Bluetooth Connected Devices Count

## Problem
The Bluetooth connected devices count on the Dashboard and in the Bluetooth info section was consistently showing `0`, even when devices like headphones (A2DP) were connected.

This occurred because `BluetoothManager.getConnectedDevices(profile)` is documented to return an empty list for many common profiles (A2DP, Headset) unless a specific Profile Proxy is established, which is an asynchronous and complex process unsuitable for a rapid status polling loop.

## Solution
I implemented a multi-tiered detection strategy in [DeviceInfoViewModel.kt](file:///C:/Users/luken/StudioProjects/relab_controlcenter/app/src/main/java/com/example/relab_tool/ui/DeviceInfoViewModel.kt) to reliably count connected devices without adding latency or complexity.

### Multi-tiered Detection Logic
1.  **GATT Profile Check**: Uses the standard API to detect many Low Energy (LE) devices.
2.  **Reflection-based `isConnected()` Check**: Iterates through all bonded (paired) devices and reflectively calls the hidden `isConnected()` method. This is the industry-standard "hack" to reliably detect active connections for A2DP, HFP, and HID profiles in a synchronous manner.
3.  **Broad Profile Fallback**: Performs standard checks for multiple profiles including A2DP, Headset, HID Host, and LE Audio, as some OEM implementations may populate these lists.

The result is a set of unique device addresses that accurately represents the total number of connected Bluetooth peripherals.

## Verification Results
- **Static Analysis**: Verified with `analyze_file` to ensure no syntax errors or permission handling regressions were introduced.
- **Logic Integrity**: The implementation handles `BLUETOOTH_CONNECT` permissions (API 31+) and gracefully catches exceptions during reflection or profile polling.
