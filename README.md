# Relab Control Center (rlcc) - v0.3

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

## 🏁 v0.3 Highlights

This release significantly expands the tool's capabilities:
*   **Gemini AI Integration**: New "Identify Device" feature using generative AI for precise marketing name resolution.
*   **Expressive UI Update**: Completely redesigned navigation with floating bars, search overlays, and dynamic Material You themes.
*   **Advanced Display Specs**: Support for detecting 10-bit color depth and variable refresh rate (VRR) modes.
*   **Root Toolkit**: Introduced battery charging limits and CPU performance profiles for power users.
*   **Enhanced CIT**: Added Biometric (Fingerprint) and Microphone diagnostic tests.

---

## 📥 Getting Started

1.  **Download**: Obtain the latest `rlcc_v0.3.apk`.
2.  **Permissions**: Grant Camera, Location, and Phone permissions to enable physical sensor detection and network identification.
3.  **Installation**: Enable "Install from Unknown Sources" for the App Hub to function as an alternative installer.

---

*Disclaimer: Features, UI, and hardware detection databases are subject to frequent updates. Root-level features require a rooted device and may vary by manufacturer.*
