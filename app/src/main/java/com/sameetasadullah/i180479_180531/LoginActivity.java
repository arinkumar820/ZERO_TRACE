package com.sameetasadullah.i180479_180531;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Login Activity with Admin Approval System
 * - Authenticates user with Firebase Auth
 * - Checks user approval status in Realtime Database
 * - Only allows approved users to access MainActivity
 * - Signs out pending/rejected users with appropriate messages
 */
public class LoginActivity extends AppCompatActivity {
    
    private static final String TAG = "LoginActivity";
    
    // UI Components
    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegisterLink, tvForgotPassword;
    private ProgressBar progressBar;
    
    // Firebase components
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        // === SNATCH DETECTION INTEGRATION (DISABLED ON STARTUP) ===
        Log.i(TAG, "🔒 Snatch detection available but disabled on startup to prevent crashes...");
        
        /* COMMENTED OUT TO PREVENT STARTUP CRASHES
        // CHECK DEVICE FIRST
        if (!DeviceChecker.checkDeviceAndShowResult(this)) {
            Log.w(TAG, "⚠️ Device not compatible with snatch detection");
            // Continue with normal login anyway
        } else {
            // ULTRA-SENSITIVE SETUP
            try {
                SnatchDetectionIntegration.enableFullProtection(this);
                SnatchDetectionIntegration.configureSensitivity(this, "high"); // INSANELY SENSITIVE
                
                Toast.makeText(this, "🔥 ULTRA-SENSITIVE security active!", Toast.LENGTH_LONG).show();
                Log.i(TAG, "✅ Ultra-sensitive snatch detection enabled");
                
                // LAUNCH ULTRA TEST for immediate verification
                UltraSnatchTest.runUltraTest(this);
                
            } catch (Exception e) {
                Log.e(TAG, "❌ Failed to initialize snatch detection: " + e.getMessage());
                Toast.makeText(this, "⚠️ Security initialization failed", Toast.LENGTH_SHORT).show();
            }
        }
        */
        
        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("users");
        
        initializeViews();
        setupClickListeners();
        
