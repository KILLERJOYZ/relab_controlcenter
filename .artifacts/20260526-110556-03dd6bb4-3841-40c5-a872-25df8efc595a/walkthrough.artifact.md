# Walkthrough: USB Information Tab Implementation

## Goal
Implement a comprehensive USB information tab in the Device Info section to provide users with deep insights into their device's USB capabilities, current connection status, OTG devices, video output support, and security settings.

## Implementation Details

### Data Models
I expanded the `UsbInfo` model and introduced `UsbStatusInfo` in [DeviceInfoModels.kt](file:///C:/Users/luken/StudioProjects/relab_controlcenter/app/src/main/java/com/example/relab_tool/model/DeviceInfoModels.kt) to capture:
- Detailed OTG device metadata (Manufacturer, Product Name, Serial Number).
- Comprehensive connection status (Mode, Uptime).
- Hardware standards (Version, Connector Type, Bandwidth).
- Advanced features (Video Output, Security).

### ViewModel Integration
In `DeviceInfoViewModel.kt`, I implemented:
- **Real-time Monitoring**: A `usbReceiver` (`BroadcastReceiver`) tracks `ACTION_USB_DEVICE_ATTACHED`, `ACTION_USB_DEVICE_DETACHED`, and `USB_STATE` to update the UI immediately when a cable is plugged in or a mode changes.
- **Hardware Probing**: Logic to detect USB versions and connector types using system properties and sysfs path heuristics.
- **OTG Scanning**: Uses `UsbManager` to list connected peripherals and maps their class codes to human-readable labels (e.g., "Mass Storage", "HID").
- **Security Audit**: Monitors ADB status and calculates USB tethering state.

### UI Design
The [UsbTab](file:///C:/Users/luken/StudioProjects/relab_controlcenter/app/src/main/java/com/example/relab_tool/ui/DeviceInfoScreen.kt) was rewritten using Jetpack Compose and Material3:
1.  **Connection Status Card**: Shows real-time connection state, mode (Charging/MTP/etc.), and a live connection uptime counter.
2.  **USB Standard Card**: Displays identified USB versions and bandwidth capabilities.
3.  **OTG Devices Card**: Lists all connected peripherals with their IDs and classes.
4.  **Video Output Card**: Highlights DisplayPort and HDMI Alt Mode support (or lack thereof).
5.  **USB Security Card**: Shows ADB and Tethering status for quick auditing.

## Verification Results
- **Reactive UI**: Confirmed that the `usbStatus` StateFlow correctly triggers UI updates.
- **Robustness**: Implemented fallback values ("N/A", "Không xác định") and try-catch blocks for hardware-specific property lookups to prevent crashes on non-compliant devices.
- **Design Consistency**: Followed the app's `InfoGroupCard` and `InfoRow` patterns with Material3 color tokens.
