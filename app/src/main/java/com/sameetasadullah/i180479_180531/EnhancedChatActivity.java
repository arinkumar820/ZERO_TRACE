package com.sameetasadullah.i180479_180531;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Enhanced Chat Activity
 * 
 * This activity handles:
 * 1. Real-time messaging using MySQL database via WebSocket
 * 2. Display chat messages in RecyclerView
 * 3. Send and receive messages in real-time
 * 4. Join chat rooms automatically
 */
public class EnhancedChatActivity extends AppCompatActivity implements EnhancedWebSocketClient.EnhancedWebSocketListener {

    private static final String TAG = "EnhancedChatActivity";
    
    // UI Components
    private TextView tvContactName, tvConnectionStatus;
    private ImageView ivBack, ivSend, ivContactImage, ivPanicButton;
    private EditText etMessage;
    private RecyclerView recyclerViewMessages;
    private View layoutConnectionStatus;
    
    // WebSocket Client
    private EnhancedWebSocketClient webSocketClient;
    
    // Adapter and Data
    private MessagesAdapter messagesAdapter;
    private List<ChatMessage> messagesList;
    
    // Chat details
    private String roomId;
    private String contactUid;
    private String contactName;
    private String contactEmail;
    private String contactImageUrl;
    
    // User info
    private SharedPreferences sharedPreferences;
    private String currentUserUid;
    private String currentUserName;
    private String currentUserEmail;
    
    // State
    private boolean isConnected = false;
    private boolean hasJoinedRoom = false;
    
    // Panic button functionality
    private PanicButtonHandler panicButtonHandler;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        
        initializeComponents();
        getChatDetailsFromIntent();
        setupWebSocket();
        setupRecyclerView();
        setupClickListeners();
        setupMessageInput();
        setupPanicButton();
        
