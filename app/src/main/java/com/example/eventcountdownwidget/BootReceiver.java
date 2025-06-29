// In: src/main/java/com/example/eventcountdownwidget/BootReceiver.java
package com.example.eventcountdownwidget;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.i(TAG, "Device boot completed. Rescheduling widget updates.");
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

            // Reschedule for EventCountdownWidget instances
            ComponentName countdownProvider = new ComponentName(context, EventCountdownWidget.class);
            int[] countdownWidgetIds = appWidgetManager.getAppWidgetIds(countdownProvider);
            if (countdownWidgetIds != null && countdownWidgetIds.length > 0) {
                Log.d(TAG, "Found " + countdownWidgetIds.length + " EventCountdownWidgets to reschedule.");
                for (int appWidgetId : countdownWidgetIds) {
                    WidgetUpdateReceiver.scheduleNextUpdate(context, appWidgetId, EventCountdownWidget.class.getName());
                }
            } else {
                Log.d(TAG, "No active EventCountdownWidgets found.");
            }

            // Reschedule for SimpleEventListWidgetProvider instances
            ComponentName listProvider = new ComponentName(context, SimpleEventListWidgetProvider.class);
            int[] listWidgetIds = appWidgetManager.getAppWidgetIds(listProvider);
            if (listWidgetIds != null && listWidgetIds.length > 0) {
                Log.d(TAG, "Found " + listWidgetIds.length + " SimpleEventListWidgets to reschedule.");
                for (int appWidgetId : listWidgetIds) {
                    WidgetUpdateReceiver.scheduleNextUpdate(context, appWidgetId, SimpleEventListWidgetProvider.class.getName());
                }
            } else {
                Log.d(TAG, "No active SimpleEventListWidgets found.");
            }

            Log.i(TAG, "Widget update rescheduling finished.");
        }
    }
}