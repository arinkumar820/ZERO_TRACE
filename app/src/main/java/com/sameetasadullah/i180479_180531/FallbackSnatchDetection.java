package com.sameetasadullah.i180479_180531;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Fallback snatch detection for devices with limited sensor support
 * Works with just accelerometer (more common than gyroscope)
 */
public class FallbackSnatchDetection extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "FallbackSnatchDetection";
    
    // Lower thresholds for accelerometer-only detection
    private static final float SHAKE_THRESHOLD = 15.0f;      // m/s² - vigorous shake
    private static final float CONTINUOUS_THRESHOLD = 12.0f;  // m/s² - continuous movement
    
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Vibrator vibrator;
    
    private boolean monitoring = false;
    private boolean sensorsReady = false;
    
    // Detection variables
    private int shakeCount = 0;
    private long lastShakeTime = 0;
    private static final long SHAKE_WINDOW = 2000; // 2 seconds
    private static final int REQUIRED_SHAKES = 3; // Need 3 vigorous shakes
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.i(TAG, "🛡️ Fallback Snatch Detection Starting...");
        
        try {
            if (!initializeComponents()) {
                showErrorAndExit("No motion sensors available on this device");
                return;
            }
            
            startDetection();
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to start fallback detection: " + e.getMessage());
            showErrorAndExit("Motion detection not supported on this device");
        }
    }
    
    private boolean initializeComponents() {
        try {
            sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            if (sensorManager == null) {
                Log.e(TAG, "❌ SensorManager not available");
                return false;
            }
            
            // Try to get accelerometer (most common sensor)
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accelerometer == null) {
                Log.e(TAG, "❌ Accelerometer not available");
                return false;
            }
            
            vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            
            Log.i(TAG, "✅ Fallback sensors initialized - using accelerometer only");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error initializing components: " + e.getMessage());
            return false;
        }
    }
    
    private void startDetection() {
        try {
            // Register accelerometer with normal delay
            boolean registered = sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            
            if (!registered) {
                throw new RuntimeException("Failed to register accelerometer");
            }
            
            monitoring = true;
            
            // Sensor stabilization
            new Handler().postDelayed(() -> {
                sensorsReady = true;
                Log.i(TAG, "🎯 Fallback detection active");
                Toast.makeText(this, "🛡️ Basic motion detection active - shake VIGOROUSLY multiple times", Toast.LENGTH_LONG).show();
            }, 2000);
            
            // Auto-stop after 30 seconds
            new Handler().postDelayed(() -> {
                Log.i(TAG, "⏰ Auto-stopping detection");
                finish();
            }, 30000);
            
            Toast.makeText(this, "🛡️ Initializing basic motion detection...", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to start detection: " + e.getMessage());
            throw e;
        }
    }
    
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!monitoring || !sensorsReady) {
            return;
        }
        
        try {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                handleAccelerometer(event);
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Sensor error: " + e.getMessage());
            stopDetection();
        }
    }
    
    private void handleAccelerometer(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        
        // Calculate total acceleration
        float totalForce = (float) Math.sqrt(x*x + y*y + z*z);
        float acceleration = Math.abs(totalForce - SensorManager.GRAVITY_EARTH);
        
        // Log occasionally for debugging
        if (System.currentTimeMillis() % 3000 < 50) { // Every 3 seconds
            Log.d(TAG, String.format("📊 Motion: %.2f m/s² (threshold: %.1f)", 
                acceleration, SHAKE_THRESHOLD));
        }
        
        long currentTime = System.currentTimeMillis();
        
        // Check for vigorous shake
        if (acceleration > SHAKE_THRESHOLD) {
            
            // Reset count if too much time passed since last shake
            if (currentTime - lastShakeTime > SHAKE_WINDOW) {
                shakeCount = 0;
            }
            
            shakeCount++;
            lastShakeTime = currentTime;
            
            Log.w(TAG, String.format("⚡ SHAKE %d/%d: %.2f m/s²", shakeCount, REQUIRED_SHAKES, acceleration));
            
            if (vibrator != null) {
                vibrator.vibrate(100); // Short feedback
            }
            
            if (shakeCount >= REQUIRED_SHAKES) {
                // SNATCH DETECTED!
                Log.e(TAG, String.format("🚨 MOTION SNATCH DETECTED! %d vigorous shakes detected", shakeCount));
                
                // Strong feedback
                if (vibrator != null) {
                    long[] pattern = {0, 300, 100, 300, 100, 300};
                    vibrator.vibrate(pattern, -1);
                }
                
                Toast.makeText(this, "🚨 SUSPICIOUS MOTION DETECTED! Security activated!", Toast.LENGTH_LONG).show();
                
                // Log security actions
                Log.e(TAG, "🗑️ Security action: would clear app data");
                Log.e(TAG, "📧 Security action: would send alerts");
                Log.e(TAG, "🔒 Security action: would lock device");
                
                stopDetection();
                
                // Show result and exit
                new Handler().postDelayed(() -> {
                    Toast.makeText(this, "✅ Fallback detection test completed", Toast.LENGTH_SHORT).show();
                    finish();
                }, 2000);
                
            } else {
                Toast.makeText(this, String.format("⚡ Shake %d/%d detected", shakeCount, REQUIRED_SHAKES), Toast.LENGTH_SHORT).show();
            }
        }
        
        // Check for continuous high movement (alternative detection)
        if (acceleration > CONTINUOUS_THRESHOLD) {
            // Could add additional logic here for continuous movement detection
        }
    }
    
    private void stopDetection() {
        monitoring = false;
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        Log.i(TAG, "🛑 Fallback detection stopped");
    }
    
    private void showErrorAndExit(String message) {
        Toast.makeText(this, "❌ " + message, Toast.LENGTH_LONG).show();
        Log.e(TAG, "❌ Exiting: " + message);
        
        new Handler().postDelayed(() -> finish(), 2000);
    }
    
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopDetection();
        Log.i(TAG, "🔚 Fallback snatch detection finished");
    }
}