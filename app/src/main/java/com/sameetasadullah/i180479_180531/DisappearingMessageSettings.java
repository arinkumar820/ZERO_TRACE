package com.sameetasadullah.i180479_180531;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Manages settings and presets for disappearing messages
 */
public class DisappearingMessageSettings {
    
    private static final String PREFS_NAME = "disappearing_message_settings";
    private static final String KEY_DEFAULT_TIMER = "default_timer";
    private static final String KEY_SHOW_COUNTDOWN = "show_countdown";
    private static final String KEY_AUTO_ENABLE = "auto_enable";
    
    // Time presets in milliseconds
    public static final long TIMER_30_SECONDS = 30 * 1000;
    public static final long TIMER_45_SECONDS = 45 * 1000;
    public static final long TIMER_1_MINUTE = 60 * 1000;
    public static final long TIMER_3_MINUTES = 3 * 60 * 1000;
    public static final long TIMER_5_MINUTES = 5 * 60 * 1000;
    
    // Default timer (1 minute by default - user can change in settings)
    public static final long DEFAULT_TIMER = TIMER_1_MINUTE;
    
    private SharedPreferences prefs;
    private static DisappearingMessageSettings instance;
    
    private DisappearingMessageSettings(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    public static synchronized DisappearingMessageSettings getInstance(Context context) {
        if (instance == null) {
            instance = new DisappearingMessageSettings(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Get the default timer setting for new messages
     */
    public long getDefaultTimer() {
        return prefs.getLong(KEY_DEFAULT_TIMER, DEFAULT_TIMER);
    }
    
    /**
     * Set the default timer for new messages
     */
    public void setDefaultTimer(long timerMs) {
        prefs.edit().putLong(KEY_DEFAULT_TIMER, timerMs).apply();
    }
    
    /**
     * Check if countdown should be shown for disappearing messages
     */
    public boolean shouldShowCountdown() {
        return prefs.getBoolean(KEY_SHOW_COUNTDOWN, true);
    }
    
    /**
     * Set whether to show countdown for disappearing messages
     */
    public void setShowCountdown(boolean showCountdown) {
        prefs.edit().putBoolean(KEY_SHOW_COUNTDOWN, showCountdown).apply();
    }
    
    /**
     * Check if disappearing messages should be auto-enabled for all messages
     */
    public boolean isAutoEnableDisappearing() {
        return prefs.getBoolean(KEY_AUTO_ENABLE, false);
    }
    
    /**
     * Set whether to auto-enable disappearing messages for all messages
     */
    public void setAutoEnableDisappearing(boolean autoEnable) {
        prefs.edit().putBoolean(KEY_AUTO_ENABLE, autoEnable).apply();
    }
    
    /**
     * Get all available timer presets with display names
     */
    public static TimerPreset[] getTimerPresets() {
        return new TimerPreset[] {
            new TimerPreset("30 seconds", TIMER_30_SECONDS, "🕐"),
            new TimerPreset("45 seconds", TIMER_45_SECONDS, "⏱️"),
            new TimerPreset("1 minute", TIMER_1_MINUTE, "⏰"),
            new TimerPreset("3 minutes", TIMER_3_MINUTES, "🕒"),
            new TimerPreset("5 minutes", TIMER_5_MINUTES, "🕔")
        };
    }
    
    /**
     * Get display name for a timer value
     */
    public static String getTimerDisplayName(long timerMs) {
        for (TimerPreset preset : getTimerPresets()) {
            if (preset.timeMs == timerMs) {
                return preset.displayName;
            }
        }
        
        
        // Custom timer - format it
        if (timerMs < 60000) {
            return (timerMs / 1000) + " seconds";
        } else if (timerMs < 3600000) {
            return (timerMs / 60000) + " minutes";
        } else if (timerMs < 86400000) {
            return (timerMs / 3600000) + " hours";
        } else {
            return (timerMs / 86400000) + " days";
        }
    }
    
    /**
     * Get emoji icon for a timer value
     */
    public static String getTimerIcon(long timerMs) {
        for (TimerPreset preset : getTimerPresets()) {
            if (preset.timeMs == timerMs) {
                return preset.icon;
            }
        }
        return "⏲️"; // Default timer icon
    }
    
    /**
     * Format remaining time for display
     */
    public static String formatRemainingTime(long remainingMs) {
        if (remainingMs <= 0) {
            return "Expired";
        }
        
        long seconds = remainingMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        seconds = seconds % 60;
        minutes = minutes % 60;
        
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%d:%02d", minutes, seconds);
        }
    }
    
    /**
     * Check if a timer value represents a valid disappearing time
     */
    public static boolean isValidDisappearingTime(long timerMs) {
        return timerMs > 0;
    }
    
    /**
     * Class to represent timer presets
     */
    public static class TimerPreset {
        public final String displayName;
        public final long timeMs;
        public final String icon;
        
        public TimerPreset(String displayName, long timeMs, String icon) {
            this.displayName = displayName;
            this.timeMs = timeMs;
            this.icon = icon;
        }
        
        @Override
        public String toString() {
            return icon + " " + displayName;
        }
    }
}