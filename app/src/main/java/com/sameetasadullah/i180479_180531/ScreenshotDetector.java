package com.sameetasadullah.i180479_180531;

import android.app.Activity;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Screenshot detection utility class
 * Detects when a user attempts to take a screenshot (even when blocked by FLAG_SECURE)
 * and provides callbacks for handling these events
 */
public class ScreenshotDetector {
    
    private static final String TAG = "ScreenshotDetector";
    private static final String[] SCREENSHOT_KEYWORDS = {
        "screenshot", "screen_shot", "screen-shot", "screen", "capture"
    };
    
    private Activity activity;
    private ScreenshotListener listener;
    private ScreenshotContentObserver contentObserver;
    private boolean isListening = false;
    
    public interface ScreenshotListener {
        void onScreenshotDetected(String path);
        void onScreenshotAttempted();
    }
    
    public ScreenshotDetector(Activity activity) {
        this.activity = activity;
    }
    
    public void setListener(ScreenshotListener listener) {
        this.listener = listener;
    }
    
    /**
     * Start monitoring for screenshot attempts
     */
    public void startListening() {
        if (isListening || activity == null) {
            return;
        }
        
        try {
            contentObserver = new ScreenshotContentObserver(new Handler(Looper.getMainLooper()));
            activity.getContentResolver().registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                contentObserver
            );
            isListening = true;
            Log.d(TAG, "📸 Screenshot detection started");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to start screenshot detection", e);
        }
    }
    
    /**
     * Stop monitoring for screenshot attempts
     */
    public void stopListening() {
        if (!isListening || activity == null || contentObserver == null) {
            return;
        }
        
        try {
            activity.getContentResolver().unregisterContentObserver(contentObserver);
            isListening = false;
            Log.d(TAG, "📸 Screenshot detection stopped");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop screenshot detection", e);
        }
    }
    
    /**
     * Check if currently monitoring
     */
    public boolean isListening() {
        return isListening;
    }
    
    /**
     * Show default warning when screenshot is attempted
     */
    private void showScreenshotWarning() {
        if (activity != null) {
            Toast.makeText(activity, 
                "⚠️ Screenshot blocked for privacy protection", 
                Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * Check if a file path is likely a screenshot
     */
    private boolean isLikelyScreenshot(String path) {
        if (path == null) return false;
        
        String lowerPath = path.toLowerCase();
        for (String keyword : SCREENSHOT_KEYWORDS) {
            if (lowerPath.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get recent images to check for screenshots
     */
    private List<String> getRecentImages() {
        List<String> recentImages = new ArrayList<>();
        
        if (activity == null) return recentImages;
        
        try {
            String[] projection = {MediaStore.Images.Media.DATA, MediaStore.Images.Media.DATE_ADDED};
            String sortOrder = MediaStore.Images.Media.DATE_ADDED + " DESC";
            
            Cursor cursor = activity.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            );
            
            if (cursor != null) {
                int dataIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                int timeIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED);
                
                long currentTime = System.currentTimeMillis() / 1000;
                
                while (cursor.moveToNext() && recentImages.size() < 5) {
                    long addedTime = cursor.getLong(timeIndex);
                    // Only check images added within the last 10 seconds
                    if (currentTime - addedTime <= 10) {
                        String path = cursor.getString(dataIndex);
                        if (path != null) {
                            recentImages.add(path);
                        }
                    } else {
                        break; // Images are sorted by date, so we can break here
                    }
                }
                cursor.close();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking recent images", e);
        }
        
        return recentImages;
    }
    
    /**
     * Content observer for detecting new screenshots
     */
    private class ScreenshotContentObserver extends ContentObserver {
        
        public ScreenshotContentObserver(Handler handler) {
            super(handler);
        }
        
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            
            Log.d(TAG, "Media store changed, checking for screenshots...");
            
            // Check recent images for potential screenshots
            List<String> recentImages = getRecentImages();
            
            for (String imagePath : recentImages) {
                if (isLikelyScreenshot(imagePath)) {
                    Log.w(TAG, "🚨 Potential screenshot detected: " + imagePath);
                    
                    if (listener != null) {
                        listener.onScreenshotDetected(imagePath);
                    } else {
                        showScreenshotWarning();
                    }
                    
                    // Also trigger the attempt callback
                    if (listener != null) {
                        listener.onScreenshotAttempted();
                    }
                    
                    break; // Only handle the first detected screenshot
                }
            }
            
            // If no screenshots detected but media changed, still might be an attempt
            if (recentImages.isEmpty() && listener != null) {
                listener.onScreenshotAttempted();
            }
        }
    }
    
    /**
     * Create a basic screenshot detector with default warning
     */
    public static ScreenshotDetector createBasicDetector(Activity activity) {
        ScreenshotDetector detector = new ScreenshotDetector(activity);
        detector.setListener(new ScreenshotListener() {
            @Override
            public void onScreenshotDetected(String path) {
                Log.w(TAG, "🚨 Screenshot attempt blocked: " + path);
                Toast.makeText(activity, 
                    "🚨 Screenshot blocked! This content is private.", 
                    Toast.LENGTH_LONG).show();
            }
            
            @Override
            public void onScreenshotAttempted() {
                Log.w(TAG, "🚨 Screenshot attempt detected");
                // Could log this event to analytics or security monitoring
            }
        });
        return detector;
    }
}