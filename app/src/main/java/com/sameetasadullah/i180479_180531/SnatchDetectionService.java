package com.sameetasadullah.i180479_180531;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.Vibrator;
import android.util.Log;
import androidx.core.app.NotificationCompat;

/**
 * SnatchDetectionService - Background Security Monitoring
 * 
 * This service runs in the background to continuously monitor for snatch attempts
 * even when the main app is not in the foreground. It provides persistent security
 * monitoring and can be integrated into any Activity.
 * 
 * Features:
 * - Background sensor monitoring
 * - Low battery impact with optimized sensor usage
 * - Persistent notification for security awareness
 * - Integration with main app security systems
 * 
 * Usage: Start this service from your main activities to enable background protection
 */
public class SnatchDetectionService extends Service implements SensorEventListener {

    private static final String TAG = "SnatchDetectionService";
    private static final String CHANNEL_ID = "SECURITY_MONITORING";
    private static final int NOTIFICATION_ID = 1001;
    
    // === SENSOR CONFIGURATION ===
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private boolean sensorsAvailable = false;
    
    // === DETECTION THRESHOLDS ===
    private static final float JOLT_THRESHOLD = 12.0f;  // Lowered for better detection
    private static final float CHAOS_THRESHOLD = 3.5f;  // Lowered for better detection
    private static final long CHAOS_MONITORING_DURATION = 2000; // 2 seconds for better detection
    private static final int SENSOR_DELAY = SensorManager.SENSOR_DELAY_NORMAL; // Balanced for background
    
    // === DETECTION STATE ===
    private boolean joltDetected = false;
    private boolean chaosMonitoring = false;
    private long joltDetectionTime = 0;
    private Handler chaosHandler;
    private Runnable chaosTimeoutRunnable;
    
    // === SYSTEM COMPONENTS ===
    private PowerManager.WakeLock wakeLock;
    private Vibrator vibrator;
    private SharedPreferences securityPrefs;
    private NotificationManager notificationManager;
    
