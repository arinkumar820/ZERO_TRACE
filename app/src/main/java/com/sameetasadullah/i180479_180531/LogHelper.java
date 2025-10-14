package com.sameetasadullah.i180479_180531;

import android.util.Log;

/**
 * Helper class to manage logging and filter out known non-critical warnings
 */
public class LogHelper {
    
    private static final String TAG = "LogHelper";
    
    // Known non-critical warning patterns to ignore
    private static final String[] IGNORED_WARNINGS = {
        "ItemStore: getItems RPC failed",
        "Firebase-Locale",
        "DynamiteModule",
        "GooglePlayServicesUtil"
    };
    
    /**
     * Check if a log message should be ignored based on known patterns
     */
    public static boolean shouldIgnoreWarning(String message) {
        if (message == null) return false;
        
        for (String pattern : IGNORED_WARNINGS) {
            if (message.contains(pattern)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Custom log method that filters out known non-critical warnings
     */
    public static void logInfo(String tag, String message) {
        if (!shouldIgnoreWarning(message)) {
            Log.i(tag, message);
        }
    }
    
    /**
     * Custom warning log that filters known non-critical warnings
     */
    public static void logWarning(String tag, String message) {
        if (!shouldIgnoreWarning(message)) {
            Log.w(tag, message);
        }
    }
    
    /**
     * Log application startup information
     */
    public static void logAppInfo() {
        Log.i(TAG, "=== Bisto Chat Application Info ===");
        Log.i(TAG, "Package: com.sameetasadullah.i180479_180531");
        Log.i(TAG, "Environment: Development");
        Log.i(TAG, "Note: Google Play Services warnings are expected in development");
        Log.i(TAG, "====================================");
    }
}