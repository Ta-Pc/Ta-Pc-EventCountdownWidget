package com.example.eventcountdownwidget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class WidgetUpdateReceiver extends BroadcastReceiver {

    private static final String TAG = "WidgetUpdateReceiver";
    public static final String ACTION_UPDATE_WIDGET = "com.example.eventcountdownwidget.ACTION_UPDATE_WIDGET";
    public static final String EXTRA_WIDGET_ID = "com.example.eventcountdownwidget.EXTRA_WIDGET_ID";
    public static final String EXTRA_PROVIDER_CLASS = "com.example.eventcountdownwidget.EXTRA_PROVIDER_CLASS";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && ACTION_UPDATE_WIDGET.equals(intent.getAction())) {
            int appWidgetId = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            String providerClassName = intent.getStringExtra(EXTRA_PROVIDER_CLASS);

            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID && providerClassName != null) {
                Log.d(TAG, "Received update alarm for widget ID: " + appWidgetId + " of type: " + providerClassName);

                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

                // Trigger the update for the correct widget provider
                try {
                    Class<?> providerClass = Class.forName(providerClassName);
                    if (EventCountdownWidget.class.getName().equals(providerClassName)) {
                        EventCountdownWidget.updateAppWidget(context, appWidgetManager, appWidgetId);
                    } else if (SimpleEventListWidgetProvider.class.getName().equals(providerClassName)) {
                        SimpleEventListWidgetProvider.updateAppWidget(context, appWidgetManager, appWidgetId);
                    } else {
                        Log.w(TAG, "Unknown provider class name: " + providerClassName);
                    }
                } catch (ClassNotFoundException e) {
                    Log.e(TAG, "Provider class not found: " + providerClassName, e);
                } catch (Exception e) {
                    Log.e(TAG, "Error updating widget ID " + appWidgetId, e);
                }

                // Reschedule the next update for the start of the next minute
                scheduleNextUpdate(context, appWidgetId, providerClassName);
            } else {
                Log.w(TAG, "Invalid widget ID or provider class received in intent.");
            }
        } else {
            Log.w(TAG, "Received unexpected intent: " + intent);
        }
    }

    public static void scheduleNextUpdate(Context context, int appWidgetId, String providerClassName) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "Cannot get AlarmManager service.");
            return;
        }

        // Get event time from preferences to determine appropriate update interval
        long eventStartTime = getEventStartTime(context, appWidgetId, providerClassName);
        long eventEndTime = getEventEndTime(context, appWidgetId, providerClassName);

        // Calculate update interval based on event timing
        int updateIntervalMinutes = calculateUpdateInterval(eventStartTime, eventEndTime);

        // Calculate next update time
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, updateIntervalMinutes);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long triggerAtMillis = calendar.getTimeInMillis();

        Log.d(TAG, "Scheduling next update for widget " + appWidgetId +
                " in " + updateIntervalMinutes + " minutes at " + calendar.getTime());

        PendingIntent pendingIntent = createUpdatePendingIntent(context, appWidgetId,
                providerClassName, PendingIntent.FLAG_UPDATE_CURRENT);
        try {
            // Use setExactAndAllowWhileIdle for better reliability across Doze modes
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                    Log.w(TAG, "Cannot schedule exact alarms. Scheduled inexact update for widget " + appWidgetId);
                }
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            }
        } catch (SecurityException se) {
            Log.e(TAG, "SecurityException scheduling alarm for widget " + appWidgetId + ". Check exact alarm permissions.", se);
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling alarm for widget " + appWidgetId, e);
        }
    }

    // Helper method to calculate appropriate update interval based on event timing
    private static int calculateUpdateInterval(long eventStartTime, long eventEndTime) {
        long now = System.currentTimeMillis();

        // For events happening now (between start and end time)
        if (now >= eventStartTime && now < eventEndTime) {
            return 1; // Update every minute for active events
        }

        // For past events
        if (now >= eventEndTime) {
            return 60; // Update hourly for past events
        }

        // For future events, scale based on proximity
        long diffMillis = eventStartTime - now;

        if (diffMillis <= TimeUnit.HOURS.toMillis(1)) {
            return 1; // Every minute if less than 1 hour away
        } else if (diffMillis <= TimeUnit.HOURS.toMillis(3)) {
            return 5; // Every 5 minutes if 1-3 hours away
        } else if (diffMillis <= TimeUnit.DAYS.toMillis(1)) {
            return 15; // Every 15 minutes if 3-24 hours away
        } else if (diffMillis <= TimeUnit.DAYS.toMillis(7)) {
            return 60; // Hourly if 1-7 days away
        } else {
            return 240; // Every 4 hours if more than a week away
        }
    }

    // Helper method to get event start time for the given widget
    private static long getEventStartTime(Context context, int appWidgetId, String providerClassName) {
        SharedPreferences prefs = context.getSharedPreferences("com.example.eventcountdownwidget.WidgetPrefs", 0);
        String keyPrefix;

        if (providerClassName.equals(EventCountdownWidget.class.getName())) {
            keyPrefix = "widget_";
        } else {
            keyPrefix = "simple_list_widget_";
        }

        return prefs.getLong(keyPrefix + appWidgetId + "_event_time", System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1));
    }

    // Helper method to get event end time for the given widget
    private static long getEventEndTime(Context context, int appWidgetId, String providerClassName) {
        SharedPreferences prefs = context.getSharedPreferences("com.example.eventcountdownwidget.WidgetPrefs", 0);
        String keyPrefix;

        if (providerClassName.equals(EventCountdownWidget.class.getName())) {
            keyPrefix = "widget_";
        } else {
            keyPrefix = "simple_list_widget_";
        }

        // If no end time is stored, default to start time + 1 hour
        long startTime = getEventStartTime(context, appWidgetId, providerClassName);
        return prefs.getLong(keyPrefix + appWidgetId + "_event_end_time", startTime + TimeUnit.HOURS.toMillis(1));
    }


    public static void cancelUpdate(Context context, int appWidgetId, String providerClassName) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            PendingIntent pendingIntent = createUpdatePendingIntent(context, appWidgetId, providerClassName, PendingIntent.FLAG_NO_CREATE);
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent);
                pendingIntent.cancel(); // Also cancel the PendingIntent itself
                Log.d(TAG, "Cancelled update alarm for widget ID: " + appWidgetId);
            } else {
                Log.d(TAG, "No existing alarm found to cancel for widget ID: " + appWidgetId);
            }
        }
    }

    private static PendingIntent createUpdatePendingIntent(Context context, int appWidgetId, String providerClassName, int flags) {
        Intent intent = new Intent(context, WidgetUpdateReceiver.class);
        intent.setAction(ACTION_UPDATE_WIDGET);
        intent.putExtra(EXTRA_WIDGET_ID, appWidgetId);
        intent.putExtra(EXTRA_PROVIDER_CLASS, providerClassName);
        // Ensure the intent is unique for each widget ID to avoid conflicts
        intent.setData(android.net.Uri.parse("update://widget/" + appWidgetId));

        int pendingIntentFlags = flags | PendingIntent.FLAG_IMMUTABLE;

        return PendingIntent.getBroadcast(
                context,
                appWidgetId, // Use widget ID as the request code for uniqueness
                intent,
                pendingIntentFlags
        );
    }
}