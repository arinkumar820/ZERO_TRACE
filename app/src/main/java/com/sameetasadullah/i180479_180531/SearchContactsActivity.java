package com.sameetasadullah.i180479_180531;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class SearchContactsActivity extends AppCompatActivity implements UserSearchAdapter.OnUserAddedListener {

    private static final String TAG = "SearchContactsActivity";
    private static final int SEARCH_DELAY_MS = 800; // Delay before starting search

    // UI Components
    private ImageView backButton;
    private EditText searchEditText;
    private ImageView clearSearchButton;
    private RecyclerView searchResultsRecyclerView;
    private LinearLayout emptyStateLayout;
    private ProgressBar progressBar;

    // Firebase Auth and Database
    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;
    private DatabaseReference usersRef; // Firebase database reference for user search

    // Adapter
    private UserSearchAdapter searchAdapter;

    // Search functionality
    private Timer searchTimer;
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 🛡️ SCREENSHOT PROTECTION: Prevent screenshots of contact search
        ScreenshotProtection.enableProtection(this);
        
        setContentView(R.layout.activity_search_contacts);

        // Initialize Firebase Auth and Database
        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();
        usersRef = FirebaseDatabase.getInstance().getReference("Users"); // Match your database structure
        
        if (currentUser == null) {
            Toast.makeText(this, "Please login to search for contacts", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize UI
        initializeViews();
        setupRecyclerView();
        setupSearchFunctionality();
        setupClickListeners();

        Log.d(TAG, "SearchContactsActivity initialized");
    }
    

    private void initializeViews() {
        backButton = findViewById(R.id.iv_back);
        searchEditText = findViewById(R.id.et_search);
        clearSearchButton = findViewById(R.id.iv_clear_search);
        searchResultsRecyclerView = findViewById(R.id.rv_search_results);
        emptyStateLayout = findViewById(R.id.ll_empty_state);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupRecyclerView() {
        searchAdapter = new UserSearchAdapter(this);
        searchAdapter.setOnUserAddedListener(this);
        
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        searchResultsRecyclerView.setAdapter(searchAdapter);
    }

    private void setupSearchFunctionality() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                
                // Show/hide clear button
                if (query.length() > 0) {
                    clearSearchButton.setVisibility(View.VISIBLE);
                } else {
                    clearSearchButton.setVisibility(View.GONE);
                }

                // Cancel previous search timer
                if (searchTimer != null) {
                    searchTimer.cancel();
                }

                // Start new search timer
                if (query.length() >= 2) { // Minimum 2 characters to search
                    searchTimer = new Timer();
                    searchTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            runOnUiThread(() -> performSearch(query));
                        }
                    }, SEARCH_DELAY_MS);
                } else {
                    // Clear results if query is too short
                    currentSearchQuery = "";
                    searchAdapter.clearUsers();
                    showEmptyState();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not needed
            }
        });
    }

    private void setupClickListeners() {
        // Back button
        backButton.setOnClickListener(v -> finish());

        // Clear search button
        clearSearchButton.setOnClickListener(v -> {
            searchEditText.setText("");
            searchAdapter.clearUsers();
            showEmptyState();
        });
    }

    private void performSearch(String query) {
        if (query.equals(currentSearchQuery)) {
            return; // Same query, don't search again
        }

        currentSearchQuery = query;
        showLoading(true);
        hideEmptyState();

        Log.d(TAG, "=== FIREBASE SEARCH DEBUG START ===");
        Log.d(TAG, "Performing search for: '" + query + "'");
        Log.d(TAG, "Firebase Auth current user: " + (currentUser != null ? currentUser.getUid() : "null"));
        Log.d(TAG, "Firebase Database reference: " + (usersRef != null ? "valid" : "null"));
        
        // Add timeout mechanism to detect hanging searches
        android.os.Handler timeoutHandler = new android.os.Handler();
        Runnable timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, "SEARCH TIMEOUT: Search took longer than 15 seconds - possible hanging");
                showLoading(false);
                Toast.makeText(SearchContactsActivity.this, 
                        "Search timeout - please check your connection", Toast.LENGTH_LONG).show();
            }
        };
        timeoutHandler.postDelayed(timeoutRunnable, 15000); // 15 second timeout

        // Use Firebase for search
        Log.d(TAG, "Starting Firebase database search...");
        searchFirebaseUsers(query, timeoutHandler, timeoutRunnable);
        
    }
    
    private void searchFirebaseUsers(String query, android.os.Handler timeoutHandler, Runnable timeoutRunnable) {
        Log.d(TAG, "Searching Firebase users with query: " + query);
        
        // Test string matching logic
        String testName = "Arin Kumar";
        String testQuery = query.toLowerCase();
        boolean testMatch = testName.toLowerCase().contains(testQuery);
        Log.d(TAG, "🧪 String test: '" + testName + "'.toLowerCase().contains('" + testQuery + "') = " + testMatch);
        
        // Search all users and filter client-side (Firebase doesn't support complex LIKE queries)
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                timeoutHandler.removeCallbacks(timeoutRunnable); // Cancel timeout
                List<User> filteredUsers = new ArrayList<>();
                String lowerQuery = query.toLowerCase();
                
                Log.d(TAG, "Firebase returned " + snapshot.getChildrenCount() + " total users");
                
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    User user = parseUserFromSnapshot(userSnapshot);
                    if (user != null) {
                        Log.d(TAG, "Checking user: " + user.getDisplayName() + " (" + user.getEmail() + ") UID: " + user.getUid());
                        Log.d(TAG, "Current user UID: " + currentUser.getUid());
                        Log.d(TAG, "Is current user? " + user.getUid().equals(currentUser.getUid()));
                        
                        // Temporarily allow searching current user for testing
                        // if (!user.getUid().equals(currentUser.getUid())) {
                        if (true) {
                            // Search in email and display name
                            boolean matchesEmail = user.getEmail() != null && 
                                                 user.getEmail().toLowerCase().contains(lowerQuery);
                            boolean matchesName = user.getDisplayName() != null && 
                                                user.getDisplayName().toLowerCase().contains(lowerQuery);
                            
                            Log.d(TAG, "Email match (" + user.getEmail() + " contains " + lowerQuery + "): " + matchesEmail);
                            Log.d(TAG, "Name match (" + user.getDisplayName() + " contains " + lowerQuery + "): " + matchesName);
                            
                            if (matchesEmail || matchesName) {
                                filteredUsers.add(user);
                                Log.d(TAG, "✅ Match found: " + user.getDisplayName() + " (" + user.getEmail() + ")");
                            } else {
                                Log.d(TAG, "❌ No match for: " + user.getDisplayName() + " (" + user.getEmail() + ")");
                            }
                        } else {
                            Log.d(TAG, "🚫 Skipping current user: " + user.getDisplayName());
                        }
                    }
                }
                
                Log.d(TAG, "Firebase Search SUCCESS: Found " + filteredUsers.size() + " matching users");
                displaySearchResults(filteredUsers);
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                timeoutHandler.removeCallbacks(timeoutRunnable); // Cancel timeout
                Log.e(TAG, "Firebase Search FAILED: " + error.getMessage());
                showLoading(false);
                Toast.makeText(SearchContactsActivity.this, 
                        "Search failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                showEmptyState();
            }
        });
    }
    
    private User parseUserFromSnapshot(DataSnapshot userSnapshot) {
        try {
            User user = new User();
            user.setUid(userSnapshot.getKey());
            user.setEmail(userSnapshot.child("email").getValue(String.class));
            
            // Try both displayName and displayText (your DB has displayText)
            String displayName = userSnapshot.child("displayName").getValue(String.class);
            if (displayName == null) {
                displayName = userSnapshot.child("displayText").getValue(String.class);
            }
            user.setDisplayName(displayName);
            
            user.setPhoneNumber(userSnapshot.child("phoneNumber").getValue(String.class));
            user.setBio(userSnapshot.child("bio").getValue(String.class));
            user.setStatus(userSnapshot.child("status").getValue(String.class));
            user.setProfileImageUrl(userSnapshot.child("profileImageUrl").getValue(String.class));
            
            // Handle online field (your DB has "online: false" instead of status)
            Boolean online = userSnapshot.child("online").getValue(Boolean.class);
            if (online != null) {
                user.setStatus(online ? "online" : "offline");
            }
            
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
            
            Log.d(TAG, "Parsed user: " + user.getDisplayName() + " (" + user.getEmail() + ") - Status: " + user.getStatus());
            return user;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing user from snapshot", e);
            return null;
        }
    }

    private void searchUsersByEmail(String email) {
        Query emailQuery = usersRef.orderByChild("email").equalTo(email.toLowerCase());
        
        emailQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<User> foundUsers = new ArrayList<>();

                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    User user = userSnapshot.getValue(User.class);
                    if (user != null && !user.getUid().equals(currentUser.getUid())) {
                        // Don't include current user in search results
                        foundUsers.add(user);
                    }
                }

                // If no exact email match found, search by display name
                if (foundUsers.isEmpty()) {
                    searchUsersByDisplayName(currentSearchQuery);
                } else {
                    displaySearchResults(foundUsers);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Email search cancelled", databaseError.toException());
                showLoading(false);
                Toast.makeText(SearchContactsActivity.this, 
                        "Search failed: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void searchUsersByDisplayName(String displayName) {
        // Search for users whose display name contains the query (case-insensitive)
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<User> foundUsers = new ArrayList<>();

                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    User user = userSnapshot.getValue(User.class);
                    if (user != null && !user.getUid().equals(currentUser.getUid())) {
                        String userDisplayName = user.getDisplayName();
                        String userEmail = user.getEmail();
                        
                        // Check if display name or email contains search query
                        if ((userDisplayName != null && userDisplayName.toLowerCase()
                                .contains(displayName.toLowerCase())) ||
                            (userEmail != null && userEmail.toLowerCase()
                                .contains(displayName.toLowerCase()))) {
                            foundUsers.add(user);
                        }
                    }
                }

                displaySearchResults(foundUsers);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Display name search cancelled", databaseError.toException());
                showLoading(false);
                Toast.makeText(SearchContactsActivity.this, 
                        "Search failed: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displaySearchResults(List<User> users) {
        showLoading(false);

        if (users.isEmpty()) {
            showEmptyState();
        } else {
            hideEmptyState();
            searchAdapter.updateUsers(users);
        }

        Log.d(TAG, "Found " + users.size() + " users");
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showEmptyState() {
        emptyStateLayout.setVisibility(View.VISIBLE);
        searchResultsRecyclerView.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        emptyStateLayout.setVisibility(View.GONE);
        searchResultsRecyclerView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onUserAdded(User user) {
        // Called when a user is successfully added as a contact
        Log.d(TAG, "User added: " + user.getDisplayText());
        
        // Optional: Remove user from search results or update UI
        // You could also navigate back to contacts list here
    }
    
    @Override
    public void onChatStarted(String roomId, User user) {
        // Called when a chat is started with a user
        Log.d(TAG, "Chat started with " + user.getDisplayText() + " in room: " + roomId);
        
        // Optional: Close search activity after chat is started
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Cancel search timer to prevent memory leaks
        if (searchTimer != null) {
            searchTimer.cancel();
            searchTimer = null;
        }
        
        // No cleanup needed for Firebase
    }

    // Method to create and store a user in Firebase (call this when user registers)
    public static void createUserInDatabase(String uid, String email, String displayName, 
                                          String profileImageUrl, String phoneNumber) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");
        
        User user = new User(uid, email, displayName, profileImageUrl, phoneNumber);
        
        usersRef.child(uid).setValue(user)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User created in database: " + email))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to create user in database", e));
    }

    // Method to update user status (call this from main activities)
    public static void updateUserStatus(String uid, String status) {
        if (uid == null || status == null) return;
        
        DatabaseReference userStatusRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(uid)
                .child("status");
        
        userStatusRef.setValue(status);
        
        // Also update last seen timestamp
        DatabaseReference lastSeenRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(uid)
                .child("lastSeen");
        
        lastSeenRef.setValue(System.currentTimeMillis());
    }
    
    /**
     * Test Firebase database connectivity and structure
     */
    private void testFirebaseConnectivity() {
        Log.d(TAG, "=== FIREBASE CONNECTIVITY TEST ===");
        
        // Test 1: Check if we can read from the Users node
        usersRef.limitToFirst(1).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "Firebase connection SUCCESS - Users node accessible");
                Log.d(TAG, "Users node exists: " + dataSnapshot.exists());
                Log.d(TAG, "Users count (first check): " + dataSnapshot.getChildrenCount());
                
                if (dataSnapshot.exists()) {
                    for (DataSnapshot child : dataSnapshot.getChildren()) {
                        Log.d(TAG, "Sample user key: " + child.getKey());
                        User user = child.getValue(User.class);
                        if (user != null) {
                            Log.d(TAG, "Sample user: " + user.getDisplayText() + " (" + user.getEmail() + ")");
                        }
                        break; // Just show first user
                    }
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Firebase connection FAILED: " + databaseError.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(SearchContactsActivity.this, 
                            "Database connection failed: " + databaseError.getMessage(), 
                            Toast.LENGTH_LONG).show();
                });
            }
        });
        
        // Test 2: Count total users in database
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                long userCount = dataSnapshot.getChildrenCount();
                Log.d(TAG, "Total users in database: " + userCount);
                
                if (userCount == 0) {
                    Log.w(TAG, "WARNING: No users found in database - this explains why search returns nothing");
                    runOnUiThread(() -> {
                        Toast.makeText(SearchContactsActivity.this, 
                                "No users in database. Register more users to test search.", 
                                Toast.LENGTH_LONG).show();
                    });
                } else {
                    Log.d(TAG, "Database contains " + userCount + " users - search should work");
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "User count check failed: " + databaseError.getMessage());
            }
        });
    }
    
    /**
     * Fix Firebase locale warning by ensuring locale is properly set
     */
    private void fixFirebaseLocale() {
        try {
            // Get current locale
            Locale currentLocale = Locale.getDefault();
            
            // If locale is null or not properly set, set a default
            if (currentLocale == null || currentLocale.toString().isEmpty()) {
                Locale.setDefault(new Locale("en", "US"));
                Log.d(TAG, "Default locale set to en_US");
            } else {
                Log.d(TAG, "Current locale: " + currentLocale.toString());
            }
            
            // Set system property for Firebase
            System.setProperty("user.language", Locale.getDefault().getLanguage());
            System.setProperty("user.country", Locale.getDefault().getCountry());
            
        } catch (Exception e) {
            Log.w(TAG, "Error fixing Firebase locale", e);
            try {
                // Fallback to English
                Locale.setDefault(Locale.ENGLISH);
                System.setProperty("user.language", "en");
                System.setProperty("user.country", "US");
            } catch (Exception ex) {
                Log.e(TAG, "Failed to set fallback locale", ex);
            }
        }
    }
}
