package com.sameetasadullah.i180479_180531;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Helper class to initialize user profile when they login/register
 * Call this from any authentication activity after successful login/registration
 */
public class UserInitializationHelper {
    
    private static final String TAG = "UserInitHelper";
    
    /**
     * Initialize user profile in Firebase database after successful authentication
     * Should be called from login/registration activities
     */
    public static void initializeUserProfile(Context context) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "Cannot initialize profile - user not authenticated");
            return;
        }
        
        FirebaseUserManager userManager = new FirebaseUserManager();
        
        userManager.createOrUpdateUserProfile(currentUser, new FirebaseUserManager.UserCreationCallback() {
            @Override
            public void onSuccess(String userId) {
                Log.d(TAG, "User profile initialized successfully for: " + currentUser.getEmail());
                
                // Update user status to online
                userManager.updateUserStatus(userId, "online");
                
                // Optional: Show success message
                if (context != null) {
                    Toast.makeText(context, "Welcome to Bisto Chat!", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Failed to initialize user profile: " + error);
                
                // Optional: Show error message
                if (context != null) {
                    Toast.makeText(context, "Profile setup failed: " + error, Toast.LENGTH_LONG).show();
                }
            }
        });
    }
    
    /**
     * Update user status (call from onResume/onPause in activities)
     */
    public static void updateUserStatus(String status) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;
        
        FirebaseUserManager userManager = new FirebaseUserManager();
        userManager.updateUserStatus(currentUser.getUid(), status);
        
        Log.d(TAG, "User status updated to: " + status);
    }
    
    /**
     * Set user status to online (call from onResume in main activities)
     */
    public static void setUserOnline() {
        updateUserStatus("online");
    }
    
    /**
     * Set user status to offline (call from onPause/onDestroy in main activities)
     */
    public static void setUserOffline() {
        updateUserStatus("offline");
    }
}