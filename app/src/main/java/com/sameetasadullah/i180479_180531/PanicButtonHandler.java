package com.sameetasadullah.i180479_180531;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

/**
 * PanicButtonHandler manages the panic button functionality
 * including confirmation dialogs and data clearing execution
 */
public class PanicButtonHandler {
    
    private static final String TAG = "PanicButtonHandler";
    private Context context;
    private DataClearingManager dataClearingManager;
    private PanicAcknowledgmentManager acknowledgmentManager;
    
    public PanicButtonHandler(Context context) {
        this.context = context;
        this.dataClearingManager = new DataClearingManager(context);
        this.acknowledgmentManager = new PanicAcknowledgmentManager(context);
    }
    
    /**
     * Handle panic button press with confirmation dialog
     */
    public void handlePanicButtonPress() {
        Log.w(TAG, "🚨 Panic button pressed");
        showConfirmationDialog();
    }
    
    /**
     * Show confirmation dialog before data clearing
     */
    private void showConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("🚨 PANIC MODE")
                .setMessage(dataClearingManager.getDataClearingSummary())
                .setIcon(R.drawable.ic_panic_button)
                .setCancelable(true)
                .setPositiveButton("DELETE ALL DATA", (dialog, which) -> {
                    dialog.dismiss();
                    showFinalConfirmation();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                    Log.d(TAG, "Panic mode cancelled by user");
                });
        
        AlertDialog dialog = builder.create();
        dialog.show();
        
        // Make the positive button red to indicate danger
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
            context.getResources().getColor(android.R.color.holo_red_dark)
        );
    }
    
    /**
     * Show final confirmation dialog with countdown
     */
    private void showFinalConfirmation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("⚠️ FINAL WARNING")
                .setMessage("This will permanently delete ALL your data!\n\n" +
                           "• All conversations will be lost\n" +
                           "• You will be logged out\n" +
                           "• All settings will be reset\n\n" +
                           "Are you absolutely sure?")
                .setIcon(R.drawable.ic_panic_button)
                .setCancelable(true)
                .setPositiveButton("YES, DELETE EVERYTHING", (dialog, which) -> {
                    dialog.dismiss();
                    executeDataClearing();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                    Log.d(TAG, "Final confirmation cancelled by user");
                });
        
        AlertDialog dialog = builder.create();
        dialog.show();
        
        // Make the positive button red and bold
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
            context.getResources().getColor(android.R.color.holo_red_dark)
        );
    }
    
    /**
     * Execute the data clearing operation
     */
    private void executeDataClearing() {
        Log.w(TAG, "🚨 Executing panic mode data clearing");
        
        // Send acknowledgment BEFORE clearing data
        acknowledgmentManager.sendPanicAcknowledgment("normal");
        
        // Show progress toast
        Toast.makeText(context, "🚨 PANIC MODE: Clearing all data...", Toast.LENGTH_LONG).show();
        
        // Execute data clearing in background thread
        new Thread(() -> {
            boolean success = dataClearingManager.clearAllAppData();
            
            // Switch back to main thread for UI updates
            new Handler(Looper.getMainLooper()).post(() -> {
                if (success) {
                    showDataClearedMessage();
                } else {
                    showDataClearingError();
                }
            });
        }).start();
    }
    
    /**
     * Show message when data clearing is completed
     */
    private void showDataClearedMessage() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("🚨 PANIC MODE COMPLETE")
                .setMessage("All app data has been permanently deleted.\n\n" +
                           "The app will now close and you will need to " +
                           "set up your account again if you wish to continue using it.")
                .setIcon(R.drawable.ic_panic_button)
                .setCancelable(false)
                .setPositiveButton("EXIT APP", (dialog, which) -> {
                    dialog.dismiss();
                    exitApplication();
                });
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    /**
     * Show error message if data clearing fails
     */
    private void showDataClearingError() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("⚠️ ERROR")
                .setMessage("There was an error clearing some data. " +
                           "Some information may still remain on the device.\n\n" +
                           "You may want to manually uninstall and reinstall " +
                           "the app for complete data removal.")
                .setIcon(R.drawable.ic_panic_button)
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> {
                    dialog.dismiss();
                    exitApplication();
                });
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    /**
     * Exit the application completely
     */
    private void exitApplication() {
        Log.w(TAG, "🚨 Exiting application after panic mode");
        
        try {
            // Close current activity
            if (context instanceof Activity) {
                ((Activity) context).finishAffinity();
            }
            
            // Clear task stack and exit
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            
            // Force exit the process
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
            
        } catch (Exception e) {
            Log.e(TAG, "Error exiting application", e);
            
            // Fallback: just finish activity if it's an Activity
            if (context instanceof Activity) {
                ((Activity) context).finish();
            }
        }
    }
    
    /**
     * Quick panic mode (double-tap or long press implementation)
     * Skips confirmation dialogs for immediate action
     */
    public void handleQuickPanic() {
        Log.w(TAG, "🚨 QUICK PANIC MODE ACTIVATED");
        
        // Send acknowledgment BEFORE clearing data (even in quick mode)
        acknowledgmentManager.sendPanicAcknowledgment("quick");
        
        Toast.makeText(context, "🚨 EMERGENCY: Clearing all data immediately!", Toast.LENGTH_LONG).show();
        
        // Execute immediate data clearing
        new Thread(() -> {
            dataClearingManager.clearAllAppData();
            
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(context, "🚨 Data cleared - Exiting app", Toast.LENGTH_SHORT).show();
                
                // Small delay to show the toast
                new Handler(Looper.getMainLooper()).postDelayed(this::exitApplication, 1500);
            });
        }).start();
    }
    
    /**
     * Check if panic mode is available
     */
    public boolean isPanicModeAvailable() {
        return dataClearingManager != null;
    }
}