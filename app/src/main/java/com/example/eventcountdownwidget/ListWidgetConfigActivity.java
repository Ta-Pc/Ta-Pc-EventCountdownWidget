package com.example.eventcountdownwidget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
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

/**
 * Configuration activity for the list widget.
 * Allows setting maximum events to display, theme, and color.
 */
public class ListWidgetConfigActivity extends Activity {
    private static final String PREFS_NAME = "com.example.eventcountdownwidget.WidgetPrefs";
    private static final String PREF_PREFIX_KEY = "list_widget_";
    private static final String THEME_STYLE_KEY = "_theme_style";
    private static final String WIDGET_COLOR_KEY = "_widget_color";
    private static final String MAX_EVENTS_KEY = "_max_events";

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
            finish();
            return;
        }

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

        // Set up color selection options
        setupColorOptions();

        // Update max events text on seekbar change
        maxEventsSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateMaxEventsText(progress + 5); // Minimum 5 events
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
                updateWidget();
                Snackbar.make(v, R.string.config_saved, Snackbar.LENGTH_SHORT).show();
                finishWithResult();
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
                selectedColorIndex = -1; // Reset selected index since we're using a custom color
                updateColorPreview(customColor);
                Snackbar.make(v, R.string.color_randomized, Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void loadSavedPreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, 0);
        int savedThemeStyle = prefs.getInt(PREF_PREFIX_KEY + mAppWidgetId + THEME_STYLE_KEY, 0);
        int savedColor = prefs.getInt(PREF_PREFIX_KEY + mAppWidgetId + WIDGET_COLOR_KEY, -1);
        int savedMaxEvents = prefs.getInt(PREF_PREFIX_KEY + mAppWidgetId + MAX_EVENTS_KEY, 10);

        // Set UI to saved values
        if (themeStyleRadioGroup != null) {
            themeStyleRadioGroup.check(getThemeRadioButtonId(savedThemeStyle));
        }

        // Set max events (adjust for minimum of 5)
        maxEventsSeekBar.setProgress(savedMaxEvents - 5);
        updateMaxEventsText(savedMaxEvents);

        // Handle saved color
        if (savedColor != -1) {
            customColor = savedColor;
            updateColorPreview(savedColor);
        } else {
            // If no saved color, generate one
            customColor = generateUniqueRandomColor(mAppWidgetId);
            updateColorPreview(customColor);
        }
    }

    private void setupColorOptions() {
        // Clear existing views
        colorOptionsContainer.removeAllViews();

        // Add color option buttons
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
            colorOptionsContainer.addView(colorOption);
        }
    }

    private View createColorOptionView(int color, View.OnClickListener listener) {
        MaterialCardView colorCard = new MaterialCardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                getResources().getDimensionPixelSize(R.dimen.color_option_size),
                getResources().getDimensionPixelSize(R.dimen.color_option_size));
        params.setMargins(
                getResources().getDimensionPixelSize(R.dimen.color_option_margin),
                0,
                getResources().getDimensionPixelSize(R.dimen.color_option_margin),
                0);
        colorCard.setLayoutParams(params);
        colorCard.setCardBackgroundColor(color);
        colorCard.setRadius(getResources().getDimensionPixelSize(R.dimen.color_option_radius));
        colorCard.setOnClickListener(listener);
        return colorCard;
    }

    private void updateColorPreview(int color) {
        if (colorPreviewCard != null) {
            colorPreviewCard.setCardBackgroundColor(color);
        }
    }

    private void updateColorSelectionUI() {
        // Update the UI to show which color is selected
        // This would typically add a border or indicator to the selected color
    }

    private void updateMaxEventsText(int value) {
        maxEventsTextView.setText(String.valueOf(value));
    }

    private int getThemeRadioButtonId(int themeStyle) {
        switch (themeStyle) {
            case 0: return R.id.radio_dynamic;
            case 1: return R.id.radio_light;
            case 2: return R.id.radio_dark;
            default: return R.id.radio_dynamic;
        }
    }

    private int getSelectedThemeStyle() {
        int selectedId = themeStyleRadioGroup.getCheckedRadioButtonId();
        if (selectedId == R.id.radio_light) return 1;
        else if (selectedId == R.id.radio_dark) return 2;
        else return 0; // Dynamic
    }

    private void savePreferences() {
        int themeStyleOption = getSelectedThemeStyle();
        int maxEvents = maxEventsSeekBar.getProgress() + 5; // Add 5 to get actual value (minimum of 5)

        SharedPreferences.Editor prefs = getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putInt(PREF_PREFIX_KEY + mAppWidgetId + THEME_STYLE_KEY, themeStyleOption);
        prefs.putInt(PREF_PREFIX_KEY + mAppWidgetId + MAX_EVENTS_KEY, maxEvents);

        // Save the selected color
        if (customColor != -1) {
            prefs.putInt(PREF_PREFIX_KEY + mAppWidgetId + WIDGET_COLOR_KEY, customColor);
        }

        prefs.apply();
    }

    private void updateWidget() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        SimpleEventListWidgetProvider.updateAppWidget(this, appWidgetManager, mAppWidgetId);
    }

    private void finishWithResult() {
        // Return OK result with the widget ID
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }

    private void launchCalendarSettings() {
        // Launch the CalendarSettingsActivity
        Intent intent = new Intent(this, CalendarSettingsActivity.class);
        startActivity(intent);
    }

    // Improved random color generation to ensure better distribution
    private static int generateUniqueRandomColor(long widgetId) {
        int color;
        int attempts = 0;

        // Seed with widget ID and current time for better distribution
        random.setSeed(widgetId + System.currentTimeMillis());

        do {
            // Get a random color from the material colors array
            color = MATERIAL_COLORS[random.nextInt(MATERIAL_COLORS.length)];
            attempts++;
        } while (recentlyUsedColors.contains(color) && attempts < 10);

        // Add to recently used and maintain size
        recentlyUsedColors.add(color);
        if (recentlyUsedColors.size() > MAX_RECENT_COLORS) {
            // Remove oldest color
            List<Integer> colorsList = new ArrayList<>(recentlyUsedColors);
            recentlyUsedColors.remove(colorsList.get(0));
        }

        return color;
    }
}
