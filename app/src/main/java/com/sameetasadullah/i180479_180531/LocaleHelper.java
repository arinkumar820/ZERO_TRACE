package com.sameetasadullah.i180479_180531;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.util.Log;

import java.util.Locale;

/**
 * Helper class to fix Firebase locale warnings
 * Use this in activities where you see the X-Firebase-Locale warning
 */
public class LocaleHelper {
    
    private static final String TAG = "LocaleHelper";
    
    /**
     * Fix locale configuration to prevent Firebase locale warnings
     * Call this in onCreate() of your main activities
     */
    public static void fixLocaleConfiguration(Activity activity) {
        try {
            Configuration config = activity.getResources().getConfiguration();
            
            // Check if locale is null or empty
            Locale currentLocale = getCurrentLocale(config);
            
            if (currentLocale == null) {
                Log.w(TAG, "Current locale is null, setting default");
                setDefaultLocale(activity, config);
            } else {
                Log.d(TAG, "Current locale: " + currentLocale.toString());
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error fixing locale configuration", e);
        }
    }
    
    /**
     * Get current locale based on Android version
     */
    private static Locale getCurrentLocale(Configuration config) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            return config.getLocales().get(0);
        } else {
            return config.locale;
        }
    }
    
    /**
     * Set default locale if none is configured
     */
    private static void setDefaultLocale(Context context, Configuration config) {
        Locale defaultLocale = Locale.getDefault();
        
        if (defaultLocale == null) {
            defaultLocale = Locale.ENGLISH;
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            config.setLocale(defaultLocale);
        } else {
            config.locale = defaultLocale;
        }
        
        // Apply configuration
        context.getResources().updateConfiguration(config, 
                context.getResources().getDisplayMetrics());
        
        Log.d(TAG, "Default locale set to: " + defaultLocale.toString());
    }
    
    /**
     * Log current locale information for debugging
     */
    public static void logLocaleInfo(Context context) {
        try {
            Configuration config = context.getResources().getConfiguration();
            Locale locale = getCurrentLocale(config);
            
            Log.d(TAG, "=== Locale Information ===");
            Log.d(TAG, "Locale: " + (locale != null ? locale.toString() : "null"));
            Log.d(TAG, "Language: " + (locale != null ? locale.getLanguage() : "null"));
            Log.d(TAG, "Country: " + (locale != null ? locale.getCountry() : "null"));
            Log.d(TAG, "Display Name: " + (locale != null ? locale.getDisplayName() : "null"));
            Log.d(TAG, "========================");
            
        } catch (Exception e) {
            Log.e(TAG, "Error logging locale info", e);
        }
    }
}