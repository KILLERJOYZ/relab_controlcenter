# Relab Control Center (rlcc) - v0.1-beta

**Relab Control Center** is a powerful, all-in-one utility and diagnostic suite built for Android enthusiasts, developers, and power users. It combines deep hardware intelligence with real-time performance monitoring and a specialized installer hub to provide total transparency into the Android ecosystem.

---

## 🏛 Project Overview

The project is designed to bridge the gap between high-level "About Phone" settings and low-level system parameters. It serves four main purposes:
1.  **Hardware Verification**: Identifying exact sensor models, SoC architecture, and component specifications.
2.  **Hardware Diagnostics (CIT)**: A comprehensive suite of interactive tests to verify device integrity.
3.  **Performance Analytics**: Providing real-time telemetry for CPU, GPU, RAM, and Network.
4.  **Smart App Management**: Streamlining the installation of essential benchmarks and games with intelligent compatibility handling.

---

## 🚀 Key Modules

### 📱 Device & OS Intelligence
*   **Camera Sensor Probe**: A specialized tool that detects physical vs. binned resolution and identifies exact sensor hardware (Sony IMX, Samsung S5K, OmniVision, etc.) using a built-in sensor database.
*   **SoC Architecture**: Goes beyond basic names to identify CPU clusters (Prime/Gold/Silver) and correctly brands the latest flagships (e.g., Qualcomm "Oryon" cores).
*   **Advanced OS Detection**: Robust identification of OEM skins including Samsung **One UI**, Xiaomi **HyperOS**, Vivo **OriginOS**, **Nothing OS**, and major Custom ROMs.
*   **Memory & Storage**: High-fidelity detection of RAM types (LPDDR5X, etc.) and storage bus models (UFS 4.0).

### 🛠 Hardware Diagnostic Suite (CIT)
A professional-grade **Common Interaction Toolkit** for verifying hardware functionality:
*   **Display & Touch**: LCD pixel tests, multi-touch accuracy, and gesture verification.
*   **Audio & Haptics**: Frequency tests for Earpiece, Speaker, and Mic, plus Vibration motor diagnostics.
*   **Sensor Array**: Real-time data from Accelerometer, Gyroscope, Proximity, Light, Compass, and Barometer.
*   **Connectivity**: Functional testing for Wi-Fi, Bluetooth, GPS, and NFC.

### 📊 Real-time Dashboard
*   **Live Telemetry**: Smooth, high-frequency graphs for CPU utilization and per-core frequencies.
*   **Thermal Monitoring**: Tracking temperature sensors across multiple internal thermal zones (SoC, Battery, Skin).
*   **Battery Analytics**: Precision wattage monitoring for charging/discharging and 24-hour capacity history.
*   **Network Intelligence**: Real-time link speeds, standard detection (Wi-Fi 7 ready), and signal strength.

### 📦 Smart App Hub (The Installer)
*   **Curated Repository**: A centralized hub for benchmarks and high-performance games.
*   **Split APK Intelligence**: Automatically detects apps using App Bundles/Split APKs and recommends the most reliable installation path.
*   **Reliable Downloads**: Custom network logic with dynamic Referer headers and redirect handling to bypass CDN blocks.

---

## 🛠 Technical Foundation

*   **Language**: 100% Kotlin.
*   **UI Framework**: Jetpack Compose with Material 3 Design.
*   **Architecture**: MVVM (Model-View-ViewModel) with Kotlin Coroutines and StateFlow for reactive, thread-safe UI updates.
*   **Modern Android Standards**:
    *   **16 KB Page Size Support**: Fully compatible with Android 15's 16 KB memory configuration.
    *   **Scoped Storage & URI Handling**: Uses modern `content://` URI management for system-level installations.
    *   **Bilingual Interface**: Fully localized in **English** and **Vietnamese**.

---

## 🏁 Beta v0.1 Highlights

This pre-release version introduces:
*   **Expanded OS Support**: New robust detection for One UI (Samsung), HyperOS (Xiaomi), and OriginOS (Vivo).
*   **Enhanced SoC Branding**: Full support for Qualcomm Elite / Gen 5 "Oryon" core detection.
*   **Improved Camera Database**: Updated mapping for Sony, Samsung, and OmniVision sensors.
*   **Installation Safeguards**: Improved download reliability with dynamic headers and Split APK warnings.

---

## 📥 Getting Started

1.  **Download**: Obtain the latest `rlcc_beta-v0.1.apk`.
2.  **Permissions**: Grant Camera and Location permissions to enable physical sensor detection and network identification.
3.  **Installation**: Enable "Install from Unknown Sources" for the App Hub to function as an alternative installer.

---

*Disclaimer: This is a pre-release beta version. Features, UI, and hardware detection databases are subject to frequent updates.*
