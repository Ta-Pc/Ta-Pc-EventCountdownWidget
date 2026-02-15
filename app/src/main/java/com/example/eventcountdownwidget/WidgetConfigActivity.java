package com.example.eventcountdownwidget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log; // Import Log
import android.util.TypedValue; // Import TypedValue
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
// import android.widget.SeekBar; // REMOVE
// import android.widget.TextView; // REMOVE

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

public class WidgetConfigActivity extends Activity {
    private static final int PERMISSION_REQUEST_CODE = 200;

    private static final String PREFS_NAME = "com.example.eventcountdownwidget.WidgetPrefs";
    private static final String PREF_PREFIX_KEY = "widget_";
    // private static final String FONT_SIZE_KEY = "_font_size"; // REMOVE
    private static final String THEME_STYLE_KEY = "_theme_style";
    private static final String WIDGET_COLOR_KEY = "_widget_color";

    // Logging Tag
    private static final String TAG = "WidgetConfigActivity";

    // Static random instance for better randomization
    private static final Random random = new Random();
    private static final Set<Integer> recentlyUsedColors = new HashSet<>();
    private static final int MAX_RECENT_COLORS = 3;

    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    // private SeekBar fontSizeSeekBar; // REMOVE
    // private TextView fontSizePreview; // REMOVE
    private RadioGroup themeStyleRadioGroup;
    private MaterialCardView colorPreviewCard;
    private LinearLayout colorOptionsContainer;

    // Material design colors (Keep as is)
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

