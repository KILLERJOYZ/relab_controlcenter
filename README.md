# Relab Control Center (rlcc) - v0.4.3.4-devbeta

**Relab Control Center** is a professional-grade utility and diagnostic suite built for Android enthusiasts, developers, and power users. It combines deep hardware intelligence with real-time performance telemetry and a specialized installer hub to provide total transparency into the Android ecosystem.

---

## 🏛 Project Overview

The project is designed to bridge the gap between high-level "About Phone" settings and low-level system parameters. It serves five main purposes:
1.  **Hardware Verification**: Identifying exact sensor models, SoC architecture, and component specifications.
2.  **Hardware Diagnostics (CIT)**: A comprehensive suite of interactive tests to verify device integrity.
3.  **Performance Analytics**: Providing real-time telemetry for CPU, GPU, RAM, and Network.
4.  **Smart App Management**: Streamlining the installation of essential benchmarks and games with intelligent compatibility handling.
5.  **Power User Tools**: Advanced optimizations including root-level controls and professional audit reporting.

---

## 🚀 Key Modules

### 📱 Device & OS Intelligence
*   **AI-Powered Resolution**: Leverages **Google Gemini AI** to accurately resolve marketing names for obscure devices and models.
*   **Camera Sensor Probe**: Detects physical vs. binned resolution and identifies exact sensor hardware (Sony IMX, Samsung S5K, OmniVision, etc.) using a built-in sensor database.
*   **SoC Architecture**: Goes beyond basic names to identify CPU clusters (Prime/Gold/Silver) and correctly brands the latest flagships (Qualcomm "Oryon", Apple A-series, MediaTek Dimensity).
*   **Advanced OS Detection**: Robust identification of OEM skins including Samsung **One UI**, Xiaomi **HyperOS**, Vivo **OriginOS**, Motorola **Hello UI**, Honor **MagicOS**, and major Custom ROMs.
*   **Memory & Storage**: High-fidelity detection of RAM types (LPDDR5X), Storage bus models (UFS 4.0), and **S.M.A.R.T. health status**.

### 🛠 Hardware Diagnostic Suite (CIT)
A professional-grade **Common Interaction Toolkit** for verifying hardware functionality:
*   **Display & Touch**: 10-bit (1.07B colors) detection, LCD pixel tests, and multi-touch/gesture verification.
*   **Audio & Haptics**: Frequency tests for Earpiece/Speaker, Microphone recording/playback, and Vibration motor diagnostics.
*   **Biometrics & Sensors**: Verification of Fingerprint scanners, Face Unlock, and real-time data from the full sensor array (Barometer, UWB, etc.).
*   **Connectivity**: Functional testing for Wi-Fi 7, Bluetooth 5.4 (including LE Audio features), GPS, and NFC.

### 📊 Real-time Dashboard & Benchmarks
*   **Live Telemetry**: Smooth, high-frequency graphs for CPU utilization and per-core frequencies with **JankStats** integration.
*   **Thermal Monitoring**: Tracking temperature sensors across multiple internal thermal zones (SoC, Battery, Skin).
*   **Performance Tests**: Built-in multi-core CPU stress tests, RAM speed benchmarks, and network throughput analytics.
*   **Battery Analytics**: Precision wattage monitoring, 24-hour capacity history, and **Deep Sleep** ratio tracking.

### 📦 Smart App Hub (The Installer)
*   **Curated Repository**: A centralized hub for benchmarks, high-performance games, and essential utilities.
*   **Split APK Intelligence**: Automatically detects apps using App Bundles/Split APKs and recommends the most reliable installation path.
*   **View Modes**: Toggle between high-density **Gallery View** and detailed **List View**.

### ⚡ Power User & Root Tools
*   **Battery Charge Limiter**: Protect battery longevity by setting custom charging thresholds (requires Root).
*   **CPU Governor Tuner**: Switch between power-save and performance profiles for maximum gaming throughput (requires Root).
*   **Hardware Audit Reports**: Generate professional PDF reports containing the full device specification for documentation or resale.

---

## 🛠 Technical Foundation

*   **Language**: 100% Kotlin.
*   **UI Framework**: Jetpack Compose with **Material 3 Expressive** design and fluid 120Hz/LTPO support.
*   **Architecture**: MVVM with Kotlin Coroutines, StateFlow, and Hilt DI.
*   **Modern Android Standards**:
    *   **16 KB Page Size Support**: Fully compatible with Android 15's memory configuration.
    *   **JankStats & Metrics**: Real-time frame-timing monitoring to ensure UI fluidity.
    *   **Edge-to-Edge**: Seamless integration with system bars and navigation gestures.
    *   **Bilingual Interface**: Fully localized in **English** and **Vietnamese**.

