package com.sameetasadullah.i180479_180531;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * AutoLockReceiver - Broadcast receiver for automatic app locking
 * This receiver is triggered by AlarmManager to automatically lock the app
 * after a period of inactivity
 */
public class AutoLockReceiver extends BroadcastReceiver {
    
    private static final String TAG = "AutoLockReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Auto-lock timer triggered");
        
        try {
            // Check if auto-lock is still enabled
            SharedPreferences securityPrefs = context.getSharedPreferences("security_preferences", Context.MODE_PRIVATE);
            boolean autoLockEnabled = securityPrefs.getBoolean("auto_lock_enabled", false);
            
            if (autoLockEnabled) {
                // Lock the app
                lockApp(context, securityPrefs);
                Log.d(TAG, "App automatically locked due to inactivity");
            } else {
                Log.d(TAG, "Auto-lock disabled, skipping lock");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error in auto-lock receiver", e);
        }
    }
    
    private void lockApp(Context context, SharedPreferences securityPrefs) {
        // Mark app as locked
        securityPrefs.edit().putBoolean("app_locked", true).apply();
        
        // Launch lock screen activity
        Intent lockIntent = new Intent(context, AppLockActivity.class);
        lockIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        try {
            context.startActivity(lockIntent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start lock activity", e);
            // Fallback - just mark as locked, user will see lock screen on next app open
        }
    }
}