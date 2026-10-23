package com.example.smarthealthfitness;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * MainActivity serves as the primary dashboard for the Smart Health and Fitness Monitor.
 *
 * Responsibilities:
 * 1. Tracks daily estimated steps using the phone's accelerometer sensor.
 * 2. Tracks active duration while the app is in the foreground and tracking is active.
 * 3. Manages daily step goals persisted via SharedPreferences.
 * 4. Sends a one-time local notification upon reaching the daily goal.
 * 5. Saves and restores daily records via SQLiteOpenHelper (DatabaseHelper).
 * 6. Demonstrates proper Android Activity lifecycle management with Logcat logging.
 */
public class MainActivity extends AppCompatActivity implements SensorEventListener {

    // Lifecycle and general logging tag
    public static final String TAG = "FitnessLifecycle";

    // Notification constants
    private static final String CHANNEL_ID = "fitness_goal_channel";
    private static final int NOTIFICATION_ID = 1001;
    private static final int PERMISSION_REQUEST_NOTIFICATIONS = 101;

    // SharedPreferences configuration for Daily Step Goal
    public static final String PREF_GOAL_NAME = "fitness_goal_prefs";
    public static final String KEY_DAILY_STEP_GOAL = "key_daily_step_goal";
    public static final int DEFAULT_STEP_GOAL = 6000;

    // Bundle state keys for screen rotation preservation
    private static final String STATE_CURRENT_STEPS = "state_current_steps";
    private static final String STATE_DURATION_SECONDS = "state_duration_seconds";
    private static final String STATE_IS_TRACKING = "state_is_tracking";
    private static final String STATE_TODAY_DATE = "state_today_date";

    // =========================================================================
    // STEP DETECTION ALGORITHM CONSTANTS AND STATE
    // =========================================================================
    // Algorithm Overview:
    // 1. Acceleration Magnitude: We calculate the Euclidean norm (magnitude) of the 3D
    //    acceleration vector: magnitude = sqrt(x^2 + y^2 + z^2). This makes the detection
    //    invariant to how the user holds or rotates the phone.
    // 2. Low-Pass Gravity Filter: Standard gravity on Earth is ~9.81 m/s^2, but the device's
    //    orientation and slight baseline shifts require a moving average baseline:
    //    smoothedGravity = alpha * smoothedGravity + (1 - alpha) * magnitude.
    // 3. Dynamic Acceleration: We compute the deviation: linearAcceleration = magnitude - smoothedGravity.
    // 4. Threshold Crossing: A step is counted when dynamic acceleration crosses above
    //    STEP_THRESHOLD (2.5 m/s^2) from below.
    // 5. Minimum Interval (Debouncing): Human footsteps in normal walking or running have a
    //    maximum cadence of around 3 to 4 steps per second. We enforce MIN_STEP_INTERVAL_MS (300 ms)
    //    between successive steps to filter out high-frequency vibrations and false positives.
    //
    // Limitations:
    // - Pocket shifting, hand gestures, vehicle rides (bus, car, train) over bumpy surfaces,
    //   or dropping/tapping the phone can generate acceleration spikes and false steps.
    // - No GPS or stride-length calibration is performed.
    // - Steps are estimates and are not medical measurements.
    // =========================================================================
    private static final float GRAVITY_ALPHA = 0.8f;
    private static final float STEP_THRESHOLD = 2.5f; // Acceleration deviation in m/s^2
    private static final long MIN_STEP_INTERVAL_MS = 300; // Minimum interval between steps (0.3s)

    private float smoothedGravity = 9.81f;
    private boolean isAboveThreshold = false;
    private long lastStepTimestampMs = 0;

    // Sensor Management
    private SensorManager sensorManager;
    private Sensor accelerometerSensor;
    private boolean isSensorAvailable = false;
    private boolean isSensorRegistered = false;

    // Tracking state variables
    private boolean isTracking = false;
    private int todaySteps = 0;
    private int todayDurationSeconds = 0;
    private boolean todayGoalNotified = false;
    private String currentDateStr = "";

