package com.example.smarthealthfitness;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * SQLiteOpenHelper implementation for managing the local SQLite database.
 * Stores daily activity records including date, step count, active duration,
 * and goal notification status.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "FitnessDatabaseHelper";

    // Database information
    private static final String DATABASE_NAME = "fitness_monitor.db";
    private static final int DATABASE_VERSION = 1;

    // Table and column names
    public static final String TABLE_ACTIVITY = "daily_activity";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_DATE = "activity_date";
    public static final String COLUMN_STEPS = "step_count";
    public static final String COLUMN_DURATION = "duration_seconds";
    public static final String COLUMN_GOAL_NOTIFIED = "goal_notified";

    // SQL statement to create table
    private static final String SQL_CREATE_TABLE =
            "CREATE TABLE " + TABLE_ACTIVITY + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_DATE + " TEXT UNIQUE NOT NULL, " +
                    COLUMN_STEPS + " INTEGER NOT NULL DEFAULT 0, " +
                    COLUMN_DURATION + " INTEGER NOT NULL DEFAULT 0, " +
                    COLUMN_GOAL_NOTIFIED + " INTEGER NOT NULL DEFAULT 0" +
                    ");";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE);
        Log.d(TAG, "Database table created: " + TABLE_ACTIVITY);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Simple upgrade policy: drop old table and recreate for schema changes
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ACTIVITY);
        onCreate(db);
    }

    /**
     * Inserts a new record or updates the existing record for a specific date.
     * Ensures only one record exists per date.
     *
     * @param date            Date in YYYY-MM-DD format
     * @param steps           Today's estimated step count
     * @param durationSeconds Active tracking duration in seconds
     * @param goalNotified    Whether daily goal notification has already been triggered
     * @return true if save/update was successful
     */
    public boolean saveOrUpdateDailyRecord(String date, int steps, int durationSeconds, boolean goalNotified) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_DATE, date);
        values.put(COLUMN_STEPS, steps);
        values.put(COLUMN_DURATION, durationSeconds);
        values.put(COLUMN_GOAL_NOTIFIED, goalNotified ? 1 : 0);

        // Check if an entry for this date already exists
        Cursor cursor = null;
        boolean exists = false;
        boolean previouslyNotified = false;

        try {
            cursor = db.query(
                    TABLE_ACTIVITY,
                    new String[]{COLUMN_ID, COLUMN_GOAL_NOTIFIED},
                    COLUMN_DATE + " = ?",
                    new String[]{date},
                    null, null, null
            );

            if (cursor != null && cursor.moveToFirst()) {
                exists = true;
                previouslyNotified = (cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_GOAL_NOTIFIED)) == 1);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking existing record for date " + date, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        // If it was already notified, keep it marked as notified
        if (previouslyNotified) {
            values.put(COLUMN_GOAL_NOTIFIED, 1);
        }

        long result;
        if (exists) {
            result = db.update(TABLE_ACTIVITY, values, COLUMN_DATE + " = ?", new String[]{date});
        } else {
            result = db.insert(TABLE_ACTIVITY, null, values);
        }

        return result != -1;
    }

    /**
     * Retrieves the DailyRecord for a given date.
     *
     * @param date Date in YYYY-MM-DD format
     * @return DailyRecord if found, or null if no record exists yet
     */
    public DailyRecord getDailyRecord(String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        DailyRecord record = null;

        try {
            cursor = db.query(
                    TABLE_ACTIVITY,
                    new String[]{COLUMN_ID, COLUMN_DATE, COLUMN_STEPS, COLUMN_DURATION, COLUMN_GOAL_NOTIFIED},
                    COLUMN_DATE + " = ?",
                    new String[]{date},
                    null, null, null
            );

            if (cursor != null && cursor.moveToFirst()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String recordDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE));
                int steps = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_STEPS));
                int duration = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DURATION));
                boolean notified = (cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_GOAL_NOTIFIED)) == 1);

                record = new DailyRecord(id, recordDate, steps, duration, notified);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching record for date " + date, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return record;
    }

    /**
     * Updates the goal notified flag for a specific date.
     *
     * @param date     Date in YYYY-MM-DD format
     * @param notified Flag state (true = notified)
     */
    public void setGoalNotified(String date, boolean notified) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_GOAL_NOTIFIED, notified ? 1 : 0);

        try {
            db.update(TABLE_ACTIVITY, values, COLUMN_DATE + " = ?", new String[]{date});
        } catch (Exception e) {
            Log.e(TAG, "Error updating goal notification state for date " + date, e);
        }
    }

    /**
     * Retrieves all activity records, ordered newest first (by date descending).
     *
     * @return List of DailyRecord objects
     */
    public List<DailyRecord> getAllRecordsNewestFirst() {
        List<DailyRecord> records = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.query(
                    TABLE_ACTIVITY,
                    new String[]{COLUMN_ID, COLUMN_DATE, COLUMN_STEPS, COLUMN_DURATION, COLUMN_GOAL_NOTIFIED},
                    null, null, null, null,
                    COLUMN_DATE + " DESC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
                    String date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE));
                    int steps = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_STEPS));
                    int duration = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DURATION));
                    boolean notified = (cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_GOAL_NOTIFIED)) == 1);

                    records.add(new DailyRecord(id, date, steps, duration, notified));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching all records", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return records;
    }
}
