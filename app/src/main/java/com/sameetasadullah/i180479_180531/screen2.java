package com.sameetasadullah.i180479_180531;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.content.Intent;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;

public class screen2 extends AppCompatActivity {

    EditText email, password;
    RelativeLayout loginButton;
    TextView registerButton;
    private FirebaseAuth mAuth;
    private boolean isLoggingIn = false; // Prevent multiple concurrent requests

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen2);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI components with correct IDs from layout
        email = findViewById(R.id.et_email_address);
        password = findViewById(R.id.et_password);
        loginButton = findViewById(R.id.rl_login_button);
        registerButton = findViewById(R.id.register);

        // Temporary: Check if buttons exist before setting listeners to prevent crashes
        if (loginButton != null) {
            loginButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Prevent multiple concurrent login attempts
                    if (isLoggingIn) {
                        Toast.makeText(screen2.this, "Login in progress...", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    if (email != null && password != null) {
                        String emailText = email.getText().toString().trim();
                        String passwordText = password.getText().toString().trim();
                        
                        if (emailText.isEmpty() || passwordText.isEmpty()) {
                            Toast.makeText(screen2.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        // Basic email validation
                        if (!emailText.contains("@") || !emailText.contains(".")) {
                            Toast.makeText(screen2.this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        // Set login state
                        isLoggingIn = true;
                        Toast.makeText(screen2.this, "Signing in...", Toast.LENGTH_SHORT).show();
                        
                        // Firebase sign in with email and password
                        mAuth.signInWithEmailAndPassword(emailText, passwordText)
                            .addOnCompleteListener(screen2.this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    isLoggingIn = false; // Reset state
                                    
                                    if (task.isSuccessful()) {
                                        // Sign in success
                                        Log.d("Screen2", "signInWithEmail:success");
                                        FirebaseUser user = mAuth.getCurrentUser();
                                        Toast.makeText(screen2.this, "Login successful!", Toast.LENGTH_SHORT).show();
                                        
                                        // Navigate to main activity
                                        Intent intent = new Intent(screen2.this, fragmentsContainer.class);
                                        startActivity(intent);
                                        finish(); // Close login screen
                                    } else {
                                        // Sign in failed
                                        Log.w("Screen2", "signInWithEmail:failure", task.getException());
                                        String errorMessage = "Authentication failed.";
                                        
                                        // Provide more specific error messages
                                        if (task.getException() != null) {
                                            String exceptionMessage = task.getException().getMessage();
                                            if (exceptionMessage != null) {
                                                if (exceptionMessage.contains("password")) {
                                                    errorMessage = "Invalid password. Please try again.";
                                                } else if (exceptionMessage.contains("email")) {
                                                    errorMessage = "Invalid email address.";
                                                } else if (exceptionMessage.contains("user not found")) {
                                                    errorMessage = "No account found with this email. Please sign up first.";
                                                }
                                            }
                                        }
                                        
                                        Toast.makeText(screen2.this, errorMessage, Toast.LENGTH_LONG).show();
                                    }
                                }
                            });
                    }
                }
            });
        } else {
            // Show message that UI is being migrated
            Toast.makeText(this, "Screen under migration - missing layout resources", Toast.LENGTH_LONG).show();
        }

        if (registerButton != null) {
            registerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(screen2.this, screen3.class);
                    startActivity(intent);
                }
            });
        }
    }
}

