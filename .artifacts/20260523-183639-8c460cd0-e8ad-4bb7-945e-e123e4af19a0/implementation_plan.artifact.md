# Fix for Dashboard Tile Resizing Crashes

Diagnose and resolve crashes occurring when tile sizes change dynamically in the Dashboard.

## Proposed Changes

### UI Components

#### [DeviceInfoScreen.kt](file:///C:/Users/luken/AndroidStudioProjects/relab_tool/app/src/main/java/com/example/relab_tool/ui/DeviceInfoScreen.kt)
- Review `DashboardTab` and how it handles dynamic size changes.
- Ensure state keys are stable during resizing.

#### [LiveTile.kt](file:///C:/Users/luken/AndroidStudioProjects/relab_tool/app/src/main/java/com/example/relab_tool/ui/LiveTile.kt)
- Check animation logic and state preservation during size transitions.

## Verification Plan

### Manual Verification
- Trigger tile resizing multiple times.
- Monitor logcat for specific error stack traces.
