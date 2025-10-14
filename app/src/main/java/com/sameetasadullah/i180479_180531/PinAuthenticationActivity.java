package com.sameetasadullah.i180479_180531;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

/**
 * PIN Authentication Activity - First screen shown when app starts
 * Provides secure PIN-based authentication before accessing the main app
 */
public class PinAuthenticationActivity extends AppCompatActivity {

    private static final String TAG = "PinAuthentication";
    private static final String PREFS_NAME = "pin_preferences";
    private static final String PIN_KEY = "app_pin";
    private static final String FIRST_RUN_KEY = "first_run";
    private static final String ATTEMPTS_KEY = "pin_attempts";
    private static final String LOCKED_KEY = "app_locked";
    private static final String DEFAULT_PIN = "123456"; // 6-digit PIN
    private static final int MAX_ATTEMPTS = 2; // Only 2 attempts allowed

    // UI Components
    private TextView tvSecurityMessage;
    private TextView tvErrorMessage;
    private TextView[] pinDots;
    private StringBuilder currentPin;
    private Vibrator vibrator;

    // PIN Management
    private boolean isSettingNewPin = false;
    private String newPinConfirmation = "";
    
    // Secret gesture for admin unlock
    private int tapCount = 0;
    private long lastTapTime = 0;
    private static final int SECRET_TAP_COUNT = 7; // 7 taps to unlock
    private static final long TAP_TIMEOUT = 2000; // 2 seconds between taps

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_authentication);

        Log.i(TAG, "🔒 PIN Authentication Activity started");

        initializeComponents();
        setupPinKeypad();
        checkFirstRun();
    }

    private void initializeComponents() {
        tvSecurityMessage = findViewById(R.id.tv_security_message);
        tvErrorMessage = findViewById(R.id.tv_error_message);
        
        // Initialize PIN dots for 6-digit PIN
        pinDots = new TextView[6];
        pinDots[0] = findViewById(R.id.pin_dot_1);
        pinDots[1] = findViewById(R.id.pin_dot_2);
        pinDots[2] = findViewById(R.id.pin_dot_3);
        pinDots[3] = findViewById(R.id.pin_dot_4);
        pinDots[4] = findViewById(R.id.pin_dot_5);
        pinDots[5] = findViewById(R.id.pin_dot_6);

        currentPin = new StringBuilder();
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
    }

    private void checkFirstRun() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        // Check if app is permanently locked
        if (prefs.getBoolean(LOCKED_KEY, false)) {
            showAppLocked();
            return;
        }
        
        boolean isFirstRun = prefs.getBoolean(FIRST_RUN_KEY, true);
        int remainingAttempts = MAX_ATTEMPTS - prefs.getInt(ATTEMPTS_KEY, 0);

        if (isFirstRun) {
            // First run - set default PIN and show setup message
            prefs.edit()
                    .putString(PIN_KEY, DEFAULT_PIN)
                    .putBoolean(FIRST_RUN_KEY, false)
                    .putInt(ATTEMPTS_KEY, 0)
                    .apply();
            
            tvSecurityMessage.setText("Welcome! Default PIN is 123456\nEnter PIN to continue");
            Toast.makeText(this, "🔒 Default PIN: 123456", Toast.LENGTH_LONG).show();
            Log.i(TAG, "✅ First run - default PIN set");
        } else {
            tvSecurityMessage.setText(String.format("Enter your 6-digit PIN\n(%d attempts remaining)", remainingAttempts));
        }
    }

    private void setupPinKeypad() {
        // Number buttons
        int[] numberButtons = {
            R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
            R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9
        };

        for (int i = 0; i < numberButtons.length; i++) {
            Button button = findViewById(numberButtons[i]);
            final int digit = (i == 0) ? 0 : i; // btn_0 is at index 0, but represents digit 0
            
            button.setOnClickListener(v -> {
                addDigit(String.valueOf(digit));
                if (vibrator != null) {
                    vibrator.vibrate(50); // Short haptic feedback
                }
            });
        }

        // Clear button
        findViewById(R.id.btn_clear).setOnClickListener(v -> {
            clearLastDigit();
            if (vibrator != null) {
                vibrator.vibrate(50);
            }
        });

        // Emergency button (optional - could trigger emergency features)
        findViewById(R.id.btn_emergency).setOnClickListener(v -> {
            handleEmergencyButton();
        });
    }

    private void addDigit(String digit) {
        if (currentPin.length() < 6) {
            currentPin.append(digit);
            updatePinDisplay();
            
            Log.d(TAG, "PIN digit added. Length: " + currentPin.length());
            
            if (currentPin.length() == 6) {
                // 6-digit PIN complete - verify after short delay
                new Handler().postDelayed(this::verifyPin, 300);
            }
        }
    }

    private void clearLastDigit() {
        if (currentPin.length() > 0) {
            currentPin.deleteCharAt(currentPin.length() - 1);
            updatePinDisplay();
            hideError();
            Log.d(TAG, "PIN digit cleared. Length: " + currentPin.length());
        }
    }

    private void updatePinDisplay() {
        for (int i = 0; i < pinDots.length; i++) {
            if (i < currentPin.length()) {
                pinDots[i].setBackgroundResource(R.drawable.pin_dot_filled);
            } else {
                pinDots[i].setBackgroundResource(R.drawable.pin_dot_empty);
            }
        }
    }

    private void verifyPin() {
        String enteredPin = currentPin.toString();
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String storedPin = prefs.getString(PIN_KEY, DEFAULT_PIN);
        int currentAttempts = prefs.getInt(ATTEMPTS_KEY, 0);

        Log.d(TAG, "Verifying 6-digit PIN... Attempt: " + (currentAttempts + 1));

        // Check if it's the master PIN first (works even when locked)
        if (AdminUnlockUtility.isMasterPin(this, enteredPin)) {
            Log.w(TAG, "🔑 Master PIN accepted - performing emergency unlock");
            AdminUnlockUtility.emergencyUnlock(this);
            showSuccess();
            new Handler().postDelayed(this::proceedToMainApp, 800);
            return;
        }
        
        if (enteredPin.equals(storedPin)) {
            // PIN correct - reset attempts
            Log.i(TAG, "✅ PIN verification successful");
            prefs.edit().putInt(ATTEMPTS_KEY, 0).apply();
            showSuccess();
            
            // Proceed to main app after short delay
            new Handler().postDelayed(this::proceedToMainApp, 800);
            
        } else {
            // PIN incorrect - increment attempts
            int newAttempts = currentAttempts + 1;
            Log.w(TAG, String.format("❌ PIN verification failed. Attempt %d/%d", newAttempts, MAX_ATTEMPTS));
            
            if (newAttempts >= MAX_ATTEMPTS) {
                // Maximum attempts reached - lock app permanently
                prefs.edit()
                        .putInt(ATTEMPTS_KEY, newAttempts)
                        .putBoolean(LOCKED_KEY, true)
                        .apply();
                        
                Log.e(TAG, "🔒 App permanently locked after " + MAX_ATTEMPTS + " failed attempts");
                showAppLocked();
                
            } else {
                // Still have attempts left
                prefs.edit().putInt(ATTEMPTS_KEY, newAttempts).apply();
                int remaining = MAX_ATTEMPTS - newAttempts;
                
                showError(String.format("Incorrect PIN!\n%d attempt%s remaining", remaining, remaining == 1 ? "" : "s"));
                clearPin();
                
                // Update message to show remaining attempts
                tvSecurityMessage.setText(String.format("Enter your 6-digit PIN\n(%d attempts remaining)", remaining));
                
                if (vibrator != null) {
                    vibrator.vibrate(new long[]{0, 300, 150, 300, 150, 300}, -1);
                }
            }
        }
    }

    private void showSuccess() {
        tvErrorMessage.setVisibility(View.GONE);
        tvSecurityMessage.setText("✅ Access Granted");
        tvSecurityMessage.setTextColor(getColor(android.R.color.holo_green_light));
        
        // Make all dots green
        for (TextView dot : pinDots) {
            dot.setBackgroundResource(R.drawable.pin_dot_filled);
        }
    }

    private void showError(String message) {
        tvErrorMessage.setText(message);
        tvErrorMessage.setVisibility(View.VISIBLE);
        tvSecurityMessage.setTextColor(getColor(android.R.color.holo_red_light));
    }

    private void hideError() {
        tvErrorMessage.setVisibility(View.GONE);
        tvSecurityMessage.setTextColor(getColor(android.R.color.white));
    }
    
    private void showAppLocked() {
        // Disable all input
        findViewById(R.id.gl_keypad).setVisibility(View.GONE);
        
        // Show locked message
        tvSecurityMessage.setText("🔒 APPLICATION LOCKED");
        tvSecurityMessage.setTextColor(getColor(android.R.color.holo_red_dark));
        
        tvErrorMessage.setText("Too many failed attempts.\nApp has been permanently locked.\nContact administrator for access.");
        tvErrorMessage.setVisibility(View.VISIBLE);
        
        // Add secret gesture listener (7 taps to unlock)
        tvSecurityMessage.setOnClickListener(v -> handleSecretTap());
        
        // Make all dots red
        for (TextView dot : pinDots) {
            dot.setBackgroundResource(R.drawable.pin_dot_filled);
            dot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(android.R.color.holo_red_dark)));
        }
        
        Log.e(TAG, "🚫 App permanently locked - no access allowed");
        
        // Strong vibration pattern
        if (vibrator != null) {
            vibrator.vibrate(new long[]{0, 500, 200, 500, 200, 500, 200, 500}, -1);
        }
        
        // Prevent any further interaction
        Toast.makeText(this, "🔒 App permanently locked", Toast.LENGTH_LONG).show();
    }

    private void clearPin() {
        currentPin.setLength(0);
        updatePinDisplay();
    }

    private void proceedToMainApp() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void handleEmergencyButton() {
        Log.w(TAG, "🚨 Emergency button pressed");
        
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isLocked = prefs.getBoolean(LOCKED_KEY, false);
        
        if (isLocked) {
            // Show admin unlock option for locked app
            Toast.makeText(this, "🚨 Admin Emergency Access\nPress and hold for 5 seconds to unlock", Toast.LENGTH_LONG).show();
            
            // Long press handler for admin unlock
            findViewById(R.id.btn_emergency).setOnLongClickListener(v -> {
                unlockApp();
                return true;
            });
        } else {
            // Normal emergency options
            Toast.makeText(this, "🚨 Emergency mode - Hold for 3 seconds to activate", Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * Handle secret tap gesture for emergency unlock
     */
    private void handleSecretTap() {
        long currentTime = System.currentTimeMillis();
        
        // Reset tap count if too much time passed
        if (currentTime - lastTapTime > TAP_TIMEOUT) {
            tapCount = 0;
        }
        
        tapCount++;
        lastTapTime = currentTime;
        
        Log.d(TAG, "Secret tap " + tapCount + "/" + SECRET_TAP_COUNT);
        
        if (tapCount == 3) {
            Toast.makeText(this, "🤫 Keep tapping... (" + tapCount + "/" + SECRET_TAP_COUNT + ")", Toast.LENGTH_SHORT).show();
        } else if (tapCount == 5) {
            Toast.makeText(this, "🔓 Almost there... (" + tapCount + "/" + SECRET_TAP_COUNT + ")", Toast.LENGTH_SHORT).show();
        } else if (tapCount >= SECRET_TAP_COUNT) {
            // Secret unlock triggered
            Log.w(TAG, "🔓 Secret gesture unlock activated!");
            Toast.makeText(this, "🔓 Secret unlock activated!", Toast.LENGTH_SHORT).show();
            
            // Perform emergency unlock
            AdminUnlockUtility.emergencyUnlock(this);
            
            // Restart activity to show unlocked state
            new Handler().postDelayed(() -> {
                Intent intent = getIntent();
                finish();
                startActivity(intent);
            }, 1000);
            
            tapCount = 0; // Reset
        }
    }
    
    /**
     * Admin emergency unlock - resets the app lock (for testing/admin purposes)
     */
    private void unlockApp() {
        Log.w(TAG, "🔓 ADMIN UNLOCK: Resetting app lock");
        
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit()
                .putBoolean(LOCKED_KEY, false)
                .putInt(ATTEMPTS_KEY, 0)
                .apply();
        
        Toast.makeText(this, "🔓 App unlocked by administrator\nRestarting...", Toast.LENGTH_LONG).show();
        
        // Restart the activity
        new Handler().postDelayed(() -> {
            Intent intent = getIntent();
            finish();
            startActivity(intent);
        }, 1500);
    }

    // Prevent back button from exiting app
    @Override
    public void onBackPressed() {
        // Optional: Show "Press again to exit" functionality
        Toast.makeText(this, "Enter PIN to access app", Toast.LENGTH_SHORT).show();
    }

    // Security: Clear PIN from memory when activity is destroyed
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (currentPin != null) {
            // Clear PIN from memory for security
            for (int i = 0; i < currentPin.length(); i++) {
                currentPin.setCharAt(i, '0');
            }
            currentPin.setLength(0);
        }
        Log.i(TAG, "🔒 PIN Authentication Activity destroyed");
    }

    // Lock the app if it goes to background
    @Override
    protected void onPause() {
        super.onPause();
        // Clear entered PIN for security when app goes to background
        clearPin();
        hideError();
    }
}