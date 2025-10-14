package com.sameetasadullah.i180479_180531;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Firebase-Only Search Contacts Activity
 * Searches for users directly from Firebase Realtime Database
 */
public class FirebaseSearchContactsActivity extends AppCompatActivity {

    private static final String TAG = "FirebaseSearchContacts";
    
    // UI Components
    private EditText etSearch;
    private ImageView ivBack, ivClear;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private View tvNoResults;
    private TextView tvSearchHint;
    
    // Firebase
    private DatabaseReference usersRef;
    
    // Adapter and Data
    private ContactsAdapter contactsAdapter;
    private List<User> contactsList;
    
    // User info
    private SharedPreferences sharedPreferences;
    private String currentUserUid;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_contacts);
        
        initializeComponents();
        setupRecyclerView();
        setupSearchFunctionality();
        setupClickListeners();
        
        // Load all users initially
        loadAllUsers();
        
        Log.d(TAG, "Firebase Search Contacts Activity created");
    }
    
    private void initializeComponents() {
        // Find UI components
        etSearch = findViewById(R.id.et_search);
        ivBack = findViewById(R.id.iv_back);
        ivClear = findViewById(R.id.iv_clear_search);
        recyclerView = findViewById(R.id.rv_search_results);
        progressBar = findViewById(R.id.progress_bar);
        tvNoResults = findViewById(R.id.ll_empty_state);
        
        // Initialize Firebase
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        
        // Initialize data structures
        contactsList = new ArrayList<>();
        
        // Get current user info
        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        currentUserUid = sharedPreferences.getString("user_uid", "");
    }
    
    private void setupRecyclerView() {
        contactsAdapter = new ContactsAdapter(contactsList, new ContactsAdapter.OnContactClickListener() {
            @Override
            public void onContactClick(User user) {
                startChatWithUser(user);
            }
            
            @Override
            public void onContactLongClick(User user) {
                showUserProfile(user);
            }
        });
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(contactsAdapter);
    }
    
    private void setupSearchFunctionality() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                
                // Show/hide clear button
                ivClear.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                
                // Perform search
                if (query.isEmpty()) {
                    loadAllUsers();
                } else {
                    searchUsers(query);
                }
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
    
    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> onBackPressed());
        
        ivClear.setOnClickListener(v -> {
            etSearch.setText("");
            etSearch.clearFocus();
            loadAllUsers();
        });
    }
    
    // ====================================================================================
    // FIREBASE SEARCH FUNCTIONALITY
    // ====================================================================================
    
    private void loadAllUsers() {
        showLoading(true);
        tvNoResults.setVisibility(View.GONE);
        
        Log.d(TAG, "Loading all users from Firebase");
        
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<User> users = new ArrayList<>();
                
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    User user = parseUserFromSnapshot(userSnapshot);
                    if (user != null && !user.getUid().equals(currentUserUid)) {
                        users.add(user);
                    }
                }
                
                showResults(users);
                Log.d(TAG, "Loaded " + users.size() + " users from Firebase");
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load users", error.toException());
                showError("Failed to load users: " + error.getMessage());
            }
        });
    }
    
    private void searchUsers(String query) {
        showLoading(true);
        tvNoResults.setVisibility(View.GONE);
        
        Log.d(TAG, "Searching users with query: " + query);
        
        // Firebase doesn't support complex LIKE queries, so we'll load all users and filter client-side
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<User> filteredUsers = new ArrayList<>();
                String lowerQuery = query.toLowerCase();
                
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    User user = parseUserFromSnapshot(userSnapshot);
                    if (user != null && !user.getUid().equals(currentUserUid)) {
                        
                        // Search in email and display name
                        boolean matchesEmail = user.getEmail() != null && 
                                             user.getEmail().toLowerCase().contains(lowerQuery);
                        boolean matchesName = user.getDisplayName() != null && 
                                            user.getDisplayName().toLowerCase().contains(lowerQuery);
                        
                        if (matchesEmail || matchesName) {
                            filteredUsers.add(user);
                        }
                    }
                }
                
                showResults(filteredUsers);
                Log.d(TAG, "Found " + filteredUsers.size() + " users matching query: " + query);
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to search users", error.toException());
                showError("Search failed: " + error.getMessage());
            }
        });
    }
    
    private User parseUserFromSnapshot(DataSnapshot userSnapshot) {
        try {
            User user = new User();
            user.setUid(userSnapshot.getKey());
            user.setEmail(userSnapshot.child("email").getValue(String.class));
            user.setDisplayName(userSnapshot.child("displayName").getValue(String.class));
            user.setPhoneNumber(userSnapshot.child("phoneNumber").getValue(String.class));
            user.setBio(userSnapshot.child("bio").getValue(String.class));
            user.setStatus(userSnapshot.child("status").getValue(String.class));
            user.setProfileImageUrl(userSnapshot.child("profileImageUrl").getValue(String.class));
            
            // Set default values if null
            if (user.getDisplayName() == null || user.getDisplayName().isEmpty()) {
                user.setDisplayName(user.getEmail() != null ? user.getEmail().split("@")[0] : "Unknown User");
            }
            if (user.getBio() == null) {
                user.setBio("Hey there! I'm using Bisto Chat.");
            }
            if (user.getStatus() == null) {
                user.setStatus("offline");
            }
            
            return user;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing user from snapshot", e);
            return null;
        }
    }
    
    // ====================================================================================
    // NAVIGATION AND ACTIONS
    // ====================================================================================
    
    private void startChatWithUser(User user) {
        Log.d(TAG, "Starting chat with user: " + user.getEmail());
        
        // Create a room ID based on user IDs (alphabetically sorted for consistency)
        String roomId = createRoomId(currentUserUid, user.getUid());
        
        Intent chatIntent = new Intent(this, EnhancedChatActivity.class);
        chatIntent.putExtra("room_id", roomId);
        chatIntent.putExtra("contact_uid", user.getUid());
        chatIntent.putExtra("contact_name", user.getDisplayName());
        chatIntent.putExtra("contact_email", user.getEmail());
        chatIntent.putExtra("contact_image_url", user.getProfileImageUrl());
        
        startActivity(chatIntent);
    }
    
    private void showUserProfile(User user) {
        Log.d(TAG, "Showing user profile: " + user.getEmail());
        
        String profileInfo = "Name: " + (user.getDisplayName() != null ? user.getDisplayName() : "N/A") +
                "\nEmail: " + (user.getEmail() != null ? user.getEmail() : "N/A") +
                "\nPhone: " + (user.getPhoneNumber() != null ? user.getPhoneNumber() : "N/A") +
                "\nBio: " + (user.getBio() != null ? user.getBio() : "N/A") +
                "\nStatus: " + (user.getStatus() != null ? user.getStatus() : "offline");
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("User Profile")
                .setMessage(profileInfo)
                .setPositiveButton("Start Chat", (dialog, which) -> startChatWithUser(user))
                .setNegativeButton("Close", null)
                .show();
    }
    
    private String createRoomId(String uid1, String uid2) {
        // Create consistent room ID by sorting UIDs alphabetically
        if (uid1.compareTo(uid2) < 0) {
            return uid1 + "_" + uid2;
        } else {
            return uid2 + "_" + uid1;
        }
    }
    
    // ====================================================================================
    // UI HELPER METHODS
    // ====================================================================================
    
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }
    
    private void showResults(List<User> users) {
        showLoading(false);
        tvNoResults.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        
        contactsList.clear();
        contactsList.addAll(users);
        contactsAdapter.notifyDataSetChanged();
        
        if (contactsList.isEmpty()) {
            tvNoResults.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        }
    }
    
    private void showError(String message) {
        showLoading(false);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}