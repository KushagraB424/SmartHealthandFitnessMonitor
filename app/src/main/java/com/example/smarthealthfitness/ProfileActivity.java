package com.example.smarthealthfitness;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * ProfileActivity manages the user's health profile (Name, Age, Gender, Height, Weight).
 * Data is validated and persisted locally using SharedPreferences.
 * Explains and logs Android Activity lifecycle events.
 */
public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "FitnessLifecycle";

    // SharedPreferences configuration
    public static final String PREF_PROFILE_NAME = "fitness_user_profile";
    public static final String KEY_USER_NAME = "key_user_name";
    public static final String KEY_USER_AGE = "key_user_age";
    public static final String KEY_USER_GENDER = "key_user_gender";
    public static final String KEY_USER_HEIGHT = "key_user_height";
    public static final String KEY_USER_WEIGHT = "key_user_weight";

    // UI elements
    private EditText etName;
    private EditText etAge;
    private Spinner spGender;
    private EditText etHeight;
    private EditText etWeight;
    private Button btnSave;
    private Button btnBack;

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "ProfileActivity: onCreate() - Activity is being created.");
        setContentView(R.layout.activity_profile);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREF_PROFILE_NAME, Context.MODE_PRIVATE);

        // Initialize UI components
        initViews();

        // Load existing profile if available
        loadSavedProfile();

        // Set up save button listener
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveProfile();
            }
        });

        // Set up back button listener
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Return to previous screen
            }
        });
    }

    private void initViews() {
        etName = findViewById(R.id.etProfileName);
        etAge = findViewById(R.id.etProfileAge);
        spGender = findViewById(R.id.spProfileGender);
        etHeight = findViewById(R.id.etProfileHeight);
        etWeight = findViewById(R.id.etProfileWeight);
        btnSave = findViewById(R.id.btnSaveProfile);
        btnBack = findViewById(R.id.btnBackFromProfile);
    }

    /**
     * Loads saved user details from SharedPreferences and populates the form fields.
     */
    private void loadSavedProfile() {
        String name = sharedPreferences.getString(KEY_USER_NAME, "");
        int age = sharedPreferences.getInt(KEY_USER_AGE, -1);
        String gender = sharedPreferences.getString(KEY_USER_GENDER, "");
        float height = sharedPreferences.getFloat(KEY_USER_HEIGHT, -1f);
        float weight = sharedPreferences.getFloat(KEY_USER_WEIGHT, -1f);

        if (!name.isEmpty()) {
            etName.setText(name);
        }
        if (age > 0) {
            etAge.setText(String.valueOf(age));
        }
        if (height > 0) {
            etHeight.setText(String.valueOf(height));
        }
        if (weight > 0) {
            etWeight.setText(String.valueOf(weight));
        }

        // Set spinner selection based on stored gender string
        if (!gender.isEmpty()) {
            String[] genderOptions = getResources().getStringArray(R.array.gender_options);
            for (int i = 0; i < genderOptions.length; i++) {
                if (genderOptions[i].equalsIgnoreCase(gender)) {
                    spGender.setSelection(i);
                    break;
                }
            }
        }
    }

    /**
     * Validates input values and saves them into SharedPreferences.
     */
    private void saveProfile() {
        String nameStr = etName.getText().toString().trim();
        String ageStr = etAge.getText().toString().trim();
        String heightStr = etHeight.getText().toString().trim();
        String weightStr = etWeight.getText().toString().trim();
        int genderPosition = spGender.getSelectedItemPosition();

        // 1. Validate Name
        if (nameStr.isEmpty()) {
            etName.setError(getString(R.string.error_empty_name));
            etName.requestFocus();
            return;
        }
        if (nameStr.length() < 2) {
            etName.setError(getString(R.string.error_invalid_name));
            etName.requestFocus();
            return;
        }

        // 2. Validate Age
        if (ageStr.isEmpty()) {
            etAge.setError(getString(R.string.error_empty_age));
            etAge.requestFocus();
            return;
        }
        int age;
        try {
            age = Integer.parseInt(ageStr);
            if (age < 1 || age > 120) {
                etAge.setError(getString(R.string.error_invalid_age));
                etAge.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            etAge.setError(getString(R.string.error_invalid_age));
            etAge.requestFocus();
            return;
        }

        // 3. Validate Gender
        if (genderPosition <= 0) {
            Toast.makeText(this, getString(R.string.error_empty_gender), Toast.LENGTH_SHORT).show();
            spGender.requestFocus();
            return;
        }
        String genderStr = spGender.getSelectedItem().toString();

        // 4. Validate Height (allows decimal)
        if (heightStr.isEmpty()) {
            etHeight.setError(getString(R.string.error_empty_height));
            etHeight.requestFocus();
            return;
        }
        float height;
        try {
            height = Float.parseFloat(heightStr);
            if (height < 50.0f || height > 250.0f) {
                etHeight.setError(getString(R.string.error_invalid_height));
                etHeight.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            etHeight.setError(getString(R.string.error_invalid_height));
            etHeight.requestFocus();
            return;
        }

        // 5. Validate Weight (allows decimal)
        if (weightStr.isEmpty()) {
            etWeight.setError(getString(R.string.error_empty_weight));
            etWeight.requestFocus();
            return;
        }
        float weight;
        try {
            weight = Float.parseFloat(weightStr);
            if (weight < 20.0f || weight > 300.0f) {
                etWeight.setError(getString(R.string.error_invalid_weight));
                etWeight.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            etWeight.setError(getString(R.string.error_invalid_weight));
            etWeight.requestFocus();
            return;
        }

        // Save validated values to SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_USER_NAME, nameStr);
        editor.putInt(KEY_USER_AGE, age);
        editor.putString(KEY_USER_GENDER, genderStr);
        editor.putFloat(KEY_USER_HEIGHT, height);
        editor.putFloat(KEY_USER_WEIGHT, weight);
        editor.apply();

        Toast.makeText(this, getString(R.string.profile_saved_success), Toast.LENGTH_SHORT).show();
        Log.d(TAG, "ProfileActivity: User profile saved successfully: Name=" + nameStr + ", Age=" + age);
    }

    // ==========================================
    // Android Activity Lifecycle Callbacks & Logs
    // ==========================================

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "ProfileActivity: onStart() - Activity is becoming visible to the user.");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "ProfileActivity: onResume() - Activity is in the foreground and active/interactive.");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "ProfileActivity: onPause() - Activity is paused, losing focus.");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "ProfileActivity: onStop() - Activity is no longer visible (stopped).");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "ProfileActivity: onDestroy() - Activity is being destroyed.");
    }
}
