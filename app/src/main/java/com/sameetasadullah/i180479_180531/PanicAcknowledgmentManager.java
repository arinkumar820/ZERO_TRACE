package com.sameetasadullah.i180479_180531;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * PanicAcknowledgmentManager handles sending acknowledgments and notifications
 * when users activate the panic button. This provides admin visibility and
 * security auditing capabilities.
 */
public class PanicAcknowledgmentManager {
    
    private static final String TAG = "PanicAcknowledgment";
    private Context context;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseRef;
    private PanicNotificationHelper notificationHelper;
    
    public PanicAcknowledgmentManager(Context context) {
        this.context = context;
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.databaseRef = FirebaseDatabase.getInstance().getReference();
        this.notificationHelper = new PanicNotificationHelper(context);
    }
    
    /**
     * Send panic acknowledgment with user information
     * @param panicType "normal" or "quick" panic mode
     */
    public void sendPanicAcknowledgment(String panicType) {
        try {
            String userInfo = getCurrentUserInfo();
            String timestamp = getCurrentTimestamp();
            
            // Log locally first
            Log.w(TAG, "🚨 PANIC BUTTON ACTIVATED by: " + userInfo + " at " + timestamp + " (Type: " + panicType + ")");
            
            // Send to Firebase (if available)
            sendToFirebase(userInfo, panicType, timestamp);
            
            // Save to local emergency log
            saveToLocalEmergencyLog(userInfo, panicType, timestamp);
            
            // Show system notification for admin/emergency contacts
            notificationHelper.showPanicNotification(userInfo, panicType);
            
        } catch (Exception e) {
            Log.e(TAG, "Error sending panic acknowledgment", e);
            // Continue with panic mode even if acknowledgment fails
        }
    }
    
    /**
     * Get current user information for acknowledgment
     */
    private String getCurrentUserInfo() {
        StringBuilder userInfo = new StringBuilder();
        
        // Try Firebase Auth first
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            String displayName = currentUser.getDisplayName();
            String email = currentUser.getEmail();
            String uid = currentUser.getUid();
            
            if (displayName != null && !displayName.isEmpty()) {
                userInfo.append("Name: ").append(displayName);
            } else if (email != null && !email.isEmpty()) {
                String nameFromEmail = email.contains("@") ? email.substring(0, email.indexOf("@")) : email;
                userInfo.append("User: ").append(nameFromEmail);
            } else {
                userInfo.append("User: ").append(uid.substring(0, Math.min(8, uid.length())));
            }
            
            if (email != null) {
                userInfo.append(" (").append(email).append(")");
            }
        } else {
            // Fallback to SharedPreferences
            SharedPreferences userPrefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
            String userName = userPrefs.getString("user_name", "");
            String userEmail = userPrefs.getString("user_email", "");
            
            if (!userName.isEmpty()) {
                userInfo.append("Name: ").append(userName);
            } else if (!userEmail.isEmpty()) {
                String nameFromEmail = userEmail.contains("@") ? userEmail.substring(0, userEmail.indexOf("@")) : userEmail;
                userInfo.append("User: ").append(nameFromEmail);
            } else {
                userInfo.append("User: Unknown");
            }
            
            if (!userEmail.isEmpty()) {
                userInfo.append(" (").append(userEmail).append(")");
            }
        }
        
