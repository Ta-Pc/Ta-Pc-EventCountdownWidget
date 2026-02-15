package com.example.eventcountdownwidget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.elevation.SurfaceColors;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import android.Manifest;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class SimpleListWidgetConfigActivity extends Activity {
    private static final int PERMISSION_REQUEST_CODE = 200;
    private static final String PREFS_NAME = "com.example.eventcountdownwidget.WidgetPrefs";
    private static final String PREF_PREFIX_KEY = "simple_list_widget_";
    private static final String THEME_STYLE_KEY = "_theme_style";
    private static final String WIDGET_COLOR_KEY = "_widget_color";
    private static final String MAX_EVENTS_KEY = "_max_events";
    // Updated constants for event limits
    private static final int MAX_CONFIGURABLE_EVENTS = 25; // User can choose up to 25
    private static final int DEFAULT_EVENTS_TO_SHOW = 10; // Default value

    // Logging Tag
    private static final String TAG = "SimpleListWidgetConfig";

    // Static random instance for better randomization
    private static final Random random = new Random();
    private static final Set<Integer> recentlyUsedColors = new HashSet<>();
    private static final int MAX_RECENT_COLORS = 3;

    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private RadioGroup themeStyleRadioGroup;
    private MaterialCardView colorPreviewCard;
    private LinearLayout colorOptionsContainer;
    private SeekBar maxEventsSeekBar;
    private TextView maxEventsTextView;

    // Material design colors
    private static final int[] MATERIAL_COLORS = {
            Color.parseColor("#F44336"), // Red
            Color.parseColor("#E91E63"), // Pink
            Color.parseColor("#9C27B0"), // Purple
            Color.parseColor("#673AB7"), // Deep Purple
            Color.parseColor("#3F51B5"), // Indigo
            Color.parseColor("#2196F3"), // Blue
            Color.parseColor("#03A9F4"), // Light Blue
            Color.parseColor("#00BCD4"), // Cyan
            Color.parseColor("#009688"), // Teal
            Color.parseColor("#4CAF50"), // Green
            Color.parseColor("#8BC34A"), // Light Green
            Color.parseColor("#CDDC39"), // Lime
            Color.parseColor("#FFC107"), // Amber
            Color.parseColor("#FF9800"), // Orange
            Color.parseColor("#FF5722")  // Deep Orange
    };

    private int selectedColorIndex = -1;
    private int customColor = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Apply dynamic color to window
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getWindow().setStatusBarColor(SurfaceColors.SURFACE_2.getColor(this));
            getWindow().setNavigationBarColor(SurfaceColors.SURFACE_2.getColor(this));
        }

        setContentView(R.layout.activity_event_list_config);

        // Set result to CANCELED in case user backs out
        setResult(RESULT_CANCELED);

        // Get widget ID from intent
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // If this activity was started with an invalid widget ID, finish
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Log.e(TAG, "Invalid AppWidget ID received, finishing configuration.");
            finish();
            return;
        }
        Log.d(TAG, "Configuring list widget ID: " + mAppWidgetId);


        // Initialize UI components
        themeStyleRadioGroup = findViewById(R.id.theme_style_radio_group);
        colorPreviewCard = findViewById(R.id.color_preview_card);
        colorOptionsContainer = findViewById(R.id.color_options_container);
        maxEventsSeekBar = findViewById(R.id.max_events_seekbar);
        maxEventsTextView = findViewById(R.id.max_events_value);

        MaterialButton saveButton = findViewById(R.id.save_button);
        MaterialButton calendarSettingsButton = findViewById(R.id.calendar_settings_button);
        MaterialButton randomizeColorButton = findViewById(R.id.randomize_color_button);

        // Load saved preferences
        loadSavedPreferences();

        // Check for permissions
        checkCalendarPermission();

        // Set up color selection options
        setupColorOptions();

        // Update max events text on seekbar change
        // ADJUST MAX PROGRESS based on MAX_CONFIGURABLE_EVENTS
        // SeekBar range will be 0 to (MAX_CONFIGURABLE_EVENTS - 1)
        maxEventsSeekBar.setMax(MAX_CONFIGURABLE_EVENTS - 1); // e.g., Max progress 24 for 1-25 events
        maxEventsSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateMaxEventsText(progress + 1); // Map 0-24 -> 1-25
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Save button click handler
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                savePreferences();
                updateWidget(); // Update the widget view immediately
                // Schedule first update after saving config
                WidgetUpdateReceiver.scheduleNextUpdate(SimpleListWidgetConfigActivity.this, mAppWidgetId, SimpleEventListWidgetProvider.class.getName());
                Log.d(TAG, "Initial update scheduling triggered for list widget ID: " + mAppWidgetId);
                Snackbar.make(v, R.string.config_saved, Snackbar.LENGTH_SHORT).show();
                finishWithResult(); // Finish and return RESULT_OK
            }
        });

        // Calendar settings button click handler
        calendarSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchCalendarSettings();
            }
        });

        // Randomize color button click handler
        randomizeColorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                customColor = generateUniqueRandomColor(mAppWidgetId);
                selectedColorIndex = -1;
                updateColorPreview(customColor);
                updateColorSelectionUI(); // Deselect highlight
                Snackbar.make(v, R.string.color_randomized, Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void loadSavedPreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, 0);
        int savedThemeStyle = prefs.getInt(PREF_PREFIX_KEY + mAppWidgetId + THEME_STYLE_KEY, 0);
        int savedColor = prefs.getInt(PREF_PREFIX_KEY + mAppWidgetId + WIDGET_COLOR_KEY, -1);
        // ADJUST Default and Ensure Max for MaxEvents
        int savedMaxEvents = prefs.getInt(PREF_PREFIX_KEY + mAppWidgetId + MAX_EVENTS_KEY, DEFAULT_EVENTS_TO_SHOW); // Use new default
        savedMaxEvents = Math.min(savedMaxEvents, MAX_CONFIGURABLE_EVENTS); // Ensure saved value doesn't exceed allowed max
        savedMaxEvents = Math.max(1, savedMaxEvents); // Ensure at least 1

        Log.d(TAG, "Loading prefs for list widget " + mAppWidgetId + ": Theme=" + savedThemeStyle + ", Color=" + savedColor + ", MaxEvents=" + savedMaxEvents);

        // Set theme style
        if (themeStyleRadioGroup != null) {
            themeStyleRadioGroup.check(getThemeRadioButtonId(savedThemeStyle));
        }

        // Set max events SeekBar progress (value - 1)
        maxEventsSeekBar.setProgress(savedMaxEvents - 1); // e.g., 10 events -> progress 9
        updateMaxEventsText(savedMaxEvents);

        // Handle saved color
        if (savedColor != -1) {
            customColor = savedColor;
            updateColorPreview(savedColor);
            selectedColorIndex = -1; // Reset first
            for(int i=0; i < MATERIAL_COLORS.length; i++) {
                if (MATERIAL_COLORS[i] == savedColor) {
                    selectedColorIndex = i;
                    break;
                }
            }
        } else {
            customColor = generateUniqueRandomColor(mAppWidgetId);
            updateColorPreview(customColor);
            selectedColorIndex = -1;
        }
        updateColorSelectionUI(); // Highlight selected color if applicable
    }

    private void setupColorOptions() {
        colorOptionsContainer.removeAllViews();
        for (int i = 0; i < MATERIAL_COLORS.length; i++) {
            final int colorIndex = i;
            View colorOption = createColorOptionView(MATERIAL_COLORS[i], new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedColorIndex = colorIndex;
                    customColor = MATERIAL_COLORS[colorIndex];
                    updateColorPreview(customColor);
                    updateColorSelectionUI();
                }
            });
            colorOption.setTag(colorIndex);
            colorOptionsContainer.addView(colorOption);
        }
        updateColorSelectionUI();
    }

    private View createColorOptionView(int color, View.OnClickListener listener) {
        MaterialCardView colorCard = new MaterialCardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                getResources().getDimensionPixelSize(R.dimen.color_option_size),
                getResources().getDimensionPixelSize(R.dimen.color_option_size));
        int margin = getResources().getDimensionPixelSize(R.dimen.color_option_margin);
        params.setMargins(margin, margin, margin, margin);
        colorCard.setLayoutParams(params);
        colorCard.setCardBackgroundColor(color);
        colorCard.setRadius(getResources().getDimensionPixelSize(R.dimen.color_option_radius));
        colorCard.setOnClickListener(listener);
        colorCard.setStrokeWidth(0);
        colorCard.setStrokeColor(Color.DKGRAY); // Color for selected stroke highlight
        return colorCard;
    }

    private void updateColorPreview(int color) {
        if (colorPreviewCard != null) {
            colorPreviewCard.setCardBackgroundColor(color);
        }
    }

    private void updateColorSelectionUI() {
        for (int i = 0; i < colorOptionsContainer.getChildCount(); i++) {
            View child = colorOptionsContainer.getChildAt(i);
            if (child instanceof MaterialCardView && child.getTag() != null) {
                MaterialCardView card = (MaterialCardView) child;
                int index = (int) card.getTag();
                // Highlight the selected card
                card.setStrokeWidth((index == selectedColorIndex) ?
                        getResources().getDimensionPixelSize(R.dimen.color_option_margin) : 0);
            }
        }
    }


    private void updateMaxEventsText(int value) {
        if (maxEventsTextView != null) {
            maxEventsTextView.setText(String.valueOf(value));
        }
    }

    private int getThemeRadioButtonId(int themeStyle) {
        switch (themeStyle) {
            case 1: return R.id.radio_light;
            case 2: return R.id.radio_dark;
            case 0:
            default: return R.id.radio_dynamic;
        }
    }

    private int getSelectedThemeStyle() {
        int selectedId = themeStyleRadioGroup.getCheckedRadioButtonId();
        if (selectedId == R.id.radio_light) return 1;
        else if (selectedId == R.id.radio_dark) return 2;
        else return 0; // Dynamic is default
    }

    private void savePreferences() {
        int themeStyleOption = getSelectedThemeStyle();
        // ADJUST MaxEvents calculation
        int maxEvents = maxEventsSeekBar.getProgress() + 1; // Maps 0-24 -> 1-25

        SharedPreferences.Editor prefs = getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putInt(PREF_PREFIX_KEY + mAppWidgetId + THEME_STYLE_KEY, themeStyleOption);
        prefs.putInt(PREF_PREFIX_KEY + mAppWidgetId + MAX_EVENTS_KEY, maxEvents); // Save the chosen value (1-25)

        // Save color
        if (customColor != -1) {
            prefs.putInt(PREF_PREFIX_KEY + mAppWidgetId + WIDGET_COLOR_KEY, customColor);
            Log.d(TAG, "Saving prefs for list widget " + mAppWidgetId + ": Theme=" + themeStyleOption + ", MaxEvents=" + maxEvents + ", Color=" + customColor);
        } else {
            prefs.remove(PREF_PREFIX_KEY + mAppWidgetId + WIDGET_COLOR_KEY);
            Log.w(TAG, "Saving prefs for list widget " + mAppWidgetId + ": Color is -1 (unset), removing pref.");
        }
        prefs.apply();
    }

    private void updateWidget() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        // Trigger an update for this specific widget
        SimpleEventListWidgetProvider.updateAppWidget(this, appWidgetManager, mAppWidgetId);
        // Also notify the service that data might have changed (though updateAppWidget should handle adapter setup)
        appWidgetManager.notifyAppWidgetViewDataChanged(mAppWidgetId, R.id.event_list_view);
        Log.d(TAG, "Configuration update triggered for list widget ID: " + mAppWidgetId);
    }

    private void finishWithResult() {
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, resultValue);
        Log.d(TAG, "Finishing configuration with RESULT_OK for list widget ID: " + mAppWidgetId);
        finish();
    }

    private void launchCalendarSettings() {
        Intent intent = new Intent(this, CalendarSettingsActivity.class);
        startActivity(intent);
    }

    private static int generateUniqueRandomColor(long widgetId) {
        int color;
        int attempts = 0;
        random.setSeed(widgetId + System.currentTimeMillis());
        do {
            color = MATERIAL_COLORS[random.nextInt(MATERIAL_COLORS.length)];
            attempts++;
        } while (recentlyUsedColors.contains(color) && attempts < MATERIAL_COLORS.length);

        recentlyUsedColors.add(color);
        if (recentlyUsedColors.size() > MAX_RECENT_COLORS) {
            List<Integer> colorsList = new ArrayList<>(recentlyUsedColors);
            if (!colorsList.isEmpty()) {
                recentlyUsedColors.remove(colorsList.get(random.nextInt(colorsList.size())));
            }
        }
        Log.d(TAG, "Generated unique random color: #" + Integer.toHexString(color));
        return color;
    }
    // Check and request calendar permission
    private void checkCalendarPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CALENDAR}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                 // Permission granted
                 Snackbar.make(findViewById(android.R.id.content), "Permission granted", Snackbar.LENGTH_SHORT).show();
            } else {
                 // Permission denied
                 Snackbar.make(findViewById(android.R.id.content), R.string.permission_required, Snackbar.LENGTH_LONG).show();
            }
        }
    }
}