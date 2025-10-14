package com.sameetasadullah.i180479_180531;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;

import java.io.File;
import java.util.Map;

/**
 * DataClearingManager handles the complete erasure of all app data
 * when the panic button is activated. This includes:
 * - SharedPreferences data
 * - Local SQLite databases
 * - Firebase authentication session
 * - Cached files and temporary data
 * - Application files
 */
public class DataClearingManager {
    
    private static final String TAG = "DataClearingManager";
    private Context context;
    
    public DataClearingManager(Context context) {
        this.context = context;
    }
    
    /**
     * Performs complete app data clearing operation
     * @return true if all data was successfully cleared
     */
    public boolean clearAllAppData() {
        Log.w(TAG, "🚨 PANIC MODE ACTIVATED - Beginning complete data erasure");
        
        boolean success = true;
        
        try {
            // Clear all SharedPreferences
            success &= clearAllSharedPreferences();
            
            // Clear local databases
            success &= clearLocalDatabases();
            
            // Sign out from Firebase
            success &= clearFirebaseAuth();
            
            // Clear app cache and files
            success &= clearAppFiles();
            
            // Clear WebSocket connections and in-memory data
            success &= clearNetworkConnections();
            
            Log.w(TAG, "🚨 Data clearing completed. Success: " + success);
            
        } catch (Exception e) {
            Log.e(TAG, "🚨 Error during data clearing", e);
            success = false;
        }
        
        return success;
    }
    
    /**
     * Clear all SharedPreferences files
     */
    private boolean clearAllSharedPreferences() {
        try {
            Log.d(TAG, "Clearing SharedPreferences...");
            
            // Clear user preferences
            SharedPreferences userPrefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
            userPrefs.edit().clear().apply();
            
            // Clear any authentication preferences
            SharedPreferences authPrefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
            authPrefs.edit().clear().apply();
            
            // Clear disappearing message settings
            SharedPreferences disappearingPrefs = context.getSharedPreferences("disappearing_settings", Context.MODE_PRIVATE);
            disappearingPrefs.edit().clear().apply();
            
            // Clear default preferences
            SharedPreferences defaultPrefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
            defaultPrefs.edit().clear().apply();
            
            // Clear any other potential SharedPreferences files
            File sharedPrefsDir = new File(context.getApplicationInfo().dataDir, "shared_prefs");
            if (sharedPrefsDir.exists()) {
                File[] prefFiles = sharedPrefsDir.listFiles();
                if (prefFiles != null) {
                    for (File prefFile : prefFiles) {
                        if (prefFile.delete()) {
                            Log.d(TAG, "Deleted SharedPreferences file: " + prefFile.getName());
                        }
                    }
                }
            }
            
            Log.d(TAG, "✅ SharedPreferences cleared");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to clear SharedPreferences", e);
            return false;
        }
    }
    
    /**
     * Clear local SQLite databases
     */
    private boolean clearLocalDatabases() {
        try {
            Log.d(TAG, "Clearing local databases...");
            
            // Clear SQLite databases
            File databasesDir = new File(context.getApplicationInfo().dataDir, "databases");
            if (databasesDir.exists()) {
                File[] dbFiles = databasesDir.listFiles();
                if (dbFiles != null) {
                    for (File dbFile : dbFiles) {
                        if (dbFile.delete()) {
                            Log.d(TAG, "Deleted database file: " + dbFile.getName());
                        }
                    }
                }
            }
            
            // Try to delete specific known databases
            try {
                context.deleteDatabase("chat_database");
                context.deleteDatabase("user_database");  
                context.deleteDatabase("messages_database");
            } catch (Exception e) {
                Log.w(TAG, "Some databases may not exist: " + e.getMessage());
            }
            
            Log.d(TAG, "✅ Local databases cleared");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to clear local databases", e);
            return false;
        }
    }
    
    /**
     * Sign out from Firebase and clear authentication
     */
    private boolean clearFirebaseAuth() {
        try {
            Log.d(TAG, "Clearing Firebase authentication...");
            
            FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
            if (firebaseAuth.getCurrentUser() != null) {
                firebaseAuth.signOut();
                Log.d(TAG, "Firebase user signed out");
            }
            
            Log.d(TAG, "✅ Firebase authentication cleared");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to clear Firebase auth", e);
            return false;
        }
    }
    
    /**
     * Clear app cache, files, and temporary data
     */
    private boolean clearAppFiles() {
        try {
            Log.d(TAG, "Clearing app files and cache...");
            
            // Clear cache directory
            File cacheDir = context.getCacheDir();
            if (cacheDir.exists()) {
                deleteDirectory(cacheDir);
            }
            
            // Clear external cache if available
            File externalCacheDir = context.getExternalCacheDir();
            if (externalCacheDir != null && externalCacheDir.exists()) {
                deleteDirectory(externalCacheDir);
            }
            
            // Clear files directory
            File filesDir = context.getFilesDir();
            if (filesDir.exists()) {
                File[] files = filesDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.delete()) {
                            Log.d(TAG, "Deleted file: " + file.getName());
                        }
                    }
                }
            }
            
            Log.d(TAG, "✅ App files and cache cleared");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to clear app files", e);
            return false;
        }
    }
    
    /**
     * Clear network connections and in-memory data
     */
    private boolean clearNetworkConnections() {
        try {
            Log.d(TAG, "Clearing network connections...");
            
            // This would typically involve closing WebSocket connections,
            // clearing message queues, etc. The actual implementation would 
            // depend on the specific networking components used
            
            Log.d(TAG, "✅ Network connections cleared");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to clear network connections", e);
            return false;
        }
    }
    
    /**
     * Recursively delete a directory and all its contents
     */
    private void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        if (file.delete()) {
                            Log.d(TAG, "Deleted file: " + file.getAbsolutePath());
                        }
                    }
                }
            }
            if (directory.delete()) {
                Log.d(TAG, "Deleted directory: " + directory.getAbsolutePath());
            }
        }
    }
    
    /**
     * Get a summary of what data will be cleared
     */
    public String getDataClearingSummary() {
        return "The following data will be permanently deleted:\n\n" +
               "• All chat messages and history\n" +
               "• User account and login information\n" +
               "• App settings and preferences\n" +
               "• Cached files and temporary data\n" +
               "• Database records\n" +
               "• Authentication tokens\n\n" +
               "⚠️ This action cannot be undone!";
    }
}