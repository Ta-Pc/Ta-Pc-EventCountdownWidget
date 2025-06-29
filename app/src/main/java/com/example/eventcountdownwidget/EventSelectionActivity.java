package com.example.eventcountdownwidget;

import android.appwidget.AppWidgetManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class EventSelectionActivity extends AppCompatActivity {
    private static final String TAG = "EventSelectionActivity";
    private static final String PREFS_NAME = "com.example.eventcountdownwidget.WidgetPrefs";
    private static final String CALENDAR_PREFS = "calendar_preferences";
    private static final String SELECTED_CALENDARS = "selected_calendars";
    private static final String PREF_PREFIX_KEY = "widget_";
    private static final String EVENT_ID_KEY = "_event_id";
    private static final String EVENT_TITLE_KEY = "_event_title";
    private static final String EVENT_TIME_KEY = "_event_time"; // Start Time
    private static final String EVENT_END_TIME_KEY = "_event_end_time"; // End Time

    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private RecyclerView recyclerView;
    private LinearProgressIndicator progressIndicator;
    private List<CalendarEvent> eventList = new ArrayList<>();
    private EventAdapter eventAdapter; // Keep adapter instance
    private View emptyStateContainer;
    private TextView emptyViewTextView;
    private Button emptyStateActionButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_selection);

        // Set the result to CANCELED in case the user backs out
        setResult(RESULT_CANCELED);

        // Find the widget id from the intent
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // If this activity was started with an invalid widget ID, finish
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Log.e(TAG, "Invalid widget ID received. Finishing.");
            finish();
            return;
        }

        Log.d(TAG, "Selecting event for widget ID: " + mAppWidgetId);

        // Set up toolbar
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.select_event_title);

        // Initialize views
        recyclerView = findViewById(R.id.event_recycler_view);
        progressIndicator = findViewById(R.id.progress_indicator);
        emptyStateContainer = findViewById(R.id.empty_state_container);
        emptyViewTextView = findViewById(R.id.empty_view_text);
        emptyStateActionButton = findViewById(R.id.empty_state_action_button);

        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize adapter here, update data later
        eventAdapter = new EventAdapter(eventList, event -> {
            // Save the event data (including end time now)
            saveEventData(event.getId(), event.getTitle(), event.getStartTime(), event.getEndTime());
            // Update the widget (this also schedules the first update)
            updateWidget();
            // Close the activity
            finish();
        });

        recyclerView.setAdapter(eventAdapter);

        // Load events initially
        loadCalendarEvents();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_event_selection, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_calendar_settings) {
            Intent intent = new Intent(this, CalendarSettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Always reload events when returning to this activity
        Log.d(TAG, "Resuming EventSelectionActivity, reloading events from selected calendars.");
        loadCalendarEvents();
    }

    private void loadCalendarEvents() {
        progressIndicator.setVisibility(View.VISIBLE);
        eventList.clear(); // Clear previous results before loading
        eventAdapter.notifyDataSetChanged(); // Notify adapter about clearing

        // Hide empty state until we determine if it's needed
        if (emptyStateContainer != null) {
            emptyStateContainer.setVisibility(View.GONE);
        }

        // Show recycler view
        recyclerView.setVisibility(View.VISIBLE);

        // Get selected calendars
        SharedPreferences prefs = getSharedPreferences(CALENDAR_PREFS, MODE_PRIVATE);
        Set<String> selectedCalendarIds = prefs.getStringSet(SELECTED_CALENDARS, new HashSet<>());

        // If no calendars are selected, show the empty state with action button
        if (selectedCalendarIds.isEmpty()) {
            progressIndicator.setVisibility(View.GONE);

            // Show an actionable empty state
            if (emptyStateContainer != null && emptyViewTextView != null && emptyStateActionButton != null) {
                // Set empty state visibility and text
                emptyStateContainer.setVisibility(View.VISIBLE);
                emptyViewTextView.setText(R.string.no_calendars_selected);

                // Configure and show the action button
                emptyStateActionButton.setText(R.string.select_calendars);
                emptyStateActionButton.setVisibility(View.VISIBLE);
                emptyStateActionButton.setOnClickListener(v -> {
                    Intent intent = new Intent(EventSelectionActivity.this, CalendarSettingsActivity.class);
                    startActivity(intent);
                });

                // Hide the RecyclerView when showing empty state
                recyclerView.setVisibility(View.GONE);
            } else {
                // Fallback to toast if views aren't found
                Toast.makeText(this, R.string.no_calendars_selected, Toast.LENGTH_LONG).show();
            }

            return;
        }

        // Get time range: from now to one year from now
        Calendar beginTime = Calendar.getInstance();
        long startMillis = beginTime.getTimeInMillis();
        Calendar endTimeCal = Calendar.getInstance(); // Renamed to avoid conflict
        endTimeCal.add(Calendar.YEAR, 1); // Show events up to one year in the future
        long endMillis = endTimeCal.getTimeInMillis();

        // Projection includes start and end times
        final String[] INSTANCE_PROJECTION = new String[] {
                CalendarContract.Instances.EVENT_ID, // 0
                CalendarContract.Instances.TITLE, // 1
                CalendarContract.Instances.DESCRIPTION, // 2
                CalendarContract.Instances.BEGIN, // 3 Start Time
                CalendarContract.Instances.END, // 4 End Time
                CalendarContract.Instances.CALENDAR_ID // 5
        };

        // Column indices based on projection
        final int PROJECTION_EVENT_ID_INDEX = 0;
        final int PROJECTION_TITLE_INDEX = 1;
        final int PROJECTION_DESCRIPTION_INDEX = 2;
        final int PROJECTION_BEGIN_INDEX = 3;
        final int PROJECTION_END_INDEX = 4;
        final int PROJECTION_CALENDAR_ID_INDEX = 5;

        // Build the URI with the time range parameters
        Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
        // Use ContentUris for safe appending of IDs
        ContentUris.appendId(builder, startMillis);
        ContentUris.appendId(builder, endMillis);
        Uri uri = builder.build();

        // Build the selection criteria for calendars
        StringBuilder selectionBuilder = new StringBuilder();
        List<String> selectionArgsList = new ArrayList<>(); // Use List

        // Append calendar ID selection
        selectionBuilder.append(CalendarContract.Instances.CALENDAR_ID).append(" IN (");
        for (int i = 0; i < selectedCalendarIds.size(); i++) {
            selectionBuilder.append("?");
            if (i < selectedCalendarIds.size() - 1) {
                selectionBuilder.append(",");
            }
        }
        selectionBuilder.append(")");
        selectionArgsList.addAll(selectedCalendarIds);

        // Add selection to only show future events or events currently happening
        selectionBuilder.append(" AND ").append(CalendarContract.Instances.END).append(" >= ?");
        selectionArgsList.add(String.valueOf(startMillis)); // Only events ending now or later

        String selection = selectionBuilder.toString();
        String[] selectionArgs = selectionArgsList.toArray(new String[0]);

        // Sort by date, soonest first
        String sortOrder = CalendarContract.Instances.BEGIN + " ASC";

        // Execute the query on a background thread
        new Thread(() -> {
            ContentResolver contentResolver = getContentResolver();
            Cursor cursor = null;
            List<CalendarEvent> loadedEvents = new ArrayList<>(); // Load into temporary list

            try {
                cursor = contentResolver.query(
                        uri,
                        INSTANCE_PROJECTION,
                        selection,
                        selectionArgs,
                        sortOrder
                );

                if (cursor != null && cursor.getCount() > 0) {
                    Log.d(TAG, "Calendar events found: " + cursor.getCount());
                    while (cursor.moveToNext()) {
                        long eventId = cursor.getLong(PROJECTION_EVENT_ID_INDEX);
                        String title = cursor.getString(PROJECTION_TITLE_INDEX);
                        String description = cursor.getString(PROJECTION_DESCRIPTION_INDEX);
                        long startTime = cursor.getLong(PROJECTION_BEGIN_INDEX);
                        long eventEndTime = cursor.getLong(PROJECTION_END_INDEX); // Get end time

                        // Basic check for valid title
                        if (title == null || title.trim().isEmpty()) {
                            title = "(No Title)";
                        }

                        // Format date for display
                        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d, yyyy HH:mm", Locale.getDefault());
                        String formattedDate = dateFormat.format(new Date(startTime));

                        loadedEvents.add(new CalendarEvent(eventId, title, description, startTime, eventEndTime, formattedDate)); // Add end time
                    }
                } else {
                    Log.d(TAG, "No upcoming events found in selected calendars within the next year.");
                }
            } catch (SecurityException se) {
                Log.e(TAG, "Permission error loading calendar events", se);
                // Post error message to UI thread
                runOnUiThread(() -> {
                    progressIndicator.setVisibility(View.GONE);
                    Toast.makeText(EventSelectionActivity.this, "Calendar permission denied.", Toast.LENGTH_LONG).show();
                });
                return; // Stop background thread
            } catch (Exception e) {
                Log.e(TAG, "Error loading calendar events", e);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            // Update UI on main thread
            runOnUiThread(() -> {
                progressIndicator.setVisibility(View.GONE);
                eventList.clear(); // Clear the main list
                eventList.addAll(loadedEvents); // Add newly loaded events
                eventAdapter.notifyDataSetChanged(); // Notify adapter of data change

                if (eventList.isEmpty()) {
                    // Show empty state for no events found
                    if (emptyStateContainer != null && emptyViewTextView != null && emptyStateActionButton != null) {
                        emptyStateContainer.setVisibility(View.VISIBLE);
                        emptyViewTextView.setText(R.string.no_events_found);
                        emptyStateActionButton.setText(R.string.check_calendar_settings);
                        emptyStateActionButton.setVisibility(View.VISIBLE);
                        emptyStateActionButton.setOnClickListener(v -> {
                            Intent intent = new Intent(EventSelectionActivity.this, CalendarSettingsActivity.class);
                            startActivity(intent);
                        });
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        Toast.makeText(EventSelectionActivity.this,
                                R.string.no_events_found, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Ensure recycler view is visible if it was hidden
                    recyclerView.setVisibility(View.VISIBLE);
                    if (emptyStateContainer != null) {
                        emptyStateContainer.setVisibility(View.GONE);
                    }
                }
            });
        }).start();
    }

    // Updated saveEventData to include endTime
    private void saveEventData(long eventId, String title, long startTime, long endTime) {
        SharedPreferences.Editor prefs = getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putLong(PREF_PREFIX_KEY + mAppWidgetId + EVENT_ID_KEY, eventId);
        prefs.putString(PREF_PREFIX_KEY + mAppWidgetId + EVENT_TITLE_KEY, title);
        prefs.putLong(PREF_PREFIX_KEY + mAppWidgetId + EVENT_TIME_KEY, startTime);
        prefs.putLong(PREF_PREFIX_KEY + mAppWidgetId + EVENT_END_TIME_KEY, endTime); // Save end time
        prefs.apply();
        Log.d(TAG, "Saved event data for widget " + mAppWidgetId + ": ID=" + eventId + ", Start=" + startTime + ", End=" + endTime);
    }

    private void updateWidget() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        // Call the static update method for the specific widget type being configured
        EventCountdownWidget.updateAppWidget(this, appWidgetManager, mAppWidgetId);
        Log.d(TAG, "Triggered widget update for ID: " + mAppWidgetId);
        // Create result data
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, resultValue);
        Log.d(TAG, "Set RESULT_OK for widget ID: " + mAppWidgetId);
    }

    // Calendar event model class
    public static class CalendarEvent {
        private long id;
        private String title;
        private String description;
        private long startTime;
        private long endTime; // Added end time support
        private String formattedDate;

        public CalendarEvent(long id, String title, String description, long startTime, long endTime, String formattedDate) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.startTime = startTime;
            this.endTime = endTime;
            this.formattedDate = formattedDate;
        }

        public long getId() { return id; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public long getStartTime() { return startTime; }
        public long getEndTime() { return endTime; }
        public String getFormattedDate() { return formattedDate; }
    }
}
