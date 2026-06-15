# Android Performance Contract — relab_controlcenter

This contract is a binding agreement for all current and future developers (including AI agents) working on the `relab_controlcenter` project. Any new feature, refactoring, or bug fix MUST adhere to these rules. Performance is a first-class feature of this application.

---

## 1. Threading & I/O Constraints (Zero Main-Thread Blockers)

*   **No Disk I/O on Main Thread:** Reading or writing to disk, including `SharedPreferences`, database operations, and linux/sysfs device config files (e.g., files in `/sys` or `/proc`), MUST be executed on a background dispatcher (`Dispatchers.IO`).
*   **No Network I/O on Main Thread:** Network calls MUST use connection pooling, OkHttp caching, and be offloaded to `Dispatchers.IO`.
*   **No IPC Binder Calls in Composition:** Synchronous system calls (e.g., querying `TelephonyManager`, `BluetoothAdapter`, `WifiManager`, `SensorManager` status) perform synchronous IPC binder transactions and MUST NOT be invoked within Compose composition/layout scopes. These MUST be queried asynchronously in ViewModels on `Dispatchers.IO` and exposed via lifecycle-aware flows.
*   **StrictMode Enforcement:** StrictMode is enabled in debug builds. Any transaction triggering `DiskReadViolation`, `DiskWriteViolation`, or `NetworkViolation` on the main thread will crash the app in debug builds to prevent regressions from reaching production.

---

## 2. Structured Concurrency & Coroutines

*   **No GlobalScope / CoroutineScope(Dispatchers.Default) for view-bound work:** Use `viewModelScope` for ViewModel operations and `lifecycleScope` for Activity/Fragment operations to ensure proper lifecycle bound execution.
*   **Cooperative Cancellation:** Long-running loops, intensive computations (such as benchmark loops), or stream processing inside coroutines MUST check `ensureActive()` or check `isActive` periodically to allow cancellation.
*   **Context Preservation:** Always use `withContext` to transition between thread pools. Do not leak dispatcher dependencies or launch unmanaged fire-and-forget coroutines.

---

## 3. UI, Drawing, & Compose Rendering Optimization

*   **Pre-allocate Objects for Drawing:** Inside custom drawing scopes (e.g., Jetpack Compose `Canvas` or custom View `onDraw`), DO NOT allocate new objects. `Paint`, `Path`, `Brush`, and other drawing tools MUST be pre-allocated and cached (e.g., using `remember` or properties outside the draw lambda).
*   **No Intensive Calculations in Canvas:** Sorting lists, calculating min/max values of histories, or processing raw data MUST NOT occur inside custom `Canvas` drawing blocks. Perform these calculations in the ViewModel, or precompute and `remember` them outside of the drawing lambda.
*   **Lifecycle-Aware Flow Collection:** Inside Composable screens, always use `.collectAsStateWithLifecycle()` from `androidx.lifecycle:lifecycle-runtime-compose` instead of `.collectAsState()`. This stops flow collection when the app is in the background, saving CPU cycles, wake locks, and battery.
*   **RecyclerView List Diffing:** RecyclerView adapters MUST NOT call the generic `notifyDataSetChanged()`. All list modifications MUST be computed using `DiffUtil` to enable fine-grained animations and prevent complete view re-binding, which causes frames to drop.

---

## 4. Networking & Bandwidth

*   **OkHttp Cache:** HTTP clients MUST utilize connection pooling and an on-disk cache (target size: 10MB) to avoid duplicate network calls, reduce network latency, and save device battery.
*   **Connection Reuse:** Reuse `OkHttpClient` instances (e.g., via Hilt dependency injection or singletons) to leverage connection pooling and avoid repeating TLS handshakes.

---

## 5. Asset & Binary Size Constraints

*   **Compressed Assets:** All raw drawables and image assets MUST be optimized. PNGs should be compressed using tools like pngquant, or ideally converted to highly compressed `WebP` formats to keep APK size to a minimum and reduce bitmap memory footprints. No raw, uncompressed PNG files exceeding 200KB are allowed.

---

## 6. Maximize Frame Rate

*   **Dynamic Refresh Rate:** The application MUST request the maximum supported device refresh rate at runtime to guarantee smooth scroll/render operations on 90Hz, 120Hz, or 144Hz displays.

---

## String Resource Contract

