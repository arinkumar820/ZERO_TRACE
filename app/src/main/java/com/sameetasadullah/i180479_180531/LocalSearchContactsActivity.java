package com.sameetasadullah.i180479_180531;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Search contacts from local SQLite database instead of Firebase
 * Uses Firebase Auth for authentication but searches local data
 */
public class LocalSearchContactsActivity extends AppCompatActivity implements UserSearchAdapter.OnUserAddedListener {

    private static final String TAG = "LocalSearchContacts";
    private static final int SEARCH_DELAY_MS = 300; // Faster since local search

    // UI Components
    private ImageView backButton;
    private EditText searchEditText;
    private ImageView clearSearchButton;
    private RecyclerView searchResultsRecyclerView;
    private LinearLayout emptyStateLayout;
    private ProgressBar progressBar;

    // Firebase Auth (for authentication only)
    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;

    // Local Database
    private LocalUserDatabase localDB;

    // Adapter
    private UserSearchAdapter searchAdapter;

    // Search functionality
    private Timer searchTimer;
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_contacts);

        // Initialize Firebase Auth (for authentication only)
        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();
        
        if (currentUser == null) {
            Toast.makeText(this, "Please login to search for contacts", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Local Database
        localDB = LocalUserDatabase.getInstance(this);

        // Initialize UI
        initializeViews();
        setupRecyclerView();
        setupSearchFunctionality();
        setupClickListeners();

        Log.d(TAG, "Local SearchContactsActivity initialized");
        
        // Show database info
        showDatabaseInfo();
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

                // Start new search timer (faster for local search)
                if (query.length() >= 2) { // Minimum 2 characters to search
                    searchTimer = new Timer();
                    searchTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            runOnUiThread(() -> performLocalSearch(query));
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

    /**
     * Perform search in local SQLite database
     */
    private void performLocalSearch(String query) {
        if (query.equals(currentSearchQuery)) {
            return; // Same query, don't search again
        }

        currentSearchQuery = query;
        showLoading(true);
        hideEmptyState();

        Log.d(TAG, "=== LOCAL SEARCH START ===");
        Log.d(TAG, "Searching local database for: '" + query + "'");
        Log.d(TAG, "Current user ID: " + currentUser.getUid());

        // Perform local database search in background thread
        new Thread(() -> {
            try {
                List<User> foundUsers = localDB.searchUsers(query, currentUser.getUid());
                
                Log.d(TAG, "Local search completed - found " + foundUsers.size() + " users");
                
                // Update UI on main thread
                runOnUiThread(() -> {
                    displaySearchResults(foundUsers);
                    Log.d(TAG, "=== LOCAL SEARCH COMPLETE ===");
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Local search failed", e);
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(LocalSearchContactsActivity.this, 
                            "Search failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    showEmptyState();
                });
            }
        }).start();
    }

    private void displaySearchResults(List<User> users) {
        showLoading(false);

        if (users.isEmpty()) {
            showEmptyState();
            Log.d(TAG, "No users found for query: " + currentSearchQuery);
        } else {
            hideEmptyState();
            searchAdapter.updateUsers(users);
            Log.d(TAG, "Displayed " + users.size() + " search results");
        }
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
    
    private void showDatabaseInfo() {
        int totalUsers = localDB.getUserCount();
        Log.d(TAG, "Local database contains " + totalUsers + " users");
        
        if (totalUsers == 0) {
            Toast.makeText(this, "Local database is empty. Add some users first.", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Ready to search " + totalUsers + " users locally", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onUserAdded(User user) {
        // Called when a user is successfully added as a contact
        Log.d(TAG, "User added as contact: " + user.getDisplayText());
        
        // Add to local contacts
        boolean success = localDB.addContact(currentUser.getUid(), user.getUid());
        if (success) {
            Toast.makeText(this, "Contact added successfully", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to add contact", Toast.LENGTH_SHORT).show();
        }
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
    }

    /**
     * Add sample users to local database (for testing)
     */
    public void addSampleUsers() {
        new Thread(() -> {
            // Sample users
            User user1 = new User("sample1", "john.doe@example.com", "John Doe");
            user1.setBio("Hello, I'm John!");
            user1.setStatus("online");
            
            User user2 = new User("sample2", "jane.smith@gmail.com", "Jane Smith");
            user2.setBio("Nice to meet you!");
            user2.setStatus("offline");
            
            User user3 = new User("sample3", "bob.wilson@test.com", "Bob Wilson");
            user3.setBio("Let's chat!");
            user3.setStatus("online");

            // Add to local database
            localDB.addOrUpdateUser(user1);
            localDB.addOrUpdateUser(user2);
            localDB.addOrUpdateUser(user3);

            runOnUiThread(() -> {
                Toast.makeText(this, "Sample users added to local database", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Sample users added successfully");
            });
        }).start();
    }
}