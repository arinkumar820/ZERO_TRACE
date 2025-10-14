package com.sameetasadullah.i180479_180531;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import java.util.List;

/**
 * Enhanced Authentication Activity
 * 
 * This activity handles:
 * 1. Firebase Authentication (login/register)
 * 2. User synchronization with MySQL database via WebSocket
 * 3. Seamless transition to the main app after successful authentication
 */
public class EnhancedAuthenticationActivity extends AppCompatActivity implements EnhancedWebSocketClient.EnhancedWebSocketListener {

    private static final String TAG = "EnhancedAuthActivity";
    
    // UI Components
    private EditText etEmail, etPassword, etName, etPhone, etBio;
    private Button btnLogin, btnRegister, btnSwitchMode, btnForgotPassword;
    private TextView tvTitle, tvSubtitle;
    private ImageView ivLogo, ivPasswordVisibility;
    private ProgressBar progressBar;
    
    // Firebase
    private FirebaseAuth firebaseAuth;
    
    // WebSocket Client for MySQL sync
    private EnhancedWebSocketClient webSocketClient;
    
    // State
    private boolean isLoginMode = true;
    private boolean isPasswordVisible = false;
    private boolean isAuthenticating = false;
    private FirebaseUser pendingUser = null; // User waiting for MySQL sync
    
