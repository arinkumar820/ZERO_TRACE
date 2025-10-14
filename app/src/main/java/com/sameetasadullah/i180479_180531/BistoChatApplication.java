package com.sameetasadullah.i180479_180531;

import android.app.Application;
import android.content.res.Configuration;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Locale;

/**
 * Custom Application class to properly initialize Firebase and handle configuration
 */
public class BistoChatApplication extends Application {
    
    private static final String TAG = "ZeroTraceApp";
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Install crash handler first to catch all crashes
        CrashHandler.install(this);
        
        // Initialize Firebase with proper configuration
        initializeFirebase();

        // Start network monitor for connectivity events
        NetworkMonitor.init(this);
        
        // Set default locale if not set
        ensureLocaleConfiguration();
        
        Log.d(TAG, "ZeroTraceApplication initialized successfully");
    }
    
    private void initializeFirebase() {
        try {
            // Initialize Firebase if not already initialized
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this);
                Log.d(TAG, "Firebase initialized");
            }
            
            // Enable offline persistence for Firebase Database
            try {
                FirebaseDatabase.getInstance().setPersistenceEnabled(true);
                Log.d(TAG, "Firebase offline persistence enabled");
            } catch (Exception e) {
                // Persistence might already be enabled
                Log.d(TAG, "Firebase persistence already configured");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase", e);
        }
    }
    
    private void ensureLocaleConfiguration() {
        try {
            // Set system locale to prevent Firebase warnings
            Locale defaultLocale = Locale.getDefault();
            if (defaultLocale == null) {
                defaultLocale = new Locale("en", "US");
                Locale.setDefault(defaultLocale);
            }
            
            // Set system properties for Firebase
            System.setProperty("user.language", defaultLocale.getLanguage());
            System.setProperty("user.country", defaultLocale.getCountry());
            
            Log.d(TAG, "Locale configured: " + defaultLocale.toString());
            Log.d(TAG, "System language: " + System.getProperty("user.language"));
            Log.d(TAG, "System country: " + System.getProperty("user.country"));
            
        } catch (Exception e) {
            Log.w(TAG, "Error configuring locale, setting fallback", e);
            try {
                Locale.setDefault(new Locale("en", "US"));
            } catch (Exception ex) {
                Log.e(TAG, "Failed to set fallback locale", ex);
            }
        }
    }
    
    private Locale getCurrentLocale() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            return getResources().getConfiguration().getLocales().get(0);
        } else {
            return getResources().getConfiguration().locale;
        }
    }
    
    private void setDefaultLocale() {
        try {
            Locale defaultLocale = Locale.ENGLISH; // or Locale.getDefault()
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                Configuration config = getResources().getConfiguration();
                config.setLocale(defaultLocale);
            } else {
                Configuration config = getResources().getConfiguration();
                config.locale = defaultLocale;
            }
            
            Log.d(TAG, "Default locale set to: " + defaultLocale.toString());
            
        } catch (Exception e) {
            Log.w(TAG, "Failed to set default locale", e);
        }
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        
        // Handle configuration changes (like locale changes)
        Log.d(TAG, "Configuration changed. New locale: " + 
                (newConfig.locale != null ? newConfig.locale.toString() : "null"));
    }
}