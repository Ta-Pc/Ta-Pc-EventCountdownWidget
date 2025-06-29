package com.example.eventcountdownwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.text.TextUtils; // Import TextUtils
import android.util.Log;
import android.util.TypedValue;
import android.widget.RemoteViews;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.core.content.ContextCompat;

import com.example.eventcountdownwidget.utils.ColorUtil;

import java.util.ArrayList; // Import ArrayList
import java.util.Calendar;
import java.util.List; // Import List
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of App Widget functionality.
 * Handles widget updates, deletion, and dynamic theming.
 */
public class EventCountdownWidget extends AppWidgetProvider {
    // Constants for SharedPreferences
    private static final String PREFS_NAME = "com.example.eventcountdownwidget.WidgetPrefs";
    private static final String PREF_PREFIX_KEY = "widget_";
    private static final String EVENT_ID_KEY = "_event_id";
    private static final String EVENT_TITLE_KEY = "_event_title";
    private static final String EVENT_TIME_KEY = "_event_time"; // Start time
    private static final String EVENT_END_TIME_KEY = "_event_end_time"; // End time
    // private static final String FONT_SIZE_KEY = "_font_size"; // REMOVED
    private static final String THEME_STYLE_KEY = "_theme_style";
    private static final String WIDGET_COLOR_KEY = "_widget_color";

    // Logging Tag
    private static final String TAG = "EventCountdownWidget";

    // Time constants for formatting
    private static final long MINUTE_MILLIS = TimeUnit.MINUTES.toMillis(1);
    private static final long HOUR_MILLIS = TimeUnit.HOURS.toMillis(1);
    private static final long DAY_MILLIS = TimeUnit.DAYS.toMillis(1);
    private static final long MONTH_APPROX_MILLIS = TimeUnit.DAYS.toMillis(30); // Approximation
    private static final long YEAR_APPROX_MILLIS = TimeUnit.DAYS.toMillis(365); // Approximation

