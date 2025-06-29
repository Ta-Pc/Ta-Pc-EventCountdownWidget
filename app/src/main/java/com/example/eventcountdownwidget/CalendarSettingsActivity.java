package com.example.eventcountdownwidget;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.util.Log;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CalendarSettingsActivity extends AppCompatActivity {
    private static final String TAG = "CalendarSettings";
    private static final String CALENDAR_PREFS = "calendar_preferences";
    private static final String SELECTED_CALENDARS = "selected_calendars";
    private static final String HAS_MADE_SELECTION = "has_made_selection";

    private RecyclerView calendarRecyclerView;
    private List<CalendarInfo> calendarList = new ArrayList<>();
    private Set<String> selectedCalendarIds = new HashSet<>();
    private boolean selectionChanged = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar_settings);

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.calendar_settings_title);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        calendarRecyclerView = findViewById(R.id.calendar_recycler_view);
        calendarRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Load saved calendar preferences
        loadSelectedCalendars();

        // Load available calendars and display them
        loadCalendars();
    }

    private void loadSelectedCalendars() {
        SharedPreferences prefs = getSharedPreferences(CALENDAR_PREFS, MODE_PRIVATE);
        boolean hasSelection = prefs.getBoolean(HAS_MADE_SELECTION, false);
        selectedCalendarIds = new HashSet<>(prefs.getStringSet(SELECTED_CALENDARS, new HashSet<>()));

        // If no calendars have been selected yet and this is first run, we'll let the user select
        if (!hasSelection) {
            Log.d(TAG, "No saved calendar preferences found, user will need to select calendars");
        } else {
            Log.d(TAG, "Loaded " + selectedCalendarIds.size() + " selected calendars");
        }
    }

    private void loadCalendars() {
        String[] projection = new String[] {
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.CALENDAR_COLOR,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.VISIBLE // Include visibility flag
        };

        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME + " ASC");

        calendarList.clear();

        if (cursor != null && cursor.getCount() > 0) {
            // Get shared preferences to check if this is first run
            SharedPreferences prefs = getSharedPreferences(CALENDAR_PREFS, MODE_PRIVATE);
            boolean hasSelection = prefs.getBoolean(HAS_MADE_SELECTION, false);

            // No longer auto-selecting calendars on first run
            // boolean selectAllByDefault = !hasSelection;
            // Set<String> initialSelection = new HashSet<>();

            while (cursor.moveToNext()) {
                long id = cursor.getLong(0);
                String displayName = cursor.getString(1);
                int color = cursor.getInt(2);
                String accountName = cursor.getString(3);
                int visible = cursor.getInt(4); // 0 = invisible, 1 = visible

                String calendarId = String.valueOf(id);

                // No longer auto-selecting visible calendars
                // if (selectAllByDefault && visible == 1) {
                //     initialSelection.add(calendarId);
                // }

                boolean isSelected = selectedCalendarIds.contains(calendarId);

                // Only include visible calendars in the list
                if (visible == 1) {
                    calendarList.add(new CalendarInfo(id, displayName, accountName, color, isSelected));
                }
            }

            // No longer saving initial selection automatically
            // if (selectAllByDefault && !initialSelection.isEmpty()) {
            //     selectedCalendarIds = initialSelection;
            //     SharedPreferences.Editor editor = prefs.edit();
            //     editor.putStringSet(SELECTED_CALENDARS, selectedCalendarIds);
            //     editor.putBoolean(HAS_MADE_SELECTION, true);
            //     editor.apply();
            // }

            cursor.close();
        }

        // Set up the adapter
        CalendarAdapter adapter = new CalendarAdapter(calendarList, (calendarInfo, isChecked) -> {
            selectionChanged = true;
            if (isChecked) {
                selectedCalendarIds.add(String.valueOf(calendarInfo.getId()));
            } else {
                selectedCalendarIds.remove(String.valueOf(calendarInfo.getId()));
            }

            // We'll save in onPause/onBackPressed instead of every change for efficiency
        });

        calendarRecyclerView.setAdapter(adapter);
    }

    private void saveSelectedCalendars() {
        Log.d(TAG, "Saving selected calendars: " + selectedCalendarIds.size());
        SharedPreferences.Editor editor = getSharedPreferences(CALENDAR_PREFS, MODE_PRIVATE).edit();
        editor.putStringSet(SELECTED_CALENDARS, selectedCalendarIds);
        editor.putBoolean(HAS_MADE_SELECTION, true);
        editor.apply();
        selectionChanged = false;

        // Notify other components that calendar selection changed
        Intent intent = new Intent("com.example.eventcountdownwidget.CALENDAR_SELECTION_CHANGED");
        sendBroadcast(intent);

        // Give user feedback that selections were saved
        Snackbar.make(calendarRecyclerView, R.string.calendars_saved, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (selectionChanged) {
            saveSelectedCalendars();
        }
    }

    @Override
    public void onBackPressed() {
        if (selectionChanged) {
            saveSelectedCalendars();
        }
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (selectionChanged) {
                saveSelectedCalendars();
            }
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Calendar info data class
    public static class CalendarInfo {
        private final long id;
        private final String displayName;
        private final String accountName;
        private final int color;
        private boolean selected;

        public CalendarInfo(long id, String displayName, String accountName, int color, boolean selected) {
            this.id = id;
            this.displayName = displayName;
            this.accountName = accountName;
            this.color = color;
            this.selected = selected;
        }

        // Getters and setters
        public long getId() { return id; }
        public String getDisplayName() { return displayName; }
        public String getAccountName() { return accountName; }
        public int getColor() { return color; }
        public boolean isSelected() { return selected; }
        public void setSelected(boolean selected) { this.selected = selected; }
    }
}
