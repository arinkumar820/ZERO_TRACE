package com.sameetasadullah.i180479_180531;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.AlertDialog;
import androidx.annotation.NonNull;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ArrayAdapter;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity implements WebSocketClientListener {
    
    private static final String TAG = "ChatActivity";
    // Use "ws://10.0.2.2:5000/" for emulator or "ws://YOUR_PC_IP:5000/" for real device
    private static final String WEBSOCKET_SERVER_URL = "ws://10.48.121.125:8081/"; // PC IPv4 for physical device
    // For emulator: "ws://10.0.2.2:5000/"
    
    private RecyclerView chatRecyclerView;
    private EditText messageEditText;
    private ImageButton sendButton;
    private ImageButton panicButton;
    private TextView connectionStatusText;
    
    private ChatMessageAdapter messageAdapter;
    private List<WebSocketMessage> messageList;
    private WebSocketClientManager webSocketClient;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;
    private String chatName;
    private String chatId;
    
    // Screenshot protection
    private ScreenshotDetector screenshotDetector;
    
    // Panic button functionality
    private PanicButtonHandler panicButtonHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 🛡️ SCREENSHOT PROTECTION: Prevent screenshots and screen recording
        ScreenshotProtection.enableProtection(this);
        
        // 🔍 DEBUG: Verify protection is actually enabled
        boolean isProtected = ScreenshotProtection.isProtectionActive(this);
        Log.d(TAG, "🛡️ Screenshot protection status: " + (isProtected ? "ACTIVE" : "INACTIVE"));
        if (isProtected) {
            Toast.makeText(this, "🛡️ Chat protected from screenshots - Try taking a screenshot!", Toast.LENGTH_LONG).show();
        }
        
        // 🗞️ TEMPORARY TEST: Add button to toggle protection (REMOVE IN PRODUCTION)
        // addTemporaryTestButton();
        
        // Initialize screenshot detection for additional security
        screenshotDetector = ScreenshotDetector.createBasicDetector(this);
        
        setContentView(R.layout.activity_chat);
        
        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();
        
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Get chat details from intent
        chatName = getIntent().getStringExtra("chat_name");
        chatId = getIntent().getStringExtra("chat_id");
        
        if (chatName == null) chatName = "Chat";
        if (chatId == null) chatId = "general";
        
        // Set activity title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(chatName);
        }
        
        initializeViews();
        setupRecyclerView();
        setupWebSocketClient();
        setupSendButton();
        setupPanicButton();
    }
    
    private void initializeViews() {
        chatRecyclerView = findViewById(R.id.chat_recycler_view);
        messageEditText = findViewById(R.id.message_edit_text);
        sendButton = findViewById(R.id.send_button);
        panicButton = findViewById(R.id.panic_button);
        connectionStatusText = findViewById(R.id.connection_status_text);
    }
    
    private void setupRecyclerView() {
        messageList = new ArrayList<>();
        messageAdapter = new ChatMessageAdapter(messageList, currentUser.getUid(), this);
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(messageAdapter);
        
        // Set up read receipt listener for the adapter
        messageAdapter.setReadReceiptListener(new ChatMessageAdapter.ReadReceiptListener() {
            @Override
            public void onMessageSeen(WebSocketMessage message, int position) {
                // Send read receipt via WebSocket
                sendReadReceipt(message);
            }
        });
        
        // Add scroll listener to automatically mark messages as seen when they come into view
        chatRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                markVisibleMessagesAsSeen();
            }
            
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    // Mark messages as seen when scrolling stops
                    markVisibleMessagesAsSeen();
                }
            }
        });
    }
    
    private void setupWebSocketClient() {
        webSocketClient = new WebSocketClientManager(WEBSOCKET_SERVER_URL, this);
        webSocketClient.connect();
    }

    // Network status listener to show "Out of range" when no internet
    private final NetworkMonitor.Listener networkListener = new NetworkMonitor.Listener() {
        @Override
        public void onNetworkAvailable() {
            runOnUiThread(() -> {
                if (connectionStatusText != null) {
                    connectionStatusText.setText("Connected");
                    connectionStatusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                }
            });
        }
        @Override
        public void onNetworkLost() {
            runOnUiThread(() -> {
                if (connectionStatusText != null) {
                    connectionStatusText.setText("Out of range: No network");
                    connectionStatusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                }
                android.widget.Toast.makeText(ChatActivity.this, "Out of range: No network", android.widget.Toast.LENGTH_SHORT).show();
            });
        }
    };
    
    private void joinChat() {
        if (currentUser != null && webSocketClient != null) {
            String displayName = currentUser.getDisplayName();
            if (displayName == null || displayName.isEmpty()) {
                displayName = currentUser.getEmail();
                if (displayName != null && displayName.contains("@")) {
                    displayName = displayName.substring(0, displayName.indexOf("@"));
                }
            }
            
            WebSocketMessage joinMessage = WebSocketMessage.createJoinMessage(
                currentUser.getUid(),
                currentUser.getEmail(),
                displayName,
                chatId  // Use the actual chat ID, not default "general_chat"
            );
            
            webSocketClient.sendMessage(joinMessage);
            Log.d(TAG, "Sent join message for user: " + currentUser.getEmail());
        }
    }
    
    private void setupSendButton() {
        sendButton.setOnClickListener(v -> sendMessage());
        
        messageEditText.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });
    }
    
    private void setupPanicButton() {
        // Initialize panic button handler
        panicButtonHandler = new PanicButtonHandler(this);
        
        // Set up panic button click listener
        panicButton.setOnClickListener(v -> {
            Log.w(TAG, "🚨 Panic button pressed in ChatActivity");
            panicButtonHandler.handlePanicButtonPress();
        });
        
        // Set up long press for quick panic mode (optional - immediate action)
        panicButton.setOnLongClickListener(v -> {
            Log.w(TAG, "🚨 Panic button LONG PRESSED - Quick panic mode");
            panicButtonHandler.handleQuickPanic();
            return true; // Consume the long click event
        });
        
        Log.d(TAG, "Panic button functionality initialized");
    }
    
    
    
    private void sendMessage() {
        String messageText = messageEditText.getText().toString().trim();
        
        if (TextUtils.isEmpty(messageText)) {
            return;
        }
        
        if (!webSocketClient.isConnected()) {
            Toast.makeText(this, "Not connected to server", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create persistent message
        WebSocketMessage message = WebSocketMessage.createMessage(
                currentUser.getUid(),
                currentUser.getEmail(),
                messageText
        );
        
        // Set the correct chat room ID
        message.setChat_room_id(chatId);
        
        // Send message via WebSocket
        webSocketClient.sendMessage(message);
        
        // Clear input
        messageEditText.setText("");
        
        Log.d(TAG, "Sent persistent message: '" + messageText + "'");
    }
    
    /**
     * Mark visible messages as seen and send read receipts
     */
    private void markVisibleMessagesAsSeen() {
        if (chatRecyclerView == null || messageAdapter == null) {
            return;
        }
        
        LinearLayoutManager layoutManager = (LinearLayoutManager) chatRecyclerView.getLayoutManager();
        if (layoutManager == null) {
            return;
        }
        
        int firstVisiblePosition = layoutManager.findFirstVisibleItemPosition();
        int lastVisiblePosition = layoutManager.findLastVisibleItemPosition();
        
        for (int position = firstVisiblePosition; position <= lastVisiblePosition; position++) {
            if (position >= 0 && position < messageList.size()) {
                WebSocketMessage message = messageList.get(position);
                
                // Only mark messages from others as seen
                if (message.getSender_uid() != null && 
                    !message.getSender_uid().equals(currentUser.getUid()) && 
                    !message.isSeen()) {
                    
                    // Mark locally as seen
                    messageAdapter.markMessageAsSeen(position, currentUser.getUid());
                    
                    // Send read receipt to sender
                    sendReadReceipt(message);
                    
                    Log.d(TAG, "Marked message as seen: " + message.getMessage_id());
                }
            }
        }
    }
    
    /**
     * Send read receipt for a message
     */
    private void sendReadReceipt(WebSocketMessage originalMessage) {
        if (webSocketClient == null || !webSocketClient.isConnected() || originalMessage == null) {
            return;
        }
        
        // Don't send read receipt for our own messages
        if (originalMessage.getSender_uid() != null && 
            originalMessage.getSender_uid().equals(currentUser.getUid())) {
            return;
        }
        
        try {
            WebSocketMessage readReceipt = WebSocketMessage.createReadReceipt(
                currentUser.getUid(),
                currentUser.getEmail(),
                originalMessage.getMessage_id(),
                chatId
            );
            
            webSocketClient.sendMessage(readReceipt);
            Log.d(TAG, "Sent read receipt for message: " + originalMessage.getMessage_id());
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to send read receipt", e);
        }
    }

    // WebSocketClientListener implementation
    @Override
    public void onConnected() {
        runOnUiThread(() -> {
            connectionStatusText.setText("Connected");
            connectionStatusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            Toast.makeText(ChatActivity.this, "Connected to chat server", Toast.LENGTH_SHORT).show();
        });
        Log.d(TAG, "WebSocket connected");
        
        // Join the chat after connection
        joinChat();
    }

    @Override
    public void onMessageReceived(WebSocketMessage message) {
        String messageType = message.getType();
        Log.d(TAG, "Received message type: " + messageType + ", content: " + message.getMessage());
        Log.d(TAG, "Message details - sender_uid: " + message.getSender_uid() + ", sender_email: " + message.getSender_email() + ", timestamp: " + message.getTimestamp());
        
        if ("connected".equals(messageType)) {
            Log.d(TAG, "Server connection confirmed");
            return;
        }
        
        if ("join_success".equals(messageType)) {
            runOnUiThread(() -> {
                Toast.makeText(this, "Joined chat successfully", Toast.LENGTH_SHORT).show();
            });
            Log.d(TAG, "Successfully joined chat");
            return;
        }
        
        if ("error".equals(messageType)) {
            runOnUiThread(() -> {
                Toast.makeText(this, "Server error: " + message.getMessage(), Toast.LENGTH_LONG).show();
            });
            Log.e(TAG, "Server error: " + message.getMessage());
            return;
        }
        
        // Handle regular messages, new messages from server, and history messages
        if ("message".equals(messageType) || "new_message".equals(messageType) || "history".equals(messageType)) {
            // CRITICAL: Filter messages by room ID to prevent cross-chat message display
            String messageRoomId = message.getRoom_id(); // Server sends 'room_id', not 'chat_room_id'
            if (messageRoomId != null && !messageRoomId.equals(chatId)) {
                Log.d(TAG, "Ignoring message from different room: " + messageRoomId + " (current: " + chatId + ")");
                return; // Ignore messages from other rooms
            }
            
            // Fix timestamp if it's 0 or missing
            if (message.getTimestamp() == 0) {
                message.setTimestamp(System.currentTimeMillis());
            }
            
            // Send seen acknowledgement for messages received from others (only for live/new messages)
            if (("message".equals(messageType) || "new_message".equals(messageType))
                    && message.getSender_uid() != null
                    && !message.getSender_uid().equals(currentUser.getUid())
                    && message.getMessage_id() > 0) {
                webSocketClient.sendSeenAck(chatId, message.getMessage_id(), currentUser.getUid());
            }
            
            runOnUiThread(() -> {
                Log.d(TAG, "Adding message to list. Current size: " + messageList.size());
                
                // Check for duplicate messages (prevent double display)
                boolean isDuplicate = false;
                String messageText = message.getMessage();
                String senderUid = message.getSender_uid();
                long timestamp = message.getTimestamp();
                
                // Check if we already have this exact message from the same sender within 5 seconds
                for (WebSocketMessage existingMsg : messageList) {
                    if (existingMsg.getMessage() != null && existingMsg.getMessage().equals(messageText) &&
                        existingMsg.getSender_uid() != null && existingMsg.getSender_uid().equals(senderUid) &&
                        Math.abs(existingMsg.getTimestamp() - timestamp) < 5000) { // Within 5 seconds
                        isDuplicate = true;
                        Log.d(TAG, "Duplicate message detected, skipping");
                        break;
                    }
                }
                
                if (!isDuplicate) {
                    // Add message to list
                    messageList.add(message);
                    messageAdapter.notifyItemInserted(messageList.size() - 1);
                    Log.d(TAG, "Message added. New size: " + messageList.size());
                    
                    // Scroll to bottom for new messages (not history)
                    if ("message".equals(messageType) || "new_message".equals(messageType)) {
                        chatRecyclerView.scrollToPosition(messageList.size() - 1);
                        Log.d(TAG, "Scrolled to position: " + (messageList.size() - 1));
                        
                        // Mark visible messages as seen after scrolling
                        chatRecyclerView.post(() -> markVisibleMessagesAsSeen());
                    }
                }
            });
            
            String logMessage = "history".equals(messageType) ? "History" : "New message";
            Log.d(TAG, logMessage + " processed: " + message.getMessage());
        } else if ("message_seen".equals(messageType) || "read_receipt".equals(messageType)) {
            // Mark the corresponding sent message as seen
            runOnUiThread(() -> {
                int pos = -1;
                int targetId = message.getMessage_id();
                String seenBy = message.getSender_uid(); // Who marked it as seen
                
                if (targetId > 0) {
                    for (int i = messageList.size() - 1; i >= 0; i--) {
                        WebSocketMessage m = messageList.get(i);
                        if (m.getMessage_id() == targetId && m.getSender_uid() != null && m.getSender_uid().equals(currentUser.getUid())) {
                            pos = i;
                            m.markAsSeen(seenBy);
                            Log.d(TAG, "Message " + targetId + " marked as seen by " + seenBy);
                            break;
                        }
                    }
                    if (pos >= 0) {
                        messageAdapter.notifyItemChanged(pos);
                    }
                }
            });
        } else {
            Log.w(TAG, "Unknown message type: " + messageType + " with content: " + message.getMessage());
        }
    }

    @Override
    public void onDisconnected(int code, String reason, boolean remote) {
        runOnUiThread(() -> {
            connectionStatusText.setText("Out of range: Server not connected");
            connectionStatusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            android.widget.Toast.makeText(ChatActivity.this, "Out of range: Server not connected", android.widget.Toast.LENGTH_SHORT).show();
        });
        Log.d(TAG, "WebSocket disconnected: " + reason);
        
        // Don't clear messages on disconnect - keep them visible
        // The reconnection will sync any new messages
    }

    @Override
    public void onError(Exception error) {
        runOnUiThread(() -> {
            connectionStatusText.setText("Out of range: Server not connected");
            connectionStatusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            Toast.makeText(ChatActivity.this, "Out of range: Server not connected", Toast.LENGTH_SHORT).show();
        });
        Log.e(TAG, "WebSocket error", error);
    }

    @Override
    public void onReconnecting(int attempt) {
        runOnUiThread(() -> {
            connectionStatusText.setText("Out of range: Server not connected (Reconnecting " + attempt + ")");
            connectionStatusText.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
        });
        Log.d(TAG, "WebSocket reconnecting, attempt: " + attempt);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Activity resumed - messages preserved: " + messageList.size());
        
        // Start screenshot detection
        if (screenshotDetector != null) {
            screenshotDetector.startListening();
        }
        
        // Reconnect if needed
        if (webSocketClient != null && !webSocketClient.isConnected()) {
            Log.d(TAG, "Reconnecting WebSocket on resume");
            webSocketClient.resetReconnectAttempts();
            webSocketClient.connect();
        }
        
        // Register for network changes
        NetworkMonitor.addListener(networkListener);
        
        // Mark visible messages as seen when returning to the chat
        if (chatRecyclerView != null) {
            chatRecyclerView.post(() -> markVisibleMessagesAsSeen());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "Activity paused - preserving " + messageList.size() + " messages");
        
        // Stop screenshot detection to save resources
        if (screenshotDetector != null) {
            screenshotDetector.stopListening();
        }
        
        // Don't disconnect WebSocket on pause - keep it active for background messages
        
        // Unregister network listener
        NetworkMonitor.removeListener(networkListener);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Activity destroyed - cleaning up WebSocket and screenshot detection");
        
        // Cleanup screenshot detection
        if (screenshotDetector != null) {
            screenshotDetector.stopListening();
        }
        
        if (webSocketClient != null) {
            webSocketClient.cleanup();
        }
        if (messageAdapter != null) {
            messageAdapter.cleanup();
        }
    }
}