    // SharedPreferences for user data
    private SharedPreferences sharedPreferences;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_credentials);
        
        initializeComponents();
        setupFirebase();
        setupWebSocket();
        setupUI();
        
        // Check if user is already logged in
        checkExistingLogin();
        
        Log.d(TAG, "Enhanced Authentication Activity created");
    }
    
    private void initializeComponents() {
        // Find UI components - adapting to existing layout
        etEmail = null; // Will need to add email field to existing layout or repurpose existing
        etPassword = null; // Will need to add password field to existing layout or repurpose existing
        etName = findViewById(R.id.first_name); // Using first name field
        etPhone = findViewById(R.id.phoneNumber);
        etBio = findViewById(R.id.bio);
        
        btnLogin = null; // Not available in existing layout
        btnRegister = findViewById(R.id.create); // Using create button
        btnSwitchMode = null; // Not available in existing layout
        btnForgotPassword = null; // Not available in existing layout
        
        tvTitle = findViewById(R.id.create_profile);
        tvSubtitle = null; // Not available in existing layout
        
        ivLogo = null; // Not available in existing layout
        ivPasswordVisibility = null; // Not available in existing layout
        
        progressBar = null; // Not available in existing layout
        
        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
    }
    
    private void setupFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
    }
    
    private void setupWebSocket() {
        webSocketClient = new EnhancedWebSocketClient(this, this);
        webSocketClient.connect();
    }
    
    private void setupUI() {
        updateUIForMode();
        setupClickListeners();
    }
    
    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> handleLogin());
        btnRegister.setOnClickListener(v -> handleRegister());
        btnSwitchMode.setOnClickListener(v -> switchAuthMode());
        btnForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
        ivPasswordVisibility.setOnClickListener(v -> togglePasswordVisibility());
    }
    
    private void updateUIForMode() {
        if (isLoginMode) {
            // Login mode
            tvTitle.setText("Welcome Back!");
            tvSubtitle.setText("Sign in to continue");
            
            etName.setVisibility(View.GONE);
            etPhone.setVisibility(View.GONE);
            etBio.setVisibility(View.GONE);
            
            btnLogin.setVisibility(View.VISIBLE);
            btnRegister.setVisibility(View.GONE);
            btnForgotPassword.setVisibility(View.VISIBLE);
            
            btnSwitchMode.setText("New user? Create account");
            
        } else {
            // Register mode
            tvTitle.setText("Create Account");
            tvSubtitle.setText("Join Bisto Chat today");
            
            etName.setVisibility(View.VISIBLE);
            etPhone.setVisibility(View.VISIBLE);
            etBio.setVisibility(View.VISIBLE);
            
            btnLogin.setVisibility(View.GONE);
            btnRegister.setVisibility(View.VISIBLE);
            btnForgotPassword.setVisibility(View.GONE);
            
            btnSwitchMode.setText("Already have account? Sign in");
        }
    }
    
    private void switchAuthMode() {
        isLoginMode = !isLoginMode;
        updateUIForMode();
        clearFields();
    }
    
    private void clearFields() {
        etEmail.setText("");
        etPassword.setText("");
        etName.setText("");
        etPhone.setText("");
        etBio.setText("");
    }
    
    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            etPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            // Use system drawable for visibility off
            ivPasswordVisibility.setImageResource(android.R.drawable.ic_menu_view);
        } else {
            etPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            // Use system drawable for visibility on
            ivPasswordVisibility.setImageResource(android.R.drawable.ic_menu_view);
        }
        etPassword.setSelection(etPassword.length());
        isPasswordVisible = !isPasswordVisible;
    }
    
    private void checkExistingLogin() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "User already logged in: " + currentUser.getEmail());
            // User is already authenticated, sync with MySQL and proceed
            syncUserWithMySQL(currentUser, true);
        }
    }
    
    // ====================================================================================
    // AUTHENTICATION METHODS
    // ====================================================================================
    
    private void handleLogin() {
        if (isAuthenticating) return;
        
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        
        if (!validateLoginInput(email, password)) {
            return;
        }
        
        setAuthenticating(true);
        
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    Log.d(TAG, "Firebase login successful");
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        syncUserWithMySQL(user, false);
                    } else {
                        setAuthenticating(false);
                        showToast("Login failed - no user data");
                    }
                })
                .addOnFailureListener(e -> {
                    setAuthenticating(false);
                    Log.e(TAG, "Firebase login failed", e);
                    showToast("Login failed: " + e.getMessage());
                });
    }
    
    private void handleRegister() {
        if (isAuthenticating) return;
        
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String bio = etBio.getText().toString().trim();
        
        if (!validateRegisterInput(email, password, name)) {
            return;
        }
        
        setAuthenticating(true);
        
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    Log.d(TAG, "Firebase registration successful");
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        // Update Firebase profile with display name
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(name)
                                .build();
                        
                        user.updateProfile(profileUpdates)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Firebase profile updated");
                                    // Also write minimal searchable profile to Firebase Realtime Database
                                    writeUserToFirebaseDatabase(user.getUid(), name, email);
                                    syncUserWithMySQL(user, false);
                                })
                                .addOnFailureListener(e -> {
                                    Log.w(TAG, "Failed to update Firebase profile", e);
                                    // Still write minimal profile and continue
                                    writeUserToFirebaseDatabase(user.getUid(), name, email);
                                    syncUserWithMySQL(user, false);
                                });
                    } else {
                        setAuthenticating(false);
                        showToast("Registration failed - no user data");
                    }
                })
                .addOnFailureListener(e -> {
                    setAuthenticating(false);
                    Log.e(TAG, "Firebase registration failed", e);
                    showToast("Registration failed: " + e.getMessage());
                });
    }
    
    // ====================================================================================
    // MYSQL SYNCHRONIZATION
    // ====================================================================================
    
    private void writeUserToFirebaseDatabase(String uid, String name, String email) {
        try {
            DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("uid", uid);
            userMap.put("email", email);
            userMap.put("display_name", name);
            userMap.put("email_lower", email != null ? email.toLowerCase() : "");
            userMap.put("name_lower", name != null ? name.toLowerCase() : "");
            userMap.put("status", "online");
            userMap.put("last_seen", System.currentTimeMillis());
            usersRef.child(uid).updateChildren(userMap);
            Log.d(TAG, "✅ Wrote user to Firebase RTDB for search: " + email);
        } catch (Exception e) {
            Log.w(TAG, "⚠️ Failed to write user to Firebase RTDB", e);
        }
    }

    private void syncUserWithMySQL(FirebaseUser firebaseUser, boolean isExistingLogin) {
        if (!webSocketClient.isConnected()) {
            Log.w(TAG, "WebSocket not connected, attempting to connect for user sync");
            pendingUser = firebaseUser;
            webSocketClient.connect();
            return;
        }
        
        String uid = firebaseUser.getUid();
        String name = firebaseUser.getDisplayName();
        String email = firebaseUser.getEmail();
        
        // Get additional data from form (if available)
        String phone = etPhone.getText().toString().trim();
        String bio = etBio.getText().toString().trim();
        
        // Use default values if fields are empty
        if (name == null || name.isEmpty()) {
            name = email.substring(0, email.indexOf("@")); // Use email prefix as fallback
        }
        if (bio.isEmpty()) {
            bio = "Hey there! I am using Bisto Chat.";
        }
        
        Log.d(TAG, "Syncing user with MySQL: " + email);
        webSocketClient.saveUser(uid, name, email, phone.isEmpty() ? null : phone, bio);
    }
    
    // ====================================================================================
    // UI HELPER METHODS
    // ====================================================================================
    
    private void setAuthenticating(boolean authenticating) {
        isAuthenticating = authenticating;
        
        if (authenticating) {
            progressBar.setVisibility(View.VISIBLE);
            btnLogin.setEnabled(false);
            btnRegister.setEnabled(false);
            btnSwitchMode.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            btnLogin.setEnabled(true);
            btnRegister.setEnabled(true);
            btnSwitchMode.setEnabled(true);
        }
    }
    
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    private void showForgotPasswordDialog() {
        EditText emailInput = new EditText(this);
        emailInput.setHint("Enter your email");
        emailInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        
        new AlertDialog.Builder(this)
                .setTitle("Reset Password")
                .setMessage("Enter your email address to receive password reset instructions")
                .setView(emailInput)
                .setPositiveButton("Send", (dialog, which) -> {
                    String email = emailInput.getText().toString().trim();
                    if (email.isEmpty()) {
                        showToast("Please enter your email");
                        return;
                    }
                    
                    firebaseAuth.sendPasswordResetEmail(email)
                            .addOnSuccessListener(aVoid -> {
                                showToast("Password reset email sent");
                            })
                            .addOnFailureListener(e -> {
                                showToast("Failed to send reset email: " + e.getMessage());
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    // ====================================================================================
    // VALIDATION METHODS
    // ====================================================================================
    
    private boolean validateLoginInput(String email, String password) {
        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return false;
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Invalid email format");
            etEmail.requestFocus();
            return false;
        }
        
        if (password.isEmpty()) {
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
    
    private boolean validateRegisterInput(String email, String password, String name) {
        if (!validateLoginInput(email, password)) {
            return false;
        }
        
        if (name.isEmpty()) {
            etName.setError("Name is required");
            etName.requestFocus();
            return false;
        }
        
        if (name.length() < 2) {
            etName.setError("Name must be at least 2 characters");
            etName.requestFocus();
            return false;
        }
        
        return true;
    }
    
    // ====================================================================================
    // ENHANCED WEBSOCKET LISTENER IMPLEMENTATION
    // ====================================================================================
    
    @Override
    public void onConnected() {
        Log.d(TAG, "WebSocket connected for authentication");
        
        // If we have a pending user sync, do it now
        if (pendingUser != null) {
            syncUserWithMySQL(pendingUser, true);
            pendingUser = null;
        }
    }
    
    @Override
    public void onDisconnected(int code, String reason, boolean remote) {
        Log.w(TAG, "WebSocket disconnected during authentication: " + reason);
        
        // If we're in the middle of authentication, show error
        if (isAuthenticating) {
            setAuthenticating(false);
            showToast("Connection lost. Please try again.");
        }
    }
    
    @Override
    public void onReconnecting(int attempt) {
        Log.d(TAG, "WebSocket reconnecting attempt: " + attempt);
    }
    
    @Override
    public void onError(Exception error) {
        Log.e(TAG, "WebSocket error during authentication", error);
        
        if (isAuthenticating) {
            setAuthenticating(false);
            showToast("Connection error. Please check your internet and try again.");
        }
    }
    
    @Override
    public void onUserSaved(String userUid, String message) {
        Log.d(TAG, "User successfully synced with MySQL: " + userUid);
        
        // Save user info to SharedPreferences
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("user_uid", currentUser.getUid());
            editor.putString("user_email", currentUser.getEmail());
            editor.putString("user_name", currentUser.getDisplayName());
            editor.putBoolean("is_logged_in", true);
            editor.apply();
        }
        
        setAuthenticating(false);
        showToast("Welcome to Bisto Chat!");
        
        // Navigate to main activity
        navigateToMainActivity();
    }
    
    @Override
    public void onUserSaveFailed(String error) {
        Log.e(TAG, "Failed to sync user with MySQL: " + error);
        setAuthenticating(false);
        
        // Show error but don't prevent login if Firebase auth succeeded
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            showToast("Logged in but sync failed. Some features may be limited.");
            navigateToMainActivity();
        } else {
            showToast("User synchronization failed: " + error);
        }
    }
    
    // Unused WebSocket listener methods (required by interface)
    @Override public void onUsersReceived(List<User> users, String searchQuery) { }
    @Override public void onUsersRequestFailed(String error) { }
    @Override public void onUserProfileReceived(User user) { }
    @Override public void onUserProfileRequestFailed(String error) { }
    @Override public void onRoomJoined(String roomId, String message) { }
    @Override public void onRoomJoinFailed(String error) { }
    @Override public void onMessageSent(String messageId) { }
    @Override public void onMessageSendFailed(String error) { }
    @Override public void onMessageReceived(EnhancedWebSocketClient.ChatMessage message) { }
    @Override public void onUserJoinedRoom(String roomId, String userName) { }
    
    // ====================================================================================
    // NAVIGATION
    // ====================================================================================
    
    private void navigateToMainActivity() {
        Intent intent = new Intent(this, fragmentsContainer.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webSocketClient != null) {
            webSocketClient.cleanup();
        }
    }
}