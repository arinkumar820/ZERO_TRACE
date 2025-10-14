package com.sameetasadullah.i180479_180531;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import android.widget.Toast;
import android.content.Intent;
import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class inputCredentials extends AppCompatActivity {

    EditText firstName, lastName, phoneNumber, bio;
    RadioGroup genderRadioGroup;
    Button createButton;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private HybridUserManager hybridManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_credentials);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        
        // Initialize Hybrid User Manager
        hybridManager = new HybridUserManager(this);
        
        // Get user email from intent (passed from registration)
        String userEmail = getIntent().getStringExtra("user_email");
        String userUid = getIntent().getStringExtra("user_uid");
        
        Log.d("InputCredentials", "Creating profile for user: " + userEmail);

        // Initialize UI components with correct IDs from layout
        firstName = findViewById(R.id.first_name);
        lastName = findViewById(R.id.last_name);
        phoneNumber = findViewById(R.id.phoneNumber);
        bio = findViewById(R.id.bio);
        genderRadioGroup = findViewById(R.id.genderRadioGroup);
        createButton = findViewById(R.id.create);
        
        // Log UI component initialization
        Log.d("InputCredentials", "UI Components - firstName: " + (firstName != null) + 
              ", lastName: " + (lastName != null) + 
              ", createButton: " + (createButton != null));

        // Set up create button click listener
        if (createButton != null) {
            createButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d("InputCredentials", "Create button clicked!");
                    createUserProfile();
                }
            });
        } else {
            Log.e("InputCredentials", "Create button not found in layout!");
            Toast.makeText(this, "Error: Create button not found", Toast.LENGTH_LONG).show();
        }
    }
    
    private void createUserProfile() {
        // Get form data
        String firstNameText = firstName.getText().toString().trim();
        String lastNameText = lastName.getText().toString().trim();
        String phoneText = phoneNumber.getText().toString().trim();
        String bioText = bio.getText().toString().trim();
        
        // Get selected gender
        String selectedGender = "";
        int selectedId = genderRadioGroup.getCheckedRadioButtonId();
        if (selectedId != -1) {
            RadioButton selectedRadioButton = findViewById(selectedId);
            if (selectedRadioButton != null) {
                selectedGender = selectedRadioButton.getText().toString();
            }
        }
        
        // Validate required fields
        if (firstNameText.isEmpty()) {
            firstName.setError("First name is required");
            firstName.requestFocus();
            return;
        }
        
        if (lastNameText.isEmpty()) {
            lastName.setError("Last name is required");
            lastName.requestFocus();
            return;
        }
        
        // Show progress
        createButton.setEnabled(false);
        createButton.setText("Creating Profile...");
        
        Log.d("InputCredentials", "Creating profile: " + firstNameText + " " + lastNameText);
        
        // Use Hybrid Manager to create profile (Firebase Auth + Local DB)
        hybridManager.completeUserProfile(firstNameText, lastNameText, phoneText, bioText,
                new HybridUserManager.UserRegistrationCallback() {
                    @Override
                    public void onSuccess(User user) {
                        Log.d("InputCredentials", "Profile created successfully: " + user.getDisplayName());
                        createButton.setEnabled(true);
                        createButton.setText("CREATE");
                        Toast.makeText(inputCredentials.this, "Profile created successfully!", Toast.LENGTH_SHORT).show();
                        
                        // Navigate to main app
                        Intent intent = new Intent(inputCredentials.this, fragmentsContainer.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    }
                    
                    @Override
                    public void onFailure(String error) {
                        Log.e("InputCredentials", "Profile creation failed: " + error);
                        createButton.setEnabled(true);
                        createButton.setText("CREATE");
                        Toast.makeText(inputCredentials.this, "Failed to create profile: " + error, Toast.LENGTH_LONG).show();
                    }
                });
        
        Log.d("InputCredentials", "Profile creation initiated with HybridUserManager");
    }
    
    private void storeUserInDatabase(FirebaseUser user, String firstName, String lastName, String phone, String bioText) {
        String displayName = firstName + " " + lastName;
        String email = user.getEmail();
        String uid = user.getUid();
        
        // Create User object
        User userObj = new User(uid, email, displayName);
        userObj.setPhoneNumber(phone);
        userObj.setBio(bioText.isEmpty() ? "Hey there! I'm using Zero Trace." : bioText);
        userObj.setStatus("online");
        
        // Store in Firebase Realtime Database
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");
        usersRef.child(uid).setValue(userObj)
                .addOnSuccessListener(aVoid -> {
                    Log.d("InputCredentials", "User stored in database successfully");
                    createButton.setEnabled(true);
                    createButton.setText("CREATE");
                    Toast.makeText(inputCredentials.this, "Profile created successfully!", Toast.LENGTH_SHORT).show();
                    
                    // Navigate to main app
                    Intent intent = new Intent(inputCredentials.this, fragmentsContainer.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("InputCredentials", "Failed to store user in database", e);
                    createButton.setEnabled(true);
                    createButton.setText("CREATE");
                    Toast.makeText(inputCredentials.this, "Profile created but search may not work. Please try again.", Toast.LENGTH_LONG).show();
                    
                    // Still navigate to main app even if database storage fails
                    Intent intent = new Intent(inputCredentials.this, fragmentsContainer.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                });
    }
}

