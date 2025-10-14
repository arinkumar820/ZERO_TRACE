package com.sameetasadullah.i180479_180531;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

/**
 * PanicNotificationHelper creates system notifications for panic button activations
 * This can be used for admin alerts or emergency contact notifications
 */
public class PanicNotificationHelper {
    
    private static final String TAG = "PanicNotification";
    private static final String CHANNEL_ID = "panic_channel";
    private static final int NOTIFICATION_ID = 9999;
    
    private Context context;
    private NotificationManager notificationManager;
    
    public PanicNotificationHelper(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }
    
    /**
     * Create notification channel for panic alerts (Android 8.0+)
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Emergency Panic Alerts";
            String description = "Notifications for panic button activations";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            
            notificationManager.createNotificationChannel(channel);
            Log.d(TAG, "Panic notification channel created");
        }
    }
    
    /**
     * Show panic activation notification
     * @param userInfo User display name and info
     * @param panicType "normal" or "quick" panic mode
     */
    public void showPanicNotification(String userInfo, String panicType) {
        try {
            String title = "🚨 PANIC BUTTON ACTIVATED";
            String message = userInfo + " activated " + panicType + " panic mode";
            
            // Create intent to open app when notification is tapped
            Intent intent = new Intent(context, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            
            PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            // Build notification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_panic_button)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(
                    title + "\n\n" + message + "\n\nTime: " + getCurrentTime() + 
                    "\n\nThis is an automated emergency alert."
                ))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(false) // Keep notification visible
                .setOngoing(false)
                .setContentIntent(pendingIntent)
                .setVibrate(new long[]{0, 1000, 500, 1000})
                .setColor(context.getResources().getColor(R.color.red));
            
            // Show notification
            notificationManager.notify(NOTIFICATION_ID, builder.build());
            
            Log.w(TAG, "📱 Panic notification shown: " + message);
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing panic notification", e);
        }
    }
    
    /**
     * Clear panic notification
     */
    public void clearPanicNotification() {
        notificationManager.cancel(NOTIFICATION_ID);
        Log.d(TAG, "Panic notification cleared");
    }
    
    /**
     * Get current time string
     */
    private String getCurrentTime() {
        return new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(new java.util.Date());
    }
}