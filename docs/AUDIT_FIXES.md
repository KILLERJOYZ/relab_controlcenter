# Audit implementation status — 2026-09-09

This is the implementation ledger for FULL_APP_AUDIT.md, not a claim that all 42 findings are closed. Changes are in the working tree; no release, commit, or device deployment was performed. “Implemented” below means source changes exist, not that Android runtime acceptance tests passed.

## Verification

- Production battery-history policy and fail-closed root controls compiled with the installed Kotlin compiler and executed: **9 checks passed** (`.artifacts/audit/PolicyChecks.kt`). Equivalent JUnit regression classes were added.
- Kotlin PSI/Java parser check: **103 Kotlin/KTS files and 1 Java file, zero syntax errors**. This includes audit helpers and does not resolve Android types or dependencies.
- Resource XML parsed successfully; `git diff --check` passed (line-ending warnings only).
- Attempted `:app:compileDebugKotlin :app:testDebugUnitTest --offline`. Gradle configured the project, then failed because **Android SDK location is not configured**. Neither Android compilation nor Gradle unit tests ran. Compiler/plugin alignment remains unverified.
- No emulator/device tests, minified-release test, lint run, network throughput experiment, or visual PDF verification has passed in this environment.

## Finding-by-finding status

| Finding | Implementation and remaining acceptance work |
| --- | --- |
| B01 | Open: validate/align AGP built-in Kotlin, Compose compiler and KSP after SDK setup. |
| B02 | Added Vintage runtime so existing JUnit 4 classes can be discovered alongside Jupiter. Test counts still need a Gradle run. |
| B03 | Instrumentation package assertion now uses `BuildConfig.APPLICATION_ID`; emulator verification pending. |
| B04 | Removed tracked machine-specific Java paths and restored normal toolchain detection. Clean-machine CI pending. |
| B05 | Added production battery/root regression checks. Permission, cancellation, report, and service tests remain. |
| B06 | Open: lint and reviewed baseline reduction require Android tooling. |
| S01 | Unsafe root charge/governor writes disabled; compatibility methods always fail closed. Do not re-enable without device-specific units/read-back/rollback. |
| S02 | Removed root execution UI and shell mutation implementation, eliminating the blocking path. No replacement root functionality is claimed. |
| S03 | Integrity fields now nullable/unmeasured; UI states no Play Integrity evaluation. Removed fabricated AVB/green-boot and StrongBox-derived biometric defaults. Remaining local security heuristics need further typed provenance. |
| S04 | Removed mandatory startup permission gate. Settings now links to the OS permission controls; existing feature-specific prompts remain. Contextual Bluetooth/phone requests and deny-all device tests remain. |
| S05 | Added in-app privacy disclosure and repository policy; hardware report excludes Android ID and Wi-Fi SSID. Full preview/redaction controls, diagnostic free-text review, and public policy publication remain. |
| S06 | Open: package-visibility and foreground-service policy review. |
| D01 | Removed fabricated touch latency and unsupported Bluetooth/USB/SIM-tray passes; empty Wi-Fi results skip; unhealthy battery states fail; charging requires observed external power. Full 66-test outcome redesign and real RF/data measurements remain. |
| D02 | Status-only updates preserve value/notes. Separate manual-override audit metadata and export round-trip tests remain. |
| D03 | Slider changes now reach CameraControl and clamp to camera limits. Device verification and asynchronous command-error handling remain. |
| D04 | Camera bindings are disposed per test/lens; delayed callbacks are guarded. Autofocus sends an explicit request and observes success. Device lifecycle tests remain. |
| D05 | Stereo PCM and separate left/right controls implemented. Actual routed-device reporting and physical microphone identification remain. |
| D06 | Interactive layout is keyed by test ID, isolating state and coroutine lifetime. Recorder/player preparation moved off Main. Rapid-switch device tests remain. |
| D07 | Automated work moved to IO with timeout, cancellation propagation and exception handling. Errors currently map to SKIPPED; a richer outcome model and interruptible blocking collectors remain. |
| D08 | CPU worker counts are returned and summed, resets are explicit; storage timing is guarded and readback compared. Longer monotonic storage trials and honest RAM-test scope remain. |
| D09 | Test index uses saveable state and completion timestamp is retained. Explicit reruns/versioned persistence remain. |
| T01 | Removed invented stale FPS fallback; overlay labels app frame callbacks. Other graphics-memory/FPS labels still require a complete scope audit. |
| T02 | Profiles only change app telemetry sampling; removed thread-priority, wake-lock and reflection-hint mutations. |
| T03 | Telemetry snapshots carry monotonic timestamps and HUD warns when stale. Service-owned producers are still needed; expanded stale metrics are not yet individually suppressed. |
| T04 | Rolling 24-hour retention, minute sampling, full-charge transitions, contiguous discharge segments and serialized history recording implemented. Pure calculations tested; persistence/device tests remain. |
| T05 | Notification no longer replaces valid 0% CPU with an estimate. General typed availability/unit/source model remains. |
| T06 | Open: raw numeric wattage/temperature must replace parsing display strings. |
| T07 | Busy cleanup/concurrency guard, cancellation/error handling, monotonic timing, checksum and read+write byte accounting implemented. Warmups, repeats and stability workload redesign remain. |
| T08 | HTTP success checks, timeout/cancellation, decimal Mbps, monotonic timing, throttled UI progress and visible errors implemented. Controlled-server tests remain. |
| T09 | Widget collection moved off the receiver thread using goAsync. Unified sampling, bounded receiver lifetime and worker failure semantics remain. |
| U01 | Icon results merge into current app state by package, preserving installation status. Initial-load sequencing and race regression tests remain. |
| U02 | Mutex serializes fetches; failed fetches can retry; stale cache remains available to lookups. Schema/provenance and cancellable HTTP improvements remain. |
| U03 | System bars and active app theme now observe the same ThemeSettings state. Dead alternate repository cleanup and theme device tests remain. |
| U04 | Open: hide unsupported AssistiveTouch actions and replace reflection-only actions. |
| U05 | Replaced unsupported android.print bridge with public PdfDocument/StaticLayout rendering; deleted the obsolete Java bridge (recoverable from Git). Release export/long-text visual verification remains. |
| U06 | JSON/PDF generation runs on IO, uses unique files, propagates write failures and shares only completed outputs. UI reports failures. PDF pagination, cancellation during rendering and storage/share-target failure tests remain. |
| U07 | Fixed French placeholder token and restored elapsed-time placeholders in Japanese, Russian, Vietnamese and Chinese. New UI messages use base resources. Full translation/format/RTL audit remains. |
| U08 | SMART returns unknown instead of healthy-by-file-existence. Gemini/split-APK/photo-video capability claims still need release-documentation cleanup. |
| U09 | Open: domain collector decomposition and typed injectable interfaces. |
| U10 | Open: orphan navigation, duplicate settings/catalogues, resource/library/keep-rule cleanup. |
| U11 | Disabled backup, excluded app data from cloud/device-transfer rules, removed broad external Download FileProvider path. Device/OEM verification remains. |
| U12 | Open: reconcile and enforce performance contract with targeted StrictMode/CI tests and measurements. |

## Next release gate

1. Configure an Android SDK with platform 35 and compatible build tools, then run debug compilation, unit tests, lint, and minified release build. Resolve toolchain mismatches from actual compiler output.
2. Add/execute production tests for CIT evidence preservation, export errors, permission denial, cancellation, and installer races.
3. Test camera lens switching/zoom/focus and departure; microphone switching and stereo routing; rotation; permission revocation; service-only restart; and PDF rendering on supported devices.
4. Complete the explicitly open/partial items above before describing the app as fully audited and release-ready.
