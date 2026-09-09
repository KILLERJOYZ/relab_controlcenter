# ReLab Control Center — full application audit

Audited revision: `d979bd8` (version 0.4.2). Audit scope: build configuration, Kotlin/Java syntax, resources, feature logic, state and lifecycle handling, diagnostics, telemetry, background services, reports, privacy, and tests. No production behavior was changed during this audit.

This is a source audit with executable parser checks and an attempted Android build. A source-reviewed feature is not a device-tested feature. Physical camera, radio, sensor, root, and OEM behavior still require the device matrix at the end of this report.

## Verification evidence

| Check | Result | What this establishes |
|---|---|---|
| Kotlin PSI parser | 98 tracked `.kt`/`.kts` files; zero syntax errors | Syntax only, using Android Studio's bundled Kotlin parser; no symbol/type/Compose validation |
| Java compiler parse phase | 1 tracked Java file; zero syntax errors | Grammar only; Android imports and runtime package access are not resolved |
| XML parser | 64 tracked XML files; all parsed | Well-formed XML; not AAPT resource compilation |
| JSON parser | 22 tracked JSON files; all parsed | Valid JSON syntax; not schema or factual accuracy |
| Gradle configuration | Passed project configuration using Gradle 9.4.1 and downloaded JDK 21 | Build scripts/plugins configured; deprecation warnings remain |
| Android compilation, lint, unit tests | Blocked before task execution: `SDK location not found` | No semantic Android compile, lint, or unit-test pass is claimed |
| Physical-device/instrumentation run | Not executed | No connected/configured Android device or SDK was established |
| Main source inventory | 89 Kotlin/Java files; 33,071 physical lines | Count includes blank/comment lines |
| CIT inventory | 66 tests in 8 categories: 28 automated, 38 manual | All 66 mapped below |
| Resource inventory | 1,479 base strings plus 13 synonym arrays; 16 translated locales | Each translated file has 1,473 strings, missing the same 6 new settings strings |
| Existing lint baseline | 687 stored entries; 30 have no remaining location file | Historical debt, not a fresh lint result |

Parser harness: [SyntaxAudit.java](C:/Users/ASUS/OneDrive/Documents/GitHub/relab_controlcenter/.artifacts/audit/SyntaxAudit.java). It normalizes Windows line endings before handing text to Kotlin PSI, parses Java without attribution, and deliberately makes no claim about semantic compilation.

The first wrapper attempt could not find Java on PATH. A usable Java 25 runtime was subsequently found inside Android Studio. The wrapper downloaded Gradle 9.4.1, then requested Java 21 through the committed daemon criteria. Its first JDK installation failed moving files inside the OneDrive workspace. The downloaded JDK was extracted successfully into a temporary directory and the build was retried there. It configured `:app`, warned about deprecated `enableStrongSkippingMode`/`stabilityConfigurationFile` and experimental `android.disallowKotlinSourceSets`, then failed while determining `testDebugUnitTest` dependencies because the Android SDK location was missing. The requested command was `testDebugUnitTest lintDebug assembleDebug --continue --console=plain`. This supersedes the earlier report's statement that no usable Java runtime was installed.

The build's temporary Gradle cache/JDK is outside the repository under `C:\Users\ASUS\AppData\Local\Temp\relab-audit-0f6fb47a17ac4464937e66bd4f7f69ac`. No SDK was installed or license agreement accepted. The remaining build prerequisite is a valid Android SDK with the required platform/build tools, exposed through `ANDROID_HOME` or an untracked `local.properties` file.

## Overall assessment

The app contains substantial working implementations, but diagnostic credibility and lifecycle correctness need attention before a release advertised for professional verification. The most consequential issues are incorrect root-node units, local heuristics labeled as integrity results, diagnostics returning PASS without testing the advertised behavior, camera controls that do not apply, and missing test discovery. Syntax is currently much healthier than behavioral correctness.

Priority meanings: **P1** = address before a trusted/public release; **P2** = next stabilization cycle; **P3** = maintainability or product enhancement. **Confirmed** means the behavior is evident in source; **risk** means a plausible execution path needs device/build reproduction; **improvement** means no specific failure is asserted.

## Prioritized findings and acceptance criteria

### Build and quality gates

