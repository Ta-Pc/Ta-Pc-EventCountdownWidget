package com.example.eventcountdownwidget;

import android.Manifest;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class MainActivity extends AppCompatActivity {
    private static final int CALENDAR_PERMISSION_REQUEST_CODE = 1001;

    private MaterialCardView permissionStatusCard;
    private TextView permissionStatusText;
    private MaterialButton grantPermissionButton;
    private TextView countdownWidgetsCount;
    private TextView listWidgetsCount;
    private MaterialButton manageCalendarsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Views
        permissionStatusCard = findViewById(R.id.permission_status_card);
        permissionStatusText = findViewById(R.id.permission_status_text);
        grantPermissionButton = findViewById(R.id.grant_permission_button);
        countdownWidgetsCount = findViewById(R.id.countdown_widgets_count);
        listWidgetsCount = findViewById(R.id.list_widgets_count);
        manageCalendarsButton = findViewById(R.id.manage_calendars_button);

        // Set up interactions
        grantPermissionButton.setOnClickListener(v -> requestCalendarPermission());
        manageCalendarsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CalendarSettingsActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    private void updateUI() {
        // 1. Check Permissions
        boolean hasPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR)
                == PackageManager.PERMISSION_GRANTED;

        if (hasPermission) {
            permissionStatusCard.setVisibility(View.GONE);
            manageCalendarsButton.setVisibility(View.VISIBLE);
        } else {
            permissionStatusCard.setVisibility(View.VISIBLE);
            permissionStatusText.setText(R.string.permission_denied_status);
            manageCalendarsButton.setVisibility(View.GONE); // Can't manage if no permission
        }

        // 2. Update Widget Counts
        updateWidgetCounts();
    }

    private void updateWidgetCounts() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);

        // Countdown Widgets
        int[] countdownIds = appWidgetManager.getAppWidgetIds(new ComponentName(this, EventCountdownWidget.class));
        countdownWidgetsCount.setText(getString(R.string.countdown_widgets, countdownIds.length));

        // List Widgets
        int[] listIds = appWidgetManager.getAppWidgetIds(new ComponentName(this, SimpleEventListWidgetProvider.class));
        listWidgetsCount.setText(getString(R.string.list_widgets, listIds.length));
    }

    private void requestCalendarPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_CALENDAR},
                CALENDAR_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CALENDAR_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.permission_granted_status, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Calendar permissions denied. Widget functionality limited.",
                        Toast.LENGTH_LONG).show();
            }
            updateUI(); // Refresh UI state
        }
    }
}