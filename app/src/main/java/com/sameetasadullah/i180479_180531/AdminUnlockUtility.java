package com.sameetasadullah.i180479_180531;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

/**
 * AdminUnlockUtility - Emergency unlock utilities for PIN-protected app
 * 
 * This class provides multiple ways to unlock the app when permanently locked:
 * 1. Direct unlock via SharedPreferences reset
 * 2. Master PIN functionality
 * 3. Emergency codes
 * 
 * Usage for developers/administrators when users get locked out
 */
public class AdminUnlockUtility {
    
    private static final String TAG = "AdminUnlock";
    private static final String PREFS_NAME = "pin_preferences";
    private static final String PIN_KEY = "app_pin";
    private static final String ATTEMPTS_KEY = "pin_attempts";
    private static final String LOCKED_KEY = "app_locked";
    private static final String MASTER_PIN_KEY = "master_pin";
    
    // Master admin PIN (set this to your desired admin PIN)
    private static final String DEFAULT_MASTER_PIN = "999999";
    private static final String EMERGENCY_CODE = "112233";
    
    /**
     * Emergency unlock - completely resets the app lock state
     * Use this method when user is completely locked out
     */
    public static boolean emergencyUnlock(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            
            // Reset all lock-related preferences
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(LOCKED_KEY, false);      // Unlock the app
            editor.putInt(ATTEMPTS_KEY, 0);            // Reset attempts
            editor.putString(PIN_KEY, "123456");       // Reset to default PIN
            editor.apply();
            
            Log.i(TAG, "🔓 Emergency unlock performed - app is now accessible");
            
            // Show confirmation if possible
            try {
                Toast.makeText(context, "🔓 Emergency unlock successful!\nDefault PIN: 123456", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                // Toast might fail in some contexts, that's OK
            }
            
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Emergency unlock failed", e);
            return false;
        }
    }
    
    /**
     * Set up master PIN that always works (even when app is locked)
     * Call this once to set up the master PIN system
     */
    public static void setupMasterPin(Context context, String masterPin) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(MASTER_PIN_KEY, masterPin).apply();
        Log.i(TAG, "✅ Master PIN configured");
    }
    
    /**
     * Check if entered PIN is the master PIN
     * Use this in PinAuthenticationActivity to allow master PIN access
     */
    public static boolean isMasterPin(Context context, String enteredPin) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String masterPin = prefs.getString(MASTER_PIN_KEY, DEFAULT_MASTER_PIN);
        
        boolean isMaster = enteredPin.equals(masterPin) || enteredPin.equals(EMERGENCY_CODE);
        
        if (isMaster) {
            Log.w(TAG, "🔑 Master PIN used - granting emergency access");
        }
        
        return isMaster;
    }
    
    /**
     * Reset attempts counter without unlocking
     * Useful when you want to give user more chances
     */
    public static void resetAttempts(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(ATTEMPTS_KEY, 0).apply();
        Log.i(TAG, "🔄 PIN attempts counter reset");
    }
    
    /**
     * Check current lock status
     */
    public static boolean isAppLocked(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(LOCKED_KEY, false);
    }
    
    /**
     * Get remaining attempts
     */
    public static int getRemainingAttempts(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int usedAttempts = prefs.getInt(ATTEMPTS_KEY, 0);
        return Math.max(0, 2 - usedAttempts); // MAX_ATTEMPTS = 2
    }
    
    /**
     * Set a new PIN (admin function)
     */
    public static void setNewPin(Context context, String newPin) {
        if (newPin != null && newPin.length() == 6) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(PIN_KEY, newPin).apply();
            Log.i(TAG, "🔐 New PIN set by administrator");
        }
    }
    
    /**
     * Complete reset - back to first run state
     * This will reset everything including the first run flag
     */
    public static void factoryReset(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply(); // Clear all preferences
        Log.w(TAG, "🏭 Factory reset performed - app will show first run screen");
    }
    
    /**
     * Debug info - get current status
     */
    public static String getDebugInfo(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        return String.format(
            "📊 Debug Info:\n" +
            "- Locked: %s\n" +
            "- Attempts: %d/2\n" +
            "- Current PIN: %s\n" +
            "- Master PIN set: %s\n" +
            "- Remaining attempts: %d",
            prefs.getBoolean(LOCKED_KEY, false),
            prefs.getInt(ATTEMPTS_KEY, 0),
            prefs.getString(PIN_KEY, "default"),
            prefs.contains(MASTER_PIN_KEY),
            getRemainingAttempts(context)
        );
    }
}