        // Check if user is already logged in and approved
        checkCurrentUser();
    }
    
    /**
     * Initialize UI components
     */
    private void initializeViews() {
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvRegisterLink = findViewById(R.id.tv_register_link);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
        progressBar = findViewById(R.id.progress_bar);
    }
    
    /**
     * Setup click listeners
     */
    private void setupClickListeners() {
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });
        
        tvRegisterLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegistrationActivity.class));
            }
        });
        
        tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Implement forgot password functionality
                Toast.makeText(LoginActivity.this, "Forgot password functionality coming soon", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Debug option: Long click on forgot password to fix database
        tvForgotPassword.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                fixDatabaseForApprovalSystem();
                return true;
            }
        });
        
        // Debug option: Double tap on Zero Trace title to open database debug
        TextView appTitle = findViewById(R.id.app_title);
        if (appTitle != null) {
            appTitle.setOnClickListener(new View.OnClickListener() {
                int clickCount = 0;
                long lastClickTime = 0;
                
                @Override
                public void onClick(View v) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastClickTime < 1000) {
                        clickCount++;
                    } else {
                        clickCount = 1;
                    }
                    lastClickTime = currentTime;
                    
                    if (clickCount >= 3) {
                        Intent intent = new Intent(LoginActivity.this, DatabaseDebugActivity.class);
                        startActivity(intent);
                    } else if (clickCount == 2) {
                        // Double tap to enable snatch detection
                        enableSnatchDetectionManually();
                    }
                }
            });
        }
    }
    
    /**
     * Enable snatch detection manually (now with actual detection!)
     */
    private void enableSnatchDetectionManually() {
        Log.i(TAG, "🔒 Manual snatch detection requested...");
        
        try {
            // Quick sensor availability check
            android.hardware.SensorManager sm = (android.hardware.SensorManager) getSystemService(SENSOR_SERVICE);
            android.hardware.Sensor accelerometer = sm.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER);
            android.hardware.Sensor gyroscope = sm.getDefaultSensor(android.hardware.Sensor.TYPE_GYROSCOPE);
            
            Intent detectionIntent;
            
            if (accelerometer != null && gyroscope != null) {
                // Full sensor support - use advanced detection
                Log.i(TAG, "✅ Full sensors available - launching advanced detection");
                detectionIntent = new Intent(this, SimpleSnatchDetection.class);
                Toast.makeText(this, "🛡️ Advanced security detection launched!", Toast.LENGTH_LONG).show();
                
            } else if (accelerometer != null) {
                // Only accelerometer available - use fallback detection
                Log.i(TAG, "✅ Accelerometer available - launching basic detection");
                detectionIntent = new Intent(this, FallbackSnatchDetection.class);
                Toast.makeText(this, "🛡️ Basic motion detection launched! Shake VIGOROUSLY multiple times", Toast.LENGTH_LONG).show();
                
            } else {
                // No motion sensors available
                Toast.makeText(this, "⚠️ No motion sensors available on this device", Toast.LENGTH_LONG).show();
                return;
            }
            
            startActivity(detectionIntent);
            
        } catch (SecurityException e) {
            Log.e(TAG, "❌ Security permission error: " + e.getMessage());
            Toast.makeText(this, "❌ Permission denied - please check app permissions", Toast.LENGTH_LONG).show();
        } catch (RuntimeException e) {
            Log.e(TAG, "❌ Runtime error: " + e.getMessage());
            Toast.makeText(this, "❌ Feature not supported on this device", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "❌ Unexpected error: " + e.getMessage(), e);
            String friendlyMessage = "Detection failed";
            if (e.getMessage() != null) {
                if (e.getMessage().contains("sensor")) {
                    friendlyMessage = "Sensors not available on this device";
                } else if (e.getMessage().contains("permission")) {
                    friendlyMessage = "Permissions required - check settings";
                }
            }
            Toast.makeText(this, "❌ " + friendlyMessage, Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * Check if user is already logged in and approved
     */
    private void checkCurrentUser() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "User already logged in, checking approval status");
            checkUserApprovalStatus(currentUser.getUid());
        }
    }
    
    /**
     * Validate user input and attempt login
     */
    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        
        // Validate inputs
        if (!validateInputs(email, password)) {
            return;
        }
        
        showProgress(true);
        
        // Authenticate with Firebase Auth
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Firebase Auth login successful");
                            
                            FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                            if (firebaseUser != null) {
                                // Check user approval status before allowing access
                                checkUserApprovalStatus(firebaseUser.getUid());
                            } else {
                                showProgress(false);
                                Toast.makeText(LoginActivity.this, "Login failed: User data not found", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            showProgress(false);
                            String errorMessage = task.getException() != null ? 
                                task.getException().getMessage() : "Authentication failed";
                            Log.e(TAG, "Login failed: " + errorMessage);
                            Toast.makeText(LoginActivity.this, "Login failed: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
    
    /**
     * Check user approval status in Firebase Realtime Database
     */
    private void checkUserApprovalStatus(String uid) {
        Log.d(TAG, "Checking approval status for user: " + uid);
        
        databaseReference.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                showProgress(false);
                
                if (dataSnapshot.exists()) {
                    try {
                        UserProfile userProfile = dataSnapshot.getValue(UserProfile.class);
                        
                        if (userProfile != null) {
                            handleUserApprovalStatus(userProfile);
                        } else {
                            Log.e(TAG, "User profile is null");
                            handleDatabaseError("User profile not found");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing user profile: " + e.getMessage());
                        handleDatabaseError("Error loading user profile");
                    }
                } else {
                    Log.e(TAG, "User profile does not exist in database");
                    handleDatabaseError("User profile not found in database");
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                showProgress(false);
                Log.e(TAG, "Database error: " + databaseError.getMessage());
                handleDatabaseError("Database error: " + databaseError.getMessage());
            }
        });
    }
    
    /**
     * Handle user approval status and redirect accordingly
     */
    private void handleUserApprovalStatus(UserProfile userProfile) {
        Log.d(TAG, "User status: " + userProfile.getStatus());
        
        switch (userProfile.getStatus()) {
            case UserProfile.STATUS_APPROVED:
                // User is approved - update last login and proceed to MainActivity
                updateLastLoginAndProceed(userProfile);
                break;
                
            case UserProfile.STATUS_PENDING:
                // User is pending approval - sign out and show message
                signOutUserWithMessage("Your account is pending admin approval. Please wait for approval before logging in.");
                break;
                
            case UserProfile.STATUS_REJECTED:
                // User is rejected - sign out and show message
                signOutUserWithMessage("Your account has been rejected by the administrator. Please contact support if you believe this is an error.");
                break;
                
            default:
                // Unknown status - sign out and show message
                signOutUserWithMessage("Your account status is unknown. Please contact support.");
                break;
        }
    }
    
    /**
     * Update user's last login timestamp and proceed to MainActivity
     */
    private void updateLastLoginAndProceed(UserProfile userProfile) {
        Log.d(TAG, "User is approved, updating last login timestamp");
        
        // Update last login timestamp in database
        userProfile.updateLastLogin();
        databaseReference.child(userProfile.getUid()).child("lastLoginTimestamp")
                .setValue(userProfile.getLastLoginTimestamp())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Last login timestamp updated successfully");
                    proceedToMainActivity(userProfile);
                })
                .addOnFailureListener(e -> {
                    // Proceed anyway if timestamp update fails
                    Log.w(TAG, "Failed to update last login timestamp, proceeding anyway", e);
                    proceedToMainActivity(userProfile);
                });
    }
    
    /**
     * Proceed to MainActivity for approved users
     */
    private void proceedToMainActivity(UserProfile userProfile) {
        Log.d(TAG, "Proceeding to MainActivity for approved user: " + userProfile.getEmail());
        
        Toast.makeText(this, "Welcome back, " + userProfile.getFormattedDisplayName() + "!", 
                Toast.LENGTH_SHORT).show();
        
        // Start MainActivity
        Intent intent = new Intent(this, fragmentsContainer.class); // or MainActivity.class
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("user_profile", userProfile.toString());
        startActivity(intent);
        finish();
    }
    
    /**
     * Sign out user and show message for non-approved users
     */
    private void signOutUserWithMessage(String message) {
        Log.d(TAG, "Signing out user with message: " + message);
        
        // Sign out from Firebase Auth
        firebaseAuth.signOut();
        
        // Show message to user
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        
        // Clear any stored user data
        clearUserSession();
    }
    
    /**
     * Clear user session data
     */
    private void clearUserSession() {
        // Clear any shared preferences or cached data if needed
        getSharedPreferences("user_prefs", MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
    }
    
    /**
     * Handle database errors during approval status check
     */
    private void handleDatabaseError(String errorMessage) {
        Log.e(TAG, "Database error during approval check: " + errorMessage);
        
        // Sign out user on database errors
        firebaseAuth.signOut();
        
        Toast.makeText(this, "Unable to verify account status. Please try again later.", 
                Toast.LENGTH_LONG).show();
    }
    
    /**
     * Validate user inputs
     */
    private boolean validateInputs(String email, String password) {
        // Validate email
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return false;
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email address");
            etEmail.requestFocus();
            return false;
        }
        
        // Validate password
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return false;
        }
        
        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return false;
        }
        
        return true;
    }
    
    /**
     * Show/hide progress indicator
     */
    private void showProgress(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        
        if (btnLogin != null) {
            btnLogin.setEnabled(!show);
            btnLogin.setText(show ? "Signing in..." : "Login");
        }
        
        // Disable input fields during login
        if (etEmail != null) etEmail.setEnabled(!show);
        if (etPassword != null) etPassword.setEnabled(!show);
    }
    
    /**
     * Fix existing database users for approval system
     * Converts status from "offline"/"online" to approval status
     */
    private void fixDatabaseForApprovalSystem() {
        Log.d(TAG, "Starting database fix for approval system");
        Toast.makeText(this, "Starting database fix...", Toast.LENGTH_SHORT).show();
        
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    Toast.makeText(LoginActivity.this, "No users found in database", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                Map<String, Object> updates = new HashMap<>();
                int[] processedCount = {0}; // Use array to make it effectively final
                
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    String uid = userSnapshot.getKey();
                    String currentStatus = userSnapshot.child("status").getValue(String.class);
                    String email = userSnapshot.child("email").getValue(String.class);
                    
                    // Fix users with activity status instead of approval status
                    if ("offline".equals(currentStatus) || "online".equals(currentStatus) || currentStatus == null ||
                        (!UserProfile.STATUS_PENDING.equals(currentStatus) && 
                         !UserProfile.STATUS_APPROVED.equals(currentStatus) && 
                         !UserProfile.STATUS_REJECTED.equals(currentStatus))) {
                        
                        // Set existing users as approved
                        updates.put(uid + "/status", UserProfile.STATUS_APPROVED);
                        
                        // Add missing fields
                        if (!userSnapshot.hasChild("registrationTimestamp")) {
                            updates.put(uid + "/registrationTimestamp", System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000));
                        }
                        if (!userSnapshot.hasChild("lastLoginTimestamp")) {
                            updates.put(uid + "/lastLoginTimestamp", 0L);
                        }
                        if (!userSnapshot.hasChild("profileImageUrl")) {
                            updates.put(uid + "/profileImageUrl", "");
                        }
                        if (!userSnapshot.hasChild("phoneNumber")) {
                            updates.put(uid + "/phoneNumber", "");
                        }
                        
                        // Preserve original activity status
                        if ("offline".equals(currentStatus) || "online".equals(currentStatus)) {
                            updates.put(uid + "/activityStatus", currentStatus);
                        }
                        
                        processedCount[0]++;
                        Log.d(TAG, "Prepared fix for user: " + (email != null ? email : uid));
                    }
                }
                
                if (updates.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "All users already configured for approval system!", Toast.LENGTH_LONG).show();
                    return;
                }
                
                // Apply updates
                final int finalProcessedCount = processedCount[0];
                Log.d(TAG, "Applying fixes for " + finalProcessedCount + " users");
                databaseReference.updateChildren(updates)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            String message = "✅ Database fixed! " + finalProcessedCount + " users updated. All existing users can now login!";
                            Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
                            Log.d(TAG, "Database fix completed successfully");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            String errorMsg = "Database fix failed: " + e.getMessage();
                            Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Database fix failed", e);
                        }
                    });
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                String errorMsg = "Failed to read users: " + databaseError.getMessage();
                Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                Log.e(TAG, "Failed to read users", databaseError.toException());
            }
        });
    }
}
