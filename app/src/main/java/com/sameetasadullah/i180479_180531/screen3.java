package com.sameetasadullah.i180479_180531;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import android.content.Intent;
import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import androidx.annotation.NonNull;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;

public class screen3 extends AppCompatActivity {

    EditText email, password, confirmPassword;
    RelativeLayout registerButton;
    TextView loginLink;
    private Toast currentToast; // To manage toast overflow
    private boolean isRegistering = false; // Prevent multiple requests
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("Screen3", "onCreate: Starting screen3 activity");
        setContentView(R.layout.activity_screen3);
        
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize with correct IDs from layout
        Log.d("Screen3", "Initializing UI components");
        email = findViewById(R.id.et_email_address);
        password = findViewById(R.id.et_password);
        confirmPassword = findViewById(R.id.et_confirm_password);
        registerButton = findViewById(R.id.rl_signup_button);
        loginLink = findViewById(R.id.login);
        
        Log.d("Screen3", "UI Components initialized - email: " + (email != null) + ", password: " + (password != null) + ", confirmPassword: " + (confirmPassword != null) + ", registerButton: " + (registerButton != null));
        
        // IMMEDIATE TEST: Log something right away to verify logging works
        Log.e("LOGGING_TEST", "=== CRITICAL: This log should appear immediately if logging works ====");
        System.out.println("CONSOLE_TEST: This should appear in system output");

        // Temporary: Check if button exists before setting listener to prevent crashes
        if (registerButton != null) {
            registerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d("Screen3", "Register button clicked!");
                    
                    // Prevent multiple concurrent requests
                    if (isRegistering) {
                        showSafeToast("Registration in progress...", Toast.LENGTH_SHORT);
                        return;
                    }
                    
                    // Validate input fields
                    if (email == null || password == null || confirmPassword == null) {
                        showSafeToast("Form elements not found", Toast.LENGTH_SHORT);
                        return;
                    }
                    
                    String emailText = email.getText().toString().trim();
                    String passwordText = password.getText().toString().trim();
                    String confirmPasswordText = confirmPassword.getText().toString().trim();
                    
                    if (emailText.isEmpty() || passwordText.isEmpty() || confirmPasswordText.isEmpty()) {
                        showSafeToast("Please fill all fields", Toast.LENGTH_SHORT);
                        return;
                    }
                    
                    // Basic email validation
                    if (!emailText.contains("@") || !emailText.contains(".")) {
                        showSafeToast("Please enter a valid email address", Toast.LENGTH_SHORT);
                        return;
                    }
                    
                    // Password strength check
                    if (passwordText.length() < 6) {
                        showSafeToast("Password must be at least 6 characters long", Toast.LENGTH_SHORT);
                        return;
                    }
                    
                    if (!passwordText.equals(confirmPasswordText)) {
                        showSafeToast("Passwords do not match", Toast.LENGTH_SHORT);
                        return;
                    }
                    
                    // Set registration state
                    isRegistering = true;
                    
                    // Show immediate feedback to user
                    showSafeToast("Creating account...", Toast.LENGTH_SHORT);
                    
                    // Firebase create user with email and password
                    Log.d("Screen3", "Starting Firebase signup for email: " + emailText);
                    mAuth.createUserWithEmailAndPassword(emailText, passwordText)
                        .addOnCompleteListener(screen3.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                isRegistering = false; // Reset state
                                
                                if (task.isSuccessful()) {
                                    // Sign up success
                                    Log.d("Screen3", "createUserWithEmail:success");
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    showSafeToast("Registration successful!", Toast.LENGTH_SHORT);

                                    // Also add minimal searchable profile into Firebase Realtime Database
                                    try {
                                        if (user != null) {
                                            String uid = user.getUid();
                                            String emailLower = emailText.toLowerCase();
                                            String displayName = emailLower.contains("@") ? emailLower.substring(0, emailLower.indexOf("@")) : emailLower;
                                            DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");
                                            usersRef.child(uid).child("uid").setValue(uid);
                                            usersRef.child(uid).child("email").setValue(emailText);
                                            usersRef.child(uid).child("display_name").setValue(displayName);
                                            usersRef.child(uid).child("email_lower").setValue(emailLower);
                                            usersRef.child(uid).child("name_lower").setValue(displayName);
                                            usersRef.child(uid).child("status").setValue("online");
                                            usersRef.child(uid).child("last_seen").setValue(System.currentTimeMillis());
                                            Log.d("Screen3", "✅ User indexed in Firebase RTDB: " + emailText);
                                        }
                                    } catch (Exception ex) {
                                        Log.w("Screen3", "⚠️ Failed to index user in Firebase RTDB", ex);
                                    }

                                    // Redirect to input credentials for profile setup
                                    Intent intent = new Intent(screen3.this, inputCredentials.class);
                                    intent.putExtra("user_email", emailText);
                                    intent.putExtra("user_uid", user != null ? user.getUid() : "");
                                    startActivity(intent);
                                    finish(); // Close registration screen
                                } else {
                                    // Sign up failed
                                    Log.w("Screen3", "createUserWithEmail:failure", task.getException());
                                    String errorMessage = "Registration failed.";
                                    
                                    // Provide more specific error messages
                                    if (task.getException() != null) {
                                        String exceptionMessage = task.getException().getMessage();
                                        if (exceptionMessage != null) {
                                            if (exceptionMessage.contains("email address is already in use")) {
                                                errorMessage = "This email address is already registered. Please try signing in instead.";
                                            } else if (exceptionMessage.contains("weak password")) {
                                                errorMessage = "Password is too weak. Please use at least 6 characters.";
                                            } else if (exceptionMessage.contains("email address is badly formatted")) {
                                                errorMessage = "Please enter a valid email address.";
                                            } else {
                                                errorMessage = "Registration failed: " + exceptionMessage;
                                            }
                                        }
                                    }
                                    
                                    showSafeToast(errorMessage, Toast.LENGTH_LONG);
                                }
                            }
                        });
                }
            });
        } else {
            showSafeToast("Missing signup button in layout", Toast.LENGTH_LONG);
        }

        if (loginLink != null) {
            loginLink.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }
    }
    
    // Helper method to prevent toast overflow
    private void showSafeToast(String message, int duration) {
        if (currentToast != null) {
            currentToast.cancel();
        }
        currentToast = Toast.makeText(this, message, duration);
        currentToast.show();
    }
}