---

## 🏁 v0.4.3.4 Release Highlights

This release implements comprehensive stability, precision, and crash-avoidance optimizations to the mobile hardware benchmark engine across all 7 active performance subsystems:
*   **Prevented JVM Heap OutOfMemoryErrors**: Reduced STREAM array allocation sizes from 128MB to 32MB, maintaining heavy workload intensity to bypass system cache while staying safely within JVM heap growth limits.
*   **Robust SQLite WAL Support**: Replaced query-level WAL configurations with Android's native `enableWriteAheadLogging()` API to bypass restrictions on strict OEM platforms.
*   **Dynamic Decoder Stream Feeding**: Fixed video decoder hanging and returning 0.0 FPS by dynamically capturing valid NAL bitstreams from active encoder runs, adding driver-safe estimated fallback rates.
*   **GC-Friendly Regex Backtracking**: Scaled down pattern-matching backtracking search string size from 2MB to 128KB to prevent heavy ART garbage collection loops.

---

## 🏁 v0.4.3.3 Release Highlights

This release implements a comprehensive calibration audit of the hardware benchmark engine to deliver precise performance scoring and tier classification across Entry, Mid-Range, High-End, Flagship, and Elite Android devices:
*   **Recalibrated Score Normalization**: Re-aligned base-cap limits for hardware-accelerated CPU cryptography (AES, SHA-256, CRC-32), storage write speeds (accounting for periodic fsync latency), and memory bandwidth to eliminate scores compression.
*   **Hardware-Contended Memory Bandwidth**: Implemented high-fidelity memory bandwidth measurement under multi-threaded CPU stress to simulate realistic heavy memory contention.
*   **Accurate Cache Hierarchy Detection**: Corrected cache size bounds (L1=8KB, L2=256KB, L3=4MB) for memory pointer chasing to target specific cache levels.
*   **Thermal & Codec Reliability**: Replaced deterministic seed curves with timing-based dynamic thermal simulations, marked pre-Android 11 thermal headroom queries as partial fallbacks, and filled video codec buffers with randomized YUV payloads for realistic decompression complexity.
*   **Device Reference Re-mapping**: Recalibrated the 24 reference devices mapping to reflect accurate, real-world tier distribution boundaries.

---

## 🏁 v0.4.2 Release Highlights

This release resolves performance stuttering and frame drops during startup and tab transitions by rewriting the telemetry engine to be lazy-loaded and cached:
*   **Lazy-Loaded Telemetry Architecture**: Deferred loading of all heavy system telemetry sections (SoC details, Camera specs, USB configs, Codecs list, Installed apps, Security audit) until their respective tabs are opened, yielding instant startup and fluid navigation.
*   **Telemetric Loop Caching**: Cached slow-changing telemetry details (sensor counts, installed app counts, root check status) inside the 5-second polling loop, avoiding redundant binder IPC calls and saving CPU cycles.
*   **Search & PDF Report Synced Loading**: Connected search inputs and settings PDF report generation to trigger a background load of all advanced specs, ensuring complete system indexing on-the-fly.

---

## 🏁 v0.4 Beta Highlights

This release focuses on detailed hardware spec explanations and deep internationalization:
*   **Spec-Specific Explanations**: Added 171 custom explanation entries for all 57 specs cards (Explanation, How it works, Why it matters) to replace generic templated text.
*   **Global Localization (17 Languages)**: Full localization in English, Vietnamese, Simplified Chinese, Spanish, French, German, Russian, Portuguese, Italian, Japanese, Korean, Arabic, Hindi, Indonesian, Thai, Turkish, and Dutch.
*   **Interactive Specs & Settings Navigation**: Top-right ⓘ buttons opening localized bottom sheets, and long-press shortcuts to relevant Android Settings categories with haptic feedback.

---

## 📊 Changelog: v0.4.3.4 vs v0.4.3.3

| Feature | Version 0.4.3.3 | Version 0.4.3.4 (New) |
| :--- | :--- | :--- |
| **CPU Multi-Core** | Allocated 128MB STREAM arrays per thread, causing JVM heap OOMs. | **Memory-Optimized STREAM**: Reduced array size to 32MB per thread, ensuring heap safety while retaining CPU cache-bypass capability. |
| **SQLite WAL Queries** | Used `execSQL` for database WAL setup, triggering SQL exceptions on some ROMs. | **Safe Native APIs**: Migrated database WAL configuration to use native `enableWriteAheadLogging()` and `rawQuery`. |
| **Video Codec Decoder** | Synthetic dummy NAL bitstreams rejected by modern drivers, returning 0.0 FPS. | **Dynamic Encoder Sourcing**: Feeds decoders dynamically captured valid packets from encoder runs, with safe driver-level fallbacks. |
| **CPU Single-Core** | 2MB regex backtracking string triggered heavy ART GC allocation overhead. | **Tuned Pattern Matching**: Reduced regex string size to 128KB to eliminate GC pauses. |
| **Build Configuration** | Version Name: `0.4.3.3`, Code: `9`. | Version Name: `0.4.3.4`, Code: `10`. Release tag `v0.4.3.4` with `rlcc_devbeta_v0.4.3.4.apk`. |

