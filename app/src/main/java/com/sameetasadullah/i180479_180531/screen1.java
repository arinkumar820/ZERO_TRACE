package com.sameetasadullah.i180479_180531;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class screen1 extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen1);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        
        // Check if user is signed in
        boolean userLoggedIn = (user != null);

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                // Route based on authentication status
                if (userLoggedIn) {
                    // User is signed in, go to main app
                    intent = new Intent(screen1.this, fragmentsContainer.class);
                } else {
                    // No user is signed in, go to login screen
                    intent = new Intent(screen1.this, screen2.class);
                }
                startActivity(intent);
                finish();
            }
        }, 3000); // Reduced splash time to 3 seconds
    }
}
