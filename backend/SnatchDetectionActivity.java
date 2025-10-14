package com.sameetasadullah.i180479_180531;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;

/**
 * SnatchDetectionActivity - Advanced Anti-Theft Security Feature
 * 
 * This activity implements a sophisticated snatch detection system that uses
 * the device's accelerometer and gyroscope sensors to detect unauthorized
 * device snatching attempts and automatically destroys sensitive data.
 * 
 * Detection Algorithm:
 * 1. Jolt Detection: Monitors accelerometer for sudden high-force movements
 * 2. Chaos Detection: Monitors gyroscope for erratic rotational movements
 * 3. Data Destruction: Clears all app data and logs out user when snatch detected
 * 
 * Author: Security Module
 * Version: 1.0
 */
public class SnatchDetectionActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "SnatchDetection";
    
    // === SENSOR CONFIGURATION ===
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private boolean sensorsAvailable = false;
    
    // === ULTRA-SENSITIVE DETECTION THRESHOLDS ===
    private static final float JOLT_THRESHOLD = 4.0f;           // m/s² - ULTRA SENSITIVE!
    private static final float CHAOS_THRESHOLD = 1.0f;          // rad/s - ULTRA SENSITIVE!
    private static final long CHAOS_MONITORING_DURATION = 4000; // 4 seconds - even more time
    private static final int SENSOR_DELAY = SensorManager.SENSOR_DELAY_FASTEST; // Maximum sensitivity
    
    // Alternative detection methods (EVEN MORE SENSITIVE)
    private static final float SINGLE_AXIS_THRESHOLD = 8.0f;    // Individual axis threshold - lower
    private static final float GRAVITY_CHANGE_THRESHOLD = 3.0f;  // Change in gravity - much lower
    private static final float EMERGENCY_AXIS_THRESHOLD = 10.0f; // Emergency detection - lower
    private static final float TINY_MOVEMENT_THRESHOLD = 0.5f;   // Detect tiny movements
    
    // === DETECTION STATE VARIABLES ===
    private boolean joltDetected = false;
    private boolean chaosMonitoring = false;
    private long joltDetectionTime = 0;
    private Handler chaosHandler;
    private Runnable chaosTimeoutRunnable;
    
    // === DATA TRACKING ===
    private float lastAcceleration = 0f;
    private float lastAngularVelocity = 0f;
    private int consecutiveHighReadings = 0;
    private static final int REQUIRED_CONSECUTIVE_READINGS = 1; // Immediate detection
    
    // === DEBUGGING ===
    private boolean debugMode = true; // Set to false for production
    private long lastLogTime = 0;
    private static final long LOG_INTERVAL = 1000; // Log every 1 second
    
    // === SYSTEM COMPONENTS ===
    private Vibrator vibrator;
    private SharedPreferences securityPrefs;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.i(TAG, "🔒 SnatchDetection: Security module initializing...");
        
        // Initialize system components
        initializeComponents();
        
        // Setup sensors
        initializeSensors();
        
        // Setup security preferences
        setupSecurityPreferences();
        
        // Start monitoring immediately
        startSnatchDetection();
        
        Log.i(TAG, "🛡️ SnatchDetection: Active monitoring started");
    }
    
    /**
     * Initialize core system components
     */
    private void initializeComponents() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        chaosHandler = new Handler(Looper.getMainLooper());
        
        securityPrefs = getSharedPreferences("security_prefs", Context.MODE_PRIVATE);
    }
    
    /**
     * Initialize and verify sensor availability
     */
    private void initializeSensors() {
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            
            sensorsAvailable = (accelerometer != null && gyroscope != null);
            
            if (sensorsAvailable) {
                Log.i(TAG, "✅ Sensors available: Accelerometer + Gyroscope");
            } else {
                Log.e(TAG, "❌ Required sensors not available on this device");
                Toast.makeText(this, "Security sensors not available", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    /**
     * Setup security preferences and configuration
     */
    private void setupSecurityPreferences() {
        SharedPreferences.Editor editor = securityPrefs.edit();
        editor.putBoolean("snatch_detection_active", true);
        editor.putLong("last_security_check", System.currentTimeMillis());
        editor.apply();
    }
    
    /**
     * Start snatch detection monitoring
     */
    private void startSnatchDetection() {
        if (!sensorsAvailable) {
            Log.w(TAG, "⚠️ Cannot start detection - sensors unavailable");
            return;
        }
        
        // Register sensor listeners with maximum sensitivity
        boolean accelRegistered = sensorManager.registerListener(
            this, accelerometer, SENSOR_DELAY);
        boolean gyroRegistered = sensorManager.registerListener(
            this, gyroscope, SENSOR_DELAY);
        
        if (accelRegistered && gyroRegistered) {
            Log.i(TAG, "🎯 Sensor listeners registered successfully");
        } else {
            Log.e(TAG, "❌ Failed to register sensor listeners");
        }
    }
    
    /**
     * Stop snatch detection monitoring
     */
    private void stopSnatchDetection() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
            Log.i(TAG, "🔇 Sensor monitoring stopped");
        }
        
        if (chaosTimeoutRunnable != null) {
            chaosHandler.removeCallbacks(chaosTimeoutRunnable);
        }
    }
    
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            handleAccelerometerData(event);
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            handleGyroscopeData(event);
        }
    }
    
    /**
     * Handle accelerometer data for jolt detection (AGGRESSIVE VERSION)
     */
    private void handleAccelerometerData(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        
        // Method 1: Total force magnitude (removing gravity)
        float totalForce = (float) Math.sqrt(x*x + y*y + z*z);
        float acceleration = Math.abs(totalForce - SensorManager.GRAVITY_EARTH);
        
        // Method 2: Check individual axis changes (more sensitive)
        float maxSingleAxis = Math.max(Math.max(Math.abs(x), Math.abs(y)), Math.abs(z));
        float singleAxisAccel = maxSingleAxis > SensorManager.GRAVITY_EARTH ? 
            maxSingleAxis - SensorManager.GRAVITY_EARTH : 0;
        
        // Method 3: Sudden orientation change detection
        float gravityChange = Math.abs(totalForce - SensorManager.GRAVITY_EARTH);
        
        // Use the highest of all detection methods
        float finalAcceleration = Math.max(Math.max(acceleration, singleAxisAccel), gravityChange);
        lastAcceleration = finalAcceleration;
        
        // Always log for debugging (more frequent)
        if (debugMode && System.currentTimeMillis() - lastLogTime > 500) { // Every 0.5 seconds
            Log.d(TAG, String.format("📊 ACCEL: %.2f m/s² (T:%.1f) | Raw: X=%.1f Y=%.1f Z=%.1f | Total=%.1f", 
                finalAcceleration, JOLT_THRESHOLD, x, y, z, totalForce));
            lastLogTime = System.currentTimeMillis();
        }
        
        // AGGRESSIVE JOLT DETECTION - Multiple trigger conditions
        boolean joltTriggered = false;
        String triggerReason = "";
        
        // Trigger 1: Normal acceleration threshold
        if (finalAcceleration > JOLT_THRESHOLD) {
            joltTriggered = true;
            triggerReason = "Total acceleration";
        }
        
        // Trigger 2: Single axis spike (very sensitive)
        if (singleAxisAccel > SINGLE_AXIS_THRESHOLD) {
            joltTriggered = true;
            triggerReason = "Single axis spike";
        }
        
        // Trigger 3: Sudden gravity change (phone flipping)
        if (gravityChange > GRAVITY_CHANGE_THRESHOLD) {
            joltTriggered = true;
            triggerReason = "Gravity change";
        }
        
        // Trigger 4: Any axis above threshold (emergency detection)
        if (Math.abs(x) > EMERGENCY_AXIS_THRESHOLD || Math.abs(y) > EMERGENCY_AXIS_THRESHOLD || Math.abs(z) > EMERGENCY_AXIS_THRESHOLD) {
            joltTriggered = true;
            triggerReason = "Emergency axis threshold";
        }
        
        // Trigger 5: Detect any significant movement (ultra-sensitive)
        if (Math.abs(x) > 6.0f || Math.abs(y) > 6.0f || Math.abs(z) > 6.0f) {
            joltTriggered = true;
            triggerReason = "Significant movement detected";
        }
        
        // Trigger 6: Combined axis movement
        float totalAxisMovement = Math.abs(x) + Math.abs(y) + Math.abs(z);
        if (totalAxisMovement > 18.0f) {
            joltTriggered = true;
            triggerReason = "Combined axis movement";
        }
        
        if (joltTriggered && !joltDetected) {
            consecutiveHighReadings++;
            
            Log.w(TAG, String.format("⚡ JOLT TRIGGER: %.2f m/s² via %s (Reading #%d/%d)", 
                finalAcceleration, triggerReason, consecutiveHighReadings, REQUIRED_CONSECUTIVE_READINGS));
            
            // Reduced consecutive readings requirement for real devices
            if (consecutiveHighReadings >= REQUIRED_CONSECUTIVE_READINGS) {
                detectJolt(finalAcceleration, triggerReason);
            }
        } else if (!joltTriggered) {
            if (consecutiveHighReadings > 0) {
                Log.d(TAG, "🔄 Resetting consecutive readings (no triggers active)");
            }
            consecutiveHighReadings = 0;
        }
    }
    
    /**
     * Handle gyroscope data for chaos detection (AGGRESSIVE VERSION)
     */
    private void handleGyroscopeData(SensorEvent event) {
        if (!chaosMonitoring) return;
        
        float x = event.values[0]; // rad/s around x-axis
        float y = event.values[1]; // rad/s around y-axis  
        float z = event.values[2]; // rad/s around z-axis
        
        // Method 1: Total angular velocity magnitude
        float angularVelocity = (float) Math.sqrt(x*x + y*y + z*z);
        
        // Method 2: Individual axis rotation (more sensitive)
        float maxAxisRotation = Math.max(Math.max(Math.abs(x), Math.abs(y)), Math.abs(z));
        
        // Method 3: Combined axis movement (any two axes moving)
        float combinedMovement = Math.abs(x) + Math.abs(y) + Math.abs(z);
        
        lastAngularVelocity = Math.max(angularVelocity, maxAxisRotation);
        
        // Always log for debugging
        if (debugMode) {
            Log.d(TAG, String.format("🔄 GYRO: %.2f rad/s (T:%.1f) | X=%.1f Y=%.1f Z=%.1f | Max=%.2f Combined=%.2f", 
                angularVelocity, CHAOS_THRESHOLD, x, y, z, maxAxisRotation, combinedMovement));
        }
        
        // AGGRESSIVE CHAOS DETECTION - Multiple trigger conditions
        boolean chaosTriggered = false;
        String chaosReason = "";
        
        // Trigger 1: Normal angular velocity threshold (lowered)
        if (angularVelocity > CHAOS_THRESHOLD) {
            chaosTriggered = true;
            chaosReason = "Total angular velocity";
        }
        
        // Trigger 2: Single axis rotation (very sensitive)
        if (maxAxisRotation > CHAOS_THRESHOLD * 0.8f) { // Even lower threshold
            chaosTriggered = true;
            chaosReason = "Single axis rotation";
        }
        
        // Trigger 3: Combined movement (multiple axes)
        if (combinedMovement > CHAOS_THRESHOLD * 1.5f) {
            chaosTriggered = true;
            chaosReason = "Multi-axis movement";
        }
        
        // Trigger 4: Any single axis above emergency threshold (ultra-sensitive)
        if (Math.abs(x) > 0.8f || Math.abs(y) > 0.8f || Math.abs(z) > 0.8f) {
            chaosTriggered = true;
            chaosReason = "Ultra-sensitive rotation";
        }
        
        // Trigger 5: Tiny movements (super sensitive)
        if (Math.abs(x) > TINY_MOVEMENT_THRESHOLD || Math.abs(y) > TINY_MOVEMENT_THRESHOLD || Math.abs(z) > TINY_MOVEMENT_THRESHOLD) {
            chaosTriggered = true;
            chaosReason = "Tiny movement detected";
        }
        
        // Trigger 6: Any two axes moving together (very sensitive)
        if ((Math.abs(x) > 0.3f && Math.abs(y) > 0.3f) || 
            (Math.abs(y) > 0.3f && Math.abs(z) > 0.3f) || 
            (Math.abs(x) > 0.3f && Math.abs(z) > 0.3f)) {
            chaosTriggered = true;
            chaosReason = "Multi-axis micro-movement";
        }
        
        // Trigger 7: Combined micro-movements
        if (combinedMovement > 0.8f) {
            chaosTriggered = true;
            chaosReason = "Micro combined movement";
        }
        
        if (chaosTriggered) {
            Log.w(TAG, String.format("🌪️ CHAOS DETECTED: %.2f rad/s via %s", 
                Math.max(angularVelocity, maxAxisRotation), chaosReason));
            detectChaos(Math.max(angularVelocity, maxAxisRotation), chaosReason);
        }
    }
    
    /**
     * Process jolt detection and start chaos monitoring
     */
    private void detectJolt(float acceleration, String triggerReason) {
        joltDetected = true;
        joltDetectionTime = System.currentTimeMillis();
        chaosMonitoring = true;
        consecutiveHighReadings = 0;
        
        Log.w(TAG, String.format("🚨 JOLT DETECTED! Force: %.2f m/s² via %s - Starting chaos monitoring...", 
            acceleration, triggerReason));
        
        // Provide haptic feedback
        if (vibrator != null) {
            vibrator.vibrate(100); // Short vibration
        }
        
        // Start chaos monitoring timeout
        chaosTimeoutRunnable = new Runnable() {
            @Override
            public void run() {
                if (chaosMonitoring && joltDetected) {
                    Log.i(TAG, "⏰ Chaos monitoring timeout - No snatch detected, resetting...");
                    resetDetectionState();
                }
            }
        };
        
        chaosHandler.postDelayed(chaosTimeoutRunnable, CHAOS_MONITORING_DURATION);
    }
    
    /**
     * Process chaos detection and trigger security response
     */
    private void detectChaos(float angularVelocity, String chaosReason) {
        if (!joltDetected || !chaosMonitoring) return;
        
        long timeSinceJolt = System.currentTimeMillis() - joltDetectionTime;
        
        Log.e(TAG, String.format("🔥 SNATCH DETECTED! Jolt+Chaos confirmed in %d ms", timeSinceJolt));
        Log.e(TAG, String.format("📊 Final readings - Acceleration: %.2f m/s², Angular: %.2f rad/s via %s", 
            lastAcceleration, angularVelocity, chaosReason));
        
        // Trigger immediate security response
        triggerSecurityResponse();
    }
    
    /**
     * Execute security response - destroy data and logout
     */
    private void triggerSecurityResponse() {
        Log.e(TAG, "🚨🚨 SECURITY BREACH DETECTED - INITIATING DATA DESTRUCTION 🚨🚨");
        
        // Stop all sensor monitoring immediately
        stopSnatchDetection();
        
        // Strong haptic feedback
        if (vibrator != null) {
            long[] vibrationPattern = {0, 200, 100, 200, 100, 200};
            vibrator.vibrate(vibrationPattern, -1);
        }
        
        // Show critical security alert
        Toast.makeText(this, "🔒 SECURITY BREACH - DATA CLEARED", Toast.LENGTH_LONG).show();
        
        // Execute data destruction
        clearAllAppData();
        
        // Force logout
        performSecurityLogout();
        
        // Log security incident
        logSecurityIncident();
        
        // Close application
        finishAndRemoveTask();
    }
    
    /**
     * Clear all application data and cache
     */
    private void clearAllAppData() {
        Log.e(TAG, "🗑️ Clearing all application data...");
        
        try {
            // Clear SharedPreferences
            SharedPreferences prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            editor.apply();
            
            // Clear login data
            SharedPreferences loginPrefs = getSharedPreferences("login_data", Context.MODE_PRIVATE);
            loginPrefs.edit().clear().apply();
            
            // Clear chat data
            SharedPreferences chatPrefs = getSharedPreferences("chat_data", Context.MODE_PRIVATE);
            chatPrefs.edit().clear().apply();
            
            // Clear app cache
            clearApplicationCache();
            
            // Clear internal storage files
            clearInternalFiles();
            
            Log.i(TAG, "✅ Application data cleared successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error clearing app data: " + e.getMessage());
        }
    }
    
    /**
     * Clear application cache directory
     */
    private void clearApplicationCache() {
        try {
            File cacheDir = getCacheDir();
            if (cacheDir != null && cacheDir.isDirectory()) {
                deleteDirectory(cacheDir);
                Log.i(TAG, "📁 Cache directory cleared");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error clearing cache: " + e.getMessage());
        }
    }
    
    /**
     * Clear internal files directory
     */
    private void clearInternalFiles() {
        try {
            File filesDir = getFilesDir();
            if (filesDir != null && filesDir.isDirectory()) {
                File[] files = filesDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.delete()) {
                            Log.d(TAG, "Deleted file: " + file.getName());
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error clearing files: " + e.getMessage());
        }
    }
    
    /**
     * Recursively delete directory contents
     */
    private boolean deleteDirectory(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    boolean success = deleteDirectory(new File(dir, child));
                    if (!success) {
                        return false;
                    }
                }
            }
            return dir.delete();
        } else if (dir != null && dir.isFile()) {
            return dir.delete();
        }
        return false;
    }
    
    /**
     * Perform security logout and clear session
     */
    private void performSecurityLogout() {
        Log.w(TAG, "🚪 Performing security logout...");
        
        // Clear authentication tokens
        SharedPreferences authPrefs = getSharedPreferences("auth_tokens", Context.MODE_PRIVATE);
        authPrefs.edit().clear().apply();
        
        // Clear user session
        SharedPreferences sessionPrefs = getSharedPreferences("user_session", Context.MODE_PRIVATE);
        sessionPrefs.edit().clear().apply();
        
        // Redirect to login screen
        Intent loginIntent = new Intent(this, LoginActivity.class);
        loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        loginIntent.putExtra("security_logout", true);
        startActivity(loginIntent);
        
        Log.i(TAG, "✅ Security logout completed");
    }
    
    /**
     * Log security incident for analysis
     */
    private void logSecurityIncident() {
        SharedPreferences.Editor editor = securityPrefs.edit();
        editor.putLong("last_security_incident", System.currentTimeMillis());
        editor.putFloat("incident_acceleration", lastAcceleration);
        editor.putFloat("incident_angular_velocity", lastAngularVelocity);
        editor.putString("incident_type", "SNATCH_DETECTED");
        editor.apply();
        
        Log.w(TAG, "📝 Security incident logged");
    }
    
    /**
     * Reset detection state after timeout
     */
    private void resetDetectionState() {
        joltDetected = false;
        chaosMonitoring = false;
        consecutiveHighReadings = 0;
        joltDetectionTime = 0;
        
        if (chaosTimeoutRunnable != null) {
            chaosHandler.removeCallbacks(chaosTimeoutRunnable);
        }
        
        Log.d(TAG, "🔄 Detection state reset");
    }
    
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Log accuracy changes for debugging
        String sensorName = (sensor.getType() == Sensor.TYPE_ACCELEROMETER) ? "Accelerometer" : "Gyroscope";
        Log.d(TAG, String.format("📊 %s accuracy changed: %d", sensorName, accuracy));
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "🔄 Activity resumed - Restarting sensor monitoring");
        startSnatchDetection();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "⏸️ Activity paused - Stopping sensor monitoring");
        stopSnatchDetection();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "🔚 Activity destroyed - Cleaning up resources");
        stopSnatchDetection();
        
        if (chaosHandler != null && chaosTimeoutRunnable != null) {
            chaosHandler.removeCallbacks(chaosTimeoutRunnable);
        }
    }
    
    /**
     * Public method to check if snatch detection is active
     */
    public boolean isSnatchDetectionActive() {
        return sensorsAvailable && sensorManager != null;
    }
    
    /**
     * Public method to get current sensor readings (for debugging)
     */
    public String getCurrentSensorReadings() {
        return String.format("Acceleration: %.2f m/s², Angular: %.2f rad/s, Jolt: %s, Chaos: %s",
            lastAcceleration, lastAngularVelocity, 
            joltDetected ? "DETECTED" : "Normal",
            chaosMonitoring ? "MONITORING" : "Inactive");
    }
}