**B01 — P1, build risk: align the compiler/plugin toolchain.** The version catalogue pins Compose compiler 2.0.21 and KSP 2.0.21-1.0.26 while using AGP 9.2.1 with built-in Kotlin. AGP 9.2 release notes record its Kotlin Gradle plugin dependency update to 2.3.10. The older Compose/KSP declarations need validation against the effective resolved compiler. Project configuration succeeded in this audit; a compiler incompatibility was not reproduced because the missing SDK stopped compilation. Do not add `kotlin-android` blindly: built-in Kotlin is enabled by default for AGP 9. Evidence: [versions catalogue](C:/Users/ASUS/OneDrive/Documents/GitHub/relab_controlcenter/gradle/libs.versions.toml:2), [AGP 9.2 release notes](https://developer.android.com/build/releases/agp-9-2-0-release-notes), [built-in Kotlin migration](https://developer.android.com/build/migrate-to-built-in-kotlin). Acceptance: clean debug and minified release compilation on CI with one documented, compatible version set.

**B02 — P1, confirmed: four unit-test classes are not discoverable by the selected test engine.** The build calls `useJUnitPlatform()` and supplies Jupiter, but four test classes import `org.junit.Test` and there is no Vintage engine. These tests are excluded from normal Jupiter discovery. Add Vintage or migrate their annotations and assertions consistently. Evidence: [test configuration](C:/Users/ASUS/OneDrive/Documents/GitHub/relab_controlcenter/app/build.gradle.kts:65), [JUnit migration documentation](https://docs.junit.org/6.0.2/migrating-from-junit4.html). Acceptance: assert expected test-class/test-case counts in CI; a deliberately failing test in each class must fail the task.

**B03 — P1, confirmed: instrumentation package assertion is wrong.** The test expects `com.example.relab_tool`; installed package identity is `com.relab.controlcenter`. Evidence: [ExampleInstrumentedTest.kt](C:/Users/ASUS/OneDrive/Documents/GitHub/relab_controlcenter/app/src/androidTest/java/com/example/relab_tool/ExampleInstrumentedTest.kt:22). Acceptance: verify the intended application ID on an emulator, not the source namespace.

**B04 — P2, confirmed: build portability is tied to one Windows installation.** `gradle.properties` hardcodes Android Studio's JBR path and disables toolchain autodetection, while `gradle-daemon-jvm.properties` independently requests Java 21. The installed JBR is Java 25. Evidence: [gradle.properties](C:/Users/ASUS/OneDrive/Documents/GitHub/relab_controlcenter/gradle.properties:17). Acceptance: fresh Windows and Linux CI setup can build without editing tracked absolute paths; document daemon JDK separately from Java bytecode target 17.

**B05 — P2, confirmed: current tests provide limited production coverage.** AssistiveTouch tests exercise a fake defined in the test file; camera instrumentation primarily logs; the example arithmetic test validates no app behavior. `isReturnDefaultValues=true` can also hide accidental calls to Android stubs. Add tests around production interfaces/repositories and state transitions. Acceptance: tests cover permission denial, cancellation, result preservation, collector failures, report errors, locale formats, and service restart.

**B06 — P2, confirmed: lint baseline conceals and retains debt.** Its 687 historical entries include 295 unused resources, 44 string-format matches, 35 hardcoded-text entries, and 16 missing-translation entries. Some source locations no longer exist. Regenerate only after reviewing each remaining category, then reject new baseline entries in CI. Evidence: [lint-baseline.xml](C:/Users/ASUS/OneDrive/Documents/GitHub/relab_controlcenter/app/lint-baseline.xml:1). Acceptance: current lint output and baseline are distinguishable, with no new errors and an explicit shrinking backlog.

### Root, security, permissions, and privacy

**S01 — P1, confirmed: charge-limit writes use incompatible sysfs semantics.** `setChargeLimit` can write `80`/`100` into `constant_charge_current_max`, a current-control node, as though it were a percentage. It also writes `charge_control_limit_max` without a device-specific contract. Disable the feature until supported nodes have verified units, bounds, read-back, and rollback. Evidence: [RootUtils.kt](C:/Users/ASUS/OneDrive/Documents/GitHub/relab_controlcenter/app/src/main/java/com/example/relab_tool/utils/RootUtils.kt:30). Acceptance: mocked and real supported-device tests verify exact node/unit/value, retain old values, reject unsupported devices, and surface failures.

**S02 — P1, confirmed: root work blocks the UI and ignores failures.** `su` waits synchronously; Settings invokes it directly from callbacks; charge/governor return values are ignored. Root detection only probes two legacy paths; governor changes only target cpu0. Evidence: [SettingsScreen.kt](C:/Users/ASUS/OneDrive/Documents/GitHub/relab_controlcenter/app/src/main/java/com/example/relab_tool/ui/SettingsScreen.kt:405). Acceptance: bounded IO execution, progress/cancel/error states, read-back confirmation, policy/cluster enumeration, and a root-denied test.

**S03 — P1, confirmed: local heuristics masquerade as integrity verdicts.** Basic/device/strong integrity booleans are inferred from root, bootloader, and StrongBox checks. These are not Play Integrity responses. StrongBox is also used to infer biometric strength, although it describes key storage. Defaults such as AVB 1.0 or a green boot state can turn missing evidence into apparent assurance. Evidence: [security collector](C:/Users/ASUS/OneDrive/Documents/GitHub/relab_controlcenter/app/src/main/java/com/example/relab_tool/ui/DeviceInfoViewModel.kt:4280), [integrity booleans](C:/Users/ASUS/OneDrive/Documents/GitHub/relab_controlcenter/app/src/main/java/com/example/relab_tool/ui/DeviceInfoViewModel.kt:4462), [actual Play Integrity verdicts](https://developer.android.com/google/play/integrity/verdicts). Acceptance: label local observations as such; unsupported/unknown properties remain unknown; real integrity names are used only with verified API results.

**S04 — P1, confirmed: denying optional permissions blocks the entire app.** Startup requires camera/phone permissions and, on newer releases, notifications. The app should allow features that do not need a denied permission. Evidence: [permission gate](C:/Users/ASUS/OneDrive/Documents/GitHub/relab_controlcenter/app/src/main/java/com/example/relab_tool/MainActivity.kt:272). Acceptance: deny every optional permission on a clean install and still open Dashboard, Settings, and permission-independent device details. Request access in context; see [Android guidance](https://developer.android.com/training/permissions/usage-notes).

**S05 — P1, confirmed gap: privacy policy and export disclosure.** Settings points to a repository `PRIVACY_POLICY.md` that is absent. The app reads identifiers, installed-app information, cellular/network data, and location; some flows fetch online specifications/icons. Add a working policy, data-source disclosure, retention explanation, and report redaction. Evidence: [privacy link](C:/Users/ASUS/OneDrive/Documents/GitHub/relab_controlcenter/app/src/main/java/com/example/relab_tool/ui/SettingsScreen.kt:220). Acceptance: policy resolves publicly; report previews explicitly show included identifiers and default to excluding them.

**S06 — P2, policy review: scope package visibility and service declarations.** Broad `QUERY_ALL_PACKAGES` is unnecessary for the fixed installer catalogue, though the full installed-app audit may depend on it. Three services declare `specialUse`. Their eligibility needs a documented core use case; source declarations alone do not establish Play acceptance. Evidence: [manifest](C:/Users/ASUS/OneDrive/Documents/GitHub/relab_controlcenter/app/src/main/AndroidManifest.xml:28), [package visibility policy](https://support.google.com/googleplay/android-developer/answer/10158779?hl=en), [foreground-service type requirements](https://developer.android.com/about/versions/14/changes/fgs-types-required). Acceptance: scoped queries wherever sufficient and a justified permission/service inventory.

### CIT test engine and hardware controls

**D01 — P1, confirmed: PASS can mean failure, absence, or no measurement.** Touch latency is hardcoded at 12 ms. Battery health returns PASS even for Dead, Overheated, and Over Voltage. Wi-Fi scan catches denied access and reports PASS with zero networks. Bluetooth RF scan does not perform discovery. USB/wireless charging checks return PASS while not charging. Correct the individual rules listed in the complete test matrix. Evidence: [test definitions](C:/Users/ASUS/OneDrive/Documents/GitHub/relab_controlcenter/app/src/main/java/com/example/relab_tool/ui/cit/CITSubTestManager.kt:87). Acceptance: no PASS without explicit evidence for the stated test; unavailable, permission denied, not configured, and failed have separate outcomes.

**D02 — P1, confirmed: PASS/FAIL/SKIP erase recorded evidence.** Buttons call `updateSubTestResult` without `value` or `notes`, whose defaults are null; the ViewModel overwrites both. A successful fingerprint scan, GPS accuracy, or CPU stress count can disappear when the operator confirms or changes its status. Evidence: [PASS callback](C:/Users/ASUS/OneDrive/Documents/GitHub/relab_controlcenter/app/src/main/java/com/example/relab_tool/ui/cit/CITCategoryWizardScreen.kt:507), [result update](C:/Users/ASUS/OneDrive/Documents/GitHub/relab_controlcenter/app/src/main/java/com/example/relab_tool/ui/cit/CITViewModel.kt:73). Acceptance: status-only edits preserve measurements, record manual overrides separately, and round-trip through JSON/PDF.

**D03 — P1, confirmed: zoom slider does not update the camera.** `setZoomRatio` runs once inside `AndroidView.factory`; `update` is empty. The slider changes Compose state only. Its hardcoded 1–5 range can exceed hardware limits. Evidence: [camera zoom binding](C:/Users/ASUS/OneDrive/Documents/GitHub/relab_controlcenter/app/src/main/java/com/example/relab_tool/ui/cit/CITCategoryWizardScreen.kt:1250). Acceptance: retain CameraControl, apply each slider change, derive min/max from ZoomState, observe failures, and verify the viewfinder changes.

**D04 — P1, lifecycle risk: camera remains bound after leaving the test.** Preview binds to the activity lifecycle; the camera branch has no disposal unbind. Moving to another test can leave its use case bound until another camera bind or activity lifecycle change. Autofocus also has no explicit focus request/result verification. Evidence: [camera binding](C:/Users/ASUS/OneDrive/Documents/GitHub/relab_controlcenter/app/src/main/java/com/example/relab_tool/ui/cit/CITCategoryWizardScreen.kt:1246). Acceptance: leaving the camera test immediately releases its use cases, cancels delayed provider callbacks, and autofocus reports an observed result.

**D05 — P1, confirmed: stereo test is mono; microphone names overstate routing.** All speaker tests call a generator configured with `CHANNEL_OUT_MONO`. MIC_SEC chooses CAMCORDER audio source, which does not prove a particular physical top microphone is selected. Earpiece usage attributes alone do not prove actual output route. Evidence: [audio generation](C:/Users/ASUS/OneDrive/Documents/GitHub/relab_controlcenter/app/src/main/java/com/example/relab_tool/ui/cit/CITCategoryWizardScreen.kt:127), [mic source](C:/Users/ASUS/OneDrive/Documents/GitHub/relab_controlcenter/app/src/main/java/com/example/relab_tool/ui/cit/CITCategoryWizardScreen.kt:1040). Acceptance: channel-isolated left/right playback, actual routed-device display, restoration of changed audio state, and honest source labels.

**D06 — P2, confirmed/risk: per-test state and recording lifetime are not isolated.** `InteractiveTestLayout` is called without a test-ID key. Shared `remember` positions can retain state between tests using the same branch; `micFile` is remembered without testId. The common coroutine scope survives some test switches, while disposal deletes the recording file. Acceptance: key the test session/state by ID, cancel its recording/playback work on departure, and verify switching MIC_MAIN→MIC_SEC during recording and playback. Evidence: [interactive invocation](C:/Users/ASUS/OneDrive/Documents/GitHub/relab_controlcenter/app/src/main/java/com/example/relab_tool/ui/cit/CITCategoryWizardScreen.kt:411).

**D07 — P1, confirmed: automated runner performs blocking work on Main and lacks failure isolation.** `LaunchedEffect` directly invokes tests that allocate 50 MB, perform storage IO, and call hardware services. Exceptions can terminate the effect and leave PENDING results. Evidence: [automated runner](C:/Users/ASUS/OneDrive/Documents/GitHub/relab_controlcenter/app/src/main/java/com/example/relab_tool/ui/cit/CITCategoryWizardScreen.kt:228). Acceptance: test-specific dispatchers, timeout/cancellation, explicit error outcome, and navigation responsiveness during each test.

**D08 — P2, confirmed: stress results are not robust.** CPU stress adds counts from multiple coroutines into shared Compose state using non-atomic `+=`, and does not reset count before another run. Storage timing can divide by zero at millisecond resolution and does not compare read bytes with written bytes. RAM allocation alone does not test memory leaks or integrity. Acceptance: return worker counts then sum once, reset each run, use monotonic high-resolution timing and minimum duration, verify data, and scope benchmark claims accurately. Evidence: [CPU stress](C:/Users/ASUS/OneDrive/Documents/GitHub/relab_controlcenter/app/src/main/java/com/example/relab_tool/ui/cit/CITCategoryWizardScreen.kt:1638), [storage test](C:/Users/ASUS/OneDrive/Documents/GitHub/relab_controlcenter/app/src/main/java/com/example/relab_tool/ui/cit/CITSubTestManager.kt:636).

**D09 — P2, confirmed: session restoration loses navigation context.** Results use SavedStateHandle, but the current test index uses `remember` and returns to zero on recreation; revisiting automated tests reruns them. Completion timestamps are rewritten on every edit after completion. Acceptance: restore category/test position, provide explicit rerun, retain the original completion time, and define a versioned session format.

### Telemetry, battery, benchmarks, and background operation

**T01 — P1, confirmed: FPS and graphics-memory scope are misleading.** Overlay FPS counts the app's Choreographer callbacks, not another application's rendered frames. DeviceInfo substitutes 1/10 FPS for old callback timestamps. GPU memory uses `Debug.getMemoryInfo` for this process, not total GPU memory. Evidence: [overlay counter](C:/Users/ASUS/OneDrive/Documents/GitHub/relab_controlcenter/app/src/main/java/com/example/relab_tool/worker/PerformanceOverlayService.kt:88), [GPU memory](C:/Users/ASUS/OneDrive/Documents/GitHub/relab_controlcenter/app/src/main/java/com/example/relab_tool/ui/DeviceInfoViewModel.kt:1152). Acceptance: label scope clearly, remove invented fallback FPS, and show unavailable where no authorized external measurement exists.

**T02 — P1, confirmed: performance profiles alter the UI thread's priority.** `PerformanceProfileManager.apply` is called from service onCreate and Compose onClick, both on Main; `Process.setThreadPriority(priority)` changes the calling thread. Saver can therefore put the main thread at background priority. `release()` does not restore priority. Hints reference that same thread, and no actual-work-duration reporting is present. Acceptance: target owned worker threads explicitly, restore their settings, and describe profiles as app behavior rather than global game acceleration. Evidence: [priority change](C:/Users/ASUS/OneDrive/Documents/GitHub/relab_controlcenter/app/src/main/java/com/example/relab_tool/data/PerformanceProfileManager.kt:53).

**T03 — P1, lifecycle risk: overlay telemetry depends on an activity-owned ViewModel.** The service consumes a process singleton populated by DeviceInfoViewModel. Clearing that ViewModel cancels the producers; restarting the sticky service in a fresh process does not instantiate them. The HUD can show frozen/default values. Acceptance: a service/application-owned telemetry repository with subscriber-driven collection, timestamps, and a stale indicator; test activity finish and service-only process restart. Evidence: [telemetry update](C:/Users/ASUS/OneDrive/Documents/GitHub/relab_controlcenter/app/src/main/java/com/example/relab_tool/ui/DeviceInfoViewModel.kt:1425), [service consumer](C:/Users/ASUS/OneDrive/Documents/GitHub/relab_controlcenter/app/src/main/java/com/example/relab_tool/worker/PerformanceOverlayService.kt:284).

**T04 — P2, confirmed: battery history does not implement a rolling 24-hour record.** History and full-charge markers reset when the date changes. Every sample at 100% resets last-full-charge time, so it measures the most recent sample rather than the transition. Discharge-rate calculation joins all noncharging points across intervening charging sessions. Concurrent repositories use read-modify-write without shared serialization. Acceptance: timestamp-bounded history, transition-based full-charge events, separate discharge sessions, serialized writes, and tests spanning midnight/charging interruptions. Evidence: [BatteryHistoryRepository.kt](C:/Users/ASUS/OneDrive/Documents/GitHub/relab_controlcenter/app/src/main/java/com/example/relab_tool/data/BatteryHistoryRepository.kt:142). Correction to the previous audit: history JSON is not rewritten on every unchanged 2-second sample; the 100% timestamp is repeatedly written.

**T05 — P2, confirmed: zeros and estimates hide sensor/API failure.** CPU utilization may be estimated from frequency; the notification collector even replaces a legitimate 0% with that estimate. GPU frequency/temperature return zero on missing data. CPU temperature can fall back to a battery/system zone or the hottest arbitrary zone. Graphics-frequency unit detection uses a magnitude threshold that misinterprets some low-frequency or kHz values. Acceptance: typed values with unit, source, scope, freshness and unavailable reason; node-specific conversion rules; zero remains a valid measurement.

**T06 — P2, confirmed: localized display strings are parsed back into telemetry numbers.** Wattage and temperatures are formatted first, then read with `toFloat`/`toFloatOrNull`. Decimal-comma locales can produce zero/error fallbacks. Acceptance: keep raw numeric values through collectors/history/graphs; localize only at rendering. Evidence: [history calculations](C:/Users/ASUS/OneDrive/Documents/GitHub/relab_controlcenter/app/src/main/java/com/example/relab_tool/ui/DeviceInfoViewModel.kt:1290).

**T07 — P2, confirmed: benchmark cleanup and measurement definitions need work.** CPU/RAM benchmarks lack `finally` around the busy flag; cancellation/errors can leave it true. RAM measures a write plus read but divides by one buffer's byte count, has no zero-duration guard, and does not expose a checksum. Frequency stability monitoring does not itself apply load. Acceptance: guaranteed cleanup, concurrency guard, warmups/repeats, retained checksum, documented bytes processed and workload, and reproducible comparison conditions. Evidence: [PerformanceRepository.kt](C:/Users/ASUS/OneDrive/Documents/GitHub/relab_controlcenter/app/src/main/java/com/example/relab_tool/data/PerformanceRepository.kt:24).

**T08 — P2, confirmed: speed test can measure an HTTP error body and outlive cancellation.** It never checks `response.isSuccessful`; synchronous reads are not connected to coroutine cancellation. Its ten-second limit is checked only after a read, and reported Mbps uses binary megabits. Acceptance: successful response/content validation, explicit deadline and call cancellation, monotonic timing, standard decimal Mbps or correctly labeled Mibit/s, and visible failure states. Evidence: [NetworkUtils.kt](C:/Users/ASUS/OneDrive/Documents/GitHub/relab_controlcenter/app/src/main/java/com/example/relab_tool/utils/NetworkUtils.kt:47).

**T09 — P2, confirmed: widget/background collection needs lifecycle and cost controls.** Widget `onUpdate` performs filesystem and service calls synchronously on the receiver thread. Separate worker, notification, and ViewModel samplers duplicate work. Battery worker catches Throwable and reports success even when collection fails. Acceptance: cached snapshots or bounded asynchronous receiver work, one sampling coordinator, explicit worker outcomes, and device battery/jank measurements. Evidence: [DashboardWidgets.kt](C:/Users/ASUS/OneDrive/Documents/GitHub/relab_controlcenter/app/src/main/java/com/example/relab_tool/widget/DashboardWidgets.kt:17).

### Installer, UI, reports, and architecture

**U01 — P2, confirmed: icon loading can revert installation status.** `ensureLoaded` launches `refreshStatuses` independently, snapshots `_apps`, awaits icon fetches, and assigns the whole old list back. A newer installed/uninstalled status can be lost. Acceptance: merge icon results into current state by package ID, await initial status loading, and test a package change during delayed icon fetch. Evidence: [AppInstallerViewModel.kt](C:/Users/ASUS/OneDrive/Documents/GitHub/relab_controlcenter/app/src/main/java/com/example/relab_tool/ui/AppInstallerViewModel.kt:96).

**U02 — P2, confirmed: online spec lookup cannot recover in the same process.** `fetchAttempted` is set before loading; after a failed request it remains true and prevents retries. A concurrent caller can get null while the first request is in flight. Expired data is not retained as a stale offline fallback. Acceptance: a shared in-flight Deferred/Mutex, explicit retry, last-good cache fallback, schema validation, and lookup provenance. Evidence: [OnlineSpecLookup.kt](C:/Users/ASUS/OneDrive/Documents/GitHub/relab_controlcenter/app/src/main/java/com/example/relab_tool/utils/spec/OnlineSpecLookup.kt:51).

**U03 — P2, confirmed: theme persistence is split.** MainActivity reads ThemeViewModel/DataStore for system-bar state, but active Settings and app theme use ThemeSettings/SharedPreferences. Acceptance: one repository for color/dark/high-contrast state and tests for light↔dark while the OS uses the opposite mode. Evidence: [MainActivity.kt](C:/Users/ASUS/OneDrive/Documents/GitHub/relab_controlcenter/app/src/main/java/com/example/relab_tool/MainActivity.kt:142), [Theme.kt](C:/Users/ASUS/OneDrive/Documents/GitHub/relab_controlcenter/app/src/main/java/com/example/relab_tool/ui/theme/Theme.kt:105).

**U04 — P2, confirmed: AssistiveTouch exposes unavailable actions.** Back, Recents, Screenshot and Lock Screen resolve to unavailable messages. Notification/quick-settings expansion relies on reflection; no AccessibilityService is registered even though related detection/settings remain. Acceptance: hide or disable unavailable actions with accurate explanations, use supported routes, and verify rotation, edge positions, overlay-permission revocation, and service restart. Evidence: [AssistiveTouchService.kt](C:/Users/ASUS/OneDrive/Documents/GitHub/relab_controlcenter/app/src/main/java/com/example/relab_tool/worker/AssistiveTouchService.kt).

**U05 — P1, runtime risk: hardware PDF export uses an unsupported framework-package bridge.** The custom Java `android.print.PrintResultCallbackBypass` tries to access nonpublic callback construction. Android's runtime package/classloader access rules make this fragile even though syntax parses. Acceptance: use supported PdfDocument/public print APIs and verify minified release export on supported Android versions. Evidence: [PrintResultCallbackBypass.java](C:/Users/ASUS/OneDrive/Documents/GitHub/relab_controlcenter/app/src/main/java/android/print/PrintResultCallbackBypass.java:1), [ReportGenerator.kt](C:/Users/ASUS/OneDrive/Documents/GitHub/relab_controlcenter/app/src/main/java/com/example/relab_tool/utils/ReportGenerator.kt:75).

**U06 — P2, confirmed: export failures can still launch sharing.** JSON export catches write errors and continues to create a URI and share a fixed filename; this can expose an older report or fail again. Reports are generated synchronously from UI actions. Acceptance: IO generation, explicit Result, unique/atomic output, share only after successful completion, and tests for unavailable storage, no share target, duplicate exports, and long text. Evidence: [CITReportJsonExporter.kt](C:/Users/ASUS/OneDrive/Documents/GitHub/relab_controlcenter/app/src/main/java/com/example/relab_tool/utils/CITReportJsonExporter.kt:55).

**U07 — P2, confirmed: localization affects behavior as well as text.** Six ordinary settings strings are absent from all translations; 13 arrays use base fallback. French `total_label` contains `PHTOK0FIN`. Several locales drop the elapsed-time placeholders from `last_charge_full`. CIT, notifications, and reports contain hardcoded English; widgets include hardcoded Vietnamese. Acceptance: format-signature parity, placeholder-token rejection, screenshot tests at large fonts/RTL, and end-to-end locale coverage beyond Settings. Evidence: [French placeholder](C:/Users/ASUS/OneDrive/Documents/GitHub/relab_controlcenter/app/src/main/res/values-fr/strings.xml:337), [Vietnamese elapsed-time label](C:/Users/ASUS/OneDrive/Documents/GitHub/relab_controlcenter/app/src/main/res/values-vi/strings.xml:1081).

**U08 — P2, confirmed: some advertised functionality is absent or overstated.** Gemini resolution is a placeholder. Installer opens listings and has no split-APK intelligence. SMART health checks file existence rather than health values. Photo/video CIT are generic manual prompts. Acceptance: a capability checklist used to approve release notes/README; every claim links to an implementation and validation evidence. Evidence: [DeviceNameResolver.kt](C:/Users/ASUS/OneDrive/Documents/GitHub/relab_controlcenter/app/src/main/java/com/example/relab_tool/utils/DeviceNameResolver.kt:46), [SystemHealthUtils.kt](C:/Users/ASUS/OneDrive/Documents/GitHub/relab_controlcenter/app/src/main/java/com/example/relab_tool/utils/SystemHealthUtils.kt:9).

**U09 — P2, improvement: decompose collectors and isolate presentation.** DeviceInfoViewModel/DeviceInfoScreen contain most device domains and formatting. Split by CPU/GPU/battery/camera/network/security; use injectable collector interfaces and raw typed records. Avoid blanket stable declarations for every model/data class unless immutability is guaranteed. Acceptance: each collector can be unit-tested with fake Android/sysfs inputs, and locale changes do not require reparsing measurements.

**U10 — P3, confirmed cleanup: consolidate dead/duplicate surfaces and packaging.** Standalone SatelliteCompassScreen, ThemeSettingsScreen, CameraSpecScreen and legacy Bluetooth UI are not reachable through the current main navigation; unit settings lack a reachable control; several telemetry managers/catalogues are duplicated. Review unused libraries/resources and broad Compose keep rules. Acceptance: intentional navigation or removal for each orphan; generated baseline profiles; measured release size and verified native-library alignment.

**U11 — P2, improvement: harden backup and shared-file scope.** Backup/data-extraction XML remains close to its sample form while backups are enabled; FileProvider exposes a broad external Download path. Acceptance: intentional include/exclude rules for diagnostic history/identifiers and providers limited to report directories. This is a reduction of exposure, not evidence of an observed data leak.

**U12 — P2, confirmed mismatch: performance contract is not enforced as written.** It promises debug crashes for disk/network violations; RelabApp intentionally logs only. Several audited paths still perform Main-thread IO. Acceptance: align the contract with implementation, fail targeted StrictMode/instrumentation checks in CI, and measure start/frame performance before forcing maximum refresh rates. Evidence: [PERFORMANCE_CONTRACT.md](C:/Users/ASUS/OneDrive/Documents/GitHub/relab_controlcenter/PERFORMANCE_CONTRACT.md:12), [RelabApp.kt](C:/Users/ASUS/OneDrive/Documents/GitHub/relab_controlcenter/app/src/main/java/com/example/relab_tool/RelabApp.kt:22).

## Application-wide feature coverage

All rows below were traced in source. “Present” indicates an implementation exists; it does not imply runtime validation passed.

| Feature | Source assessment | Improvement / required validation |
|---|---|---|
| Startup/splash/permissions | Present; mandatory permission gate | S04; revocation, denial, empty result, recreation |
| Adaptive navigation/pager | Present; 5 pages, bottom bar/rail | Preserve selected page/deep link across recreation; large-screen and back-navigation tests |
| Dashboard cards/layout | Present; reorder/resize/persist | Validate stale saved card IDs, empty layouts, large fonts and touch targets |
| Search | Present; normalized labels/values and synonym expansion | Localized cache refresh; unknown-value filtering uses English literals; test multiword relevance and all deep links |
| Device/system tab | Present; Build/properties/identifiers | Missing versus unknown values; redaction; confirmed versus inferred names |
| SoC/CPU tab | Present; DB/proc/sysfs | Source/confidence; frequency is not utilization; big/LITTLE and offline-core tests |
| Camera tab | Present; Camera2, physical IDs, DB/spec fallback | Distinguish advertised, exposed and measured specs; external camera/cache invalidation; OEM failures |
| Battery tab/history | Present; broadcast properties/history | T04/T06; charge cycles, midnight, unplug, unsupported current, stale readings |
| Bluetooth tab | Present; adapter/profile information | Permission/off/missing adapter; distinguish supported, enabled and connected |
| Display tab | Present; modes/refresh/capabilities | Refresh rate versus FPS; density versus physical DPI; external display and rotation |
| Memory/storage tab | Present; capacity and OS statistics | Remove SMART assertion based on path presence; GiB versus marketed GB; unavailable sysfs |
| Wi-Fi tab | Present; permission/location/connectivity states | Redacted SSID must not imply disconnected; callback re-registration and VPN/dual-network tests |
| Cellular tab/towers | Present; subscription and tower data | Refresh after permissions/SIM changes; dual-SIM, no SIM, airplane mode, unavailable cell identity |
| USB tab | Present; device enumeration and status | Access-denied serial fields, detach/reattach; capability is not negotiated cable speed; avoid reloading unrelated domains |
| Codecs/audio tab | Present; codec list/audio features | Capability does not prove realtime decode/route; hardware/software classification and service failures |
| Sensor tab | Present; inventory and live readings | Missing sensors, no event yet, accuracy status, rate limits and listener lifecycle |
| Installed-apps tab | Present; package inventory | S06; visible-versus-total distinction, package-change refresh, consistent disclosure |
| Thermal tab | Present; thermal zones/system status | T05/T06; units/source labels and unknown readings |
| Security/DRM tab | Present; local properties and DRM | S03; real integrity versus heuristics, key/biometric distinction, identifier redaction |
| Curated installer | Present; 22 entries and store/source navigation | U01; retry icons; no-browser feedback; actual split-APK feature absent |
| CPU benchmark | Present; 500×500 matrix multiplication | T07; warmup/repeat/reference dataset, cancellation/finally |
| RAM benchmark | Present; 50 MB write/read | T07; throughput definition/checksum, duration guard, low-memory behavior |
| Stability monitor | Present; frequency history | Label passive sampling or add controlled load/thermal protocol |
| Download speed test | Present; 25 MB request | T08; error body/deadline/cancellation/metered-data disclosure |
| Performance HUD | Present; drag/minimize/graphs | T01/T03; stale data, screen bounds/rotation and service-only restart |
| Gaming/Saver profiles | Present; app polling/priority/hints/wakelock | T02; Main-thread priority restoration and actual power impact |
| Crosshair | Present; style/size/color | Rotation, cutouts, overlay revocation and cleanup |
| Status notification | Present; 2-second IO loop | Single source of telemetry; zero CPU semantics, permission denial, user stop and restart |
| Battery worker | Present; 15-minute periodic task | T04/T09; report collection failure, cancellation and history transitions |
| Four widgets | Present | T09/U07; asynchronous collection, locale, last-updated timestamp, size variants |
| Boot restoration | Present for status notification preference | Foreground-start failure handling and preference/notification-permission consistency |
| AssistiveTouch | Present, partial action set | U04; hide unavailable actions; retain bounds/config through rotation and process death |
| Theme/high contrast/color | Present | U03; system bars, contrast and large-font validation |
| Language selection | Present, 17 locales | U07; reports/widgets/notifications and format parity |
| Root controls | Present, unsafe/incomplete | S01/S02 before release |
| CIT workflow | Present; all 66 tests below | D01–D09; test isolation, correct status semantics and evidence preservation |
| Hardware PDF | Present; unsupported bridge | U05/U06; minified-release export and redaction |
| CIT PDF/JSON/share | Present | U06; preserve evidence, schema version, no stale files, PDF layout checks |
| Satellite compass | Implemented but unreachable | Deliberately expose and test, or remove |
| Units setting | Formatting support exists; no reachable setter UI | Wire to settings and keep raw values independent of display units |
| Online spec database | Present; cached GitHub lookup | U02; schema/source/date/confidence; stale offline data and retries |

## Every CIT test: logic audit

Definition line numbers refer to [CITSubTestManager.kt](C:/Users/ASUS/OneDrive/Documents/GitHub/relab_controlcenter/app/src/main/java/com/example/relab_tool/ui/cit/CITSubTestManager.kt). UI logic is in [CITCategoryWizardScreen.kt](C:/Users/ASUS/OneDrive/Documents/GitHub/relab_controlcenter/app/src/main/java/com/example/relab_tool/ui/cit/CITCategoryWizardScreen.kt). A = automated; M = manual/interactive. Every row still requires physical-device execution. Common engine fixes D02/D06/D07/D09 apply across the table.

| # | Test ID / definition line | Mode | What it actually does; improvement to make |
|---|---|---|---|
| 1 | GRID_TOUCH / 33 | M | Tracks visited grid cells. Validate edge coverage, pointer cancellation, orientation, and resetting on repeat. |
| 2 | MULTI_TOUCH / 39 | M | Displays multiple contacts. Retain maximum observed count and distinguish advertised touch count from measured count. |
| 3 | GHOST_TOUCH / 45 | M | Counts delivered touch events over an observation interval. Require a hands-off baseline and record unexpected events; do not conflate deliberate touches/dead zones. |
| 4 | COLOR_UNIFORMITY / 51 | M | Fullscreen color sweep for visual judgment. Record chosen colors and operator result; cannot certify uniformity numerically. |
| 5 | DEAD_PIXEL / 57 | M | Solid backgrounds for visual inspection. Keep explicitly manual and preserve notes/defect location. |
| 6 | BRIGHTNESS_RANGE / 63 | M | Mock visual step/slider. Apply actual window brightness and restore previous value when leaving. |
| 7 | SCREEN_FLICKER / 69 | M | Displays an animated/manual flicker pattern. It does not measure panel PWM frequency; label the limitation. |
| 8 | REFRESH_RATE / 75 | A | Reads display refresh metadata. Label current mode, not measured frame throughput. |
| 9 | TOUCH_LATENCY / 87 | A | Always PASS at 12 ms. Remove fabricated measurement; implement an explicitly scoped event-latency test or mark unavailable. |
| 10 | ACCEL / 104 | M | Live XYZ values. Show sensor units/accuracy and wait for an event before implying valid values. |
| 11 | GYRO / 110 | M | Live angular-rate values. Label rad/s; validate no-sensor and stationary baselines. |
| 12 | MAGNETIC / 116 | M | Shows field components rather than a fully calibrated heading. Label µT or implement tilt-compensated heading/calibration. |
| 13 | PROXIMITY / 122 | M | Compares distance with maximum range. Require observed near→far transitions instead of initial default zero. |
| 14 | LIGHT_SENSOR / 132 | M | Live lux and progress indicator. Record covered/uncovered range and unavailable/no-event state. |
| 15 | BAROMETER / 142 | A | Checks pressure-sensor presence only. Subscribe to an event with timeout or rename as inventory. |
| 16 | STEP_COUNTER / 161 | A | Checks sensor presence only. A functional test needs activity-recognition access and observed step delta. |
| 17 | FINGERPRINT / 180 | M | General biometric prompt. A successful biometric response need not identify fingerprint modality; align label and allowed authenticators. |
| 18 | FACE_UNLOCK / 191 | A | Feature-flag check, no authentication. Remove unused IR probe and label presence only. |
| 19 | SENSOR_LIST / 205 | A | Enumerates sensor count. Inventory success is not a health verdict for every sensor. |
| 20 | EARPIECE / 223 | M | Tone with voice-communication usage. Verify actual earpiece route and restore audio routing/state. |
| 21 | SPEAKER / 229 | M | Looping mono tone. Observe actual route and failures; stop/release on every exit. |
| 22 | STEREO_BALANCE / 235 | M | Same mono tone. Implement stereo PCM with separate left/right controls and operator confirmation. |
| 23 | MIC_MAIN / 241 | M | Three-second MIC-source recording/playback. Move prepare/file work off Main; isolate session and show failures. |
| 24 | MIC_SEC / 247 | M | CAMCORDER source, not guaranteed top mic. Identify selected input when supported; otherwise rename. |
| 25 | HEADPHONE_JACK / 253 | M | Generic manual fallback. Implement AudioDeviceCallback plug/route observation or mark unimplemented. |
| 26 | BT_AUDIO / 259 | M | Generic manual fallback. Implement connected output/route and playback verification. |
| 27 | VIBRATION_MOTOR / 265 | M | Multiple vibration patterns. Check hardware/amplitude support and collect operator feedback. |
| 28 | CAMERA_REAR / 278 | M | CameraX rear preview. Handle missing camera, binding failure and disposal. |
| 29 | CAMERA_FRONT / 284 | M | CameraX front preview. Same lifecycle/error requirements as rear. |
| 30 | CAMERA_MULTI / 290 | M | Attempts logical/physical lens selection. Use verified IDs, handle unavailable physical streams, avoid absolute-focal-length lens classification. |
| 31 | AUTOFOCUS / 296 | M | Preview without explicit focus-result test. Add focus action and observed result; handle fixed-focus lenses. |
| 32 | FLASH_LIGHT / 302 | M | Toggles torch on first camera ID. Select a flash-capable rear camera, observe actual torch state and errors. |
| 33 | CAMERA_ZOOM / 308 | M | Initial zoom is applied; slider changes are not. Fix D03 and clamp to hardware range. |
| 34 | PHOTO_CAPTURE / 314 | M | Generic manual fallback. Add capture, file validation, preview and cleanup. |
| 35 | VIDEO_RECORD / 320 | M | Generic manual fallback. Add recording, audio permission, playback, sync check and cleanup. |
| 36 | OIS_EIS / 326 | A | Checks availability-array sizes and merges OIS/EIS. Inspect explicit modes per camera and distinguish capability from stabilization performance. |
| 37 | WIFI_SCAN / 358 | A | Reads cached scanResults; denied/empty becomes PASS. Track permission, freshness and actual scan outcome. |
| 38 | WIFI_CONNECT / 373 | A | Checks active transport is Wi-Fi. Does not verify Internet or LAN reachability; label transport status. |
| 39 | BT_SCAN / 390 | A | Checks adapter existence/enabled state; no scan. Rename or implement discovery with required permissions and timeout. |
| 40 | BT_PAIR / 405 | A | Counts remembered bonds; denied access becomes zero. Distinguish denied, zero bonds and live connection. |
| 41 | CELLULAR_SIM / 417 | A | Reads SIM state; all states PASS. Report absent/locked/ready separately without implied RF health. |
| 42 | MOBILE_DATA / 433 | A | Checks active cellular transport. Does not establish Internet validation or radio quality. |
| 43 | GPS_LOCK / 450 | M | Requests GPS, counts satellites used in fix, passes at <30 m accuracy. Add deadline, location age/mock checks and actual C/N0 if retaining SNR label. |
| 44 | NFC_SCAN / 456 | M | Reader-mode tag callback. Check NFC enabled state, tag details, cancellation and absent hardware. |
| 45 | USB_DATA / 465 | A | Reads connected broadcast flag, PASS even disconnected. Need configured/data-transfer evidence; cable attachment alone is insufficient. |
| 46 | USB_CHARGE / 476 | A | Reads USB plugged classification, PASS either way. Preserve not-connected outcome; do not mistake AC-classified USB chargers for a bad port. |
| 47 | WIRELESS_CHARGE / 488 | A | Reads wireless plugged classification, PASS either way. Separate supported, currently charging and untested. |
| 48 | POWER_BUTTON / 507 | M | Observes screen-state broadcasts. Screen off/on is not conclusive physical-button evidence; keep operator verification. |
| 49 | VOLUME_KEYS / 513 | M | Observes volume keypresses. Persist each direction, consume/restores behavior intentionally, and support accessibility/volume overlays. |
| 50 | SHUTTER_BUTTON / 519 | A | Uses external-camera feature flag. Test KEYCODE_CAMERA/input support; that flag does not indicate a shutter button. |
| 51 | STYLUS_BUTTON / 534 | A | Uses a feature-name heuristic. Detect stylus tool/button input; do not equate absent heuristic with absent digitizer. |
| 52 | CPU_CORES / 559 | A | Runtime available processor count. Label available logical processors, not a health test of all physical cores. |
| 53 | CPU_STRESS / 569 | M | Five-second multithreaded SHA-256 work. Fix count race/repeat reset and use thermal/cancellation criteria. Instruction incorrectly says prime calculations. |
| 54 | GPU_ID / 575 | A | Reports Build.HARDWARE platform. Query renderer identity with explicit scope; do not call platform name a GPU model. |
| 55 | GPU_STRESS / 585 | M | Rotating circle/pointer canvas. Rename rendering demonstration or implement measurable GPU workload. |
| 56 | RAM_CAPACITY / 591 | A | ActivityManager total memory. Guard missing service and distinguish capacity from memory health. |
| 57 | RAM_STRESS / 604 | A | Allocates and clears 50 MB. Add patterned read/write verification or rename allocation check. |
| 58 | STORAGE_CAPACITY / 621 | A | StatFs data-directory capacity. Label partition capacity and GiB accurately. |
| 59 | STORAGE_SPEED / 632 | A | Writes/reads 2 MB; no content comparison; zero duration possible. Fix timing/data validation and guaranteed temp cleanup. |
| 60 | EXTERNAL_STORAGE / 656 | A | Checks second external-files directory. Report mounted secondary storage, not universal physical-slot support. |
| 61 | BATTERY_HEALTH / 670 | A | Maps health but always PASS; unknown defaults Normal. Fail known adverse health and preserve unknown. |
| 62 | BATTERY_DRAIN / 688 | M | Generic manual fallback. Implement timed sampling with charging/screen/workload controls or mark unimplemented. |
| 63 | CHARGING_PORT / 701 | M | Any power-connected event passes. Wireless power can trigger it; verify plug type and full insert/remove sequence. |
| 64 | SIM_TRAY / 707 | A | SIM state heuristic; unknown can appear Tray Engaged. SIM/eSIM state is not physical tray position. |
| 65 | SPEAKER_GRILLE / 718 | M | Two visual checkboxes. Keep explicitly operator-confirmed and invalidate/review result after unchecking. |
| 66 | TEMPERATURE / 724 | A | Reads battery temperature while labeled core thermal sensors. Correct label or collect actual named CPU/SoC zones; no read is not hardware failure. |

## Ordered implementation backlog

1. Make the build reproducible, align compiler plugins, repair test discovery, and run both debug and minified release checks (B01–B04).
2. Disable unsafe root-node writes, correct security/integrity labeling, remove fabricated measurements, and fix false PASS rules (S01–S03, D01, T01).
3. Fix evidence preservation, camera zoom/release, stereo routing, per-test state, and automated-test dispatch/errors (D02–D07).
4. Repair the Main-thread priority side effect and move telemetry ownership out of the activity ViewModel (T02–T03).
5. Make permissions optional by feature and publish accurate privacy/export disclosures (S04–S06, U11).
6. Repair battery history, typed numeric telemetry, benchmark cleanup, network cancellation, and widget collection (T04–T09).
7. Fix installer races, online retries, theme state, and export atomicity/public APIs (U01–U06).
8. Complete translations and capability documentation, then remove duplication and reduce the reviewed lint baseline (U07–U12, B05–B06).

## Required device and regression validation

Run on the minimum supported API 26, API 28/29 boundaries, Android 12 permission behavior, Android 13 notifications, and Android 14/15 service restrictions; add the current Android release. Include Pixel, Samsung, and at least one Xiaomi/OPPO-style restrictive camera/sysfs implementation, a low-memory device, a device without telephony/NFC/barometer, and one explicitly supported rooted device for guarded root tests.

| Scenario | Acceptance criteria |
|---|---|
| Deny/revoke each permission | Unrelated features work; affected feature explains unavailable data and recovery |
| Rotation, background/foreground, process recreation | Navigation/results retained; no duplicate listeners, recordings, overlays or frozen telemetry |
| Leave camera/mic/audio test mid-operation | Hardware promptly released; callbacks/jobs cancelled; no stale result written into another test |
| Run every CIT test and export | All 66 have meaningful outcomes; no fabricated PASS; measurements survive manual confirmation and export |
| Battery midnight/full-charge/unplug/recharge | History and event times remain correct; discharge intervals do not cross charging sessions |
| Offline/slow/error-body networking | Last-good specs retained; retries possible; speed test reports errors and cancels promptly |
| Locale: English, Vietnamese, French, Arabic, decimal-comma locale | No placeholder tokens or format failures; graphs unchanged numerically; RTL/font layout readable |
| Service started without activity / activity finished | Telemetry producer exists or HUD explicitly reports stale/unavailable state |
| Saver→Gaming→close overlay | Correct owned-thread priorities restored; no persistent Main-thread deprioritization or held lock |
| Full/readonly/unavailable storage and no share target | Clear export failure; no stale report shared; no crash |
| Debug and minified release | Same feature behavior; report reflection/serialization/resources retained only where needed |
| Native APK checks | Verify 16 KB alignment of each bundled native ABI rather than infer from packaging flags |

No claim of complete runtime correctness should be made until those checks pass. The current parser results establish that the source grammar is clean; the backlog identifies concrete logic defects and the remaining proof needed for each feature.
