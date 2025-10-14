package com.sameetasadullah.i180479_180531;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class VerificationActivity extends AppCompatActivity {

    private static final String DEFAULT_PASSWORD = "123456"; // Default verification password

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification);

        EditText passwordInput = findViewById(R.id.password_input);
        Button verifyButton = findViewById(R.id.verify_button);

        // Ensure password field behavior
        passwordInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        passwordInput.setImeOptions(EditorInfo.IME_ACTION_DONE);

        // Trigger verification on keyboard action
        passwordInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                verifyAndProceed();
                return true;
            }
            return false;
        });

        // Trigger verification on button click
        verifyButton.setOnClickListener(v -> verifyAndProceed());
    }

    private void verifyAndProceed() {
        EditText passwordInput = findViewById(R.id.password_input);
        String entered = passwordInput.getText() != null ? passwordInput.getText().toString().trim() : "";
        if (DEFAULT_PASSWORD.equals(entered)) {
            // Proceed to the original launcher screen
            Intent intent = new Intent(this, screen1.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show();
            // Notify server about incorrect attempts
            SecurityAlertSender.sendVerificationFailure(this);
        }
    }

    @Override
    public void onBackPressed() {
        // Exit app from verification screen
        finishAffinity();
    }
}