        return userInfo.toString();
    }
    
    /**
     * Send panic acknowledgment to Firebase for admin monitoring
     */
    private void sendToFirebase(String userInfo, String panicType, String timestamp) {
        try {
            // Create panic log entry
            Map<String, Object> panicLog = new HashMap<>();
            panicLog.put("userInfo", userInfo);
            panicLog.put("panicType", panicType);
            panicLog.put("timestamp", timestamp);
            panicLog.put("deviceInfo", getDeviceInfo());
            panicLog.put("appVersion", getAppVersion());
            
            // Add user ID if available
            FirebaseUser currentUser = firebaseAuth.getCurrentUser();
            if (currentUser != null) {
                panicLog.put("userId", currentUser.getUid());
                panicLog.put("userEmail", currentUser.getEmail());
            }
            
            // Send to Firebase under panic_logs node
            String logId = "panic_" + System.currentTimeMillis();
            Log.d(TAG, "🔥 Attempting to send panic log to Firebase with ID: " + logId);
            Log.d(TAG, "🔥 Firebase Database URL: " + databaseRef.getDatabase().getReference().toString());
            
            databaseRef.child("panic_logs").child(logId).setValue(panicLog)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "✅ Panic acknowledgment sent to Firebase successfully: " + logId);
                        Log.d(TAG, "✅ Check Firebase Console at: panic_logs/" + logId);
                    } else {
                        Log.e(TAG, "❌ Failed to send panic acknowledgment to Firebase", task.getException());
                        if (task.getException() != null) {
                            Log.e(TAG, "❌ Error details: " + task.getException().getMessage());
                        }
                    }
                });
            
            // Also update user's last activity
            if (currentUser != null) {
                Map<String, Object> userUpdate = new HashMap<>();
                userUpdate.put("lastPanicActivation", timestamp);
                userUpdate.put("panicActivationCount", com.google.firebase.database.ServerValue.increment(1));
                
                databaseRef.child("users").child(currentUser.getUid()).updateChildren(userUpdate);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error sending to Firebase", e);
        }
    }
    
    /**
     * Save panic acknowledgment to local emergency log
     */
    private void saveToLocalEmergencyLog(String userInfo, String panicType, String timestamp) {
        try {
            SharedPreferences emergencyLog = context.getSharedPreferences("emergency_log", Context.MODE_PRIVATE);
            
            // Get current log count
            int logCount = emergencyLog.getInt("log_count", 0);
            logCount++;
            
            // Save new log entry
            emergencyLog.edit()
                .putString("log_" + logCount + "_user", userInfo)
                .putString("log_" + logCount + "_type", panicType)
                .putString("log_" + logCount + "_timestamp", timestamp)
                .putString("log_" + logCount + "_device", getDeviceInfo())
                .putInt("log_count", logCount)
                .putString("last_panic_timestamp", timestamp)
                .apply();
            
            Log.d(TAG, "✅ Panic acknowledgment saved to local emergency log (#" + logCount + ")");
            
        } catch (Exception e) {
            Log.e(TAG, "Error saving to local emergency log", e);
        }
    }
    
    /**
     * Get current timestamp in readable format
     */
    private String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss (z)", Locale.getDefault());
        return sdf.format(new Date());
    }
    
    /**
     * Get basic device information
     */
    private String getDeviceInfo() {
        return android.os.Build.MANUFACTURER + " " + 
               android.os.Build.MODEL + " (Android " + 
               android.os.Build.VERSION.RELEASE + ")";
    }
    
    /**
     * Get app version information
     */
    private String getAppVersion() {
        try {
            return context.getPackageManager()
                .getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "Unknown";
        }
    }
    
    /**
     * Get emergency log summary for admin purposes
     */
    public String getEmergencyLogSummary() {
        SharedPreferences emergencyLog = context.getSharedPreferences("emergency_log", Context.MODE_PRIVATE);
        int logCount = emergencyLog.getInt("log_count", 0);
        String lastPanic = emergencyLog.getString("last_panic_timestamp", "Never");
        
        return "Total panic activations: " + logCount + "\n" +
               "Last activation: " + lastPanic;
    }
    
    /**
     * Clear emergency logs (admin function)
     */
    public void clearEmergencyLogs() {
        SharedPreferences emergencyLog = context.getSharedPreferences("emergency_log", Context.MODE_PRIVATE);
        emergencyLog.edit().clear().apply();
        Log.d(TAG, "Emergency logs cleared");
    }
}