# Relab Control Center privacy information

This document describes the source in this repository, not a guarantee about third-party services or modified builds.

## On-device diagnostics

The app reads device/build characteristics, available hardware and sensor information, battery history, network/cellular information, and installed-package information. Depending on the feature and permissions, it can read identifiers, Wi-Fi information and location. Camera/microphone diagnostic tests use the corresponding hardware; microphone tests create temporary recordings for playback. Optional permissions can be denied without blocking the main app.

Battery history retains a rolling 24-hour set of samples. Preferences, selected settings, charging metadata and online-spec caches can persist longer. Clear app storage or uninstall to remove app-owned data. Copies exported or shared elsewhere are controlled by the recipient and are not deleted by uninstalling this app.

## Network access and third parties

Online specification lookup downloads a database from GitHub. Installer/icon and store-link features contact their configured providers. The optional download speed test transfers data from Cloudflare. Those services receive ordinary connection/request metadata, including the network IP address, and have their own policies. Opening a store or sharing a report invokes another application chosen by the user.

## Reports

Reports are generated in app-owned storage and shared only through an explicit user action. The hardware PDF excludes the Android ID and Wi-Fi SSID by default, but still contains detailed device/build information. Diagnostic values and operator notes may contain sensitive information. Review the output before sharing; comprehensive report preview and redaction controls are not implemented yet.

Automatic app backup is disabled and backup/transfer rules exclude app data. Platform/OEM behavior still requires device verification. Root mutations are disabled. Local security observations are not Google Play Integrity verdicts.

## Questions and publication

Use the repository's issue tracker for questions without posting private identifiers, recordings, or unredacted reports. This policy exists locally in the source tree; it must be published with the release before relying on an external policy URL.
