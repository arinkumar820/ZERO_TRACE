package com.sameetasadullah.i180479_180531;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * AppLockActivity - Handles app lock/unlock functionality
 * This activity is displayed when the app is locked and requires authentication
 */
public class AppLockActivity extends AppCompatActivity {
    
    private static final String TAG = "AppLockActivity";
    private EditText etPassword;
    private Button btnUnlock;
    private TextView tvMessage;
    private SharedPreferences securityPrefs;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Create simple lock screen layout programmatically
        setupLockScreen();
        
        securityPrefs = getSharedPreferences("security_preferences", MODE_PRIVATE);
        
        Log.d(TAG, "App lock screen displayed");
    }
    
    private void setupLockScreen() {
        // For now, create a simple programmatic layout
        // In a full implementation, you would create an XML layout
        
        setContentView(R.layout.activity_login); // Reuse login layout temporarily
        
        // Initialize views using login layout IDs
        try {
            etPassword = findViewById(R.id.et_password);
            btnUnlock = findViewById(R.id.btn_login);
            tvMessage = findViewById(R.id.app_title);
            
            if (tvMessage != null) {
                tvMessage.setText("🔒 App Locked");
            }
            
            if (btnUnlock != null) {
                btnUnlock.setText("Unlock");
                btnUnlock.setOnClickListener(v -> attemptUnlock());
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting up lock screen", e);
            // Fallback - just show a toast and navigate to login
            showUnlockFailure();
        }
    }
    
    private void attemptUnlock() {
        if (etPassword == null) {
            showUnlockFailure();
            return;
        }
        
        String password = etPassword.getText().toString().trim();
        
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please enter password", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // For now, accept any non-empty password
        // In a full implementation, you would verify against stored password/PIN
        if (password.length() >= 4) {
            unlockApp();
        } else {
            Toast.makeText(this, "Invalid password", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void unlockApp() {
        Log.d(TAG, "App unlocked successfully");
        
        // Mark app as unlocked
        securityPrefs.edit().putBoolean("app_locked", false).apply();
        
        // Navigate back to main app
        Intent intent = new Intent(this, fragmentsContainer.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        
        Toast.makeText(this, "App unlocked", Toast.LENGTH_SHORT).show();
        finish();
    }
    
    private void showUnlockFailure() {
        Toast.makeText(this, "Unable to setup lock screen. Redirecting to login.", Toast.LENGTH_LONG).show();
        
        // Navigate to login as fallback
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    @Override
    public void onBackPressed() {
        // Prevent user from bypassing lock screen with back button
        Toast.makeText(this, "App is locked. Please enter password to unlock.", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // Check if app is still locked
        boolean isLocked = securityPrefs.getBoolean("app_locked", false);
        if (!isLocked) {
            // App was unlocked elsewhere, close this activity
            finish();
        }
    }
}