---

## 📊 Changelog: v0.4.3.3 vs v0.4.2

| Feature | Version 0.4.2 | Version 0.4.3.3 (New) |
| :--- | :--- | :--- |
| **Benchmark Scoring** | Generic normalization ranges causing entry and mid-range devices to group together. | **Recalibrated Normalization**: Widened bounds to ensure linear differentiation across Entry, Mid-Range, High-End, Flagship, and Elite tiers. |
| **Memory Bandwidth** | Measured bandwidth under idle CPU states. | **Contended Memory Bandwidth**: Measures bandwidth under active multi-threaded CPU workloads. |
| **Cache Pointer Chasing** | Incorrect cache size boundaries for L1, L2, L3. | **Targeted Cache Testing**: Corrected cache sizing limits (L1=8KB, L2=256KB, L3=4MB) for precise bandwidth evaluation. |
| **Crypto Workloads** | Heavy CPU cryptography tests deflating overall score. | **Balanced Crypto Normalization**: Widened ranges to account for modern hardware-accelerated ARM cryptography instructions. |
| **Storage Testing** | Standard I/O ranges that did not account for periodic fsync. | **Realistic Storage Scoring**: Halved expected write IOPS and throughput to reflect true fsync-based system performance. |
| **Build Configuration** | Version Name: `0.4.3.2`, Code: `7`. | Version Name: `0.4.3.3`, Code: `7`. Release tag `v0.4.3.3` with `rlcc_devbeta_v0.4.3.3.apk`. |

---

## 📊 Changelog: v0.4.2 vs v0.4.1

| Feature | Version 0.4.1 | Version 0.4.2 (New) |
| :--- | :--- | :--- |
| **Telemetry Initialization** | Eagerly loaded all specs on startup. | **Lazy-Loaded & Deferred**: Initialized only basic dashboard details on startup. |
| **Polling Overhead** | Queried apps list and sensors list size every 5 seconds. | **Cached & Safe**: Apps/sensors lists size are retrieved once and cached. |
| **Bluetooth Telemetry** | Full Bluetooth checks ran in background loop. | **On-Demand**: Full updates only occur when the Bluetooth tab is visited. |
| **Build Configuration** | Version Name: `0.4.1`, Code: `5`. | Version Name: `0.4.2`, Code: `6`. Release tag `v0.4.2` with `rlcc_beta_v0.4.2.apk`. |

---

## 📊 Changelog: v0.4 Beta vs v0.3

| Feature | Version 0.3 | Version 0.4 Beta |
| :--- | :--- | :--- |
| **Spec Explanations** | Shared generic category explanations for all specification cards. | **Spec-Specific Explanations**: 171 custom entries (Explanation, How it works, Why it matters) unique for every card (57 total cards). |
| **Language Support** | Bilingual (English & Vietnamese). | **17 Languages**: Multi-language translation support across 15 additional languages (Chinese, Spanish, French, German, Russian, Portuguese, Italian, Japanese, Korean, Arabic, Hindi, Indonesian, Thai, Turkish, Dutch). |
| **Info Button (ⓘ)** | Not available. | Interactive top-right ⓘ info button on each card opening a fully localized bottom sheet. |
| **Long Press Shortcut** | Not available. | Deep linking settings redirect (e.g. Battery info, Display settings, About Phone) with haptic feedback. |
| **Build Configuration** | Version Name: `0.3`, Code: `3` | Version Name: `0.4`, Code: `4`. Release tag `v0.4` and standalone release asset. |

---

## 📥 Getting Started

1.  **Download**: Obtain the latest `rlcc_devbeta_v0.4.3.4.apk` from the [Releases](https://github.com/KILLERJOYZ/relab_controlcenter/releases) page.
2.  **Permissions**: Grant Camera, Location, and Phone permissions to enable physical sensor detection and network identification.
3.  **Installation**: Enable "Install from Unknown Sources" for the App Hub to function as an alternative installer.

---

*Disclaimer: Features, UI, and hardware detection databases are subject to frequent updates. Root-level features require a rooted device and may vary by manufacturer.*