        Log.d(TAG, "Enhanced Chat Activity created for room: " + roomId);
    }
    
    private void initializeComponents() {
        // Find UI components
        tvContactName = null; // Not available in existing layout
        tvConnectionStatus = findViewById(R.id.connection_status_text);
        ivBack = null; // Not available in existing layout  
        ivSend = findViewById(R.id.send_button);
        ivPanicButton = findViewById(R.id.panic_button);
        ivContactImage = null; // Not available in existing layout
        etMessage = findViewById(R.id.message_edit_text);
        recyclerViewMessages = findViewById(R.id.chat_recycler_view);
        layoutConnectionStatus = null; // Using connection_status_text directly
        
        // Initialize data structures
        messagesList = new ArrayList<>();
        
        // Get user info
        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        currentUserUid = sharedPreferences.getString("user_uid", "");
        currentUserName = sharedPreferences.getString("user_name", "");
        currentUserEmail = sharedPreferences.getString("user_email", "");
        
        // Initially hide send button
        ivSend.setEnabled(false);
        ivSend.setAlpha(0.5f);
    }
    
    private void getChatDetailsFromIntent() {
        roomId = getIntent().getStringExtra("room_id");
        contactUid = getIntent().getStringExtra("contact_uid");
        contactName = getIntent().getStringExtra("contact_name");
        contactEmail = getIntent().getStringExtra("contact_email");
        contactImageUrl = getIntent().getStringExtra("contact_image_url");
        
        // Set contact info in UI
        if (tvContactName != null) {
            tvContactName.setText(contactName != null ? contactName : contactEmail);
        }
        
        // Load contact image if available
        // TODO: Use image loading library like Glide or Picasso
        // Glide.with(this).load(contactImageUrl).placeholder(R.drawable.default_avatar).into(ivContactImage);
    }
    
    private void setupWebSocket() {
        webSocketClient = new EnhancedWebSocketClient(this, this);
        webSocketClient.connect();
        
        // Show connecting status
        showConnectionStatus("Connecting...", true);
    }

    // Network status listener to show "Out of range" when no internet
    private final NetworkMonitor.Listener networkListener = new NetworkMonitor.Listener() {
        @Override
        public void onNetworkAvailable() {
            runOnUiThread(() -> showConnectionStatus("", false));
        }
        @Override
        public void onNetworkLost() {
            runOnUiThread(() -> showConnectionStatus("Out of range: No network", true));
        }
    };
    
    private void setupRecyclerView() {
        messagesAdapter = new MessagesAdapter(messagesList, currentUserUid);
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Start from bottom
        
        recyclerViewMessages.setLayoutManager(layoutManager);
        recyclerViewMessages.setAdapter(messagesAdapter);
    }
    
    private void setupClickListeners() {
        if (ivBack != null) {
            ivBack.setOnClickListener(v -> onBackPressed());
        }
        
        if (ivSend != null) {
            ivSend.setOnClickListener(v -> sendMessage());
        }
    }
    
    private void setupMessageInput() {
        etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasText = s.toString().trim().length() > 0;
                boolean canSend = hasText && isConnected && hasJoinedRoom;
                
                if (ivSend != null) {
                    ivSend.setEnabled(canSend);
                    ivSend.setAlpha(canSend ? 1.0f : 0.5f);
                }
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // Send message on enter key
        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });
    }
    
    private void setupPanicButton() {
        // Initialize panic button handler
        panicButtonHandler = new PanicButtonHandler(this);
        
        // Set up panic button click listener
        if (ivPanicButton != null) {
            ivPanicButton.setOnClickListener(v -> {
                Log.w(TAG, "🚨 Panic button pressed in EnhancedChatActivity");
                panicButtonHandler.handlePanicButtonPress();
            });
            
            // Set up long press for quick panic mode
            ivPanicButton.setOnLongClickListener(v -> {
                Log.w(TAG, "🚨 Panic button LONG PRESSED - Quick panic mode");
                panicButtonHandler.handleQuickPanic();
                return true; // Consume the long click event
            });
        }
        
        Log.d(TAG, "Panic button functionality initialized in EnhancedChatActivity");
    }
    
    // ====================================================================================
    // MESSAGING FUNCTIONALITY
    // ====================================================================================
    
    private void sendMessage() {
        String messageText = etMessage.getText().toString().trim();
        
        if (messageText.isEmpty()) {
            return;
        }
        
        if (!isConnected || !hasJoinedRoom) {
            showToast("Not connected to chat. Please wait...");
            return;
        }
        
        Log.d(TAG, "Sending message: " + messageText);
        
        // Send message via WebSocket
        webSocketClient.sendChatMessage(roomId, messageText, "text");
        
        // Clear input field
        etMessage.setText("");
        
        // Add optimistic message to UI (will be replaced when server confirms)
        addOptimisticMessage(messageText);
    }
    
    private void addOptimisticMessage(String messageText) {
        ChatMessage optimisticMessage = new ChatMessage();
        optimisticMessage.setMessageId("temp_" + System.currentTimeMillis());
        optimisticMessage.setRoomId(roomId);
        optimisticMessage.setSenderUid(currentUserUid);
        optimisticMessage.setSenderName(currentUserName);
        optimisticMessage.setSenderEmail(currentUserEmail);
        optimisticMessage.setMessage(messageText);
        optimisticMessage.setMessageType("text");
        optimisticMessage.setTimestamp(getCurrentTimestamp());
        
        messagesList.add(optimisticMessage);
        messagesAdapter.notifyItemInserted(messagesList.size() - 1);
        scrollToBottom();
    }
    
    private void addReceivedMessage(EnhancedWebSocketClient.ChatMessage receivedMessage) {
        // Convert WebSocket message to local ChatMessage
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setMessageId(receivedMessage.getMessageId());
        chatMessage.setRoomId(receivedMessage.getRoomId());
        chatMessage.setSenderUid(receivedMessage.getSenderUid());
        chatMessage.setSenderName(receivedMessage.getSenderName());
        chatMessage.setSenderEmail(receivedMessage.getSenderEmail());
        chatMessage.setMessage(receivedMessage.getMessage());
        chatMessage.setMessageType(receivedMessage.getMessageType());
        chatMessage.setTimestamp(receivedMessage.getTimestamp());
        
        // Remove optimistic message if this is from current user
        if (receivedMessage.getSenderUid().equals(currentUserUid)) {
            removeOptimisticMessage();
        }
        
        messagesList.add(chatMessage);
        messagesAdapter.notifyItemInserted(messagesList.size() - 1);
        scrollToBottom();
    }
    
    private void removeOptimisticMessage() {
        // Remove last optimistic message (temporary message starting with "temp_")
        for (int i = messagesList.size() - 1; i >= 0; i--) {
            ChatMessage message = messagesList.get(i);
            if (message.getMessageId().startsWith("temp_") && 
                message.getSenderUid().equals(currentUserUid)) {
                messagesList.remove(i);
                messagesAdapter.notifyItemRemoved(i);
                break;
            }
        }
    }
    
    private void scrollToBottom() {
        if (messagesList.size() > 0) {
            recyclerViewMessages.smoothScrollToPosition(messagesList.size() - 1);
        }
    }
    
    private String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }
    
    // ====================================================================================
    // UI HELPER METHODS
    // ====================================================================================
    
    private void showConnectionStatus(String status, boolean show) {
        if (tvConnectionStatus != null) {
            if (show) {
                tvConnectionStatus.setText(status);
                tvConnectionStatus.setVisibility(View.VISIBLE);
            } else {
                tvConnectionStatus.setVisibility(View.GONE);
            }
        }
    }
    
    private void updateSendButtonState() {
        if (etMessage != null && ivSend != null) {
            boolean hasText = etMessage.getText().toString().trim().length() > 0;
            boolean canSend = hasText && isConnected && hasJoinedRoom;
            
            ivSend.setEnabled(canSend);
            ivSend.setAlpha(canSend ? 1.0f : 0.5f);
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
        Log.d(TAG, "WebSocket connected for chat");
        isConnected = true;
        
        showConnectionStatus("Joining room...", true);
        
        // Join the chat room
        webSocketClient.joinRoom(roomId);
    }
    
    @Override
    public void onDisconnected(int code, String reason, boolean remote) {
        Log.w(TAG, "WebSocket disconnected: " + reason);
        isConnected = false;
        hasJoinedRoom = false;
        
        showConnectionStatus("Out of range: Server not connected", true);
        updateSendButtonState();
    }
    
    @Override
    public void onReconnecting(int attempt) {
        Log.d(TAG, "WebSocket reconnecting attempt: " + attempt);
        showConnectionStatus("Out of range: Server not connected (Reconnecting " + attempt + "/5)", true);
    }
    
    @Override
    public void onError(Exception error) {
        Log.e(TAG, "WebSocket error in chat", error);
        isConnected = false;
        hasJoinedRoom = false;
        
        showConnectionStatus("Out of range: Server not connected", true);
        updateSendButtonState();
        showToast("Out of range: Server not connected");
    }
    
    @Override
    public void onRoomJoined(String roomId, String message) {
        Log.d(TAG, "Successfully joined room: " + roomId);
        hasJoinedRoom = true;
        
        showConnectionStatus("", false);
        updateSendButtonState();
        showToast("Connected to chat");
    }
    
    @Override
    public void onRoomJoinFailed(String error) {
        Log.e(TAG, "Failed to join room: " + error);
        hasJoinedRoom = false;
        
        showConnectionStatus("Failed to join chat", true);
        updateSendButtonState();
        showToast("Failed to join chat: " + error);
    }
    
    @Override
    public void onMessageSent(String messageId) {
        Log.d(TAG, "Message sent successfully: " + messageId);
        // Message will be received via onMessageReceived
    }
    
    @Override
    public void onMessageSendFailed(String error) {
        Log.e(TAG, "Failed to send message: " + error);
        showToast("Failed to send message: " + error);
        
        // Remove optimistic message
        removeOptimisticMessage();
    }
    
    @Override
    public void onMessageReceived(EnhancedWebSocketClient.ChatMessage message) {
        Log.d(TAG, "Received message from " + message.getSenderEmail());
        
        // Only add message if it's for this room
        if (roomId.equals(message.getRoomId())) {
            addReceivedMessage(message);
        }
    }
    
    @Override
    public void onUserJoinedRoom(String roomId, String userName) {
        Log.d(TAG, "User joined room: " + userName);
        
        if (this.roomId.equals(roomId) && !userName.equals(currentUserName)) {
            showToast(userName + " joined the chat");
        }
    }
    
    // Unused WebSocket listener methods (required by interface)
    @Override public void onUserSaved(String userUid, String message) { }
    @Override public void onUserSaveFailed(String error) { }
    @Override public void onUsersReceived(List<User> users, String searchQuery) { }
    @Override public void onUsersRequestFailed(String error) { }
    @Override public void onUserProfileReceived(User user) { }
    @Override public void onUserProfileRequestFailed(String error) { }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
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
        
        // Register network listener
        NetworkMonitor.addListener(networkListener);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        
        // Keep connection alive but you could disconnect if needed
        // webSocketClient.disconnect();
        
        // Unregister network listener
        NetworkMonitor.removeListener(networkListener);
    }
}
