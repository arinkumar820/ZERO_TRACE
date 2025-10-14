package com.sameetasadullah.i180479_180531;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

/**
 * SnatchDetectionIntegration - Easy Integration Helper
 * 
 * This helper class provides simple methods to integrate snatch detection
 * into your existing Android activities with minimal code changes.
 * 
 * Key Features:
 * - One-line integration into existing activities
 * - Automatic background service management
 * - Security status monitoring
 * - Easy enable/disable functionality
 * 
 * INTEGRATION EXAMPLES:
 * 
 * 1. Add to your MainActivity onCreate():
 *    SnatchDetectionIntegration.enableSnatchDetection(this);
 * 
 * 2. Add to your LoginActivity onResume():
 *    SnatchDetectionIntegration.startBackgroundMonitoring(this);
 * 
 * 3. Add to your ChatActivity:
 *    SnatchDetectionIntegration.enableFullProtection(this);
 * 
 * Author: Security Integration Team
 * Version: 1.0
 */
public class SnatchDetectionIntegration {

    private static final String TAG = "SnatchIntegration";
    private static final String PREFS_NAME = "snatch_detection_prefs";
    
    /**
     * Enable basic snatch detection for an activity
     * This starts the SnatchDetectionActivity for foreground monitoring
     * 
     * @param activity The activity to protect
     */
    public static void enableSnatchDetection(Activity activity) {
        Log.i(TAG, "🔒 Enabling snatch detection for: " + activity.getClass().getSimpleName());
        
        try {
            Intent snatchIntent = new Intent(activity, SnatchDetectionActivity.class);
            snatchIntent.putExtra("parent_activity", activity.getClass().getSimpleName());
            activity.startActivity(snatchIntent);
            
            // Mark as protected
            markActivityAsProtected(activity);
            
            Log.i(TAG, "✅ Snatch detection enabled successfully");
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to enable snatch detection: " + e.getMessage());
            Toast.makeText(activity, "Security feature unavailable", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Start background monitoring service
     * This provides continuous protection even when app is in background
     * 
     * @param context Application context
     */
    public static void startBackgroundMonitoring(Context context) {
        Log.i(TAG, "🛡️ Starting background snatch monitoring...");
        
        try {
            SnatchDetectionService.startBackgroundMonitoring(context);
            
            // Update preferences
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("background_monitoring_enabled", true);
            editor.putLong("background_start_time", System.currentTimeMillis());
            editor.apply();
            
            Log.i(TAG, "✅ Background monitoring started successfully");
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to start background monitoring: " + e.getMessage());
        }
    }
    
    /**
     * Stop background monitoring service
     * 
     * @param context Application context
     */
    public static void stopBackgroundMonitoring(Context context) {
        Log.i(TAG, "🛑 Stopping background snatch monitoring...");
        
        try {
            SnatchDetectionService.stopBackgroundMonitoring(context);
            
            // Update preferences
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("background_monitoring_enabled", false);
            editor.putLong("background_stop_time", System.currentTimeMillis());
            editor.apply();
            
            Log.i(TAG, "✅ Background monitoring stopped");
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to stop background monitoring: " + e.getMessage());
        }
    }
    
    /**
     * Enable full protection (foreground + background)
     * This provides maximum security coverage
     * 
     * @param activity The activity to protect
     */
    public static void enableFullProtection(Activity activity) {
        Log.i(TAG, "🛡️ Enabling FULL protection for: " + activity.getClass().getSimpleName());
        
        // Start background service first
        startBackgroundMonitoring(activity);
        
        // Then enable foreground detection
        enableSnatchDetection(activity);
        
        // Show security confirmation
        Toast.makeText(activity, "🔒 Full security protection enabled", Toast.LENGTH_LONG).show();
        
        Log.i(TAG, "✅ Full protection enabled successfully");
    }
    
    /**
     * Check if snatch detection is currently active
     * 
     * @param context Application context
     * @return true if any form of snatch detection is active
     */
    public static boolean isSnatchDetectionActive(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean backgroundActive = prefs.getBoolean("background_monitoring_enabled", false);
        boolean activityProtected = prefs.getBoolean("activity_protected", false);
        
        boolean isActive = backgroundActive || activityProtected;
        Log.d(TAG, String.format("📊 Snatch detection status - Background: %s, Activity: %s, Overall: %s", 
            backgroundActive, activityProtected, isActive));
        
        return isActive;
    }
    
    /**
     * Get security status information
     * 
     * @param context Application context
     * @return Security status string for display
     */
    public static String getSecurityStatus(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences securityPrefs = context.getSharedPreferences("security_prefs", Context.MODE_PRIVATE);
        
        boolean backgroundActive = prefs.getBoolean("background_monitoring_enabled", false);
        boolean activityProtected = prefs.getBoolean("activity_protected", false);
        long lastCheck = securityPrefs.getLong("last_security_check", 0);
        
        StringBuilder status = new StringBuilder();
        
        if (backgroundActive && activityProtected) {
            status.append("🛡️ FULL PROTECTION ACTIVE");
        } else if (backgroundActive) {
            status.append("🔒 Background monitoring active");
        } else if (activityProtected) {
            status.append("⚡ Activity protection active");
        } else {
            status.append("❌ No protection active");
        }
        
        if (lastCheck > 0) {
            long timeSince = System.currentTimeMillis() - lastCheck;
            long minutesSince = timeSince / (1000 * 60);
            status.append(" (Last check: ").append(minutesSince).append("m ago)");
        }
        
        return status.toString();
    }
    
    /**
     * Mark an activity as protected in preferences
     */
    private static void markActivityAsProtected(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("activity_protected", true);
        editor.putString("protected_activity", activity.getClass().getSimpleName());
        editor.putLong("protection_start_time", System.currentTimeMillis());
        editor.apply();
    }
    
    /**
     * Configure snatch detection sensitivity
     * 
     * @param context Application context
     * @param sensitivity Sensitivity level: "low", "medium", "high"
     */
    public static void configureSensitivity(Context context, String sensitivity) {
        Log.i(TAG, "⚙️ Configuring sensitivity level: " + sensitivity);
        
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("sensitivity_level", sensitivity);
        
        // Set ultra-sensitive thresholds
        switch (sensitivity.toLowerCase()) {
            case "low":
                editor.putFloat("jolt_threshold", 6.0f);   // Ultra-low thresholds
                editor.putFloat("chaos_threshold", 1.5f);
                break;
            case "medium":
                editor.putFloat("jolt_threshold", 4.0f);   // Ultra-low thresholds
                editor.putFloat("chaos_threshold", 1.0f);
                break;
            case "high":
                editor.putFloat("jolt_threshold", 3.0f);   // ULTRA-sensitive
                editor.putFloat("chaos_threshold", 0.5f);  // ULTRA-sensitive
                break;
            default:
                Log.w(TAG, "Unknown sensitivity level, using medium");
                editor.putFloat("jolt_threshold", 4.0f);
                editor.putFloat("chaos_threshold", 1.0f);
        }
        
        editor.apply();
        Log.i(TAG, "✅ Sensitivity configured: " + sensitivity);
    }
    
    /**
     * Get the last security incident information
     * 
     * @param context Application context
     * @return Incident information or null if no incidents
     */
    public static String getLastSecurityIncident(Context context) {
        SharedPreferences securityPrefs = context.getSharedPreferences("security_prefs", Context.MODE_PRIVATE);
        
        long incidentTime = securityPrefs.getLong("last_security_incident", 0);
        if (incidentTime == 0) {
            return null;
        }
        
        float acceleration = securityPrefs.getFloat("incident_acceleration", 0f);
        float angularVelocity = securityPrefs.getFloat("incident_angular_velocity", 0f);
        String incidentType = securityPrefs.getString("incident_type", "UNKNOWN");
        
        long timeSince = System.currentTimeMillis() - incidentTime;
        long hoursSince = timeSince / (1000 * 60 * 60);
        
        return String.format("⚠️ Last incident: %s (%d hours ago)\nReadings: %.1f m/s², %.1f rad/s", 
            incidentType, hoursSince, acceleration, angularVelocity);
    }
    
    /**
     * Test snatch detection with simulated values (for development only)
     * DO NOT USE IN PRODUCTION
     */
    public static void testSnatchDetection(Activity activity) {
        if (BuildConfig.DEBUG) {
            Log.w(TAG, "🧪 TESTING: Simulating snatch detection...");
            Toast.makeText(activity, "🧪 Testing snatch detection", Toast.LENGTH_SHORT).show();
            
            // Simulate security response
            Intent testIntent = new Intent(activity, SnatchDetectionActivity.class);
            testIntent.putExtra("test_mode", true);
            testIntent.putExtra("simulated_acceleration", 30.0f);
            testIntent.putExtra("simulated_angular_velocity", 12.0f);
            activity.startActivity(testIntent);
        } else {
            Log.w(TAG, "⚠️ Test function called in production build - ignoring");
        }
    }
    
    // === INTEGRATION EXAMPLES FOR COPY-PASTE ===
    
    /**
     * EXAMPLE 1: Basic Integration
     * Add this to your MainActivity onCreate() method:
     * 
     * @Override
     * protected void onCreate(Bundle savedInstanceState) {
     *     super.onCreate(savedInstanceState);
     *     setContentView(R.layout.activity_main);
     *     
     *     // Enable snatch detection
     *     SnatchDetectionIntegration.enableSnatchDetection(this);
     * }
     */
    
    /**
     * EXAMPLE 2: Login Activity Integration
     * Add this to your LoginActivity:
     * 
     * @Override
     * protected void onResume() {
     *     super.onResume();
     *     
     *     // Start background monitoring after login
     *     SnatchDetectionIntegration.startBackgroundMonitoring(this);
     * }
     * 
     * @Override
     * protected void onPause() {
     *     super.onPause();
     *     
     *     // Optional: Stop monitoring when leaving login
     *     // SnatchDetectionIntegration.stopBackgroundMonitoring(this);
     * }
     */
    
    /**
     * EXAMPLE 3: Chat Activity Full Protection
     * Add this to your ChatActivity:
     * 
     * @Override
     * protected void onCreate(Bundle savedInstanceState) {
     *     super.onCreate(savedInstanceState);
     *     setContentView(R.layout.activity_chat);
     *     
     *     // Enable full protection for sensitive chat data
     *     SnatchDetectionIntegration.enableFullProtection(this);
     *     
     *     // Show security status to user
     *     String status = SnatchDetectionIntegration.getSecurityStatus(this);
     *     Log.i("ChatSecurity", status);
     * }
     */
    
    /**
     * EXAMPLE 4: Settings Integration
     * Add security settings to your preferences:
     * 
     * private void setupSecuritySettings() {
     *     // Check current status
     *     boolean isActive = SnatchDetectionIntegration.isSnatchDetectionActive(this);
     *     
     *     // Configure sensitivity
     *     SnatchDetectionIntegration.configureSensitivity(this, "medium");
     *     
     *     // Show security status
     *     String status = SnatchDetectionIntegration.getSecurityStatus(this);
     *     Toast.makeText(this, status, Toast.LENGTH_LONG).show();
     *     
     *     // Check for recent incidents
     *     String incident = SnatchDetectionIntegration.getLastSecurityIncident(this);
     *     if (incident != null) {
     *         Log.w("Security", incident);
     *     }
     * }
     */
}