### Prime Directive — Hardcoded Strings Are Forbidden
Under NO circumstances may any AI agent ever write, generate, suggest,
or leave a hardcoded string anywhere in this codebase.

This rule has NO exceptions for:
- "Temporary" or "placeholder" strings
- Strings that "will be moved later"
- Strings inside new features under development
- Strings in test or debug code that touch the UI
- Quick fixes or hotpatches
- Any reason whatsoever

If a string will ever be seen by a user, it belongs in strings.xml.
If it is a key, constant, or identifier, it belongs in Constants.kt.
Hardcoded strings are treated as a build-breaking error, not a warning.

---

**RULE S0 — No Hardcoded Strings, Ever (Non-Negotiable)**

An AI agent MUST follow this workflow for EVERY string it writes,
in EVERY file it touches, in EVERY task it performs:

    STEP 1 — Classify the string:
    ┌─────────────────────────────────────────────────────────────┐
    │ Is it user-facing text (labels, messages, hints, titles,    │
    │ buttons, errors, dialogs, toasts, snackbars,                │
    │ content descriptions, placeholders)?                        │
    │   → YES: Add to res/values/strings.xml                      │
    │                                                             │
    │ Is it a key, constant, or identifier (Bundle key, Intent    │
    │ extra, Preference key, API header, tag, route name)?        │
    │   → YES: Add to Constants.kt as const val                   │
    │                                                             │
    │ Is it a developer-only string (Log message, exception       │
    │ message, SQL query, annotation parameter, Regex pattern)?   │
    │   → YES: Leave inline — these are the ONLY exceptions       │
    └─────────────────────────────────────────────────────────────┘

    STEP 2 — Name it correctly:
        User-facing:  [screen]_[element_type]_[descriptor]
                      e.g. login_btn_submit, profile_dialog_title
        Constants:    KEY_[DESCRIPTOR] or [CATEGORY]_[DESCRIPTOR]
                      e.g. KEY_USER_ID, PREF_KEY_THEME

    STEP 3 — Reference it correctly:
        XML layouts:      android:text="@string/login_btn_submit"
        Kotlin Views:     getString(R.string.login_btn_submit)
        Jetpack Compose:  stringResource(R.string.login_btn_submit)
        Constants:        Constants.KEY_USER_ID

    STEP 4 — Self-verify before finishing any task:
        Run: ./gradlew lint
        Confirm: Zero HardcodedText lint warnings introduced by your changes
        Grep:    No new instances of android:text="(?!@string) in XML
        Grep:    No new instances of \.text\s*=\s*" in Kotlin for UI text

**Violation Policy:**
If an AI agent produces a hardcoded string anywhere in user-facing code:
- The change must be reverted immediately
- The string must be properly extracted before reapplying the change
- The agent must re-run lint and grep verification to confirm zero violations
- The incident must be noted in the task summary artifact

### Updated Post-Feature String Checklist
After implementing ANY new feature, the AI agent MUST confirm:

- [ ] RULE S0: Zero hardcoded strings introduced anywhere in the codebase
- [ ] All new user-facing strings are in strings.xml with correct naming
- [ ] All new keys and constants are in Constants.kt
- [ ] All Compose functions use stringResource() — no raw string literals
- [ ] All count-based strings use `<plurals>` — no if/else string logic
- [ ] ./gradlew lint → zero HardcodedText warnings
- [ ] Grep .kt files for `\.text\s*=\s*"` → zero new user-facing results
- [ ] Grep .xml files for `android:text="(?!@string)` → zero new results
- [ ] App builds and runs → zero `Resources$NotFoundException` in Logcat
- [ ] Task summary artifact notes any edge cases handled

---

## 7. Device Specification Verification Contract (Non-Negotiable)

### Prime Directive — Never Display Unverified Hardware Specs

Before displaying ANY hardware specification in the app (camera resolution, sensor model, SoC name, display specs, battery capacity, etc.), the specification MUST be cross-referenced against **manufacturer-verified sources**.

Android system APIs (`Camera2 API`, `Build.*`, `SENSOR_INFO_*`, `TelephonyManager`, etc.) are **unreliable** on many OEM devices. Common API failures include:
- Camera2 API reporting raw Quad-Bayer photosite counts (200MP) instead of effective resolution (50MP)
- `SENSOR_INFO_PIXEL_ARRAY_SIZE` inflated by remosaic pixel arrays
- SoC model strings being ambiguous across variants (e.g., MT6833 = Dimensity 700 or 810)
- OEM HALs exposing incorrect or placeholder values

