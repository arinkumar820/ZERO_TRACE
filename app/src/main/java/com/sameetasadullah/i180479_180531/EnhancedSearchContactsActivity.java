package com.sameetasadullah.i180479_180531;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Enhanced Search Contacts Activity
 * 
 * This activity handles:
 * 1. Real-time contact search using MySQL database via WebSocket
 * 2. Display search results in RecyclerView
 * 3. Navigate to chat with selected contact
 * 4. User profile viewing
 */
public class EnhancedSearchContactsActivity extends AppCompatActivity implements EnhancedWebSocketClient.EnhancedWebSocketListener {

    private static final String TAG = "EnhancedSearchContacts";
    private static final int SEARCH_DELAY_MS = 300; // Delay before executing search
    
    // UI Components
    private EditText etSearch;
    private ImageView ivBack, ivClear;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private View tvNoResults; // Changed to View since we're using the empty state layout
    private TextView tvSearchHint;
    
    // WebSocket Client
    private EnhancedWebSocketClient webSocketClient;
    
    // Adapter and Data
    private ContactsAdapter contactsAdapter;
    private List<User> contactsList;
    
    // Search functionality
    private Handler searchHandler;
    private Runnable searchRunnable;
    private String currentSearchQuery = "";
    
    // User info
    private SharedPreferences sharedPreferences;
    private String currentUserUid;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_contacts);
        
        initializeComponents();
        setupWebSocket();
        setupRecyclerView();
        setupSearchFunctionality();
        setupClickListeners();
        
        // Load all contacts initially
        loadAllContacts();
        
        Log.d(TAG, "Enhanced Search Contacts Activity created");
    }
    
    private void initializeComponents() {
        // Find UI components
        etSearch = findViewById(R.id.et_search);
        ivBack = findViewById(R.id.iv_back);
        ivClear = findViewById(R.id.iv_clear_search);
        recyclerView = findViewById(R.id.rv_search_results);
        progressBar = findViewById(R.id.progress_bar);
        tvNoResults = findViewById(R.id.ll_empty_state); // Using empty state layout as no results
        tvSearchHint = null; // Not available in existing layout
        
        // Initialize data structures
        contactsList = new ArrayList<>();
        searchHandler = new Handler();
        
        // Get user info
        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        currentUserUid = sharedPreferences.getString("user_uid", "");
    }
    
    private void setupWebSocket() {
        webSocketClient = new EnhancedWebSocketClient(this, this);
        webSocketClient.connect();
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
                
                // Cancel previous search
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                
                // Schedule new search with delay
                searchRunnable = () -> performSearch(query);
                searchHandler.postDelayed(searchRunnable, SEARCH_DELAY_MS);
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // Handle search field focus
        etSearch.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && tvSearchHint != null) {
                tvSearchHint.setVisibility(View.GONE);
            } else if (etSearch.getText().toString().trim().isEmpty() && tvSearchHint != null) {
                tvSearchHint.setVisibility(View.VISIBLE);
            }
        });
    }
    
    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> onBackPressed());
        
        ivClear.setOnClickListener(v -> {
            etSearch.setText("");
            etSearch.clearFocus();
            loadAllContacts();
        });
    }
    
    // ====================================================================================
    // SEARCH FUNCTIONALITY
    // ====================================================================================
    
    private void loadAllContacts() {
        if (!webSocketClient.isConnected()) {
            showToast("Connecting to server...");
            return;
        }
        
        showLoading(true);
        tvNoResults.setVisibility(View.GONE);
        if (tvSearchHint != null) {
            tvSearchHint.setVisibility(View.VISIBLE);
        }
        
        Log.d(TAG, "Loading all contacts");
        webSocketClient.getUsers(null); // null means get all users
    }
    
    private void performSearch(String query) {
        if (query.equals(currentSearchQuery)) {
            return; // Same query, no need to search again
        }
        
        currentSearchQuery = query;
        
        if (query.isEmpty()) {
            loadAllContacts();
            return;
        }
        
        if (!webSocketClient.isConnected()) {
            showToast("Not connected to server");
            return;
        }
        
        showLoading(true);
        tvNoResults.setVisibility(View.GONE);
        if (tvSearchHint != null) {
            tvSearchHint.setVisibility(View.GONE);
        }
        
        Log.d(TAG, "Searching contacts with query: " + query);
        webSocketClient.getUsers(query);
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
        
        // Show user profile in a dialog instead of separate activity
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
    
    private void showNoResults(String query) {
        tvNoResults.setVisibility(View.VISIBLE);
        // Note: Cannot set text since it's now a View, but the empty state layout has appropriate text
        
        recyclerView.setVisibility(View.GONE);
        if (tvSearchHint != null) {
            tvSearchHint.setVisibility(View.GONE);
        }
    }
    
    private void showResults(List<User> users) {
        tvNoResults.setVisibility(View.GONE);
        if (tvSearchHint != null) {
            tvSearchHint.setVisibility(View.GONE);
        }
        recyclerView.setVisibility(View.VISIBLE);
        
        contactsList.clear();
        
        // Filter out current user from results
        for (User user : users) {
            if (!user.getUid().equals(currentUserUid)) {
                contactsList.add(user);
            }
        }
        
        contactsAdapter.notifyDataSetChanged();
        
        if (contactsList.isEmpty()) {
            showNoResults(currentSearchQuery);
        }
    }
    
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    // ====================================================================================
    // ENHANCED WEBSOCKET LISTENER IMPLEMENTATION
    // ====================================================================================
    
    @Override
    public void onConnected() {
        Log.d(TAG, "WebSocket connected for contact search");
        
        // Load all contacts once connected
        if (contactsList.isEmpty()) {
            loadAllContacts();
        }
    }
    
    @Override
    public void onDisconnected(int code, String reason, boolean remote) {
        Log.w(TAG, "WebSocket disconnected: " + reason);
        showLoading(false);
        showToast("Connection lost. Please check your internet.");
    }
    
    @Override
    public void onReconnecting(int attempt) {
        Log.d(TAG, "WebSocket reconnecting attempt: " + attempt);
        showToast("Reconnecting...");
    }
    
    @Override
    public void onError(Exception error) {
        Log.e(TAG, "WebSocket error", error);
        showLoading(false);
        showToast("Connection error occurred");
    }
    
    @Override
    public void onUsersReceived(List<User> users, String searchQuery) {
        Log.d(TAG, "Received " + users.size() + " users for query: " + searchQuery);
        
        showLoading(false);
        
        if (users.isEmpty()) {
            showNoResults(searchQuery != null ? searchQuery : "");
        } else {
            showResults(users);
        }
    }
    
    @Override
    public void onUsersRequestFailed(String error) {
        Log.e(TAG, "Failed to get users: " + error);
        
        showLoading(false);
        showToast("Failed to search contacts: " + error);
        showNoResults(currentSearchQuery);
    }
    
    // Unused WebSocket listener methods (required by interface)
    @Override public void onUserSaved(String userUid, String message) { }
    @Override public void onUserSaveFailed(String error) { }
    @Override public void onUserProfileReceived(User user) { }
    @Override public void onUserProfileRequestFailed(String error) { }
    @Override public void onRoomJoined(String roomId, String message) { }
    @Override public void onRoomJoinFailed(String error) { }
    @Override public void onMessageSent(String messageId) { }
    @Override public void onMessageSendFailed(String error) { }
    @Override public void onMessageReceived(EnhancedWebSocketClient.ChatMessage message) { }
    @Override public void onUserJoinedRoom(String roomId, String userName) { }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Cancel pending searches
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
        
        // Cleanup WebSocket
        if (webSocketClient != null) {
            webSocketClient.cleanup();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // Reconnect WebSocket if needed
        if (webSocketClient != null && !webSocketClient.isConnected()) {
            webSocketClient.connect();
        }
    }
}
