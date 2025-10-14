package com.sameetasadullah.i180479_180531;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

/**
 * Simple Device Checker for Snatch Detection
 * 
 * Use this to verify if snatch detection will work on your device
 * and show clear error messages if it won't work.
 */
public class DeviceChecker {

    private static final String TAG = "DeviceChecker";
    
    /**
     * Complete device check with clear success/error messages
     * Call this from any activity onCreate()
     */
    public static boolean checkDeviceAndShowResult(Context context) {
        Log.i(TAG, "🔍 Checking device compatibility for snatch detection...");
        
        // Step 1: Check if real device or emulator
        if (isEmulator()) {
            showError(context, "EMULATOR DETECTED", 
                "❌ Running on emulator!\n" +
                "Snatch detection needs a REAL phone with motion sensors.\n" +
                "Please test on physical Android device.");
            return false;
        }
        
        // Step 2: Check if sensors are available
        if (!hasSensors(context)) {
            showError(context, "SENSORS MISSING", 
                "❌ Your device is missing required sensors!\n" +
                "Need: Accelerometer + Gyroscope\n" +
                "Snatch detection cannot work on this device.");
            return false;
        }
        
        // Step 3: Check sensor quality
        String sensorIssues = checkSensorQuality(context);
        if (sensorIssues != null) {
            showWarning(context, "SENSOR WARNING", sensorIssues);
            // Continue anyway, might still work
        }
        
        // SUCCESS!
        showSuccess(context);
        return true;
    }
    
    /**
     * Check if running on emulator
     */
    private static boolean isEmulator() {
        String model = Build.MODEL;
        String manufacturer = Build.MANUFACTURER;
        String hardware = Build.HARDWARE;
        String fingerprint = Build.FINGERPRINT;
        
        boolean isEmulator = 
            // Common emulator signs
            model.contains("sdk") ||
            model.contains("Emulator") ||
            model.contains("google_sdk") ||
            manufacturer.equals("Google") && model.startsWith("sdk") ||
            hardware.equals("goldfish") ||
            hardware.equals("ranchu") ||
            fingerprint.contains("generic") ||
            fingerprint.contains("emulator");
        
        Log.d(TAG, String.format("Device: %s %s (Hardware: %s)", manufacturer, model, hardware));
        
        if (isEmulator) {
            Log.e(TAG, "❌ EMULATOR DETECTED!");
        } else {
            Log.i(TAG, "✅ Real device detected");
        }
        
        return isEmulator;
    }
    
    /**
     * Check if device has required sensors
     */
    private static boolean hasSensors(Context context) {
        SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        
        Log.d(TAG, "Accelerometer available: " + (accelerometer != null));
        Log.d(TAG, "Gyroscope available: " + (gyroscope != null));
        
        if (accelerometer != null) {
            Log.i(TAG, "📊 Accelerometer: " + accelerometer.getName());
        }
        if (gyroscope != null) {
            Log.i(TAG, "📊 Gyroscope: " + gyroscope.getName());
        }
        
        return accelerometer != null && gyroscope != null;
    }
    
    /**
     * Check sensor quality and return warning message if needed
     */
    private static String checkSensorQuality(Context context) {
        SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        
        StringBuilder warnings = new StringBuilder();
        
        // Check accelerometer
        if (accelerometer != null) {
            String name = accelerometer.getName().toLowerCase();
            if (name.contains("emulator") || name.contains("goldfish")) {
                warnings.append("⚠️ Fake accelerometer detected\n");
            }
            if (accelerometer.getMaximumRange() < 20.0f) {
                warnings.append("⚠️ Low accelerometer range (").append(accelerometer.getMaximumRange()).append(" m/s²)\n");
            }
        }
        
        // Check gyroscope
        if (gyroscope != null) {
            String name = gyroscope.getName().toLowerCase();
            if (name.contains("emulator") || name.contains("goldfish")) {
                warnings.append("⚠️ Fake gyroscope detected\n");
            }
            if (gyroscope.getMaximumRange() < 10.0f) {
                warnings.append("⚠️ Low gyroscope range (").append(gyroscope.getMaximumRange()).append(" rad/s)\n");
            }
        }
        
        return warnings.length() > 0 ? warnings.toString() + "Detection might not work reliably." : null;
    }
    
    /**
     * Show error message
     */
    private static void showError(Context context, String title, String message) {
        Log.e(TAG, "❌ " + title + ": " + message.replace("\n", " "));
        Toast.makeText(context, "❌ " + title + "\n" + message, Toast.LENGTH_LONG).show();
    }
    
    /**
     * Show warning message
     */
    private static void showWarning(Context context, String title, String message) {
        Log.w(TAG, "⚠️ " + title + ": " + message.replace("\n", " "));
        Toast.makeText(context, "⚠️ " + title + "\n" + message, Toast.LENGTH_LONG).show();
    }
    
    /**
     * Show success message
     */
    private static void showSuccess(Context context) {
        String deviceInfo = Build.MANUFACTURER + " " + Build.MODEL;
        String message = "✅ DEVICE READY!\n" + deviceInfo + "\nSnatch detection should work!";
        
        Log.i(TAG, "✅ SUCCESS: Device is compatible - " + deviceInfo);
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
    
    /**
     * Quick one-line check
     */
    public static boolean isDeviceReady(Context context) {
        return !isEmulator() && hasSensors(context);
    }
    
    /**
     * Show detailed device information
     */
    public static void showDetailedInfo(Context context) {
        Log.i(TAG, "📱 DETAILED DEVICE INFO:");
        Log.i(TAG, "Model: " + Build.MODEL);
        Log.i(TAG, "Manufacturer: " + Build.MANUFACTURER);
        Log.i(TAG, "Hardware: " + Build.HARDWARE);
        Log.i(TAG, "Android: " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
        
        SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        
        if (accelerometer != null) {
            Log.i(TAG, String.format("Accelerometer: %s (Range: %.1f m/s², Resolution: %.6f)", 
                accelerometer.getName(), accelerometer.getMaximumRange(), accelerometer.getResolution()));
        }
        
        if (gyroscope != null) {
            Log.i(TAG, String.format("Gyroscope: %s (Range: %.1f rad/s, Resolution: %.6f)", 
                gyroscope.getName(), gyroscope.getMaximumRange(), gyroscope.getResolution()));
        }
    }
}