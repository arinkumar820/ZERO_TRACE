package com.sameetasadullah.i180479_180531;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Disappearing message manager - removes messages from UI after timer expires
 * Messages remain permanently stored in database
 */
public class DisappearingMessageManager {
    private static final String TAG = "DisappearingMsgMgr";
    
    private final Handler mainHandler;
    private final MessageUpdateListener listener;
    private final Map<Integer, Runnable> activeTimers;
    
    public interface MessageUpdateListener {
        void onMessageExpired(int position);
        void onMessageTimerUpdate(int position, long remainingMs);
    }
    
    public DisappearingMessageManager(Context context, MessageUpdateListener listener) {
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.listener = listener;
        this.activeTimers = new ConcurrentHashMap<>();
        Log.d(TAG, "DisappearingMessageManager initialized for UI-only functionality");
    }
    
    /**
     * Start countdown timer for message (UI removal only - DB storage remains)
     */
    public void startMessageTimer(WebSocketMessage message, int position) {
        if (message == null || !message.isDisappearing()) {
            return;
        }
        
        // Stop existing timer for this position
        stopMessageTimer(position);
        
        long remainingTime = message.getRemainingTimeMs();
        if (remainingTime <= 0) {
            // Already expired
            if (listener != null) {
                listener.onMessageExpired(position);
            }
            return;
        }
        
        Log.d(TAG, "Starting UI timer for message at position " + position + ", remaining: " + remainingTime + "ms");
        
        // Create timer runnable
        Runnable timerRunnable = new Runnable() {
            @Override
            public void run() {
                activeTimers.remove(position);
                
                if (listener != null) {
                    Log.d(TAG, "Message expired from UI at position " + position + " (DB copy preserved)");
                    listener.onMessageExpired(position);
                }
            }
        };
        
        // Store and schedule timer
        activeTimers.put(position, timerRunnable);
        mainHandler.postDelayed(timerRunnable, remainingTime);
        
        // Start periodic updates for countdown display
        startCountdownUpdates(message, position);
    }
    
    /**
     * Start countdown updates for UI display
     */
    private void startCountdownUpdates(WebSocketMessage message, int position) {
        Runnable updateRunnable = new Runnable() {
            @Override
            public void run() {
                long remaining = message.getRemainingTimeMs();
                
                if (remaining > 0 && listener != null) {
                    listener.onMessageTimerUpdate(position, remaining);
                    
                    // Schedule next update (1 second intervals, 250ms when < 10 seconds)
                    long delay = remaining < 10000 ? 250 : 1000;
                    mainHandler.postDelayed(this, delay);
                } else {
                    // Timer expired, clean up
                    activeTimers.remove(position);
                }
            }
        };
        
        mainHandler.post(updateRunnable);
    }
    
    public void stopMessageTimer(int position) {
        Runnable timer = activeTimers.remove(position);
        if (timer != null) {
            mainHandler.removeCallbacks(timer);
            Log.d(TAG, "Stopped UI timer for position " + position);
        }
    }
    
    public void clearAllTimers() {
        Log.d(TAG, "Clearing all UI timers (" + activeTimers.size() + " active)");
        
        for (Runnable timer : activeTimers.values()) {
            mainHandler.removeCallbacks(timer);
        }
        activeTimers.clear();
    }
    
    public void updatePositions(List<WebSocketMessage> messageList) {
        // Update timer positions after list changes
        Map<Integer, Runnable> newTimers = new HashMap<>();
        
        for (int i = 0; i < messageList.size(); i++) {
            WebSocketMessage message = messageList.get(i);
            if (message.isDisappearing() && !message.isExpired()) {
                // Restart timer with new position
                startMessageTimer(message, i);
            }
        }
    }
    
    public boolean shouldMessageDisappear(WebSocketMessage message) {
        return message != null && message.shouldDisappear();
    }
    
    public String getFormattedRemainingTime(WebSocketMessage message) {
        if (message == null || !message.isDisappearing()) {
            return "";
        }
        
        long remainingMs = message.getRemainingTimeMs();
        if (remainingMs <= 0) {
            return "Expired";
        }
        
        return DisappearingMessageSettings.formatRemainingTime(remainingMs);
    }
    
    public WebSocketMessage createMessageWithTimer(String senderUid, String senderEmail, String messageText, long customTimer) {
        WebSocketMessage message = new WebSocketMessage(senderUid, senderEmail, messageText);
        message.setDisappearing(true);
        message.setDisappearAfterMs(customTimer);
        message.setExpired(false);
        return message;
    }
    
    public void cleanup() {
        Log.d(TAG, "Cleaning up DisappearingMessageManager");
        clearAllTimers();
    }
    
    public static String getDisappearingIcon(long remainingMs) {
        if (remainingMs <= 0) {
            return "💨"; // Disappeared
        } else {
            return "⏱️"; // Always show clock icon for active countdown
        }
    }
    
    public static int getCountdownColorHint(long remainingMs) {
        if (remainingMs <= 0) {
            return android.R.color.darker_gray;
        } else if (remainingMs < 10000) {
            return android.R.color.holo_red_dark;
        } else if (remainingMs < 60000) {
            return android.R.color.holo_orange_dark;
        } else {
            return android.R.color.darker_gray;
        }
    }
    
}