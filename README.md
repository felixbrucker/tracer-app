# Tracer 🌐

[![Android SDK](https://img.shields.io/badge/Android%20SDK-26%20%E2%80%93%2037-brightgreen.svg)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%2B%20Material%203-blue.svg)](https://developer.android.com/jetpack/compose)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4%2B-purple.svg)](https://kotlinlang.org)
[![Hilt](https://img.shields.io/badge/DI-Hilt-orange.svg)](https://dagger.dev/hilt/)
[![Room](https://img.shields.io/badge/Database-Room%202.8.5-green.svg)](https://developer.android.com/training/data-storage/room)

**Tracer** is a professional-grade, high-performance network diagnostic and routing analysis application built natively for Android. Designed following modern Android architecture standards, Tracer empowers network administrators, engineers, and power users to diagnose connectivity issues, analyze routing paths, and monitor network performance in real-time.

---

## 🚀 Key Features

- **🏓 Advanced Ping Diagnostics**: Real-time ICMP/TCP ping execution with live Round Trip Time (RTT) statistics, packet loss calculation, jitter metrics, and response visualization.
- **🌐 Deep Traceroute Analysis**: Multi-hop path tracing with TTL tracking, IP/host resolution, packet timings, and hop-by-hop latency breakdowns.
- **⚡ MTR Diagnostics**: Combined continuous Ping and Traceroute diagnostics delivering statistical packet loss percentages, sent/received packet counts, and best/worst/average latency per router hop.
- **⭐ Saved Targets & Favorites**: Room database-backed persistent storage for frequent hosts, IP addresses, and custom diagnostic targets with quick-launch capabilities.
- **⚙️ Preferences & Customization**: Jetpack DataStore-backed configuration for timeout intervals, packet sizes, ping counts, and display settings.
- **🏃 Background Service Orchestration**: Resilient long-running diagnostics powered by `TraceForegroundService` and `TraceOrchestrationManager` ensuring uninterrupted execution.

---

## 🏗️ Architecture & Tech Stack

| Layer / Concern | Technology / Library                                                                   |
| :--- |:---------------------------------------------------------------------------------------|
| **Language & Runtime** | Kotlin 2.4+ (Java 25 Target) with KSP                                                  |
| **Android SDK** | Min SDK 26 (Android 8.0), Target/Compile SDK 37 (Android 16)                           |
| **UI Framework** | Jetpack Compose + Material 3, Navigation Compose                                       |
| **Dependency Injection** | Hilt (Scoped injection with `@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`) |
| **Database** | Room 2.8.5 + KSP (Schema exports enabled, Foreign Key cascades)                        |
| **Key-Value Storage** | Jetpack DataStore Preferences (Asynchronous Flow-based repositories)                   |
| **Networking & Diagnostics** | Custom `NetworkEngine`, Coroutines & Flows                                             |
| **Background Scheduling** | Foreground Services & Coroutine Workers                                                |
| **Testing & Coverage** | JUnit 4, MockK, Coroutines Test                                                        |

---

## 🛠️ Getting Started & Build Instructions

### Prerequisites
- Android Studio (Narwhal / Koala or newer recommended)
- Java 11+
- Android SDK 37

### Building & Testing via Gradle
Use the local Gradle wrapper for all builds and verification tasks:

- **Build Debug APK**:
  ```bash
  ./gradlew assembleDebug
  ```
- **Run Unit Tests**:
  ```bash
  ./gradlew test
  ```

---

## 📄 License

This project is open-source and available under the terms of the [LICENSE](LICENSE) file.
