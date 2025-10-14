package com.sameetasadullah.i180479_180531;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.telephony.SmsManager;
import android.widget.Toast;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.os.SystemClock;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.util.List;
import java.util.ArrayList;

/**
 * Emergency Security Manager - Handles legitimate security features
 * This class provides secure alternatives to destructive panic buttons
 */
public class EmergencySecurityManager {
    
    private Context context;
    private SharedPreferences securityPrefs;
    private static final String SECURITY_PREFS = "security_preferences";
    private static final String EMERGENCY_CONTACTS_KEY = "emergency_contacts";
    private static final String AUTO_LOCK_ENABLED = "auto_lock_enabled";
    private static final String LOCK_TIMEOUT = "lock_timeout";
    
    public EmergencySecurityManager(Context context) {
        this.context = context;
        this.securityPrefs = context.getSharedPreferences(SECURITY_PREFS, Context.MODE_PRIVATE);
    }
    
    /**
     * Emergency Logout - Safely logs out user and clears session data
     * This is transparent and reversible unlike destructive approaches
     */
    public void performEmergencyLogout() {
        try {
            // Clear user session data
            clearSessionData();
            
            // Clear recent messages cache (but preserve user data)
            clearTemporaryData();
            
            // Navigate back to login
            navigateToLogin();
            
            // Show confirmation
            Toast.makeText(context, "Emergency logout completed", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Toast.makeText(context, "Emergency logout failed", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Send Emergency Alert - Sends SMS to predefined emergency contacts
     */
    public void sendEmergencyAlert() {
        List<String> emergencyContacts = getEmergencyContacts();
        String emergencyMessage = "Emergency alert from " + getUsername() + 
                                ". This is an automated safety message.";
        
        SmsManager smsManager = SmsManager.getDefault();
        
        for (String contact : emergencyContacts) {
            try {
                smsManager.sendTextMessage(contact, null, emergencyMessage, null, null);
            } catch (Exception e) {
                // Handle SMS sending failure
            }
        }
        
        Toast.makeText(context, "Emergency alerts sent to " + 
                      emergencyContacts.size() + " contacts", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Quick Lock - Immediately locks the app requiring authentication
     */
    public void performQuickLock() {
        // Set app as locked
        securityPrefs.edit().putBoolean("app_locked", true).apply();
        
        // Navigate to lock screen
        Intent lockIntent = new Intent(context, AppLockActivity.class);
        lockIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(lockIntent);
    }
    
    /**
     * Clear Session Data - Removes current session without destroying user data
     */
    private void clearSessionData() {
        SharedPreferences sessionPrefs = context.getSharedPreferences("user_session", Context.MODE_PRIVATE);
        sessionPrefs.edit().clear().apply();
        
        // Clear authentication tokens
        SharedPreferences authPrefs = context.getSharedPreferences("auth_data", Context.MODE_PRIVATE);
        authPrefs.edit()
                .remove("access_token")
                .remove("refresh_token")
                .remove("session_id")
                .apply();
    }
    
    /**
     * Clear Temporary Data - Removes cache and temporary files
     */
    private void clearTemporaryData() {
        try {
            // Clear app cache
            File cacheDir = context.getCacheDir();
            if (cacheDir != null && cacheDir.isDirectory()) {
                deleteDirectory(cacheDir);
            }
            
            // Clear temporary chat images/files
            File tempDir = new File(context.getFilesDir(), "temp");
            if (tempDir.exists()) {
                deleteDirectory(tempDir);
            }
            
        } catch (Exception e) {
            // Handle cleanup errors gracefully
        }
    }
    
    /**
     * Set Auto-Lock Timer - Automatically locks app after inactivity
     */
    public void setAutoLockTimer(int minutes) {
        securityPrefs.edit()
                .putBoolean(AUTO_LOCK_ENABLED, true)
                .putInt(LOCK_TIMEOUT, minutes)
                .apply();
        
        // Schedule auto-lock
        scheduleAutoLock(minutes);
    }
    
    /**
     * Schedule automatic lock after specified minutes
     */
    private void scheduleAutoLock(int minutes) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AutoLockReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 
                                                               PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        long triggerTime = SystemClock.elapsedRealtime() + (minutes * 60 * 1000);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime, pendingIntent);
    }
    
    /**
     * Emergency Contacts Management
     */
    public void addEmergencyContact(String phoneNumber) {
        List<String> contacts = getEmergencyContacts();
        if (!contacts.contains(phoneNumber)) {
            contacts.add(phoneNumber);
            saveEmergencyContacts(contacts);
        }
    }
    
    public List<String> getEmergencyContacts() {
        String contactsString = securityPrefs.getString(EMERGENCY_CONTACTS_KEY, "");
        List<String> contacts = new ArrayList<>();
        
        if (!contactsString.isEmpty()) {
            String[] contactArray = contactsString.split(",");
            for (String contact : contactArray) {
                contacts.add(contact.trim());
            }
        }
        
        return contacts;
    }
    
    private void saveEmergencyContacts(List<String> contacts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < contacts.size(); i++) {
            sb.append(contacts.get(i));
            if (i < contacts.size() - 1) {
                sb.append(",");
            }
        }
        securityPrefs.edit().putString(EMERGENCY_CONTACTS_KEY, sb.toString()).apply();
    }
    
    /**
     * Navigate back to login screen
     */
    private void navigateToLogin() {
        Intent loginIntent = new Intent(context, LoginActivity.class);
        loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(loginIntent);
    }
    
    /**
     * Get current username for emergency messages
     */
    private String getUsername() {
        SharedPreferences userPrefs = context.getSharedPreferences("user_data", Context.MODE_PRIVATE);
        return userPrefs.getString("username", "Unknown User");
    }
    
    /**
     * Utility method to delete directory recursively
     */
    private void deleteDirectory(File directory) {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        directory.delete();
    }
    
    /**
     * Check if app should be auto-locked
     */
    public boolean shouldAutoLock() {
        return securityPrefs.getBoolean(AUTO_LOCK_ENABLED, false);
    }
    
    /**
     * Get auto-lock timeout in minutes
     */
    public int getAutoLockTimeout() {
        return securityPrefs.getInt(LOCK_TIMEOUT, 5); // Default 5 minutes
    }
    
    /**
     * Reset auto-lock timer (call this on user activity)
     */
    public void resetAutoLockTimer() {
        if (shouldAutoLock()) {
            scheduleAutoLock(getAutoLockTimeout());
        }
    }
}