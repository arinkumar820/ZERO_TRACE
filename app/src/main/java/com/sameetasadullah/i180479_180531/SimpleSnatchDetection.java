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
 * Simple, stable snatch detection activity
 * Uses reasonable thresholds that only trigger on vigorous shaking
 */
public class SimpleSnatchDetection extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "SimpleSnatchDetection";
    
    // High thresholds - only trigger on vigorous shaking
    private static final float SHAKE_THRESHOLD = 20.0f;      // m/s² - vigorous shake required
    private static final float ROTATION_THRESHOLD = 6.0f;    // rad/s - vigorous rotation required
    
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private Vibrator vibrator;
    
    private boolean monitoring = false;
    private boolean shakeDetected = false;
    private long shakeTime = 0;
    
    private float currentAcceleration = 0f;
    private float currentRotation = 0f;
    
    // Sensor stabilization
    private boolean sensorsReady = false;
    private long startTime = 0;
    private static final long STABILIZATION_TIME = 3000; // 3 seconds
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.i(TAG, "🛡️ Simple Snatch Detection Starting...");
        
        try {
            // Check device compatibility first
            if (!checkDeviceCompatibility()) {
                showErrorAndExit("This device does not support security features");
                return;
            }
            
            initializeComponents();
            startDetection();
            
        } catch (SecurityException e) {
            Log.e(TAG, "❌ Security permission denied: " + e.getMessage());
            showErrorAndExit("Permission denied - please check app permissions");
        } catch (RuntimeException e) {
            Log.e(TAG, "❌ Runtime error: " + e.getMessage());
            showErrorAndExit("Feature not supported on this device");
        } catch (Exception e) {
            Log.e(TAG, "❌ Unexpected error: " + e.getMessage(), e);
            showErrorAndExit("Security feature failed to start on this device");
        }
    }
    
    private boolean checkDeviceCompatibility() {
        try {
            // Check if we can access sensor service
            SensorManager testSm = (SensorManager) getSystemService(SENSOR_SERVICE);
            if (testSm == null) {
                Log.e(TAG, "❌ SensorManager not available");
                return false;
            }
            
            // Check if required sensors exist
            Sensor testAccel = testSm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            Sensor testGyro = testSm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            
            if (testAccel == null || testGyro == null) {
                Log.e(TAG, "❌ Required sensors not available");
                return false;
            }
            
            Log.i(TAG, "✅ Device compatibility check passed");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Device compatibility check failed: " + e.getMessage());
            return false;
        }
    }
    
    private void showErrorAndExit(String message) {
        Toast.makeText(this, "❌ " + message, Toast.LENGTH_LONG).show();
        Log.e(TAG, "❌ Exiting: " + message);
        
        // Exit gracefully after showing message
        new Handler().postDelayed(() -> finish(), 2000);
    }
    
    private void initializeComponents() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        
        if (accelerometer == null || gyroscope == null) {
            throw new RuntimeException("Required sensors not available");
        }
        
        Log.i(TAG, "✅ Sensors initialized successfully");
    }
    
    private void startDetection() {
        startTime = System.currentTimeMillis();
        sensorsReady = false;
        
        // Register sensors with normal delay (not fastest to avoid overwhelming)
        boolean accelOk = sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        boolean gyroOk = sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        
        if (!accelOk || !gyroOk) {
            throw new RuntimeException("Failed to register sensor listeners");
        }
        
        monitoring = true;
        
        // Sensor stabilization timer
        new Handler().postDelayed(() -> {
            sensorsReady = true;
            Log.i(TAG, "🎯 Sensors stabilized - detection active");
            Toast.makeText(this, "🛡️ Security monitoring active - shake VIGOROUSLY to test", Toast.LENGTH_LONG).show();
        }, STABILIZATION_TIME);
        
        // Auto-stop after 30 seconds for safety
        new Handler().postDelayed(() -> {
            Log.i(TAG, "⏰ Auto-stopping detection for safety");
            finish();
        }, 30000);
        
        Toast.makeText(this, "🛡️ Initializing... please wait 3 seconds", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!monitoring || !sensorsReady) {
            return; // Skip during stabilization
        }
        
        try {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                handleAccelerometer(event);
            } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                handleGyroscope(event);
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
        
        // Calculate total acceleration (removing gravity)
        float totalForce = (float) Math.sqrt(x*x + y*y + z*z);
        currentAcceleration = Math.abs(totalForce - SensorManager.GRAVITY_EARTH);
        
        // Log occasionally for debugging
        if (System.currentTimeMillis() % 2000 < 50) { // Every 2 seconds
            Log.d(TAG, String.format("📊 Acceleration: %.2f m/s² (threshold: %.1f)", 
                currentAcceleration, SHAKE_THRESHOLD));
        }
        
        // Check for vigorous shake
        if (currentAcceleration > SHAKE_THRESHOLD && !shakeDetected) {
            shakeDetected = true;
            shakeTime = System.currentTimeMillis();
            
            Log.w(TAG, String.format("⚡ SHAKE DETECTED: %.2f m/s² - waiting for rotation...", currentAcceleration));
            
            if (vibrator != null) {
                vibrator.vibrate(200); // Short vibration feedback
            }
            
            Toast.makeText(this, "⚡ SHAKE DETECTED! Now checking rotation...", Toast.LENGTH_SHORT).show();
            
            // Reset after 3 seconds if no rotation detected
            new Handler().postDelayed(() -> {
                if (shakeDetected) {
                    Log.i(TAG, "⏰ Shake timeout - resetting");
                    shakeDetected = false;
                }
            }, 3000);
        }
    }
    
    private void handleGyroscope(SensorEvent event) {
        if (!shakeDetected) {
            return; // Only check rotation after shake
        }
        
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        
        // Calculate total rotation
        currentRotation = (float) Math.sqrt(x*x + y*y + z*z);
        
        Log.d(TAG, String.format("🔄 Rotation: %.2f rad/s (threshold: %.1f)", 
            currentRotation, ROTATION_THRESHOLD));
        
        // Check for vigorous rotation
        if (currentRotation > ROTATION_THRESHOLD) {
            long timeSinceShake = System.currentTimeMillis() - shakeTime;
            
            // SNATCH DETECTED!
            Log.e(TAG, String.format("🚨 SNATCH DETECTED! Rotation: %.2f rad/s after %.0fms", 
                currentRotation, (float)timeSinceShake));
            
            // Strong feedback
            if (vibrator != null) {
                long[] pattern = {0, 500, 200, 500, 200, 500};
                vibrator.vibrate(pattern, -1);
            }
            
            Toast.makeText(this, "🚨 SNATCH DETECTED! Security system activated!", Toast.LENGTH_LONG).show();
            
            // Log what would happen in real implementation
            Log.e(TAG, "🗑️ In real implementation: would clear all app data now!");
            Log.e(TAG, "📧 In real implementation: would send emergency alerts!");
            Log.e(TAG, "🔒 In real implementation: would lock device!");
            
            // Stop monitoring
            stopDetection();
            
            // Close after showing result
            new Handler().postDelayed(() -> finish(), 3000);
        }
    }
    
    private void stopDetection() {
        monitoring = false;
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        Log.i(TAG, "🛑 Detection stopped");
    }
    
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed for this implementation
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopDetection();
        Log.i(TAG, "🔚 Simple snatch detection finished");
    }
}