package com.sameetasadullah.i180479_180531;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
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
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * Registration Activity with Admin Approval System
 * 
 * Flow:
 * 1. User registers with Firebase Auth
 * 2. Profile is created in Realtime Database with status="pending"
 * 3. User is immediately signed out
 * 4. User cannot login until admin approves via Firebase Console
 */
public class RegistrationActivity extends AppCompatActivity {
    
    private static final String TAG = "RegistrationActivity";
    
    // UI Components
    private EditText etFullName, etEmail, etPassword, etConfirmPassword;
    private Button btnRegister;
    private TextView tvLoginLink;
    private ProgressBar progressBar;
    
    // Firebase components
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);
        
        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("users");
        
        initializeViews();
        setupClickListeners();
        
        Log.d(TAG, "Registration Activity initialized");
    }
    
    /**
     * Initialize UI components
     */
    private void initializeViews() {
        etFullName = findViewById(R.id.et_full_name);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnRegister = findViewById(R.id.btn_register);
        tvLoginLink = findViewById(R.id.tv_login_link);
        progressBar = findViewById(R.id.progress_bar);
    }
    
    /**
     * Setup click listeners
     */
    private void setupClickListeners() {
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });
        
        tvLoginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(RegistrationActivity.this, LoginActivity.class));
                finish();
            }
        });
    }
    
    /**
     * Register new user with validation
     */
    private void registerUser() {
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        
        // Validate inputs
        if (!validateInputs(fullName, email, password, confirmPassword)) {
            return;
        }
        
        showProgress(true);
        
        // Create user with Firebase Authentication
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Firebase Auth registration successful");
                            
                            FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                            if (firebaseUser != null) {
                                // Update Firebase Auth profile with display name
                                updateFirebaseProfile(firebaseUser, fullName, email);
                            } else {
                                showProgress(false);
                                Toast.makeText(RegistrationActivity.this,
                                        "Registration failed: User data not found", 
                                        Toast.LENGTH_LONG).show();
                            }
                        } else {
                            showProgress(false);
                            String errorMessage = task.getException() != null ? 
                                task.getException().getMessage() : "Registration failed";
                            Log.e(TAG, "Registration failed: " + errorMessage);
                            Toast.makeText(RegistrationActivity.this,
                                    "Registration failed: " + errorMessage, 
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
    
    /**
     * Update Firebase Auth profile with display name
     */
    private void updateFirebaseProfile(FirebaseUser firebaseUser, String fullName, String email) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(fullName)
                .build();
        
        firebaseUser.updateProfile(profileUpdates)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Firebase Auth profile updated successfully");
                            // Create user profile in Realtime Database
                            createUserProfileInDatabase(firebaseUser.getUid(), email, fullName);
                        } else {
                            Log.w(TAG, "Failed to update Firebase Auth profile, continuing with database creation");
                            // Continue even if profile update fails
                            createUserProfileInDatabase(firebaseUser.getUid(), email, fullName);
                        }
                    }
                });
    }
    
    /**
     * Create user profile in Firebase Realtime Database with "pending" status
     */
    private void createUserProfileInDatabase(String uid, String email, String fullName) {
        Log.d(TAG, "=== CREATING USER PROFILE IN DATABASE ===");
        Log.d(TAG, "UID: " + uid);
        Log.d(TAG, "Email: " + email);
        Log.d(TAG, "Full Name: " + fullName);
        Log.d(TAG, "Database Reference Path: " + databaseReference.getPath());
        
        // Make variables final for use in inner classes
        final String finalUid = uid;
        final String finalEmail = email;
        final String finalFullName = fullName;
        
        // Create UserProfile object with pending status
        UserProfile userProfile = new UserProfile(finalUid, finalEmail, finalFullName);
        
        Log.d(TAG, "UserProfile created: " + userProfile.toString());
        Log.d(TAG, "UserProfile status: " + userProfile.getStatus());
        Log.d(TAG, "UserProfile displayName: " + userProfile.getDisplayName());
        
        // Save to Firebase Realtime Database
        Log.d(TAG, "Attempting to save to path: users/" + finalUid);
        databaseReference.child(finalUid).setValue(userProfile)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "✅ SUCCESS: User profile created successfully in database");
                        Log.d(TAG, "✅ User should now be visible in Firebase Console at path: users/" + finalUid);
                        Log.d(TAG, "✅ Status should be: pending");
                        
                        // Show additional success message
                        Toast.makeText(RegistrationActivity.this, 
                                "✅ User profile created successfully!", 
                                Toast.LENGTH_SHORT).show();
                        
                        // Sign out user immediately after registration
                        signOutUserAfterRegistration(finalEmail);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        showProgress(false);
                        Log.e(TAG, "❌ FAILED: Unable to create user profile in database");
                        Log.e(TAG, "❌ Error type: " + e.getClass().getSimpleName());
                        Log.e(TAG, "❌ Error message: " + e.getMessage());
                        Log.e(TAG, "❌ Full error: ", e);
                        
                        // Delete the Firebase Auth user since database creation failed
                        deleteFirebaseAuthUser();
                        
                        Toast.makeText(RegistrationActivity.this,
                                "❌ Registration failed: Unable to create user profile. Error: " + e.getMessage(), 
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
    
    /**
     * Sign out user after successful registration
     */
    private void signOutUserAfterRegistration(String email) {
        firebaseAuth.signOut();
        showProgress(false);
        
        Log.d(TAG, "User signed out after registration: " + email);
        
        // Make email final for use in inner class
        final String finalEmail = email;
        
        // Show success message and redirect to login
        Toast.makeText(this, 
                "Registration successful! Your account is pending admin approval. " +
                "You will be able to login once approved.", 
                Toast.LENGTH_LONG).show();
        
        // Clear form fields
        clearFormFields();
        
        // Redirect to login activity after a short delay
        etFullName.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
                intent.putExtra("registered_email", finalEmail);
                startActivity(intent);
                finish();
            }
        }, 2000);
    }
    
    /**
     * Delete Firebase Auth user if database creation fails
     */
    private void deleteFirebaseAuthUser() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            user.delete()
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "Firebase Auth user deleted due to database creation failure");
                            } else {
                                Log.e(TAG, "Failed to delete Firebase Auth user", task.getException());
                            }
                        }
                    });
        }
    }
    
    /**
     * Validate user input
     */
    private boolean validateInputs(String fullName, String email, String password, String confirmPassword) {
        // Reset error states
        etFullName.setError(null);
        etEmail.setError(null);
        etPassword.setError(null);
        etConfirmPassword.setError(null);
        
        boolean isValid = true;
        
        // Validate full name
        if (TextUtils.isEmpty(fullName)) {
            etFullName.setError("Full name is required");
            isValid = false;
        } else if (fullName.length() < 2) {
            etFullName.setError("Full name must be at least 2 characters");
            isValid = false;
        }
        
        // Validate email
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email address");
            isValid = false;
        }
        
        // Validate password
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            isValid = false;
        } else if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            isValid = false;
        }
        
        // Validate confirm password
        if (TextUtils.isEmpty(confirmPassword)) {
            etConfirmPassword.setError("Please confirm your password");
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            isValid = false;
        }
        
        return isValid;
    }
    
    /**
     * Show/hide progress indicator
     */
    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!show);
        
        // Disable input fields during registration
        etFullName.setEnabled(!show);
        etEmail.setEnabled(!show);
        etPassword.setEnabled(!show);
        etConfirmPassword.setEnabled(!show);
    }
    
    /**
     * Clear all form fields
     */
    private void clearFormFields() {
        etFullName.setText("");
        etEmail.setText("");
        etPassword.setText("");
        etConfirmPassword.setText("");
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        
        // If user is already logged in, redirect to appropriate activity
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "User already logged in, redirecting...");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }
}