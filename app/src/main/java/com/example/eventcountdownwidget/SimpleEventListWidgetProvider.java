package com.example.eventcountdownwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.provider.CalendarContract;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import androidx.core.content.ContextCompat;

import com.example.eventcountdownwidget.utils.ColorUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.TimeZone;


/**
 * AppWidgetProvider for the Event List Widget.
 * Uses a ListView backed by a RemoteViewsService to display upcoming calendar events.
 * Supports automatic updates via AlarmManager/BroadcastReceiver and manual refresh.
 * Allows configuration of theme, color, and maximum events shown.
 */
public class SimpleEventListWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "SimpleEventListWidget";
    private static final TimeZone SAST_TIMEZONE = TimeZone.getTimeZone("Africa/Johannesburg"); // Consider making this globally accessible if used elsewhere

    /**
     * Called when widgets need updating (first placement, periodic update, config change, system events).
     * Sets up the widget frame and triggers data loading/updates for the ListView.
     * Also schedules the next automatic update.
     */
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            Log.d(TAG, "onUpdate called for widget ID: " + appWidgetId + ". Triggering update and scheduling.");
            // Set up the widget layout and adapter connection
            updateAppWidget(context, appWidgetManager, appWidgetId);
            // Notify the service that its data might be stale (important after config change or refresh)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.event_list_view);
            // Schedule the next timed update via the BroadcastReceiver
            WidgetUpdateReceiver.scheduleNextUpdate(context, appWidgetId, SimpleEventListWidgetProvider.class.getName());
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds); // Important to call super
    }

    /**
     * Called when the first widget of this type is placed.
     */
    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        Log.d(TAG, "onEnabled called. Initial update/scheduling handled by onUpdate.");
    }

    /**
     * Called when a widget instance is deleted by the user.
     * Cleans up preferences and cancels scheduled updates for the deleted widget.
     */
    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            Log.d(TAG, "onDeleted called for widget ID: " + appWidgetId + ". Deleting prefs and canceling updates.");
            WidgetConfigManager.deleteConfig(context, appWidgetId);
            WidgetUpdateReceiver.cancelUpdate(context, appWidgetId, SimpleEventListWidgetProvider.class.getName());
        }
        super.onDeleted(context, appWidgetIds); // Important to call super
    }

    /**
     * Called when the last widget of this type is removed.
     */
    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        Log.d(TAG, "onDisabled called. All widgets of this type removed.");
        // No global resources to clean up; individual widgets handled by onDeleted.
    }

    /**
     * Sets up the main structure of the widget and connects the ListView to the RemoteViewsService.
     * Applies overall theming to the widget frame. Populating the list is handled by the service.
     * This method is static to be callable from the update receiver.
     */
    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        try {
            Log.d(TAG, "updateAppWidget: Setting up frame and ListView for ID: " + appWidgetId);
            WidgetConfig config = WidgetConfigManager.loadConfig(context, appWidgetId);
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.simple_event_list_widget);

            // 1. Apply overall theme (background, header, empty view colors/tints)
            ThemeManager.applyTheme(context, views, config);

            // 2. Set up the Intent for the RemoteViewsService
            Intent serviceIntent = new Intent(context, EventListWidgetService.class);
            serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            serviceIntent.putExtra("maxEvents", config.getMaxEvents()); // Pass configured limit
            serviceIntent.setData(Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME))); // Ensure unique Intent

            // 3. Connect the ListView in the layout to the RemoteViewsAdapter
            views.setRemoteAdapter(R.id.event_list_view, serviceIntent);

            // 4. Specify the view to display when the ListView is empty
            views.setEmptyView(R.id.event_list_view, R.id.empty_view_container);

            // 5. Setup common interaction points (header click for config, refresh button)
            SetupHelper.setupHeaderClick(context, views, appWidgetId);
            SetupHelper.setupRefreshButton(context, views, appWidgetId);

            // --- List Item Click Handling Removed ---
            // No PendingIntent template needed if items are not clickable.

            // 6. Update the widget instance
            appWidgetManager.updateAppWidget(appWidgetId, views);
            Log.d(TAG, "updateAppWidget: Widget structure update applied for ID: " + appWidgetId);

        } catch (Exception e) {
            Log.e(TAG, "updateAppWidget: Error setting up list widget ID " + appWidgetId, e);
            displayErrorView(context, appWidgetManager, appWidgetId); // Display an error state
        }
    }

    /** Displays a standard error view on the widget when an update fails severely. */
    private static void displayErrorView(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        try {
            RemoteViews errorViews = new RemoteViews(context.getPackageName(), R.layout.simple_event_list_widget);
            // Apply basic styling matching the dark theme for errors
            errorViews.setInt(R.id.widget_layout, "setBackgroundResource", R.drawable.widget_background);
            errorViews.setInt(R.id.widget_layout, "setBackgroundColor", Color.argb(230, 50, 50, 50)); // Dark gray background
            errorViews.setTextColor(R.id.widget_title, Color.WHITE);
            errorViews.setTextColor(R.id.empty_view, Color.LTGRAY);
            try { errorViews.setInt(R.id.widget_refresh, "setColorFilter", Color.WHITE); } catch (Exception ignored) {}
            try { errorViews.setInt(R.id.widget_header_icon, "setColorFilter", Color.WHITE); } catch (Exception ignored) {} // Tint icons if they exist
            try { errorViews.setInt(R.id.empty_view_icon, "setColorFilter", Color.LTGRAY); } catch (Exception ignored) {}

            // Set error text
            errorViews.setTextViewText(R.id.widget_title, context.getString(R.string.app_name)); // Show app name or generic title
            errorViews.setViewVisibility(R.id.empty_view_container, View.VISIBLE); // Show empty container area
            errorViews.setViewVisibility(R.id.event_list_view, View.GONE); // Ensure ListView is hidden
            errorViews.setTextViewText(R.id.empty_view, "Error updating widget.\nTap header to config."); // Informative error message

            // Allow user to reconfigure or refresh
            SetupHelper.setupHeaderClick(context, errorViews, appWidgetId);
            SetupHelper.setupRefreshButton(context, errorViews, appWidgetId);

            appWidgetManager.updateAppWidget(appWidgetId, errorViews);
        } catch (Exception ex) {
            // If even applying the error view fails, log it.
            Log.e(TAG, "displayErrorView: Failed to apply error view to widget ID " + appWidgetId, ex);
        }
    }

    // ===================================================================================
    //region Inner Helper Classes (Domain, Config, Data, Formatting, Theme, Setup)
    // ===================================================================================

    /**
     * Domain model representing a calendar event item.
     */
    static class CalendarEventItem {
        private final long id; private final String title; private final long startTime; private final long endTime;
        private final boolean allDay; private final String eventTimezone; private final String description;

        public CalendarEventItem(long id, String title, long startTime, long endTime, boolean allDay, String eventTimezone, String description) {
            this.id = id; this.title = title; this.startTime = startTime; this.endTime = endTime; this.allDay = allDay; this.eventTimezone = eventTimezone; this.description = description;
        }
        public long getId() { return id; } public String getTitle() { return title; } public long getStartTime() { return startTime; }
        public long getEndTime() { return endTime; } public boolean isAllDay() { return allDay; }
        // Unused getters can be removed if desired:
        // public String getEventTimezone() { return eventTimezone; } public String getDescription() { return description; }
    }

    /**
     * Value object holding configuration settings for a specific widget instance.
     */
    static class WidgetConfig {
        private final int widgetId; private final int themeStyle; private final int widgetColor; private final int maxEvents;

        public WidgetConfig(int widgetId, int themeStyle, int widgetColor, int maxEvents) {
            this.widgetId = widgetId; this.themeStyle = themeStyle; this.widgetColor = widgetColor;
            // Ensure maxEvents is at least 1. The upper bound is handled by config activity and repository query limit.
            this.maxEvents = Math.max(1, maxEvents);
        }
        // public int getWidgetId() { return widgetId; } // Often unused internally
        public int getThemeStyle() { return themeStyle; } public int getWidgetColor() { return widgetColor; } public int getMaxEvents() { return maxEvents; }
    }

    /**
     * Manages loading and deleting widget configuration from SharedPreferences.
     */
    static class WidgetConfigManager {
        private static final String PREFS_NAME = "com.example.eventcountdownwidget.WidgetPrefs";
        private static final String PREF_PREFIX_KEY = "simple_list_widget_";
        private static final String THEME_STYLE_KEY = "_theme_style";
        private static final String WIDGET_COLOR_KEY = "_widget_color";
        private static final String MAX_EVENTS_KEY = "_max_events";
        private static final int DEFAULT_EVENTS_TO_SHOW = 10; // Default number of events

        public static WidgetConfig loadConfig(Context context, int appWidgetId) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
            int themeStyle = prefs.getInt(PREF_PREFIX_KEY + appWidgetId + THEME_STYLE_KEY, 0); // Default: Dynamic
            int widgetColor = prefs.getInt(PREF_PREFIX_KEY + appWidgetId + WIDGET_COLOR_KEY, -1); // Default: None (-1)
            int maxEvents = prefs.getInt(PREF_PREFIX_KEY + appWidgetId + MAX_EVENTS_KEY, DEFAULT_EVENTS_TO_SHOW);
            return new WidgetConfig(appWidgetId, themeStyle, widgetColor, maxEvents);
        }

        public static void deleteConfig(Context context, int appWidgetId) {
            SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, 0).edit();
            editor.remove(PREF_PREFIX_KEY + appWidgetId + THEME_STYLE_KEY);
            editor.remove(PREF_PREFIX_KEY + appWidgetId + WIDGET_COLOR_KEY);
            editor.remove(PREF_PREFIX_KEY + appWidgetId + MAX_EVENTS_KEY);
            editor.apply();
        }
        // Optional bulk delete if needed elsewhere
        // public static void deleteConfigs(Context context, int[] appWidgetIds) { for(int id : appWidgetIds) deleteConfig(context, id); }
    }

    /**
     * Handles retrieving upcoming calendar events from the ContentResolver.
     */
    static class CalendarRepository {
        private static final String TAG = "CalendarRepository";
        private static final String CALENDAR_PREFS = "calendar_preferences";
        private static final String SELECTED_CALENDARS = "selected_calendars";

        public static List<CalendarEventItem> loadEvents(Context context, int maxEvents) {
            List<CalendarEventItem> eventList = new ArrayList<>();
            Cursor cursor = null;
            try {
                SharedPreferences prefs = context.getSharedPreferences(CALENDAR_PREFS, Context.MODE_PRIVATE);
                Set<String> selectedCalendarIds = prefs.getStringSet(SELECTED_CALENDARS, null);
                if (selectedCalendarIds == null || selectedCalendarIds.isEmpty()) {
                    Log.w(TAG, "No calendars selected for event query.");
                    return eventList; // Return empty if no calendars are selected
                }

                long now = System.currentTimeMillis();
                long futureLimit = now + TimeUnit.DAYS.toMillis(30); // Query events within the next 30 days

                // Build selection string dynamically for selected calendar IDs
                StringBuilder selection = new StringBuilder(CalendarContract.Instances.CALENDAR_ID + " IN (");
                String[] placeholders = new String[selectedCalendarIds.size()];
                for(int i = 0; i < selectedCalendarIds.size(); i++) placeholders[i] = "?";
                selection.append(String.join(",", placeholders)).append(")");
                List<String> selectionArgs = new ArrayList<>(selectedCalendarIds);

                // Add time constraint: Events must end at or after 'now'
                selection.append(" AND ").append(CalendarContract.Instances.END).append(" >= ?");
                selectionArgs.add(String.valueOf(now));

                // Define the URI for querying instances within the time range
                Uri.Builder uriBuilder = CalendarContract.Instances.CONTENT_URI.buildUpon();
                ContentUris.appendId(uriBuilder, now - TimeUnit.DAYS.toMillis(1)); // Look slightly back for long events
                ContentUris.appendId(uriBuilder, futureLimit);
                Uri queryUri = uriBuilder.build();

                // Define the projection (columns to fetch)
                final String[] projection = {
                        CalendarContract.Instances.EVENT_ID,        // 0
                        CalendarContract.Instances.TITLE,           // 1
                        CalendarContract.Instances.BEGIN,           // 2
                        CalendarContract.Instances.END,             // 3
                        CalendarContract.Instances.ALL_DAY,         // 4
                        CalendarContract.Instances.EVENT_TIMEZONE,  // 5
                        CalendarContract.Instances.DESCRIPTION      // 6
                };
                // Indices matching projection order
                final int IDX_EVENT_ID = 0; final int IDX_TITLE = 1; final int IDX_BEGIN = 2;
                final int IDX_END = 3; final int IDX_ALL_DAY = 4; final int IDX_TZ = 5; final int IDX_DESC = 6;

                // Define sort order and limit
                String sortOrder = CalendarContract.Instances.BEGIN + " ASC LIMIT " + maxEvents;

                // Execute the query
                ContentResolver resolver = context.getContentResolver();
                cursor = resolver.query(queryUri, projection, selection.toString(), selectionArgs.toArray(new String[0]), sortOrder);

                // Process the results
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String title = cursor.getString(IDX_TITLE);
                        if (title == null || title.trim().isEmpty()) { title = "(No Title)"; } // Handle missing titles

                        eventList.add(new CalendarEventItem(
                                cursor.getLong(IDX_EVENT_ID), title, cursor.getLong(IDX_BEGIN), cursor.getLong(IDX_END),
                                cursor.getInt(IDX_ALL_DAY) != 0, cursor.getString(IDX_TZ), cursor.getString(IDX_DESC)
                        ));
                    }
                    Log.d(TAG, "Loaded " + eventList.size() + " events from CalendarProvider.");
                } else {
                    Log.w(TAG, "Calendar query returned a null cursor.");
                }
            } catch (SecurityException se) {
                Log.e(TAG, "Permission error reading calendar.", se); // Log permission issues clearly
                // Potentially notify the user via an error state in the widget? (More complex)
                eventList.clear();
            } catch (Exception e) {
                Log.e(TAG, "Error loading events from CalendarProvider.", e);
                eventList.clear(); // Clear list on any other error
            } finally {
                if (cursor != null) { cursor.close(); } // Ensure cursor is always closed
            }
            return eventList;
        }
    }

    /**
     * Formats relative time strings for event countdowns or elapsed time.
     */
    static class CountdownFormatter {
        private static final String TAG = "CountdownFormatter";
        // Reusable DateFormat instances (ensure thread safety via synchronized blocks or use thread-local instances)
        private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm", Locale.getDefault());
        private static final SimpleDateFormat DATE_FORMAT_SHORT = new SimpleDateFormat("MMM d", Locale.getDefault());
        private static final SimpleDateFormat DATE_TIME_FORMAT_SHORT = new SimpleDateFormat("MMM d, HH:mm", Locale.getDefault());
        private static final SimpleDateFormat DAY_TIME_FORMAT_SHORT = new SimpleDateFormat("EEE HH:mm", Locale.getDefault());

        static { // Apply TimeZone once
            TIME_FORMAT.setTimeZone(SAST_TIMEZONE);
            DATE_FORMAT_SHORT.setTimeZone(SAST_TIMEZONE);
            DATE_TIME_FORMAT_SHORT.setTimeZone(SAST_TIMEZONE);
            DAY_TIME_FORMAT_SHORT.setTimeZone(SAST_TIMEZONE);
        }

        public static String formatCountdown(CalendarEventItem event) {
            // Use getters is good practice if they were public
            // Directly accessing final fields is okay for private static inner classes
            return calculateCountdown(event.startTime, event.endTime, event.allDay);
        }

        private static String calculateCountdown(long startTime, long endTime, boolean isAllDay) {
            try {
                long nowMillis = System.currentTimeMillis();
                Date startDate = new Date(startTime);
                Date endDate = new Date(endTime);

                if (nowMillis >= startTime && nowMillis < endTime) { // Happening Now
                    synchronized (TIME_FORMAT) { // Ensure thread safety
                        if (isAllDay) {
                            Calendar startCal = Calendar.getInstance(SAST_TIMEZONE); startCal.setTimeInMillis(startTime);
                            Calendar todayCal = Calendar.getInstance(SAST_TIMEZONE);
                            return (startCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR) && startCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR))
                                    ? "Today (All day)" : "Now (All day)";
                        } else {
                            return "Now (until " + TIME_FORMAT.format(endDate) + ")";
                        }
                    }
                }

                if (nowMillis >= endTime) { // Passed
                    synchronized(DATE_FORMAT_SHORT) {
                        return "Ended (" + DATE_FORMAT_SHORT.format(startDate) + ")";
                    }
                }

                // Future
                long diffMillis = startTime - nowMillis;
                Calendar todayMid = Calendar.getInstance(SAST_TIMEZONE); todayMid.set(Calendar.HOUR_OF_DAY,0);todayMid.set(Calendar.MINUTE,0);todayMid.set(Calendar.SECOND,0);todayMid.set(Calendar.MILLISECOND,0);
                Calendar eventMid = Calendar.getInstance(SAST_TIMEZONE); eventMid.setTimeInMillis(startTime); eventMid.set(Calendar.HOUR_OF_DAY,0);eventMid.set(Calendar.MINUTE,0);eventMid.set(Calendar.SECOND,0);eventMid.set(Calendar.MILLISECOND,0);
                long diffDays = TimeUnit.MILLISECONDS.toDays(eventMid.getTimeInMillis() - todayMid.getTimeInMillis());

                synchronized (TIME_FORMAT) { // Synchronize remaining formats
                    if (diffDays == 0) { // Today
                        long hrs = TimeUnit.MILLISECONDS.toHours(diffMillis);
                        if(isAllDay) return "Today (All day)";
                        if (hrs >= 1) return hrs + "h left (" + TIME_FORMAT.format(startDate) + ")";
                        long mins = Math.max(0, TimeUnit.MILLISECONDS.toMinutes(diffMillis)); return mins + "m left (" + TIME_FORMAT.format(startDate) + ")";
                    } else if (diffDays == 1) { // Tomorrow
                        return isAllDay ? "Tomorrow (All day)" : "Tomorrow " + TIME_FORMAT.format(startDate);
                    } else { // Later
                        return isAllDay ? diffDays+"d (" + DATE_FORMAT_SHORT.format(startDate)+", All day)" : diffDays+"d (" + DAY_TIME_FORMAT_SHORT.format(startDate) + ")";
                    }
                }
            } catch (Exception e) { Log.e(TAG, "Error formatting countdown", e); return ""; }
        }
    } // End CountdownFormatter

    /**
     * Manages applying theme colors to the overall widget frame.
     * Uses simpler logic (no dynamic color checking).
     */
    static class ThemeManager {
        private static final String TAG = "ThemeManager";
        private static final int REFRESH_ICON_ID = R.id.widget_refresh;

        public static void applyTheme(Context context, RemoteViews views, WidgetConfig config) {
            ThemeColors colors = getThemeColors(context, config);

            views.setInt(R.id.widget_layout, "setBackgroundResource", R.drawable.widget_background);
            views.setInt(R.id.widget_layout, "setBackgroundColor", colors.backgroundColor); // Tint background
            views.setTextColor(R.id.widget_title, colors.titleColor);
            views.setTextColor(R.id.empty_view, colors.textColor); // Empty text color

            try { views.setInt(REFRESH_ICON_ID, "setColorFilter", colors.titleColor); } // Tint refresh icon
            catch (Exception e) { Log.w(TAG, "Could not tint refresh icon", e); }
            // Tint other frame icons if needed (e.g., R.id.widget_header_icon, R.id.empty_view_icon)
        }

        private static ThemeColors getThemeColors(Context context, WidgetConfig config) {
            int backgroundColor; int titleColor; int textColor; int countdownColor; // countdownColor only used for item theming reference if needed later

            int themeStyle = config.getThemeStyle();
            int widgetColor = config.getWidgetColor();

            if (themeStyle == 1) { // Light
                backgroundColor = ContextCompat.getColor(context, R.color.widget_background_light); titleColor = ContextCompat.getColor(context, R.color.widget_text_on_light); textColor = titleColor; countdownColor = ContextCompat.getColor(context, R.color.lightTextSecondary);
            } else if (themeStyle == 2) { // Dark
                backgroundColor = ContextCompat.getColor(context, R.color.widget_background_dark); titleColor = ContextCompat.getColor(context, R.color.widget_text_on_dark); textColor = titleColor; countdownColor = ContextCompat.getColor(context, R.color.darkTextSecondary);
            } else { // Dynamic or Custom (themeStyle == 0)
                int baseBg = (widgetColor != -1) ? widgetColor : Color.parseColor("#303030"); backgroundColor = baseBg;
                boolean isDark = ColorUtil.isDarkColor(baseBg);
                titleColor = isDark ? Color.WHITE : Color.BLACK; textColor = titleColor;
                // Countdown color derived simply for reference if ThemeManager provided item colors
                countdownColor = isDark ? Color.argb(200, 255, 255, 255) : Color.DKGRAY;
            }
            int alphaBackgroundColor = Color.argb(230, Color.red(backgroundColor), Color.green(backgroundColor), Color.blue(backgroundColor));
            return new ThemeColors(alphaBackgroundColor, textColor, titleColor, countdownColor);
        }

        // Simple value object for theme colors
        static class ThemeColors { final int backgroundColor; final int textColor; final int titleColor; final int countdownColor; ThemeColors(int bg,int txt,int tit,int cd){this.backgroundColor=bg; this.textColor=txt; this.titleColor=tit; this.countdownColor=cd;} }

    } // End ThemeManager


    /**
     * Helper class for setting up common PendingIntents for header/refresh clicks.
     */
    static class SetupHelper {
        public static void setupHeaderClick(Context context, RemoteViews views, int appWidgetId) {
            Intent intent = new Intent(context, SimpleListWidgetConfigActivity.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pi = PendingIntent.getActivity(context, appWidgetId + 1000, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widget_header, pi);
        }
        public static void setupRefreshButton(Context context, RemoteViews views, int appWidgetId) {
            Intent intent = new Intent(context, SimpleEventListWidgetProvider.class);
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{appWidgetId});
            PendingIntent pi = PendingIntent.getBroadcast(context, appWidgetId + 2000, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widget_refresh_container, pi);
        }
    } // End SetupHelper

    //endregion

} // End of SimpleEventListWidgetProvider class