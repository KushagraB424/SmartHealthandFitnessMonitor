package com.example.smarthealthfitness;

import java.util.Locale;

/**
 * Model class representing a single day's health and fitness activity record.
 * Stored and retrieved using SQLiteOpenHelper.
 */
public class DailyRecord {

    private long id;
    private String date; // Format: YYYY-MM-DD
    private int steps;
    private int durationSeconds;
    private boolean goalNotified;

    public DailyRecord() {
    }

    public DailyRecord(long id, String date, int steps, int durationSeconds, boolean goalNotified) {
        this.id = id;
        this.date = date;
        this.steps = steps;
        this.durationSeconds = durationSeconds;
        this.goalNotified = goalNotified;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getSteps() {
        return steps;
    }

    public void setSteps(int steps) {
        this.steps = steps;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public boolean isGoalNotified() {
        return goalNotified;
    }

    public void setGoalNotified(boolean goalNotified) {
        this.goalNotified = goalNotified;
    }

    /**
     * Helper to format duration in seconds into a friendly HH:mm:ss string.
     */
    public String getFormattedDuration() {
        int hours = durationSeconds / 3600;
        int minutes = (durationSeconds % 3600) / 60;
        int seconds = durationSeconds % 60;

        if (hours > 0) {
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        }
    }
}
