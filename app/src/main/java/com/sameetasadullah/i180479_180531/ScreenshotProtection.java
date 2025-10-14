package com.sameetasadullah.i180479_180531;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

/**
 * Utility class for implementing screenshot and screen recording protection
 * Provides easy methods to enable/disable screenshot blocking across activities
 */
public class ScreenshotProtection {
    
    private static final String TAG = "ScreenshotProtection";
    private static final boolean SHOW_DEBUG_MESSAGES = true; // Set to false for production
    
    /**
     * Enable screenshot protection for an activity
     * This will make screenshots appear black and prevent screen recording
     * 
     * @param activity The activity to protect
     */
    public static void enableProtection(Activity activity) {
        if (activity == null) {
            Log.w(TAG, "Cannot enable protection: Activity is null");
            return;
        }
        
        try {
            activity.getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_SECURE, 
                WindowManager.LayoutParams.FLAG_SECURE
            );
            
            Log.i(TAG, "🛡️ Screenshot protection enabled for " + activity.getClass().getSimpleName());
            
            // Optional: Show user notification (can be disabled in production)
            if (SHOW_DEBUG_MESSAGES) {
                Toast.makeText(activity, "🛡️ Privacy mode: Screenshots disabled", 
                    Toast.LENGTH_SHORT).show();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable screenshot protection", e);
        }
    }
    
    /**
     * Disable screenshot protection for an activity
     * This allows normal screenshots again
     * 
     * @param activity The activity to unprotect
     */
    public static void disableProtection(Activity activity) {
        if (activity == null) {
            Log.w(TAG, "Cannot disable protection: Activity is null");
            return;
        }
        
        try {
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
            Log.i(TAG, "📸 Screenshot protection disabled for " + activity.getClass().getSimpleName());
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to disable screenshot protection", e);
        }
    }
    
    /**
     * Check if screenshot protection is currently active
     * 
     * @param activity The activity to check
     * @return true if protection is active, false otherwise
     */
    public static boolean isProtectionActive(Activity activity) {
        if (activity == null) return false;
        
        try {
            int flags = activity.getWindow().getAttributes().flags;
            return (flags & WindowManager.LayoutParams.FLAG_SECURE) != 0;
        } catch (Exception e) {
            Log.e(TAG, "Failed to check protection status", e);
            return false;
        }
    }
    
    /**
     * Show a user-friendly message about screenshot protection
     * 
     * @param context Context for showing the message
     */
    public static void showProtectionInfo(Context context) {
        if (context != null) {
            Toast.makeText(context, 
                "🛡️ Privacy Protected: Screenshots and screen recording are disabled in this app for your security", 
                Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * Apply screenshot protection to all sensitive activities
     * Call this method in activities that handle private content
     * 
     * @param activity The activity to protect
     * @param showUserInfo Whether to show info message to user
     */
    public static void protectPrivateContent(Activity activity, boolean showUserInfo) {
        enableProtection(activity);
        
        if (showUserInfo) {
            showProtectionInfo(activity);
        }
        
        Log.i(TAG, "🔒 Private content protection applied to " + 
            (activity != null ? activity.getClass().getSimpleName() : "unknown activity"));
    }
    
    /**
     * Get device information for debugging screenshot protection issues
     * 
     * @return String with device info
     */
    public static String getDeviceDebugInfo() {
        return "Device: " + Build.MANUFACTURER + " " + Build.MODEL + 
               ", SDK: " + Build.VERSION.SDK_INT + 
               ", Release: " + Build.VERSION.RELEASE;
    }
}