        // Apply dynamic color to window (Keep as is)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getWindow().setStatusBarColor(SurfaceColors.SURFACE_2.getColor(this));
            getWindow().setNavigationBarColor(SurfaceColors.SURFACE_2.getColor(this));
        }

        setContentView(R.layout.activity_widget_config);

        // Set result to CANCELED in case user backs out (Keep as is)
        setResult(RESULT_CANCELED);

        // Find the widget id from the intent (Keep as is)
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // If this activity was started with an invalid widget ID, finish (Keep as is)
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Log.e(TAG, "Invalid AppWidget ID received, finishing configuration.");
            finish();
            return;
        }
        Log.d(TAG, "Configuring widget ID: " + mAppWidgetId);

        // Initialize UI components
        // fontSizeSeekBar = findViewById(R.id.font_size_seekbar); // REMOVE
        // fontSizePreview = findViewById(R.id.font_size_preview); // REMOVE
        themeStyleRadioGroup = findViewById(R.id.theme_style_radio_group);
        colorPreviewCard = findViewById(R.id.color_preview_card);
        colorOptionsContainer = findViewById(R.id.color_options_container);

        MaterialButton saveButton = findViewById(R.id.save_button);
        MaterialButton eventSelectionButton = findViewById(R.id.select_event_button);
        MaterialButton randomizeColorButton = findViewById(R.id.randomize_color_button);

        // Load saved preferences
        loadSavedPreferences();

        // Check for permissions
        checkCalendarPermission();

        // Set up color selection options
        setupColorOptions();

        // Remove Seekbar Listener
        // fontSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() { ... }); // REMOVE

        // Save button click handler (Keep as is, will call updated savePreferences)
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                savePreferences();
                updateWidget(); // Update the widget view immediately
                WidgetUpdateReceiver.scheduleNextUpdate(WidgetConfigActivity.this, mAppWidgetId, EventCountdownWidget.class.getName());
                Log.d(TAG, "Initial update scheduling triggered for widget ID: " + mAppWidgetId);
                Snackbar.make(v, R.string.config_saved, Snackbar.LENGTH_SHORT).show();
                finishWithResult(); // Finish and return RESULT_OK
            }
        });

        // Event selection button click handler (Keep as is)
        eventSelectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchEventSelection();
            }
        });

        // Randomize color button click handler (Keep as is)
        randomizeColorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                customColor = generateUniqueRandomColor(mAppWidgetId);
                selectedColorIndex = -1; // Reset selected index since we're using a custom color
                updateColorPreview(customColor);
                updateColorSelectionUI(); // Deselect preset highlight
                Snackbar.make(v, R.string.color_randomized, Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void loadSavedPreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, 0);
        // int savedFontSize = prefs.getInt(PREF_PREFIX_KEY + mAppWidgetId + FONT_SIZE_KEY, 1); // REMOVE
        int savedThemeStyle = prefs.getInt(PREF_PREFIX_KEY + mAppWidgetId + THEME_STYLE_KEY, 0);
        int savedColor = prefs.getInt(PREF_PREFIX_KEY + mAppWidgetId + WIDGET_COLOR_KEY, -1);

        // Log loaded prefs (without font size)
        Log.d(TAG, "Loading prefs for widget " + mAppWidgetId + ": Theme=" + savedThemeStyle + ", Color=" + savedColor);

        // Set UI to saved values (without font size)
        // fontSizeSeekBar.setProgress(savedFontSize); // REMOVE
        if (themeStyleRadioGroup != null) {
            themeStyleRadioGroup.check(getThemeRadioButtonId(savedThemeStyle));
        }

        // Handle saved color (Keep as is)
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

        // Remove font size preview update
        // updateFontSizePreview(savedFontSize); // REMOVE
        updateColorSelectionUI(); // Keep color UI update
    }

    // setupColorOptions (Keep as is)
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

    // createColorOptionView (Keep as is)
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
        colorCard.setStrokeColor(Color.DKGRAY);
        return colorCard;
    }

    // updateColorPreview (Keep as is)
    private void updateColorPreview(int color) {
        if (colorPreviewCard != null) {
            colorPreviewCard.setCardBackgroundColor(color);
        }
    }

    // updateColorSelectionUI (Keep as is)
    private void updateColorSelectionUI() {
        for (int i = 0; i < colorOptionsContainer.getChildCount(); i++) {
            View child = colorOptionsContainer.getChildAt(i);
            if (child instanceof MaterialCardView && child.getTag() != null) {
                MaterialCardView card = (MaterialCardView) child;
                int index = (int) card.getTag();
                if (index == selectedColorIndex) {
                    card.setStrokeWidth(getResources().getDimensionPixelSize(R.dimen.color_option_margin));
                } else {
                    card.setStrokeWidth(0);
                }
            }
        }
    }


    // getThemeRadioButtonId (Keep as is)
    private int getThemeRadioButtonId(int themeStyle) {
        switch (themeStyle) {
            case 1: return R.id.radio_light;
            case 2: return R.id.radio_dark;
            case 0:
            default: return R.id.radio_dynamic;
        }
    }

    // Remove updateFontSizePreview method
    /*
    private void updateFontSizePreview(int sizeOption) {
        // ... REMOVE ALL CONTENT ...
    }
    */

    // getSelectedThemeStyle (Keep as is)
    private int getSelectedThemeStyle() {
        int selectedId = themeStyleRadioGroup.getCheckedRadioButtonId();
        if (selectedId == R.id.radio_light) return 1;
        else if (selectedId == R.id.radio_dark) return 2;
        else return 0; // Dynamic
    }

    private void savePreferences() {
        // int fontSizeOption = fontSizeSeekBar.getProgress(); // REMOVE
        int themeStyleOption = getSelectedThemeStyle();

        SharedPreferences.Editor prefs = getSharedPreferences(PREFS_NAME, 0).edit();
        // prefs.putInt(PREF_PREFIX_KEY + mAppWidgetId + FONT_SIZE_KEY, fontSizeOption); // REMOVE
        prefs.putInt(PREF_PREFIX_KEY + mAppWidgetId + THEME_STYLE_KEY, themeStyleOption);

        // Save the selected color (Keep as is)
        if (customColor != -1) {
            prefs.putInt(PREF_PREFIX_KEY + mAppWidgetId + WIDGET_COLOR_KEY, customColor);
            // Log without font size
            Log.d(TAG, "Saving prefs for widget " + mAppWidgetId + ": Theme=" + themeStyleOption + ", Color=" + customColor);
        } else {
            prefs.remove(PREF_PREFIX_KEY + mAppWidgetId + WIDGET_COLOR_KEY);
            Log.w(TAG, "Saving prefs for widget " + mAppWidgetId + ": Color is -1 (unset), removing pref.");
        }

        prefs.apply();
    }

    // updateWidget (Keep as is)
    private void updateWidget() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        EventCountdownWidget.updateAppWidget(this, appWidgetManager, mAppWidgetId);
        Log.d(TAG, "Configuration update triggered for widget ID: " + mAppWidgetId);
    }

    // finishWithResult (Keep as is)
    private void finishWithResult() {
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, resultValue);
        Log.d(TAG, "Finishing configuration with RESULT_OK for widget ID: " + mAppWidgetId);
        finish();
    }

    // launchEventSelection (Keep as is)
    private void launchEventSelection() {
        Intent intent = new Intent(this, EventSelectionActivity.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        startActivityForResult(intent, 1); // Use request code 1
    }

    // generateUniqueRandomColor (Keep as is)
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

    // onActivityResult (Keep as is)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            WidgetUpdateReceiver.scheduleNextUpdate(this, mAppWidgetId, EventCountdownWidget.class.getName());
            Log.d(TAG, "Update scheduling triggered after event selection for widget ID: " + mAppWidgetId);
            finishWithResult();
        }
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