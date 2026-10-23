package com.example.smarthealthfitness;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

/**
 * HistoryActivity displays past daily activity records stored in the SQLite database.
 * The records are loaded and sorted newest first.
 * Explains and logs Android Activity lifecycle events.
 */
public class HistoryActivity extends AppCompatActivity {

    private static final String TAG = "FitnessLifecycle";

    private ListView lvHistory;
    private LinearLayout emptyView;
    private Button btnBack;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "HistoryActivity: onCreate() - Activity created, setting up view and database helper.");
        setContentView(R.layout.activity_history);

        lvHistory = findViewById(R.id.lvActivityHistory);
        emptyView = findViewById(R.id.tvEmptyHistoryView);
        btnBack = findViewById(R.id.btnBackFromHistory);

        // Attach empty view so ListView automatically toggles visibility when empty
        lvHistory.setEmptyView(emptyView);

        databaseHelper = new DatabaseHelper(this);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Close activity and return to MainActivity
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "HistoryActivity: onResume() - Activity active in foreground, reloading records from SQLite.");
        loadRecordsFromDatabase();
    }

    /**
     * Queries the local SQLite database for daily activity records ordered newest first.
     */
    private void loadRecordsFromDatabase() {
        List<DailyRecord> recordList = databaseHelper.getAllRecordsNewestFirst();
        HistoryAdapter adapter = new HistoryAdapter(this, recordList);
        lvHistory.setAdapter(adapter);
        Log.d(TAG, "HistoryActivity: Loaded " + recordList.size() + " records from database.");
    }

    // ==========================================
    // Android Activity Lifecycle Callbacks & Logs
    // ==========================================

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "HistoryActivity: onStart() - Activity becoming visible.");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "HistoryActivity: onPause() - Activity losing foreground focus.");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "HistoryActivity: onStop() - Activity is stopped and hidden.");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "HistoryActivity: onDestroy() - Activity being destroyed.");
    }
}