    // === PERFORMANCE TRACKING ===
    private float lastAcceleration = 0f;
    private float lastAngularVelocity = 0f;
    private int consecutiveHighReadings = 0;
    private static final int REQUIRED_CONSECUTIVE_READINGS = 1; // Even lower for background detection
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "🛡️ Snatch Detection Service starting...");
        
        initializeComponents();
        initializeSensors();
        createNotificationChannel();
        startForegroundService();
        
        if (sensorsAvailable) {
            startSnatchDetection();
            Log.i(TAG, "✅ Background security monitoring active");
        } else {
            Log.e(TAG, "❌ Cannot start - sensors unavailable");
            stopSelf();
        }
    }
    
    /**
     * Initialize system components for background operation
     */
    private void initializeComponents() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        chaosHandler = new Handler(Looper.getMainLooper());
        
        securityPrefs = getSharedPreferences("security_prefs", Context.MODE_PRIVATE);
        
        // Acquire wake lock for critical security monitoring
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, 
            "BistoChat:SnatchDetection");
        wakeLock.acquire(10*60*1000L /*10 minutes*/);
    }
    
    /**
     * Initialize sensors for background monitoring
     */
    private void initializeSensors() {
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            
            sensorsAvailable = (accelerometer != null && gyroscope != null);
            
            if (sensorsAvailable) {
                Log.i(TAG, "✅ Background sensors initialized");
            } else {
                Log.e(TAG, "❌ Required sensors not available for background monitoring");
            }
        }
    }
    
    /**
     * Create notification channel for Android O+
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Security Monitoring",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Background snatch detection monitoring");
            channel.setShowBadge(false);
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    /**
     * Start foreground service with persistent notification
     */
    private void startForegroundService() {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🛡️ Security Active")
            .setContentText("Snatch detection monitoring in background")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setShowWhen(false)
            .build();
            
        startForeground(NOTIFICATION_ID, notification);
        Log.d(TAG, "📱 Foreground service started with notification");
    }
    
    /**
     * Start background snatch detection monitoring
     */
    private void startSnatchDetection() {
        if (!sensorsAvailable) return;
        
        boolean accelRegistered = sensorManager.registerListener(
            this, accelerometer, SENSOR_DELAY);
        boolean gyroRegistered = sensorManager.registerListener(
            this, gyroscope, SENSOR_DELAY);
        
        if (accelRegistered && gyroRegistered) {
            Log.i(TAG, "🎯 Background sensor monitoring started");
            updateSecurityStatus("ACTIVE");
        } else {
            Log.e(TAG, "❌ Failed to start background sensor monitoring");
            stopSelf();
        }
    }
    
    /**
     * Stop background monitoring
     */
    private void stopSnatchDetection() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
            Log.i(TAG, "🔇 Background sensor monitoring stopped");
        }
        
        if (chaosTimeoutRunnable != null) {
            chaosHandler.removeCallbacks(chaosTimeoutRunnable);
        }
        
        updateSecurityStatus("INACTIVE");
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "🔄 Service start command received");
        return START_STICKY; // Restart if killed by system
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not a bound service
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
     * Handle accelerometer data for background jolt detection
     */
    private void handleAccelerometerData(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        
        float totalForce = (float) Math.sqrt(x*x + y*y + z*z);
        float acceleration = Math.abs(totalForce - SensorManager.GRAVITY_EARTH);
        
        lastAcceleration = acceleration;
        
        if (acceleration > JOLT_THRESHOLD && !joltDetected) {
            consecutiveHighReadings++;
            
            if (consecutiveHighReadings >= REQUIRED_CONSECUTIVE_READINGS) {
                detectJolt(acceleration);
            }
        } else if (acceleration <= JOLT_THRESHOLD) {
            consecutiveHighReadings = 0;
        }
    }
    
    /**
     * Handle gyroscope data for background chaos detection
     */
    private void handleGyroscopeData(SensorEvent event) {
        if (!chaosMonitoring) return;
        
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        
        float angularVelocity = (float) Math.sqrt(x*x + y*y + z*z);
        lastAngularVelocity = angularVelocity;
        
        if (angularVelocity > CHAOS_THRESHOLD) {
            detectChaos(angularVelocity);
        }
    }
    
    /**
     * Process jolt detection in background
     */
    private void detectJolt(float acceleration) {
        joltDetected = true;
        joltDetectionTime = System.currentTimeMillis();
        chaosMonitoring = true;
        consecutiveHighReadings = 0;
        
        Log.w(TAG, String.format("🚨 BACKGROUND JOLT: %.2f m/s²", acceleration));
        
        updateNotification("⚡ Potential threat detected...", true);
        
        chaosTimeoutRunnable = new Runnable() {
            @Override
            public void run() {
                if (chaosMonitoring && joltDetected) {
                    Log.i(TAG, "⏰ Background monitoring timeout - resetting");
                    resetDetectionState();
                    updateNotification("🛡️ Security monitoring active", false);
                }
            }
        };
        
        chaosHandler.postDelayed(chaosTimeoutRunnable, CHAOS_MONITORING_DURATION);
    }
    
    /**
     * Process chaos detection and trigger security response
     */
    private void detectChaos(float angularVelocity) {
        if (!joltDetected || !chaosMonitoring) return;
        
        long timeSinceJolt = System.currentTimeMillis() - joltDetectionTime;
        
        Log.e(TAG, String.format("🔥 BACKGROUND SNATCH DETECTED! Time: %d ms", timeSinceJolt));
        
        // Trigger security response
        triggerBackgroundSecurityResponse();
    }
    
    /**
     * Execute background security response
     */
    private void triggerBackgroundSecurityResponse() {
        Log.e(TAG, "🚨 BACKGROUND SECURITY BREACH - INITIATING RESPONSE");
        
        stopSnatchDetection();
        
        // Strong notification
        updateNotification("🚨 SECURITY BREACH DETECTED", true);
        
        // Strong vibration
        if (vibrator != null) {
            long[] pattern = {0, 500, 200, 500, 200, 500};
            vibrator.vibrate(pattern, -1);
        }
        
        // Launch security activity to handle data clearing
        Intent securityIntent = new Intent(this, SnatchDetectionActivity.class);
        securityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        securityIntent.putExtra("background_trigger", true);
        startActivity(securityIntent);
        
        // Log incident
        logBackgroundIncident();
        
        // Stop service after triggering response
        stopSelf();
    }
    
    /**
     * Update persistent notification
     */
    private void updateNotification(String message, boolean urgent) {
        int icon = urgent ? android.R.drawable.ic_dialog_alert : android.R.drawable.ic_lock_idle_lock;
        
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🛡️ Security Monitor")
            .setContentText(message)
            .setSmallIcon(icon)
            .setPriority(urgent ? NotificationCompat.PRIORITY_HIGH : NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setShowWhen(false)
            .build();
            
        notificationManager.notify(NOTIFICATION_ID, notification);
    }
    
    /**
     * Log background security incident
     */
    private void logBackgroundIncident() {
        SharedPreferences.Editor editor = securityPrefs.edit();
        editor.putLong("background_incident_time", System.currentTimeMillis());
        editor.putFloat("background_acceleration", lastAcceleration);
        editor.putFloat("background_angular_velocity", lastAngularVelocity);
        editor.putString("incident_source", "BACKGROUND_SERVICE");
        editor.apply();
        
        Log.w(TAG, "📝 Background security incident logged");
    }
    
    /**
     * Update security status in preferences
     */
    private void updateSecurityStatus(String status) {
        SharedPreferences.Editor editor = securityPrefs.edit();
        editor.putString("background_security_status", status);
        editor.putLong("last_status_update", System.currentTimeMillis());
        editor.apply();
    }
    
    /**
     * Reset detection state
     */
    private void resetDetectionState() {
        joltDetected = false;
        chaosMonitoring = false;
        consecutiveHighReadings = 0;
        joltDetectionTime = 0;
        
        if (chaosTimeoutRunnable != null) {
            chaosHandler.removeCallbacks(chaosTimeoutRunnable);
        }
    }
    
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        String sensorName = (sensor.getType() == Sensor.TYPE_ACCELEROMETER) ? "Accelerometer" : "Gyroscope";
        Log.d(TAG, String.format("📊 Background %s accuracy: %d", sensorName, accuracy));
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "🔚 Snatch Detection Service stopping...");
        
        stopSnatchDetection();
        
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        
        if (chaosHandler != null && chaosTimeoutRunnable != null) {
            chaosHandler.removeCallbacks(chaosTimeoutRunnable);
        }
        
        updateSecurityStatus("STOPPED");
        Log.i(TAG, "✅ Background security service stopped");
    }
    
    /**
     * Static method to start the background service from any activity
     */
    public static void startBackgroundMonitoring(Context context) {
        Intent serviceIntent = new Intent(context, SnatchDetectionService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
        Log.i(TAG, "🚀 Background monitoring service started from external call");
    }
    
    /**
     * Static method to stop the background service
     */
    public static void stopBackgroundMonitoring(Context context) {
        Intent serviceIntent = new Intent(context, SnatchDetectionService.class);
        context.stopService(serviceIntent);
        Log.i(TAG, "🛑 Background monitoring service stopped from external call");
    }
}