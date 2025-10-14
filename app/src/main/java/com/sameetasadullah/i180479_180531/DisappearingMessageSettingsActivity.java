package com.sameetasadullah.i180479_180531;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Settings Activity for configuring disappearing message preferences
 */
public class DisappearingMessageSettingsActivity extends AppCompatActivity {
    
    private DisappearingMessageSettings settings;
    private TextView defaultTimerText;
    private Switch showCountdownSwitch;
    private Switch autoEnableSwitch;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_disappearing_message_settings);
        
        // Enable back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Disappearing Messages");
        }
        
        settings = DisappearingMessageSettings.getInstance(this);
        initializeViews();
        loadCurrentSettings();
    }
    
    private void initializeViews() {
        defaultTimerText = findViewById(R.id.default_timer_text);
        showCountdownSwitch = findViewById(R.id.show_countdown_switch);
        autoEnableSwitch = findViewById(R.id.auto_enable_switch);
        
        // Set up click listeners
        findViewById(R.id.default_timer_layout).setOnClickListener(v -> showDefaultTimerDialog());
        
        showCountdownSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settings.setShowCountdown(isChecked);
            showToast("Countdown display " + (isChecked ? "enabled" : "disabled"));
        });
        
        autoEnableSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settings.setAutoEnableDisappearing(isChecked);
            showToast("Auto-enable disappearing messages " + (isChecked ? "enabled" : "disabled"));
        });
    }
    
    private void loadCurrentSettings() {
        // Load default timer
        long defaultTimer = settings.getDefaultTimer();
        String timerDisplayName = DisappearingMessageSettings.getTimerDisplayName(defaultTimer);
        String timerIcon = DisappearingMessageSettings.getTimerIcon(defaultTimer);
        defaultTimerText.setText(timerIcon + " " + timerDisplayName);
        
        // Load switches
        showCountdownSwitch.setChecked(settings.shouldShowCountdown());
        autoEnableSwitch.setChecked(settings.isAutoEnableDisappearing());
    }
    
    private void showDefaultTimerDialog() {
        DisappearingMessageSettings.TimerPreset[] presets = DisappearingMessageSettings.getTimerPresets();
        String[] presetNames = new String[presets.length];
        
        for (int i = 0; i < presets.length; i++) {
            presetNames[i] = presets[i].toString();
        }
        
        // Find current selection
        long currentTimer = settings.getDefaultTimer();
        int selectedIndex = 0;
        for (int i = 0; i < presets.length; i++) {
            if (presets[i].timeMs == currentTimer) {
                selectedIndex = i;
                break;
            }
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Default Disappearing Timer")
               .setSingleChoiceItems(presetNames, selectedIndex, (dialog, which) -> {
                   long newTimer = presets[which].timeMs;
                   settings.setDefaultTimer(newTimer);
                   
                   // Update display
                   String displayName = DisappearingMessageSettings.getTimerDisplayName(newTimer);
                   String icon = DisappearingMessageSettings.getTimerIcon(newTimer);
                   defaultTimerText.setText(icon + " " + displayName);
                   
                   showToast("Default timer set to " + displayName);
                   dialog.dismiss();
               })
               .setNegativeButton("Cancel", null)
               .show();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}