    // Goal settings
    private int dailyStepGoal = DEFAULT_STEP_GOAL;
    private SharedPreferences goalPreferences;

    // Database helper
    private DatabaseHelper databaseHelper;

    // Duration timer (Foreground only)
    private Handler timerHandler;
    private Runnable timerRunnable;
    private boolean isTimerRunning = false;

    // UI Views
    private TextView tvCurrentDate;
    private TextView tvTrackingStatus;
    private TextView tvStepCount;
    private TextView tvGoalProgress;
    private TextView tvDuration;
    private Button btnTrackingToggle;
    private EditText etDailyGoal;
    private Button btnSaveGoal;
    private Button btnOpenProfile;
    private Button btnOpenHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "MainActivity: onCreate() - Initializing UI, sensors, preferences, and database.");
        setContentView(R.layout.activity_main);

        // 1. Bind Views
        initViews();

        // 2. Initialize SharedPreferences and SQLite Database
        goalPreferences = getSharedPreferences(PREF_GOAL_NAME, Context.MODE_PRIVATE);
        dailyStepGoal = goalPreferences.getInt(KEY_DAILY_STEP_GOAL, DEFAULT_STEP_GOAL);
        databaseHelper = new DatabaseHelper(this);

        // 3. Initialize Date
        currentDateStr = getTodayDateString();
        tvCurrentDate.setText(String.format(Locale.getDefault(), "Date: %s", currentDateStr));

        // 4. Initialize Notification Channel (Android 8.0+)
        createNotificationChannel();

        // 5. Initialize Accelerometer Sensor
        initSensor();

        // 6. Setup Foreground Duration Timer
        initDurationTimer();

        // 7. Load today's saved data from SQLite
        loadTodayRecordFromDatabase();

        // 8. Restore instance state if coming from screen rotation
        if (savedInstanceState != null) {
            todaySteps = savedInstanceState.getInt(STATE_CURRENT_STEPS, todaySteps);
            todayDurationSeconds = savedInstanceState.getInt(STATE_DURATION_SECONDS, todayDurationSeconds);
            isTracking = savedInstanceState.getBoolean(STATE_IS_TRACKING, false);
            String savedDate = savedInstanceState.getString(STATE_TODAY_DATE, currentDateStr);
            if (!savedDate.equals(currentDateStr)) {
                // If date changed during recreation
                currentDateStr = savedDate;
            }
            Log.d(TAG, "MainActivity: onCreate() - Restored state from Bundle: Steps=" + todaySteps + ", Duration=" + todayDurationSeconds);
        }

        // 9. Update UI displays
        updateUiState();

        // 10. Set Click Listeners
        setupListeners();

        // 11. Request notification permission on Android 13+
        requestNotificationPermissionIfNeeded();
    }

    private void initViews() {
        tvCurrentDate = findViewById(R.id.tvCurrentDate);
        tvTrackingStatus = findViewById(R.id.tvTrackingStatus);
        tvStepCount = findViewById(R.id.tvStepCount);
        tvGoalProgress = findViewById(R.id.tvGoalProgress);
        tvDuration = findViewById(R.id.tvDuration);
        btnTrackingToggle = findViewById(R.id.btnTrackingToggle);
        etDailyGoal = findViewById(R.id.etDailyGoal);
        btnSaveGoal = findViewById(R.id.btnSaveGoal);
        btnOpenProfile = findViewById(R.id.btnOpenProfile);
        btnOpenHistory = findViewById(R.id.btnOpenHistory);
    }

    /**
     * Checks for hardware accelerometer sensor and prepares SensorManager.
     */
    private void initSensor() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accelerometerSensor != null) {
                isSensorAvailable = true;
                Log.d(TAG, "MainActivity: Accelerometer sensor detected and ready.");
            } else {
                isSensorAvailable = false;
                Log.w(TAG, "MainActivity: Accelerometer sensor NOT available on this hardware.");
            }
        } else {
            isSensorAvailable = false;
            Log.e(TAG, "MainActivity: SensorManager service not found.");
        }

        if (!isSensorAvailable) {
            tvTrackingStatus.setText(R.string.status_sensor_missing);
            tvTrackingStatus.setBackgroundColor(ContextCompat.getColor(this, R.color.color_error));
            tvTrackingStatus.setTextColor(ContextCompat.getColor(this, R.color.white));
            btnTrackingToggle.setEnabled(false);
            Toast.makeText(this, R.string.status_sensor_missing, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Prepares the Handler and Runnable used to count active tracking duration every second.
     */
    private void initDurationTimer() {
        timerHandler = new Handler(Looper.getMainLooper());
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isTracking) {
                    todayDurationSeconds++;
                    tvDuration.setText(formatDuration(todayDurationSeconds));

                    // Periodic auto-save every 10 seconds while tracking
                    if (todayDurationSeconds % 10 == 0) {
                        saveTodayRecordToDatabase();
                    }

                    // Check for midnight date change
                    checkDateRollover();

                    // Schedule next tick after 1000ms
                    timerHandler.postDelayed(this, 1000);
                }
            }
        };
    }

    /**
     * Sets up UI event listeners for buttons and user interactions.
     */
    private void setupListeners() {
        // Toggle tracking Start / Pause
        btnTrackingToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isSensorAvailable) {
                    Toast.makeText(MainActivity.this, R.string.status_sensor_missing, Toast.LENGTH_SHORT).show();
                    return;
                }

                if (isTracking) {
                    pauseTracking();
                } else {
                    startTracking();
                }
            }
        });

        // Save daily step goal
        btnSaveGoal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveDailyGoal();
            }
        });

        // Open Profile Activity
        btnOpenProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                startActivity(intent);
            }
        });

        // Open History Activity
        btnOpenHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
                startActivity(intent);
            }
        });
    }

    /**
     * Starts or resumes physical activity tracking.
     */
    private void startTracking() {
        isTracking = true;
        Log.d(TAG, "MainActivity: User started tracking.");
        registerAccelerometer();
        startDurationTimer();
        updateUiState();
    }

    /**
     * Pauses physical activity tracking and immediately saves progress.
     */
    private void pauseTracking() {
        isTracking = false;
        Log.d(TAG, "MainActivity: User paused tracking.");
        unregisterAccelerometer();
        stopDurationTimer();
        saveTodayRecordToDatabase();
        updateUiState();
    }

    /**
     * Registers the accelerometer listener only if sensor is available and not already registered.
     */
    private void registerAccelerometer() {
        if (isSensorAvailable && !isSensorRegistered && sensorManager != null) {
            sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_UI);
            isSensorRegistered = true;
            Log.d(TAG, "MainActivity: Accelerometer listener registered.");
        }
    }

    /**
     * Unregisters the accelerometer listener safely.
     */
    private void unregisterAccelerometer() {
        if (isSensorRegistered && sensorManager != null) {
            sensorManager.unregisterListener(this);
            isSensorRegistered = false;
            Log.d(TAG, "MainActivity: Accelerometer listener unregistered.");
        }
    }

    /**
     * Starts the duration timer Handler.
     */
    private void startDurationTimer() {
        if (!isTimerRunning) {
            timerHandler.postDelayed(timerRunnable, 1000);
            isTimerRunning = true;
        }
    }

    /**
     * Stops the duration timer Handler.
     */
    private void stopDurationTimer() {
        if (isTimerRunning) {
            timerHandler.removeCallbacks(timerRunnable);
            isTimerRunning = false;
        }
    }

    /**
     * Updates all UI elements based on current values and tracking state.
     */
    private void updateUiState() {
        tvStepCount.setText(String.format(Locale.getDefault(), "%,d", todaySteps));
        tvDuration.setText(formatDuration(todayDurationSeconds));
        etDailyGoal.setText(String.valueOf(dailyStepGoal));

        // Format goal progress text
        int percentage = (dailyStepGoal > 0) ? (todaySteps * 100 / dailyStepGoal) : 0;
        tvGoalProgress.setText(String.format(Locale.getDefault(), "Goal: %,d steps (%d%%)", dailyStepGoal, percentage));

        if (!isSensorAvailable) {
            tvTrackingStatus.setText(R.string.status_sensor_missing);
            tvTrackingStatus.setBackgroundColor(ContextCompat.getColor(this, R.color.color_error));
            tvTrackingStatus.setTextColor(ContextCompat.getColor(this, R.color.white));
            btnTrackingToggle.setEnabled(false);
            btnTrackingToggle.setText(R.string.btn_start_tracking);
            return;
        }

        if (isTracking) {
            tvTrackingStatus.setText(R.string.status_tracking_active);
            tvTrackingStatus.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_container));
            tvTrackingStatus.setTextColor(ContextCompat.getColor(this, R.color.on_primary_container));
            btnTrackingToggle.setText(R.string.btn_pause_tracking);
            btnTrackingToggle.setBackgroundColor(ContextCompat.getColor(this, R.color.color_warning));
        } else {
            tvTrackingStatus.setText(R.string.status_tracking_paused);
            tvTrackingStatus.setBackgroundColor(ContextCompat.getColor(this, R.color.surface_variant));
            tvTrackingStatus.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            btnTrackingToggle.setText(R.string.btn_start_tracking);
            btnTrackingToggle.setBackgroundColor(ContextCompat.getColor(this, R.color.primary));
        }
    }

    /**
     * Validates and saves the new daily step goal to SharedPreferences.
     * Also checks if the updated goal was already met today.
     */
    private void saveDailyGoal() {
        String goalStr = etDailyGoal.getText().toString().trim();

        if (goalStr.isEmpty()) {
            etDailyGoal.setError(getString(R.string.error_invalid_goal));
            etDailyGoal.requestFocus();
            return;
        }

        int parsedGoal;
        try {
            parsedGoal = Integer.parseInt(goalStr);
            if (parsedGoal <= 0) {
                etDailyGoal.setError(getString(R.string.error_invalid_goal));
                etDailyGoal.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            etDailyGoal.setError(getString(R.string.error_invalid_goal));
            etDailyGoal.requestFocus();
            return;
        }

        dailyStepGoal = parsedGoal;
        SharedPreferences.Editor editor = goalPreferences.edit();
        editor.putInt(KEY_DAILY_STEP_GOAL, dailyStepGoal);
        editor.apply();

        Toast.makeText(this, R.string.goal_saved_success, Toast.LENGTH_SHORT).show();
        Log.d(TAG, "MainActivity: Daily step goal saved: " + dailyStepGoal);

        updateUiState();

        // Check if today's existing step count already achieves the newly saved goal
        checkGoalAchievement();
    }

    /**
     * Verifies if today's steps have reached or exceeded the goal.
     * Triggers a notification if not already sent today.
     */
    private void checkGoalAchievement() {
        if (todaySteps >= dailyStepGoal && !todayGoalNotified) {
            todayGoalNotified = true;
            sendGoalNotification();
            saveTodayRecordToDatabase();
            Log.d(TAG, "MainActivity: Goal reached (" + todaySteps + " >= " + dailyStepGoal + "). Notification dispatched.");
        }
    }

    /**
     * Dispatches a local system notification informing the user that the daily goal has been met.
     */
    private void sendGoalNotification() {
        // Prepare tap action that opens MainActivity
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_fitness)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_message))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(getString(R.string.notification_message)))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // Safe permission check for Android 13+ (POST_NOTIFICATIONS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "MainActivity: Notification permission not granted; notification suppressed.");
                Toast.makeText(this, R.string.permission_denied_notification, Toast.LENGTH_SHORT).show();
                return;
            }
        }

        try {
            NotificationManagerCompat managerCompat = NotificationManagerCompat.from(this);
            managerCompat.notify(NOTIFICATION_ID, builder.build());
        } catch (SecurityException se) {
            Log.e(TAG, "MainActivity: SecurityException while showing notification", se);
        }
    }

    /**
     * Creates notification channel required on Android 8.0 (API level 26) and above.
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.notification_channel_name);
            String description = getString(R.string.notification_channel_desc);
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.enableVibration(true);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Requests POST_NOTIFICATIONS runtime permission on Android 13+ (API level 33+).
     */
    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        PERMISSION_REQUEST_NOTIFICATIONS
                );
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "MainActivity: Notification permission granted by user.");
            } else {
                Log.w(TAG, "MainActivity: Notification permission denied by user.");
                Toast.makeText(this, R.string.permission_denied_notification, Toast.LENGTH_SHORT).show();
            }
        }
    }

    // =========================================================================
    // SENSOR EVENT LISTENER IMPLEMENTATION
    // =========================================================================

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event == null || event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
            return;
        }

        // Only count steps if tracking is currently active
        if (!isTracking) {
            return;
        }

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        // 1. Calculate 3D acceleration magnitude
        float magnitude = (float) Math.sqrt(x * x + y * y + z * z);

        // 2. Update smoothed gravity baseline using low-pass filter
        smoothedGravity = (GRAVITY_ALPHA * smoothedGravity) + ((1.0f - GRAVITY_ALPHA) * magnitude);

        // 3. Compute dynamic acceleration deviation
        float dynamicAcceleration = magnitude - smoothedGravity;

        // 4. Threshold crossing & debouncing (minimum interval)
        long currentTimestampMs = System.currentTimeMillis();

        if (dynamicAcceleration > STEP_THRESHOLD) {
            if (!isAboveThreshold) {
                isAboveThreshold = true;

                // Enforce minimum time interval between steps (300ms)
                if ((currentTimestampMs - lastStepTimestampMs) >= MIN_STEP_INTERVAL_MS) {
                    lastStepTimestampMs = currentTimestampMs;
                    todaySteps++;

                    // Update UI step count
                    tvStepCount.setText(String.format(Locale.getDefault(), "%,d", todaySteps));
                    int percentage = (dailyStepGoal > 0) ? (todaySteps * 100 / dailyStepGoal) : 0;
                    tvGoalProgress.setText(String.format(Locale.getDefault(), "Goal: %,d steps (%d%%)", dailyStepGoal, percentage));

                    // Check if daily goal achieved
                    checkGoalAchievement();
                }
            }
        } else {
            isAboveThreshold = false;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Accelerometer accuracy updates can be logged if needed
        Log.d(TAG, "MainActivity: Sensor accuracy changed to: " + accuracy);
    }

    // =========================================================================
    // DATABASE HELPERS FOR DAILY RECORDS
    // =========================================================================

    /**
     * Loads saved daily activity record for today's date from SQLite.
     */
    private void loadTodayRecordFromDatabase() {
        DailyRecord record = databaseHelper.getDailyRecord(currentDateStr);
        if (record != null) {
            todaySteps = record.getSteps();
            todayDurationSeconds = record.getDurationSeconds();
            todayGoalNotified = record.isGoalNotified();
            Log.d(TAG, "MainActivity: Loaded existing record for " + currentDateStr + ": Steps=" + todaySteps + ", Duration=" + todayDurationSeconds + ", Notified=" + todayGoalNotified);
        } else {
            todaySteps = 0;
            todayDurationSeconds = 0;
            todayGoalNotified = false;
            Log.d(TAG, "MainActivity: No existing record found for " + currentDateStr + ". Starting with 0 totals.");
        }
    }

    /**
     * Saves or updates today's record into SQLite.
     */
    private void saveTodayRecordToDatabase() {
        boolean success = databaseHelper.saveOrUpdateDailyRecord(
                currentDateStr,
                todaySteps,
                todayDurationSeconds,
                todayGoalNotified
        );
        if (success) {
            Log.d(TAG, "MainActivity: Saved daily record to SQLite for " + currentDateStr + " (Steps=" + todaySteps + ", Duration=" + todayDurationSeconds + ")");
        } else {
            Log.e(TAG, "MainActivity: Failed to save daily record for " + currentDateStr);
        }
    }

    /**
     * Checks if current calendar date has changed (e.g. crossing midnight).
     * If changed, saves yesterday's record and resets today's tracking data.
     */
    private void checkDateRollover() {
        String newDate = getTodayDateString();
        if (!newDate.equals(currentDateStr)) {
            Log.d(TAG, "MainActivity: Date change detected! Saving old date (" + currentDateStr + ") and resetting for " + newDate);
            saveTodayRecordToDatabase();

            currentDateStr = newDate;
            todaySteps = 0;
            todayDurationSeconds = 0;
            todayGoalNotified = false;

            tvCurrentDate.setText(String.format(Locale.getDefault(), "Date: %s", currentDateStr));
            updateUiState();
        }
    }

    /**
     * Formats seconds into HH:mm:ss.
     */
    private String formatDuration(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
    }

    /**
     * Returns today's date formatted as YYYY-MM-DD.
     */
    private String getTodayDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    // =========================================================================
    // ACTIVITY LIFECYCLE MANAGEMENT & LOGCAT EXPLANATION
    // =========================================================================

    /**
     * onStart: Activity becomes visible to user.
     */
    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "MainActivity: onStart() - Activity is becoming visible.");
    }

    /**
     * onResume: Activity is in foreground and interactive (Active state).
     * - We register the sensor listener only if tracking is enabled.
     * - We resume the duration timer if tracking is active.
     * - We restore/verify latest saved goal and totals.
     */
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "MainActivity: onResume() - Activity is active in the foreground.");

        // Check if date rolled over while in background
        checkDateRollover();

        // Refresh goal from SharedPreferences (in case changed elsewhere)
        dailyStepGoal = goalPreferences.getInt(KEY_DAILY_STEP_GOAL, DEFAULT_STEP_GOAL);
        updateUiState();

        if (isTracking) {
            registerAccelerometer();
            startDurationTimer();
        }
    }

    /**
     * onPause: Activity is losing foreground focus (Paused state).
     * Foreground-only mini-project rule: Tracking pauses when leaving the main screen!
     * - Unregister accelerometer to avoid battery drain.
     * - Stop UI timer callbacks.
     * - Save current progress into SQLite immediately before leaving the foreground.
     */
    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "MainActivity: onPause() - Activity losing foreground. Unregistering sensor, stopping timer, and saving progress.");

        // Unregister sensor to prevent battery drain and duplicate callbacks
        unregisterAccelerometer();

        // Stop timer
        stopDurationTimer();

        // Persist today's progress into SQLite
        saveTodayRecordToDatabase();
    }

    /**
     * onStop: Activity is no longer visible (Stopped state).
     */
    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "MainActivity: onStop() - Activity is hidden/stopped.");
    }

    /**
     * onDestroy: Activity is being destroyed (Destroyed state).
     * Note: We never rely on onDestroy() for saving data because it is not guaranteed
     * to be called by the Android OS under memory pressure.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "MainActivity: onDestroy() - Activity is being destroyed.");
    }

    /**
     * Preserves current values across configuration changes such as screen rotation.
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_CURRENT_STEPS, todaySteps);
        outState.putInt(STATE_DURATION_SECONDS, todayDurationSeconds);
        outState.putBoolean(STATE_IS_TRACKING, isTracking);
        outState.putString(STATE_TODAY_DATE, currentDateStr);
        Log.d(TAG, "MainActivity: onSaveInstanceState() - Saved state to Bundle.");
    }

    /**
     * Restores values preserved across configuration changes.
     */
    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        todaySteps = savedInstanceState.getInt(STATE_CURRENT_STEPS, todaySteps);
        todayDurationSeconds = savedInstanceState.getInt(STATE_DURATION_SECONDS, todayDurationSeconds);
        isTracking = savedInstanceState.getBoolean(STATE_IS_TRACKING, false);
        currentDateStr = savedInstanceState.getString(STATE_TODAY_DATE, currentDateStr);
        updateUiState();
        Log.d(TAG, "MainActivity: onRestoreInstanceState() - Restored state from Bundle.");
    }
}