    // Keep onUpdate, onEnabled, onDeleted, onDisabled as they are
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            Log.d(TAG, "onUpdate called for widget ID: " + appWidgetId + ". Triggering update and scheduling.");
            updateAppWidget(context, appWidgetManager, appWidgetId);
            WidgetUpdateReceiver.scheduleNextUpdate(context, appWidgetId, EventCountdownWidget.class.getName());
        }
    }
    @Override
    public void onEnabled(Context context) { /* ... */ }
    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        for (int appWidgetId : appWidgetIds) {
            Log.d(TAG, "Deleting preferences and canceling updates for widget ID: " + appWidgetId);
            prefs.remove(PREF_PREFIX_KEY + appWidgetId + EVENT_ID_KEY);
            prefs.remove(PREF_PREFIX_KEY + appWidgetId + EVENT_TITLE_KEY);
            prefs.remove(PREF_PREFIX_KEY + appWidgetId + EVENT_TIME_KEY);
            prefs.remove(PREF_PREFIX_KEY + appWidgetId + EVENT_END_TIME_KEY); // Remove end time
            // prefs.remove(PREF_PREFIX_KEY + appWidgetId + FONT_SIZE_KEY); // REMOVED
            prefs.remove(PREF_PREFIX_KEY + appWidgetId + THEME_STYLE_KEY);
            prefs.remove(PREF_PREFIX_KEY + appWidgetId + WIDGET_COLOR_KEY);
            WidgetUpdateReceiver.cancelUpdate(context, appWidgetId, EventCountdownWidget.class.getName());
        }
        prefs.apply();
    }
    @Override
    public void onDisabled(Context context) { /* ... */ }

    // Keep getThemeColor and generateRandomMaterialColor as they are
    @ColorInt private static int getThemeColor(@AttrRes int colorAttribute, Context context, @ColorInt int defaultColor) { /* ... */ return defaultColor;}
    private static int generateRandomMaterialColor() { /* ... */ return Color.BLUE; } // Placeholder


    /**
     * Updates a single widget instance.
     */
    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        Log.d(TAG, "Updating widget content for ID: " + appWidgetId);
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        String eventTitle = prefs.getString(PREF_PREFIX_KEY + appWidgetId + EVENT_TITLE_KEY, "");
        long eventStartTime = prefs.getLong(PREF_PREFIX_KEY + appWidgetId + EVENT_TIME_KEY, -1);
        long eventEndTime = prefs.getLong(PREF_PREFIX_KEY + appWidgetId + EVENT_END_TIME_KEY, -1); // Load end time
        int themeStyleOption = prefs.getInt(PREF_PREFIX_KEY + appWidgetId + THEME_STYLE_KEY, 0);
        int widgetUniqueColor = prefs.getInt(PREF_PREFIX_KEY + appWidgetId + WIDGET_COLOR_KEY, -1);

        // Generate color if needed
        if (widgetUniqueColor == -1) { /* ... generate and save color ... */ }

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.event_countdown_widget);

        // --- Determine Theme Colors ---
        int backgroundColor;
        int textColor;
        int subtitleTextColor;
        // ... (Keep the color determination logic as before) ...
        if (themeStyleOption == 1) { // Light
            backgroundColor = ContextCompat.getColor(context, R.color.widget_background_light);
            textColor = ContextCompat.getColor(context, R.color.widget_text_on_light);
            subtitleTextColor = ContextCompat.getColor(context, R.color.lightTextSecondary);
        } else if (themeStyleOption == 2) { // Dark
            backgroundColor = ContextCompat.getColor(context, R.color.widget_background_dark);
            textColor = ContextCompat.getColor(context, R.color.widget_text_on_dark);
            subtitleTextColor = ContextCompat.getColor(context, R.color.darkTextSecondary);
        } else { // Dynamic or Custom
            int baseColor = (widgetUniqueColor != -1) ? widgetUniqueColor : Color.parseColor("#303030");
            backgroundColor = baseColor;
            boolean isDark = ColorUtil.isDarkColor(backgroundColor);
            textColor = isDark ? Color.WHITE : Color.BLACK;
            subtitleTextColor = isDark ? Color.argb(230, 255, 255, 255) : Color.argb(230, 0, 0, 0);
            if (themeStyleOption == 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { /* ... try dynamic ... */ }
        }


        // --- Apply Colors and Background ---
        int alphaBackgroundColor = Color.argb(230, Color.red(backgroundColor), Color.green(backgroundColor), Color.blue(backgroundColor));
        views.setInt(R.id.widget_layout, "setBackgroundResource", R.drawable.widget_background);
        views.setInt(R.id.widget_layout, "setBackgroundColor", alphaBackgroundColor);
        views.setTextColor(R.id.appwidget_event_title, textColor);
        views.setTextColor(R.id.appwidget_subtitle_data, subtitleTextColor);

        // --- Apply Font Sizes (Using fixed dimensions) ---
        float titleSizeSp = context.getResources().getDimension(R.dimen.widget_title_text_size) / context.getResources().getDisplayMetrics().scaledDensity;
        float subtitleSizeSp = context.getResources().getDimension(R.dimen.widget_subtitle_text_size) / context.getResources().getDisplayMetrics().scaledDensity;
        views.setTextViewTextSize(R.id.appwidget_event_title, TypedValue.COMPLEX_UNIT_SP, titleSizeSp);
        views.setTextViewTextSize(R.id.appwidget_subtitle_data, TypedValue.COMPLEX_UNIT_SP, subtitleSizeSp);


        // --- Event Countdown Logic (Refactored) ---
        String subtitleText; // Renamed from countdownText
        if (eventStartTime != -1 && eventTitle != null && !eventTitle.isEmpty()) {
            views.setTextViewText(R.id.appwidget_event_title, eventTitle);

            long nowMillis = System.currentTimeMillis();
            long diffMillis = eventStartTime - nowMillis; // Difference until start time

            if (nowMillis >= eventStartTime && (eventEndTime == -1 || nowMillis < eventEndTime)) {
                // Event is happening now (started but not yet ended, or no end time specified)
                subtitleText = context.getString(R.string.countdown_now);
            } else if (diffMillis >= 0) {
                // Event is in the future
                subtitleText = formatTimeUntil(context, diffMillis);
            } else {
                // Event start time is in the past, and we are not "now"
                subtitleText = formatTimeSince(context, -diffMillis); // Pass positive difference
            }

        } else {
            // Display placeholder text
            views.setTextViewText(R.id.appwidget_event_title, context.getString(R.string.widget_default_title));
            subtitleText = context.getString(R.string.widget_setup_prompt);
        }
        views.setTextViewText(R.id.appwidget_subtitle_data, subtitleText);


        // --- Click Intent for Configuration ---
        Intent configIntent = new Intent(context, WidgetConfigActivity.class);
        configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent configPendingIntent = PendingIntent.getActivity(context, appWidgetId, configIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_layout, configPendingIntent);

        // Update the widget
        Log.d(TAG, "Applying update view to widget ID: " + appWidgetId);
        try {
            appWidgetManager.updateAppWidget(appWidgetId, views);
        } catch (Exception e) {
            Log.e(TAG, "Error updating widget " + appWidgetId, e);
        }
    }


    // --- NEW HELPER METHODS for Time Formatting ---

    /**
     * Formats the time remaining until a future event.
     * Shows the largest two relevant units (e.g., "In 2 days 5 hrs", "In 3 hrs 10 mins").
     *
     * @param context Context to access resources.
     * @param diffMillis The positive difference in milliseconds until the event.
     * @return Formatted string like "In X unit Y unit".
     */
    private static String formatTimeUntil(Context context, long diffMillis) {
        if (diffMillis < MINUTE_MILLIS) {
            return context.getString(R.string.countdown_starts_soon);
        }

        long years = diffMillis / YEAR_APPROX_MILLIS;
        long months = (diffMillis % YEAR_APPROX_MILLIS) / MONTH_APPROX_MILLIS;
        long days = (diffMillis % MONTH_APPROX_MILLIS) / DAY_MILLIS;
        long hours = (diffMillis % DAY_MILLIS) / HOUR_MILLIS;
        long minutes = (diffMillis % HOUR_MILLIS) / MINUTE_MILLIS;

        List<String> parts = new ArrayList<>();
        Resources res = context.getResources();

        if (years > 0) {
            parts.add(res.getQuantityString(R.plurals.years, (int) years, (int) years));
        }
        if (months > 0) {
            parts.add(res.getQuantityString(R.plurals.months, (int) months, (int) months));
        }
        if (days > 0 && years == 0) { // Show days only if no years shown
            parts.add(res.getQuantityString(R.plurals.days, (int) days, (int) days));
        }
        if (hours > 0 && years == 0 && months == 0) { // Show hours only if no years/months shown
            parts.add(res.getQuantityString(R.plurals.hours, (int) hours, (int) hours));
        }
        if (minutes > 0 && years == 0 && months == 0 && days == 0) { // Show minutes only if smallest unit
            parts.add(res.getQuantityString(R.plurals.minutes, (int) minutes, (int) minutes));
        }

        // Build the final string (e.g., "In 5 days 6 hrs") - Show max 2 largest units
        String timeString;
        if (parts.size() >= 2) {
            timeString = parts.get(0) + " " + parts.get(1);
        } else if (parts.size() == 1) {
            timeString = parts.get(0);
        } else {
            // Should have been caught by "starts soon", but as fallback:
            timeString = res.getQuantityString(R.plurals.minutes, 1, 1); // "1 min"
        }

        // Decide prefix: "In" vs "X left". Let's use "In" for simplicity.
        // Or use "left" if less than a day?
        String prefix = (diffMillis < DAY_MILLIS && years==0 && months==0 && days==0) ? "" : context.getString(R.string.label_in) + " ";
        String suffix = (diffMillis < DAY_MILLIS && years==0 && months==0 && days==0) ? " " + context.getString(R.string.label_left) : "";


        // Special case for Today: Use hour/minute format directly if less than 24 hours
        if (diffMillis < DAY_MILLIS && years == 0 && months == 0 && days == 0) {
            if (hours > 0) {
                String hourPart = res.getQuantityString(R.plurals.hours, (int) hours, (int) hours);
                String minutePart = (minutes > 0) ? " " + res.getQuantityString(R.plurals.minutes, (int) minutes, (int) minutes) : "";
                return context.getString(R.string.label_in) + " " + hourPart + minutePart;
            } else { // Only minutes left
                return context.getString(R.string.label_in) + " " + res.getQuantityString(R.plurals.minutes, (int) minutes, (int) minutes);
            }
        } else { // More than a day away
            return context.getString(R.string.label_in) + " " + timeString;
        }
        // return prefix + timeString + suffix; // Alternative prefix/suffix logic
    }


    /**
     * Formats the time elapsed since a past event.
     * Shows the largest two relevant units (e.g., "3 yrs 2 mos ago", "5 days 6 hrs ago").
     *
     * @param context Context to access resources.
     * @param diffMillis The positive difference in milliseconds since the event started.
     * @return Formatted string like "X unit Y unit ago".
     */
    private static String formatTimeSince(Context context, long diffMillis) {
        if (diffMillis < MINUTE_MILLIS) {
            // Event just passed, or happened within the last minute
            return context.getString(R.string.countdown_passed); // Or "Just now"
        }

        long years = diffMillis / YEAR_APPROX_MILLIS;
        long months = (diffMillis % YEAR_APPROX_MILLIS) / MONTH_APPROX_MILLIS;
        long days = (diffMillis % MONTH_APPROX_MILLIS) / DAY_MILLIS;
        long hours = (diffMillis % DAY_MILLIS) / HOUR_MILLIS;
        long minutes = (diffMillis % HOUR_MILLIS) / MINUTE_MILLIS;

        List<String> parts = new ArrayList<>();
        Resources res = context.getResources();

        if (years > 0) {
            parts.add(res.getQuantityString(R.plurals.years, (int) years, (int) years));
        }
        if (months > 0) {
            parts.add(res.getQuantityString(R.plurals.months, (int) months, (int) months));
        }
        if (days > 0 && years == 0) { // Show days only if no years shown
            parts.add(res.getQuantityString(R.plurals.days, (int) days, (int) days));
        }
        if (hours > 0 && years == 0 && months == 0) { // Show hours only if no years/months shown
            parts.add(res.getQuantityString(R.plurals.hours, (int) hours, (int) hours));
        }
        if (minutes > 0 && years == 0 && months == 0 && days == 0) { // Show minutes only if smallest unit
            parts.add(res.getQuantityString(R.plurals.minutes, (int) minutes, (int) minutes));
        }

        // Build the final string (Show max 2 largest units)
        String timeString;
        if (parts.size() >= 2) {
            timeString = parts.get(0) + " " + parts.get(1);
        } else if (parts.size() == 1) {
            timeString = parts.get(0);
        } else {
            // Fallback if calculation is weird (shouldn't happen if diffMillis >= MINUTE_MILLIS)
            timeString = context.getString(R.string.countdown_passed);
            return timeString; // Return early without "ago"
        }

        return timeString + " " + context.getString(R.string.label_ago);
    }

} // End of EventCountdownWidget class