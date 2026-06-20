# Relab Control Center (rlcc) - v0.4.1

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

## 🏁 v0.4.1 Release Highlights

This release introduces the adaptive app icon, safe Wi-Fi SSID location telemetry, Snapdragon 7s Gen 4 SoC resolution, and critical translation compilation fixes:
*   **Modern Adaptive App Icon**: Integrated the original logo directly as foreground and monochrome adaptive launcher layers, with customized safe zones matching standard Android launcher standards.
*   **Wi-Fi Telemetry & Safe SSID Retrieval**: Implemented Google Play compliant Wi-Fi SSID retrieval utilizing foreground-only optional Location permissions, with on-demand permission rationales, graceful degradation, and system Settings redirection.
*   **SoC Disambiguation (Snapdragon 7s Gen 4)**: Introduced frequency-based hardware detection logic to correctly distinguish between Snapdragon 7s Gen 3 and Gen 4 (both sharing hardware code `SM7635`), alongside accurate Adreno 810 GPU specifications.
*   **Language Consistency & French Compilation Fix**: Cleaned up localization and resolved resource compilation errors in French strings.xml.

---

## 🏁 v0.4 Beta Highlights

This release focuses on detailed hardware spec explanations and deep internationalization:
*   **Spec-Specific Explanations**: Added 171 custom explanation entries for all 57 specs cards (Explanation, How it works, Why it matters) to replace generic templated text.
*   **Global Localization (17 Languages)**: Full localization in English, Vietnamese, Simplified Chinese, Spanish, French, German, Russian, Portuguese, Italian, Japanese, Korean, Arabic, Hindi, Indonesian, Thai, Turkish, and Dutch.
*   **Interactive Specs & Settings Navigation**: Top-right ⓘ buttons opening localized bottom sheets, and long-press shortcuts to relevant Android Settings categories with haptic feedback.

---

## 📊 Changelog: v0.4.1 vs v0.4 Beta

| Feature | Version 0.4 Beta | Version 0.4.1 (New) |
| :--- | :--- | :--- |
| **App Icon** | Basic launcher icon. | **Modern Adaptive Icon**: Foreground/monochrome layers with insets, matching Material You wallpaper theme color. |
| **Wi-Fi Telemetry** | Default connection state. | **Safe SSID Retrieval**: Google Play compliant permission checks, rationale dialogs, and settings redirects. |
| **SoC Disambiguation** | Qualcomm SM7635 mapped to Snapdragon 7s Gen 3. | **Frequency-Based Filter**: Correctly resolves Snapdragon 7s Gen 3 vs. Gen 4, and maps Adreno 810 GPU specs. |
| **Build Configuration** | Version Name: `0.4`, Code: `4` | Version Name: `0.4.1`, Code: `5`. Release tag `v0.4.1` with `rlcc_beta_0.4.1.apk`. |

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

1.  **Download**: Obtain the latest `rlcc_beta_0.4.1.apk` from the [Releases](https://github.com/KILLERJOYZ/relab_controlcenter/releases) page.
2.  **Permissions**: Grant Camera, Location, and Phone permissions to enable physical sensor detection and network identification.
3.  **Installation**: Enable "Install from Unknown Sources" for the App Hub to function as an alternative installer.

---

*Disclaimer: Features, UI, and hardware detection databases are subject to frequent updates. Root-level features require a rooted device and may vary by manufacturer.*