### Source Hierarchy (Priority Order)

When determining a device's specifications, sources MUST be consulted in this order:

1. **Brand Official Spec Page** (HIGHEST PRIORITY)
   - mi.com, samsung.com, oneplus.com, oppo.com, vivo.com, store.google.com, etc.
   - These are the **canonical truth** for the device as marketed

2. **GSMArena / DeviceSpecifications.com** (Secondary Confirmation)
   - Cross-reference brand data with these databases
   - Use when brand page lacks detail (e.g., sensor model names)

3. **Curated JSON Asset Database** (Runtime Source)
   - `device_cameras.json` — device-specific camera overrides
   - `cpu_*.json` — SoC identification databases
   - `cpu_family.json`, `cpu_family_arm.json` — CPU core identification
   - These files are the app's **runtime truth** and override API-detected values

4. **Android System APIs** (LOWEST PRIORITY — Fallback Only)
   - Camera2 API, `Build.SOC_MODEL`, `SENSOR_INFO_*`, sysfs files, etc.
   - Use ONLY when the device is not in the curated database

### Verification Workflow

An AI agent MUST follow this workflow when adding or modifying device specifications:

    STEP 1 — Identify the device:
    ┌─────────────────────────────────────────────────────────────┐
    │ Determine Build.DEVICE codename, Build.MODEL numbers,       │
    │ and all regional variant model numbers for the device.       │
    └─────────────────────────────────────────────────────────────┘

    STEP 2 — Research specifications:
    ┌─────────────────────────────────────────────────────────────┐
    │ Search the brand's official specification page for the       │
    │ device. Confirm key specs (camera MP, sensor names, SoC,    │
    │ display, battery) from the official source.                  │
    │                                                             │
    │ Cross-reference with GSMArena for sensor model names         │
    │ and technical details not on the brand page.                 │
    └─────────────────────────────────────────────────────────────┘

    STEP 3 — Update the curated database:
    ┌─────────────────────────────────────────────────────────────┐
    │ Add verified specs to the appropriate JSON asset file:       │
    │                                                             │
    │ Camera specs → device_cameras.json                           │
    │ SoC ID mapping → cpu_*.json                                  │
    │ CPU core names → cpu_family.json / cpu_family_arm.json       │
    │                                                             │
    │ Each entry MUST include a comment or match pattern that      │
    │ links back to the verification source.                       │
    └─────────────────────────────────────────────────────────────┘

    STEP 4 — Ensure priority ordering in code:
    ┌─────────────────────────────────────────────────────────────┐
    │ The code MUST check the curated database FIRST, before      │
    │ falling back to API detection. If a database match is       │
    │ found, its values OVERRIDE any API-detected values.          │
    └─────────────────────────────────────────────────────────────┘

### Camera Specification Rules

1. **device_cameras.json** is the primary source of truth for camera specs
2. Each device entry must include: `match` patterns (codename + model numbers), and `cameras` array with `focal_min`, `focal_max`, `facing`, `res` (MP), and `sensor` (model name)
3. **Quad-Bayer/Tetrapixel sensors**: Always report the **effective marketing resolution** (e.g., 50MP for OV50H), NOT the raw photosite count (200MP)
4. The `res` field MUST match what the manufacturer advertises on their spec page
5. When unsure about a sensor model, use a descriptive label like "12MP Ultra-Wide" rather than guessing

### SoC Specification Rules

1. `cpu_*.json` and `SoCUtils.socMap` are the sources of truth for SoC names
2. When two SoCs share the same internal ID (e.g., MT6833 = Dimensity 700 or 810), the disambiguation logic in `SoCUtils.refineName()` MUST use secondary signals (max CPU frequency, core count, GPU renderer) to determine the correct variant
3. New SoC entries MUST be verified from the chip manufacturer's product page (qualcomm.com, mediatek.com, samsung.com/semiconductor)

### Violation Policy

If an AI agent displays an unverified or incorrect hardware specification:
- The change must be reverted immediately
- The correct spec must be researched from manufacturer sources
- The curated JSON database must be updated with verified data
- The incident must be noted in the task summary artifact

