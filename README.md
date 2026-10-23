# Smart Health and Fitness Monitor

**Smart Health and Fitness Monitor** is a native Android application developed in Java for a student mini-project. It uses Android's accelerometer sensor for real-time estimated step counting, tracks active foreground exercise duration, persists daily records using SQLite (`SQLiteOpenHelper`), stores user profiles and step goals in `SharedPreferences`, and sends local goal achievement notifications with runtime permission handling.

---

## Table of Contents

1. [Features Summary](#features-summary)
2. [Project Architecture & Tech Stack](#project-architecture--tech-stack)
3. [How to Open, Sync, and Run the Project](#how-to-open-sync-and-run-the-project)
4. [Running on a Physical Android Phone (USB Debugging)](#running-on-a-physical-android-phone-usb-debugging)
5. [Feature Guide](#feature-guide)
   - [Feature 1: User Profile Management](#feature-1-user-profile-management)
   - [Feature 2: Physical Activity Monitoring (Accelerometer)](#feature-2-physical-activity-monitoring-accelerometer)
   - [Feature 3: Daily Activity Records (SQLite)](#feature-3-daily-activity-records-sqlite)
   - [Feature 4: Activity Goal Settings](#feature-4-activity-goal-settings)
   - [Feature 5: Activity Notifications](#feature-5-activity-notifications)
6. [Accelerometer Step-Detection Algorithm & Limitations](#accelerometer-step-detection-algorithm--limitations)
7. [Activity Lifecycle & Logcat Monitoring](#activity-lifecycle--logcat-monitoring)
8. [Report Deliverable & PDF Export](#report-deliverable--pdf-export)

---

## Features Summary

- **Native Java & XML Layouts:** Clear, beginner-friendly codebase without third-party architecture overhead.
- **Direct SQLite (`SQLiteOpenHelper`):** One daily record per date storing date, estimated steps, active duration, and goal notification flag.
- **Fast Local Preferences (`SharedPreferences`):** Persists personal health profile and daily step goals.
- **Accelerometer-Based Step Detection:** Evaluates 3D magnitude, applies a low-pass gravity filter, detects threshold crossings, and applies time debouncing.
- **Foreground Tracking & Battery Friendly:** Sensors and timers unregister in `onPause()` to prevent battery drain.
- **Local Android Notifications:** Alert sent when daily step goal is achieved, avoiding duplicate alerts across steps or app restarts.

---

## Project Architecture & Tech Stack

- **Language:** Java 11
- **UI:** XML Layouts with Material Components (Material 3)
- **Minimum SDK:** Android 8.0 (API Level 26)
- **Compile / Target SDK:** Android 14 / 16 (API 34 / 36 compatible)
- **Local Database:** `SQLiteOpenHelper` (`DatabaseHelper.java`)
- **Key-Value Storage:** `SharedPreferences`
- **Sensors:** `SensorManager`, `Sensor.TYPE_ACCELEROMETER`, `SensorEventListener`
- **Notifications:** `NotificationCompat`, `NotificationChannel`, `POST_NOTIFICATIONS` runtime permission

### Package Structure (`com.example.smarthealthfitness`)

```text
com.example.smarthealthfitness/
├── MainActivity.java         # Main dashboard, sensor tracking, timer, goal settings
├── ProfileActivity.java      # User profile input, validation, SharedPreferences
├── HistoryActivity.java      # SQLite records viewer with ListView & empty state
├── DatabaseHelper.java       # SQLiteOpenHelper for daily_activity table
├── DailyRecord.java          # Activity record data model
└── HistoryAdapter.java       # Custom ArrayAdapter for history ListView items
```

---

## How to Open, Sync, and Run the Project

1. **Launch Android Studio:**
   - Open Android Studio (Ladybug, Koala, Jellyfish, or newer).
2. **Open the Project:**
   - Click **File > Open...**
   - Navigate to `C:\Users\<YourUser>\AndroidStudioProjects\SmartHealthandFitnessMonitor` and click **OK**.
3. **Gradle Sync:**
   - Android Studio will automatically detect the Gradle files and initiate a sync.
   - If prompted, click **Sync Project with Gradle Files** (elephant icon in top toolbar).
4. **Build APK via Terminal (Optional):**
   ```powershell
   .\gradlew.bat assembleDebug
   ```
   The built APK will be located at:
   `app\build\outputs\apk\debug\app-debug.apk`

---

## Running on a Physical Android Phone (USB Debugging)

To test the physical accelerometer sensor and notifications on a real phone:

1. **Enable Developer Options:**
   - On your Android phone, go to **Settings > About Phone**.
   - Tap **Build Number** 7 times until you see the toast message *"You are now a developer!"*.
2. **Enable USB Debugging:**
   - Go to **Settings > System > Developer Options** (or search "Developer Options" in Settings).
   - Toggle **USB Debugging** to **ON**.
3. **Connect Device to PC:**
   - Connect the phone via a USB data cable.
   - On the phone, a prompt *"Allow USB debugging?"* will appear. Check *"Always allow from this computer"* and tap **Allow**.
4. **Run from Android Studio:**
   - In Android Studio's top toolbar, ensure your connected device name appears in the device selector.
   - Click the green **Run ('app')** button (Shift + F10).
   - The application will compile, install, and launch on your phone.

---

## Feature Guide

### Feature 1: User Profile Management
- Open via the **"User Profile"** button on MainActivity.
- Form fields:
  - **Full Name:** Required, at least 2 characters.
  - **Age:** Positive integer between 1 and 120.
  - **Gender:** Dropdown selection (Male, Female, Other, Prefer not to say).
  - **Height:** Decimal allowed (50.0 cm to 250.0 cm).
  - **Weight:** Decimal allowed (20.0 kg to 300.0 kg).
- Values are validated on clicking **Save Profile**. Invalid inputs show friendly field errors (`setError`) or Toast warnings without crashing.
- Saved profile reloads automatically every time the profile screen opens.

### Feature 2: Physical Activity Monitoring (Accelerometer)
- Tap **"Start Tracking"** to begin active monitoring.
- Tap **"Pause Tracking"** to stop monitoring and duration counting.
- Displays:
  - Current day's estimated step count.
  - Active duration spent tracking while the main screen is in the foreground.
- **Sensor Availability Check:** If tested on an emulator or device without an accelerometer, the app displays a clear warning banner and disables tracking safely.
- **Disclaimer:** Prominently displays: *“Step counts are estimates and are not medical measurements.”*

### Feature 3: Daily Activity Records (SQLite)
- Today's record is updated automatically in SQLite (`fitness_monitor.db`).
- Records are saved on pausing, periodically during active tracking, and when leaving the foreground (`onPause`).
- Tap **"Activity History"** to open `HistoryActivity`.
- Records are displayed newest first.
- If no records exist yet, it displays: *"No activity records yet"*.
- When the calendar date rolls over midnight, the app archives previous totals and starts fresh for the new day.

### Feature 4: Activity Goal Settings
- Input numeric daily step target into the **Daily Step Goal** field (default: 6,000 steps).
- Tap **"Save Goal"**. Validates positive integer (> 0).
- Goal is saved into `SharedPreferences` and restored upon restarting.
- Main screen shows progress: e.g., `Goal: 6,000 steps (45%)`.

### Feature 5: Activity Notifications
- When today's steps reach or exceed the daily goal, a local notification is dispatched:
  - **Title:** `Daily Goal Achieved!`
  - **Message:** `Congratulations! You achieved your daily step goal. Keep moving!`
  - **Channel:** `fitness_goal_channel` (High importance, vibration enabled).
  - **Icon:** Custom clean fitness running icon (`ic_notification_fitness`).
  - **Tap Action:** Launches `MainActivity`.
- **Duplicate Prevention:** Sets `goal_notified = 1` in SQLite. Prevents duplicate alerts for subsequent steps or app restarts.
- **Android 13+ Support:** Requests `android.permission.POST_NOTIFICATIONS` at runtime. Denying permission suppresses notifications gracefully without errors.

---

## Accelerometer Step-Detection Algorithm & Limitations

### Mathematical Algorithm
1. **3D Magnitude Calculation:**
   $$\text{Magnitude} = \sqrt{x^2 + y^2 + z^2}$$
   Taking the Euclidean norm makes acceleration measurement invariant to phone orientation in a pocket or hand.
2. **Low-Pass Gravity Filter:**
   $$g_{\text{smoothed}} = \alpha \cdot g_{\text{smoothed}} + (1 - \alpha) \cdot \text{Magnitude} \quad (\alpha = 0.8)$$
   This maintains a dynamic baseline of static gravity (~9.81 $m/s^2$).
3. **Dynamic Acceleration Deviation:**
   $$\Delta a = \text{Magnitude} - g_{\text{smoothed}}$$
4. **Threshold Crossing:**
   A step trigger is primed when $\Delta a > 2.5 \text{ m/s}^2$ on a rising edge.
5. **Debouncing (Minimum Step Interval):**
   $$\Delta t = t_{\text{current}} - t_{\text{last\_step}} \ge 300\text{ ms}$$
   Filters out high-frequency vibrations, hand shakes, or multiple axis oscillations within a single foot strike.

### Real-World Limitations
- **False Positives:** Gentle vehicle movement, riding a bus over bumps, shaking the device, or tapping can trigger false steps.
- **No Stride Length Calibration:** Steps are raw counts without GPS distance or user height stride calculation.
- **Foreground Only:** Tracking pauses when leaving the app to conserve device battery and avoid background service complexity.

---

## Activity Lifecycle & Logcat Monitoring

The application demonstrates Android Activity lifecycle states:
- **Active:** `onResume()` – Activity interactive; accelerometer listener registered; duration timer running.
- **Paused:** `onPause()` – Activity losing focus; accelerometer unregistered; timer stopped; SQLite record saved.
- **Stopped:** `onStop()` – Activity hidden.
- **Destroyed:** `onDestroy()` – Activity destroyed.

### How to View Lifecycle Messages in Logcat

1. Connect your device or emulator.
2. Open the **Logcat** tab at the bottom of Android Studio.
3. In the search/filter box, enter:
   ```text
   tag:FitnessLifecycle
   ```
4. Perform actions on your phone to observe the logged transitions:
   - Rotate the phone (triggers `onPause -> onStop -> onDestroy -> onCreate -> onStart -> onResume`).
   - Switch apps (triggers `onPause -> onStop`).
   - Reopen app (triggers `onStart -> onResume`).
   - Start / pause tracking (logs tracking start, stop, and database save events).

---

## Report Deliverable & PDF Export

- **Editable Report Draft:** [`PROJECT_REPORT_DRAFT.md`](PROJECT_REPORT_DRAFT.md)
- **Print-Ready HTML Version:** [`PROJECT_REPORT_DRAFT.html`](PROJECT_REPORT_DRAFT.html)
- **Pre-generated PDF Report:** [`PROJECT_REPORT_DRAFT.pdf`](PROJECT_REPORT_DRAFT.pdf)

### How to Regenerate or Update the PDF:
1. Open [`PROJECT_REPORT_DRAFT.html`](PROJECT_REPORT_DRAFT.html) in Microsoft Edge or Google Chrome.
2. Press **Ctrl + P** (Print).
3. Select Destination: **Save as PDF**.
4. Check **Background graphics** and click **Save**.
