package com.sameetasadullah.i180479_180531;

import android.content.Context;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Hybrid User Manager: Uses Firebase Auth + Local Database
 * - Firebase Auth for authentication/login
 * - Local SQLite database for user profiles and search
 */
public class HybridUserManager {
    
    private static final String TAG = "HybridUserManager";
    
    private Context context;
    private LocalUserDatabase localDB;
    private FirebaseAuth firebaseAuth;
    
    public interface UserRegistrationCallback {
        void onSuccess(User user);
        void onFailure(String error);
    }
    
    public HybridUserManager(Context context) {
        this.context = context;
        this.localDB = LocalUserDatabase.getInstance(context);
        this.firebaseAuth = FirebaseAuth.getInstance();
    }
    
    /**
     * Complete user profile creation after Firebase Auth registration
     * Stores user data in local database for fast searching
     */
    public void completeUserProfile(String firstName, String lastName, String phoneNumber, String bio, UserRegistrationCallback callback) {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        
        if (firebaseUser == null) {
            callback.onFailure("User not authenticated with Firebase");
            return;
        }
        
        Log.d(TAG, "Completing user profile for: " + firebaseUser.getEmail());
        
        // Create User object with Firebase Auth data + additional info
        String uid = firebaseUser.getUid();
        String email = firebaseUser.getEmail();
        String displayName = firstName + " " + lastName;
        
        User user = new User(uid, email, displayName);
        user.setPhoneNumber(phoneNumber);
        user.setBio(bio.isEmpty() ? "Hey there! I'm using Bisto Chat." : bio);
        user.setStatus("online");
        user.setLastSeen(System.currentTimeMillis());
        
        // Save to local database (primary storage for app data)
        new Thread(() -> {
            try {
                long result = localDB.addOrUpdateUser(user);
                
                if (result > 0) {
                    Log.d(TAG, "User profile saved to local database successfully");
                    
                    // Also save current user data for future logins
                    saveCurrentUserData(user);
                    
                    // Success callback on main thread
                    if (context instanceof android.app.Activity) {
                        ((android.app.Activity) context).runOnUiThread(() -> callback.onSuccess(user));
                    } else {
                        callback.onSuccess(user);
                    }
                } else {
                    Log.e(TAG, "Failed to save user to local database");
                    if (context instanceof android.app.Activity) {
                        ((android.app.Activity) context).runOnUiThread(() -> callback.onFailure("Failed to save user profile"));
                    } else {
                        callback.onFailure("Failed to save user profile");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error saving user profile", e);
                if (context instanceof android.app.Activity) {
                    ((android.app.Activity) context).runOnUiThread(() -> callback.onFailure("Database error: " + e.getMessage()));
                } else {
                    callback.onFailure("Database error: " + e.getMessage());
                }
            }
        }).start();
    }
    
    /**
     * Handle user login - ensure user exists in local database
     */
    public void handleUserLogin(UserRegistrationCallback callback) {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        
        if (firebaseUser == null) {
            callback.onFailure("User not authenticated");
            return;
        }
        
        String uid = firebaseUser.getUid();
        Log.d(TAG, "Handling login for user: " + firebaseUser.getEmail());
        
        new Thread(() -> {
            try {
                // Check if user exists in local database
                User existingUser = localDB.getUserByUid(uid);
                
                if (existingUser != null) {
                    // User exists, update last seen
                    existingUser.setLastSeen(System.currentTimeMillis());
                    existingUser.setStatus("online");
                    localDB.addOrUpdateUser(existingUser);
                    
                    Log.d(TAG, "Existing user found and updated");
                    if (context instanceof android.app.Activity) {
                        ((android.app.Activity) context).runOnUiThread(() -> callback.onSuccess(existingUser));
                    } else {
                        callback.onSuccess(existingUser);
                    }
                } else {
                    // New user, create basic profile from Firebase Auth data
                    String email = firebaseUser.getEmail();
                    String displayName = firebaseUser.getDisplayName();
                    
                    if (displayName == null || displayName.trim().isEmpty()) {
                        displayName = email != null && email.contains("@") ? 
                                    email.substring(0, email.indexOf("@")) : "User";
                    }
                    
                    User newUser = new User(uid, email, displayName);
                    newUser.setStatus("online");
                    newUser.setLastSeen(System.currentTimeMillis());
                    newUser.setBio("Hey there! I'm using Bisto Chat.");
                    
                    // Save to local database
                    long result = localDB.addOrUpdateUser(newUser);
                    
                    if (result > 0) {
                        Log.d(TAG, "New user profile created from Firebase Auth data");
                        if (context instanceof android.app.Activity) {
                            ((android.app.Activity) context).runOnUiThread(() -> callback.onSuccess(newUser));
                        } else {
                            callback.onSuccess(newUser);
                        }
                    } else {
                        Log.e(TAG, "Failed to create user profile");
                        if (context instanceof android.app.Activity) {
                            ((android.app.Activity) context).runOnUiThread(() -> callback.onFailure("Failed to create user profile"));
                        } else {
                            callback.onFailure("Failed to create user profile");
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error handling user login", e);
                if (context instanceof android.app.Activity) {
                    ((android.app.Activity) context).runOnUiThread(() -> callback.onFailure("Login error: " + e.getMessage()));
                } else {
                    callback.onFailure("Login error: " + e.getMessage());
                }
            }
        }).start();
    }
    
    /**
     * Update user status (online/offline)
     */
    public void updateUserStatus(String status) {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser == null) return;
        
        new Thread(() -> {
            try {
                User user = localDB.getUserByUid(firebaseUser.getUid());
                if (user != null) {
                    user.setStatus(status);
                    user.setLastSeen(System.currentTimeMillis());
                    localDB.addOrUpdateUser(user);
                    Log.d(TAG, "User status updated to: " + status);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating user status", e);
            }
        }).start();
    }
    
    /**
     * Get current user from local database
     */
    public void getCurrentUser(UserRegistrationCallback callback) {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser == null) {
            callback.onFailure("No authenticated user");
            return;
        }
        
        new Thread(() -> {
            try {
                User user = localDB.getUserByUid(firebaseUser.getUid());
                if (user != null) {
                    if (context instanceof android.app.Activity) {
                        ((android.app.Activity) context).runOnUiThread(() -> callback.onSuccess(user));
                    } else {
                        callback.onSuccess(user);
                    }
                } else {
                    if (context instanceof android.app.Activity) {
                        ((android.app.Activity) context).runOnUiThread(() -> callback.onFailure("User profile not found"));
                    } else {
                        callback.onFailure("User profile not found");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting current user", e);
                if (context instanceof android.app.Activity) {
                    ((android.app.Activity) context).runOnUiThread(() -> callback.onFailure("Error: " + e.getMessage()));
                } else {
                    callback.onFailure("Error: " + e.getMessage());
                }
            }
        }).start();
    }
    
    /**
     * Add sample users for testing
     */
    public void addSampleUsers(UserRegistrationCallback callback) {
        new Thread(() -> {
            try {
                // Create sample users
                User user1 = new User("sample_john_" + System.currentTimeMillis(), 
                                     "john.doe@example.com", "John Doe");
                user1.setBio("Hello, I'm John!");
                user1.setStatus("online");
                user1.setPhoneNumber("+1234567890");
                
                User user2 = new User("sample_jane_" + System.currentTimeMillis(), 
                                     "jane.smith@gmail.com", "Jane Smith");
                user2.setBio("Nice to meet you!");
                user2.setStatus("offline");
                user2.setPhoneNumber("+9876543210");
                
                User user3 = new User("sample_bob_" + System.currentTimeMillis(), 
                                     "bob.wilson@test.com", "Bob Wilson");
                user3.setBio("Let's chat!");
                user3.setStatus("online");
                user3.setPhoneNumber("+5555555555");

                // Add to local database
                localDB.addOrUpdateUser(user1);
                localDB.addOrUpdateUser(user2);
                localDB.addOrUpdateUser(user3);
                
                Log.d(TAG, "Sample users added successfully");
                
                if (context instanceof android.app.Activity) {
                    ((android.app.Activity) context).runOnUiThread(() -> callback.onSuccess(user1));
                } else {
                    callback.onSuccess(user1);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error adding sample users", e);
                if (context instanceof android.app.Activity) {
                    ((android.app.Activity) context).runOnUiThread(() -> callback.onFailure("Error: " + e.getMessage()));
                } else {
                    callback.onFailure("Error: " + e.getMessage());
                }
            }
        }).start();
    }
    
    /**
     * Save current user data for future reference
     */
    private void saveCurrentUserData(User user) {
        // Could save to SharedPreferences for quick access
        // Or any other local storage as needed
        Log.d(TAG, "User data saved for user: " + user.getDisplayName());
    }
    
    /**
     * Get local database instance for direct access
     */
    public LocalUserDatabase getLocalDatabase() {
        return localDB;
    }
}