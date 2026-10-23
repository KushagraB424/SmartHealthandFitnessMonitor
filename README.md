# Smart Health and Fitness Monitor

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![Language](https://img.shields.io/badge/Language-Java%2011-ED8B00?logo=openjdk&logoColor=white)](https://www.java.com/)
[![Database](https://img.shields.io/badge/Database-SQLite-003B57?logo=sqlite&logoColor=white)](https://www.sqlite.org/)
[![UI](https://img.shields.io/badge/UI-Material%20Design%203-7D5260?logo=materialdesign&logoColor=white)](https://m3.material.io/)
[![Release](https://img.shields.io/badge/Release-v1.0.0-blue?logo=github)](https://github.com/KushagraB424/SmartHealthandFitnessMonitor/releases/tag/v1.0.0)

A native, battery-conscious Android application built in Java for real-time estimated step counting, active workout duration tracking, and local activity logging. The application operates entirely on-device using the phone's hardware accelerometer, direct SQLite persistence, and modern Android notification channels with zero cloud dependencies.

---

## Table of Contents

1. [Features](#features)
2. [Download & Installation](#download--installation)
3. [App Architecture](#app-architecture)
4. [Step-Detection Algorithm](#step-detection-algorithm)
5. [Activity Lifecycle & Battery Efficiency](#activity-lifecycle--battery-efficiency)
6. [How to Build from Source](#how-to-build-from-source)
7. [Tech Stack & System Requirements](#tech-stack--system-requirements)
8. [License & Acknowledgments](#license--acknowledgments)

---

## Features

- **Accelerometer Step Counting:** Evaluates 3D Euclidean acceleration vector magnitude, applies an exponential low-pass gravity filter ($\alpha = 0.8$), detects rising-edge threshold crossings ($> 2.5\text{ m/s}^2$), and enforces a $300\text{ ms}$ debounce refractory period to prevent double-counting.
- **Active Workout Timer:** Accurately measures foreground exercise duration in `HH:MM:SS` format without counting idle background periods.
- **Local SQLite History:** Automatically logs daily progress (date, steps, active duration, goal status) into an embedded SQLite database (`fitness_monitor.db`). Records are displayed newest first with an automatic empty state.
- **Dynamic Daily Step Goal:** Customize daily step targets (default: 6,000 steps). Progress updates dynamically with real-time percentage indicators.
- **Local Achievement Notifications:** Dispatches an interactive celebratory alert when the daily step target is achieved. Compliant with Android 8.0+ `NotificationChannel` and Android 13+ `POST_NOTIFICATIONS` runtime permissions, with anti-spam duplicate prevention.
- **User Health Profile:** Captures and validates user biometrics (Name, Age, Gender, Height, Weight) against physiological bounds and persists them in `SharedPreferences`.
- **100% Offline & Private:** Requires zero network permissions (`android.permission.INTERNET` is not requested). All health data remains strictly on your device.
- **Defensive Hardware Handling:** Gracefully detects missing accelerometer hardware on emulators and disables tracking safely without crashing.

---

## Download & Installation

The pre-compiled standalone APK is available on GitHub Releases:

📦 **[Download SmartHealthMonitor-v1.0.apk (6.63 MB)](https://github.com/KushagraB424/SmartHealthandFitnessMonitor/releases/download/v1.0.0/SmartHealthMonitor-v1.0.apk)**

### Install via QR Code (Direct to Phone)

Point your Android phone's camera or Google Lens at the QR code below:

<div align="center">

![Download QR Code](https://api.qrserver.com/v1/create-qr-code/?size=220x220&data=https://github.com/KushagraB424/SmartHealthandFitnessMonitor/releases/download/v1.0.0/SmartHealthMonitor-v1.0.apk)

</div>

### Installation Steps on Android
1. Download **`SmartHealthMonitor-v1.0.apk`** from the release page or scan the QR code.
2. When prompted with *"File might be harmful"*, tap **Download anyway** (standard Android prompt for apps installed outside Google Play).
3. Open the downloaded file and tap **Install** (if prompted, toggle **Allow from this source** in Settings).
4. Launch **Smart Health & Fitness Monitor**, grant the notification permission, and tap **Start Tracking**!

---

## App Architecture

The project follows the **Model-View-Controller (MVC)** design pattern:

```text
+-------------------------------------------------------------------------------+
|                             APPLICATION ARCHITECTURE                          |
+-------------------------------------------------------------------------------+
|                                                                               |
|  PRESENTATION (VIEW)         CONTROLLERS                   DATA LAYER (MODEL) |
|  [activity_main.xml]      <-> [MainActivity]           <-> [SensorManager]    |
|                                     │                  <-> [SharedPreferences]|
|                                     ▼ (Explicit Intent)                       |
|  [activity_profile.xml]   <-> [ProfileActivity]        <-> [SharedPreferences]|
|                                     │                                         |
|                                     ▼ (Explicit Intent)                       |
|  [activity_history.xml]   <-> [HistoryActivity]        <-> [DatabaseHelper]   |
|         │                           │                               │         |
|  [item_history_record]    <-> [HistoryAdapter]         <-> [fitness_monitor.db|
|                                                              (daily_activity)]|
+-------------------------------------------------------------------------------+
```

### Core Components (`com.example.smarthealthfitness`)

- **`MainActivity.java`**: Primary dashboard controller. Manages accelerometer listener events, foreground timer loop, goal settings, notification dispatch, and lifecycle telemetry.
- **`ProfileActivity.java`**: Form controller for capturing and validating user biometrics with inline error feedback (`setError()`), saved to `SharedPreferences`.
- **`HistoryActivity.java`**: History viewer querying the SQLite database descending by date and rendering records into a `ListView`.
- **`DatabaseHelper.java`**: SQLite helper managing table `daily_activity` with idempotent atomic upsert queries (`insertOrUpdateDailyRecord`).
- **`DailyRecord.java`**: POJO entity model encapsulating date, step count, active duration, and goal notification status.
- **`HistoryAdapter.java`**: Custom `ArrayAdapter` binding daily records to Material cards with duration formatting.

---

## Step-Detection Algorithm

Human walking generates rhythmic acceleration spikes superimposed on Earth's static gravity ($~9.81\text{ m/s}^2$). The app translates raw sensor streams into steps via a five-stage digital signal conditioning pipeline:

```text
  [ Raw Accelerometer ] ──> (ax, ay, az)
           │
           ▼
  [ 1. Vector Magnitude ] ──> m = √(ax² + ay² + az²)  (Orientation-Invariant)
           │
           ▼
  [ 2. Gravity Filter ]  ──> g = α·g + (1-α)·m        (α = 0.8 Low-Pass Baseline)
           │
           ▼
  [ 3. Linear Deviation ]──> Δa = m - g               (Dynamic Kinetic Force)
           │
           ▼
  [ 4. Threshold Test ]  ──> Is Δa > 2.5 m/s² ?       (Rising-Edge Crossing)
           │
           ▼
  [ 5. Debounce Window ] ──> Is Δt ≥ 300 ms ?         (Reject Vibrations & Double-Counting)
           │
           ▼
    [ STEP COUNTED! ] ──> todaySteps++ & Update UI
```

1. **3D Euclidean Vector Magnitude:**
   $$m = \sqrt{a_x^2 + a_y^2 + a_z^2}$$
   Taking the Euclidean norm makes acceleration measurement invariant to phone tilt or orientation (pocket, hand, or bag).
2. **Low-Pass Gravity Isolation Filter:**
   $$g_t = \alpha \cdot g_{t-1} + (1 - \alpha) \cdot m_t \quad (\alpha = 0.8)$$
   Maintains a dynamic baseline of static gravitational acceleration.
3. **Dynamic Acceleration Deviation:**
   $$\Delta a_t = m_t - g_t$$
   Extracts net kinetic force from the footstrike.
4. **Rising-Edge Threshold Crossing:**
   Primed when $\Delta a_t > 2.5\text{ m/s}^2$, rejecting subtle tremors and vehicle vibrations.
5. **Debounce Refractory Window:**
   $$\Delta t = t_{\text{current}} - t_{\text{last\_step}} \ge 300\text{ ms}$$
   Filters out resonance recoil and caps maximum cadence at $3.33\text{ steps/sec}$ ($200\text{ steps/min}$).

---

## Activity Lifecycle & Battery Efficiency

To conserve device battery, sensor listeners and background threads are strictly bound to Android Activity lifecycle states:

```text
  [ Activity Resumed ] ──> onResume()  ──> Register Accelerometer & Start Timer
                                 │
  [ User Minimizes App ] ─> onPause()   ──> Unregister Accelerometer & Stop Timer
                                 │      ──> Auto-Save Progress to SQLite
                                 ▼
  [ Activity Hidden ]  ──> onStop()    ──> Release Non-Critical Resources
```

- **`onResume()`**: Registers `SensorEventListener` (`SENSOR_DELAY_UI`) and resumes the timer runnable only when the screen is interactive and tracking is active.
- **`onPause()`**: Immediately unregisters the sensor listener, halts the timer runnable, and commits in-memory totals to SQLite. Zero background battery consumption.
- **`onSaveInstanceState()`**: Preserves volatile workout totals across device orientation changes.
- **Logcat Telemetry**: All lifecycle transitions are logged under tag `FitnessLifecycle`. Filter by `tag:FitnessLifecycle` in Android Studio Logcat to monitor state transitions in real time.

---

## How to Build from Source

### Prerequisites
- **Android Studio:** Ladybug (2024.2) or newer
- **JDK:** Java 11 or higher
- **Android SDK:** Platform API Level 34 or 36

### Build via Android Studio
1. Clone the repository:
   ```bash
   git clone https://github.com/KushagraB424/SmartHealthandFitnessMonitor.git
   ```
2. Open the project folder in Android Studio.
3. Wait for Gradle to finish syncing dependencies.
4. Click **Run ('app')** (Shift + F10) to deploy to your connected device or emulator.

### Build APK via Command Line
```powershell
# Windows
.\gradlew.bat assembleDebug

# macOS / Linux
./gradlew assembleDebug
```
The compiled debug APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## Tech Stack & System Requirements

| Category | Specification |
| :--- | :--- |
| **Language** | Java 11 |
| **UI Framework** | XML Layouts with Material Design 3 |
| **Minimum SDK** | Android 8.0 (API Level 26, Oreo) |
| **Target / Compile SDK** | Android 14 / 16 (API Level 34 / 36) |
| **Sensors** | `Sensor.TYPE_ACCELEROMETER` via `SensorManager` |
| **Persistence** | SQLite (`SQLiteOpenHelper`) & `SharedPreferences` |
| **Notifications** | `NotificationCompat` with high-importance channel |
| **Network Permission** | None (100% Offline) |

---

## License & Acknowledgments

This project is open-source and available under the standard open educational project guidelines.

- Built using the native **Android SDK** and **AndroidX / Material Components**.
- Special thanks to the Android open-source community and Google Developer documentation for sensors, SQLite, and modern notification channel references.
