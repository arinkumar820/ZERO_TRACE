package com.sameetasadullah.i180479_180531;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager2.widget.ViewPager2;

import android.app.FragmentManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class fragmentsContainer extends AppCompatActivity {
    fragmentAdapter fragmentAdapter;
    ViewPager2 viewPager;
    ImageView callsImage, messagesImage, contactsImage, panicButton;
    public boolean minimized = true;
    private FirebaseAuth mAuth;
    private PanicButtonHandler panicButtonHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragments_container);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        fragmentAdapter = new fragmentAdapter(getSupportFragmentManager(), getLifecycle());
        viewPager = findViewById(R.id.container);
        callsImage = findViewById(R.id.calls_image);
        messagesImage = findViewById(R.id.messages_image);
        contactsImage = findViewById(R.id.contacts_image);
        panicButton = findViewById(R.id.panic_button);

        setupViewPager(viewPager);
        setupDeveloperAccess();
        setupPanicButton();

        messagesImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                viewPager.setCurrentItem(0);
            }
        });
        contactsImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                viewPager.setCurrentItem(1);
            }
        });
        callsImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { viewPager.setCurrentItem(2); }
        });
        // Camera functionality replaced with panic button
        // (Original camera click handler removed for security)
    }

    public void changeViewPager(int fragment) {
        viewPager.setCurrentItem(fragment);
    }

    private void changeImageColorToDefault(int item) {
        System.out.println(item);
        if (item == 0) {
            messagesImage.setBackgroundTintList(AppCompatResources.getColorStateList(fragmentsContainer.this, R.color.grey));
        } else if (item == 1) {
            contactsImage.setBackgroundTintList(AppCompatResources.getColorStateList(fragmentsContainer.this, R.color.grey));
        } else if (item == 2) {
            callsImage.setBackgroundTintList(AppCompatResources.getColorStateList(fragmentsContainer.this, R.color.grey));
        }
    }

    public void changeImageColorToBlue(int item) {
        if (item == 0) {
            messagesImage.setBackgroundTintList(AppCompatResources.getColorStateList(fragmentsContainer.this, R.color.blue));
            contactsImage.setBackgroundTintList(AppCompatResources.getColorStateList(fragmentsContainer.this, R.color.grey));
            callsImage.setBackgroundTintList(AppCompatResources.getColorStateList(fragmentsContainer.this, R.color.grey));
            panicButton.setBackgroundTintList(AppCompatResources.getColorStateList(fragmentsContainer.this, R.color.red));
        }
        else if (item == 1) {
            contactsImage.setBackgroundTintList(AppCompatResources.getColorStateList(fragmentsContainer.this, R.color.blue));
            messagesImage.setBackgroundTintList(AppCompatResources.getColorStateList(fragmentsContainer.this, R.color.grey));
            callsImage.setBackgroundTintList(AppCompatResources.getColorStateList(fragmentsContainer.this, R.color.grey));
            panicButton.setBackgroundTintList(AppCompatResources.getColorStateList(fragmentsContainer.this, R.color.red));
        }
        else if (item == 2) {
            callsImage.setBackgroundTintList(AppCompatResources.getColorStateList(fragmentsContainer.this, R.color.blue));
            contactsImage.setBackgroundTintList(AppCompatResources.getColorStateList(fragmentsContainer.this, R.color.grey));
            messagesImage.setBackgroundTintList(AppCompatResources.getColorStateList(fragmentsContainer.this, R.color.grey));
            panicButton.setBackgroundTintList(AppCompatResources.getColorStateList(fragmentsContainer.this, R.color.red));
        }
    }

    private void setupViewPager(ViewPager2 viewPager) {
        fragmentAdapter fragmentAdapter = new fragmentAdapter(getSupportFragmentManager(), getLifecycle());
        fragmentAdapter.addFragment(new fragment_screen4(), "Fragment_Screen4");
        fragmentAdapter.addFragment(new fragment_screen6(), "Fragment_Screen6");
        fragmentAdapter.addFragment(new fragment_screen10(), "Fragment_Screen10");
        viewPager.setAdapter(fragmentAdapter);
    }

    private void updateUserStatus(String state) {
        if (mAuth != null && mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            SearchContactsActivity.updateUserStatus(uid, state);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        updateUserStatus("online");
        minimized = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUserStatus("online");
        minimized = true;
    }

    @Override
    public void onStop() {
        super.onStop();
        if (minimized) {
            updateUserStatus("offline");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        updateUserStatus("offline");
    }
    
    private void setupDeveloperAccess() {
        // Long press on messages tab to access developer menu
        messagesImage.setOnLongClickListener(v -> {
            showDeveloperMenu();
            return true;
        });
    }
    
    private void showDeveloperMenu() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Developer & Admin Menu")
               .setMessage("Choose an option:")
               .setPositiveButton("WebSocket Test", (dialog, which) -> {
                   Intent intent = new Intent(this, WebSocketTestActivity.class);
                   startActivity(intent);
               })
               .setNegativeButton("HTTP API Test", (dialog, which) -> {
                   Intent intent = new Intent(this, ServerTestActivity.class);
                   startActivity(intent);
               })
               .setNeutralButton("🚨 Panic Logs", (dialog, which) -> {
                   showPanicLogs();
               })
               .show();
        
        // Add test panic button option
        AlertDialog.Builder testBuilder = new AlertDialog.Builder(this);
        testBuilder.setTitle("🧪 Test Options")
                   .setMessage("Additional testing options:")
                   .setPositiveButton("🚨 Test Panic Log", (dialog, which) -> {
                       testPanicLog();
                   })
                   .setNegativeButton("Close", null);
        // Show test options for debugging
        testBuilder.show();
    }
    
    private void showPanicLogs() {
        PanicAcknowledgmentManager acknowledgmentManager = new PanicAcknowledgmentManager(this);
        String logSummary = acknowledgmentManager.getEmergencyLogSummary();
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🚨 Emergency Panic Logs")
               .setMessage("Admin View - Panic Button Usage:\n\n" + logSummary)
               .setPositiveButton("Clear Logs", (dialog, which) -> {
                   acknowledgmentManager.clearEmergencyLogs();
                   Toast.makeText(this, "Emergency logs cleared", Toast.LENGTH_SHORT).show();
               })
               .setNegativeButton("Close", null)
               .show();
    }
    
    private void setupPanicButton() {
        // Initialize panic button handler
        panicButtonHandler = new PanicButtonHandler(this);
        
        // Set up panic button click listener
        if (panicButton != null) {
            panicButton.setOnClickListener(v -> {
                android.util.Log.w("fragmentsContainer", "🚨 Panic button pressed in main navigation");
                panicButtonHandler.handlePanicButtonPress();
            });
            
            // Set up long press for quick panic mode
            panicButton.setOnLongClickListener(v -> {
                android.util.Log.w("fragmentsContainer", "🚨 Panic button LONG PRESSED - Quick panic mode");
                panicButtonHandler.handleQuickPanic();
                return true; // Consume the long click event
            });
        }
        
        android.util.Log.d("fragmentsContainer", "Panic button functionality initialized in main navigation");
    }
    
    private void testPanicLog() {
        // Test panic acknowledgment without clearing data
        PanicAcknowledgmentManager testManager = new PanicAcknowledgmentManager(this);
        testManager.sendPanicAcknowledgment("test");
        
        Toast.makeText(this, "🚨 Test panic log sent to Firebase!\nCheck Firebase Console in a few seconds.", Toast.LENGTH_LONG).show();
        
        android.util.Log.w("fragmentsContainer", "🧪 TEST: Panic acknowledgment sent to Firebase");
    }
}

