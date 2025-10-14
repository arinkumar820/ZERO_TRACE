package com.sameetasadullah.i180479_180531;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/**
 * Enhanced WebSocket Client for Bisto Chat
 * Handles both user data operations and chat messaging through a single WebSocket connection
 * 
 * Supported operations:
 * - save_user: Store user profile data
 * - get_users: Search and retrieve users  
 * - get_user_profile: Get specific user details
 * - join_room: Join a chat room
 * - send_message: Send chat messages
 */
public class EnhancedWebSocketClient extends WebSocketListener {
    
    private static final String TAG = "EnhancedWebSocketClient";
    
    // Server Configuration
    private static final String SERVER_URL = "ws://10.48.121.125:8081";  // PC IPv4 for physical device
    
    private WebSocket webSocket;
    private OkHttpClient client;
    private Handler mainHandler;
    private Context context;
    private EnhancedWebSocketListener listener;
    
    // Connection state
    private boolean isConnected = false;
    private boolean isReconnecting = false;
    private int reconnectAttempts = 0;
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    
    // Traffic shaping configuration and scheduler
    private static final long DEFAULT_BASE_INTERVAL_MS = 250; // constant rate base interval
    private static final long DEFAULT_JITTER_MS = 50;        // ± jitter per tick
    private static final int DEFAULT_PAD_MIN_CHARS = 256;    // min chars for message body padding
    private static final int TARGET_FRAME_SIZE = 512;        // target total JSON length with pad field
    private boolean enablePadding = true;                    // allow padding chat payloads
    private TransportScheduler transportScheduler;
    
    /**
     * Interface for WebSocket events and responses
     */
    public interface EnhancedWebSocketListener {
        // Connection events
        void onConnected();
        void onDisconnected(int code, String reason, boolean remote);
        void onReconnecting(int attempt);
        void onError(Exception error);
        
        // User data responses
        void onUserSaved(String userUid, String message);
        void onUserSaveFailed(String error);
        
        void onUsersReceived(List<User> users, String searchQuery);
        void onUsersRequestFailed(String error);
        
        void onUserProfileReceived(User user);
        void onUserProfileRequestFailed(String error);
        
        // Chat responses
        void onRoomJoined(String roomId, String message);
        void onRoomJoinFailed(String error);
        
        void onMessageSent(String messageId);
        void onMessageSendFailed(String error);
        
        void onMessageReceived(ChatMessage message);
        void onUserJoinedRoom(String roomId, String userName);
    }
    
    /**
     * Chat message data class
     */
    public static class ChatMessage {
        private String messageId;
        private String roomId;
        private String senderUid;
        private String senderEmail;
        private String senderName;
        private String message;
        private String messageType;
        private String timestamp;
        
        // Getters and setters
        public String getMessageId() { return messageId; }
        public void setMessageId(String messageId) { this.messageId = messageId; }
        
        public String getRoomId() { return roomId; }
        public void setRoomId(String roomId) { this.roomId = roomId; }
        
        public String getSenderUid() { return senderUid; }
        public void setSenderUid(String senderUid) { this.senderUid = senderUid; }
        
        public String getSenderEmail() { return senderEmail; }
        public void setSenderEmail(String senderEmail) { this.senderEmail = senderEmail; }
        
        public String getSenderName() { return senderName; }
        public void setSenderName(String senderName) { this.senderName = senderName; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getMessageType() { return messageType; }
        public void setMessageType(String messageType) { this.messageType = messageType; }
        
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    }
    
    public EnhancedWebSocketClient(Context context, EnhancedWebSocketListener listener) {
        this.context = context;
        this.listener = listener;
        this.mainHandler = new Handler(Looper.getMainLooper());
        
        // Configure OkHttp client
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)  // No read timeout for WebSocket
                .writeTimeout(10, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
        
        Log.d(TAG, "Enhanced WebSocket client initialized");
    }
    
    /**
     * Connect to WebSocket server
     */
    public void connect() {
        if (isConnected || isReconnecting) {
            Log.d(TAG, "Already connected or reconnecting");
            return;
        }
        
        Log.d(TAG, "Connecting to WebSocket server: " + SERVER_URL);
        
        Request request = new Request.Builder()
                .url(SERVER_URL)
                .build();
        
        webSocket = client.newWebSocket(request, this);
    }
    
    /**
     * Disconnect from WebSocket server
     */
    public void disconnect() {
        isReconnecting = false;
        reconnectAttempts = 0;
        
        if (webSocket != null) {
            webSocket.close(1000, "Client requested disconnect");
        }
    }
    
    /**
     * Check if connected to server
     */
    public boolean isConnected() {
        return isConnected;
    }
    
    // ====================================================================================
    // USER DATA OPERATIONS
    // ====================================================================================
    
    /**
     * Save user profile data to server
     */
    public void saveUser(String uid, String name, String email, String phoneNumber, String bio) {
        if (!isConnected) {
            if (listener != null) {
                mainHandler.post(() -> listener.onUserSaveFailed("Not connected to server"));
            }
            return;
        }
        
        try {
            JSONObject message = new JSONObject();
            message.put("type", "save_user");
            message.put("uid", uid);
            message.put("name", name);
            message.put("email", email);
            message.put("phone_number", phoneNumber != null ? phoneNumber : "");
            message.put("bio", bio != null ? bio : "Hey there! I am using Bisto Chat.");
            
            sendMessage(message);
            Log.d(TAG, "Saving user: " + email);
            
        } catch (JSONException e) {
            Log.e(TAG, "Error creating save_user message", e);
            if (listener != null) {
                mainHandler.post(() -> listener.onUserSaveFailed("Message creation error"));
            }
        }
    }
    
    /**
     * Get all users or search users
     */
    public void getUsers(String searchQuery) {
        if (!isConnected) {
            if (listener != null) {
                mainHandler.post(() -> listener.onUsersRequestFailed("Not connected to server"));
            }
            return;
        }
        
        try {
            JSONObject message = new JSONObject();
            message.put("type", "get_users");
            if (searchQuery != null && !searchQuery.trim().isEmpty()) {
                message.put("search_query", searchQuery.trim());
            }
            
            sendMessage(message);
            Log.d(TAG, "Requesting users with search: " + (searchQuery != null ? searchQuery : "all"));
            
        } catch (JSONException e) {
            Log.e(TAG, "Error creating get_users message", e);
            if (listener != null) {
                mainHandler.post(() -> listener.onUsersRequestFailed("Message creation error"));
            }
        }
    }
    
    /**
     * Get specific user profile
     */
    public void getUserProfile(String uid) {
        if (!isConnected) {
            if (listener != null) {
                mainHandler.post(() -> listener.onUserProfileRequestFailed("Not connected to server"));
            }
            return;
        }
        
        try {
            JSONObject message = new JSONObject();
            message.put("type", "get_user_profile");
            message.put("uid", uid);
            
            sendMessage(message);
            Log.d(TAG, "Requesting user profile: " + uid);
            
        } catch (JSONException e) {
            Log.e(TAG, "Error creating get_user_profile message", e);
            if (listener != null) {
                mainHandler.post(() -> listener.onUserProfileRequestFailed("Message creation error"));
            }
        }
    }
    
    // ====================================================================================
    // CHAT OPERATIONS  
    // ====================================================================================
    
    /**
     * Join a chat room
     */
    public void joinRoom(String roomId) {
        if (!isConnected) {
            if (listener != null) {
                mainHandler.post(() -> listener.onRoomJoinFailed("Not connected to server"));
            }
            return;
        }
        
        try {
            JSONObject message = new JSONObject();
            message.put("type", "join_room");
            message.put("room_id", roomId);
            
            sendMessage(message);
            Log.d(TAG, "Joining room: " + roomId);
            
        } catch (JSONException e) {
            Log.e(TAG, "Error creating join_room message", e);
            if (listener != null) {
                mainHandler.post(() -> listener.onRoomJoinFailed("Message creation error"));
            }
        }
    }
    
    /**
     * Send chat message
     */
    public void sendChatMessage(String roomId, String messageText, String messageType) {
        if (!isConnected) {
            if (listener != null) {
                mainHandler.post(() -> listener.onMessageSendFailed("Not connected to server"));
            }
            return;
        }
        
        if (messageText == null || messageText.trim().isEmpty()) {
            if (listener != null) {
                mainHandler.post(() -> listener.onMessageSendFailed("Message cannot be empty"));
            }
            return;
        }
        
        try {
            JSONObject message = new JSONObject();
            message.put("type", "send_message");
            message.put("room_id", roomId);
            message.put("message", messageText.trim());
            message.put("message_type", messageType != null ? messageType : "text");
            
            sendMessage(message);
            Log.d(TAG, "Sending message to room " + roomId + ": " + messageText);
            
        } catch (JSONException e) {
            Log.e(TAG, "Error creating send_message message", e);
            if (listener != null) {
                mainHandler.post(() -> listener.onMessageSendFailed("Message creation error"));
            }
        }
    }
    
    /**
     * Send ping to keep connection alive
     */
    public void sendPing() {
        if (!isConnected) {
            return;
        }
        
        try {
            JSONObject message = new JSONObject();
            message.put("type", "ping");
            sendMessage(message);
            
        } catch (JSONException e) {
            Log.e(TAG, "Error creating ping message", e);
        }
    }
    
    // ====================================================================================
    // WEBSOCKET LISTENER METHODS
    // ====================================================================================
    
    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        Log.d(TAG, "✅ WebSocket connection opened");
        isConnected = true;
        isReconnecting = false;
        reconnectAttempts = 0;
        
        // Initialize and start transport scheduler (constant-rate + jitter)
        if (transportScheduler == null) {
            transportScheduler = new TransportScheduler(DEFAULT_BASE_INTERVAL_MS, DEFAULT_JITTER_MS, new TransportScheduler.Sender() {
                @Override
                public void send(JSONObject msg) {
                    sendNow(msg);
                }
                @Override
                public void sendCover() {
                    // Use ping as harmless cover traffic and normalize size
                    try {
                        JSONObject cover = new JSONObject();
                        cover.put("type", "ping");
                        // Add pad field to reach target frame size
                        JSONObject padded = PayloadPadder.padWithPadField(cover, TARGET_FRAME_SIZE);
                        sendNow(padded);
                    } catch (Exception ignored) { }
                }
            });
        }
        transportScheduler.start();
        
        if (listener != null) {
            mainHandler.post(() -> listener.onConnected());
        }
    }
    
    @Override
    public void onMessage(WebSocket webSocket, String text) {
        Log.d(TAG, "📨 Received message: " + text);
        
        try {
            JSONObject messageJson = new JSONObject(text);
            String messageType = messageJson.optString("type", "unknown");
            
            handleMessage(messageType, messageJson);
            
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing message JSON", e);
        }
    }
    
    @Override
    public void onMessage(WebSocket webSocket, ByteString bytes) {
        Log.d(TAG, "📨 Received bytes message: " + bytes.hex());
    }
    
    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        Log.d(TAG, "🔌 WebSocket closing: " + code + " " + reason);
        webSocket.close(1000, null);
    }
    
    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
        Log.d(TAG, "🔌 WebSocket closed: " + code + " " + reason);
        isConnected = false;
        
        // Stop scheduler when disconnected
        stopScheduler();
        
        if (listener != null) {
            mainHandler.post(() -> listener.onDisconnected(code, reason, true));
        }
        
        // Attempt reconnection if it was unexpected
        if (code != 1000 && !isReconnecting) {
            attemptReconnect();
        }
    }
    
    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        Log.e(TAG, "❌ WebSocket connection failed", t);
        isConnected = false;
        
        // Stop scheduler on failure
        stopScheduler();
        
        if (listener != null) {
            mainHandler.post(() -> listener.onError(new Exception(t)));
        }
        
        if (!isReconnecting) {
            attemptReconnect();
        }
    }
    
    // ====================================================================================
    // MESSAGE HANDLING
    // ====================================================================================
    
    private void handleMessage(String messageType, JSONObject messageJson) {
        if (listener == null) return;
        
        switch (messageType) {
            case "welcome":
                String welcomeMessage = messageJson.optString("message", "");
                Log.d(TAG, "📱 Server welcome: " + welcomeMessage);
                break;
                
            case "save_user_response":
                handleSaveUserResponse(messageJson);
                break;
                
            case "get_users_response":
                handleGetUsersResponse(messageJson);
                break;
                
            case "get_user_profile_response":
                handleGetUserProfileResponse(messageJson);
                break;
                
            case "join_room_response":
                handleJoinRoomResponse(messageJson);
                break;
                
            case "send_message_response":
                handleSendMessageResponse(messageJson);
                break;
                
            case "new_message":
                handleNewMessage(messageJson);
                break;
                
            case "user_joined":
                handleUserJoined(messageJson);
                break;
                
            case "pong":
                Log.d(TAG, "🏓 Received pong from server");
                break;
                
            case "error":
                String errorMessage = messageJson.optString("message", "Unknown error");
                Log.e(TAG, "❌ Server error: " + errorMessage);
                break;
                
            default:
                Log.w(TAG, "Unknown message type: " + messageType);
                break;
        }
    }
    
    private void handleSaveUserResponse(JSONObject messageJson) {
        String status = messageJson.optString("status", "");
        String message = messageJson.optString("message", "");
        String userUid = messageJson.optString("user_uid", "");
        
        if ("saved".equals(status)) {
            mainHandler.post(() -> listener.onUserSaved(userUid, message));
        } else {
            mainHandler.post(() -> listener.onUserSaveFailed(message));
        }
    }
    
    private void handleGetUsersResponse(JSONObject messageJson) {
        String status = messageJson.optString("status", "");
        String searchQuery = messageJson.optString("search_query", "");
        
        if ("success".equals(status)) {
            try {
                JSONArray usersArray = messageJson.getJSONArray("users");
                List<User> users = new ArrayList<>();
                
                for (int i = 0; i < usersArray.length(); i++) {
                    JSONObject userJson = usersArray.getJSONObject(i);
                    User user = parseUserFromJson(userJson);
                    users.add(user);
                }
                
                mainHandler.post(() -> listener.onUsersReceived(users, searchQuery));
                
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing users response", e);
                mainHandler.post(() -> listener.onUsersRequestFailed("Response parsing error"));
            }
        } else {
            String errorMessage = messageJson.optString("message", "Failed to get users");
            mainHandler.post(() -> listener.onUsersRequestFailed(errorMessage));
        }
    }
    
    private void handleGetUserProfileResponse(JSONObject messageJson) {
        String status = messageJson.optString("status", "");
        
        if ("success".equals(status)) {
            try {
                JSONObject userJson = messageJson.getJSONObject("user");
                User user = parseUserFromJson(userJson);
                mainHandler.post(() -> listener.onUserProfileReceived(user));
                
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing user profile response", e);
                mainHandler.post(() -> listener.onUserProfileRequestFailed("Response parsing error"));
            }
        } else {
            String errorMessage = messageJson.optString("message", "Failed to get user profile");
            mainHandler.post(() -> listener.onUserProfileRequestFailed(errorMessage));
        }
    }
    
    private void handleJoinRoomResponse(JSONObject messageJson) {
        String status = messageJson.optString("status", "");
        String roomId = messageJson.optString("room_id", "");
        String message = messageJson.optString("message", "");
        
        if ("success".equals(status)) {
            mainHandler.post(() -> listener.onRoomJoined(roomId, message));
        } else {
            mainHandler.post(() -> listener.onRoomJoinFailed(message));
        }
    }
    
    private void handleSendMessageResponse(JSONObject messageJson) {
        String status = messageJson.optString("status", "");
        String messageId = messageJson.optString("message_id", "");
        String message = messageJson.optString("message", "");
        
        if ("success".equals(status)) {
            mainHandler.post(() -> listener.onMessageSent(messageId));
        } else {
            mainHandler.post(() -> listener.onMessageSendFailed(message));
        }
    }
    
    private void handleNewMessage(JSONObject messageJson) {
        try {
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setMessageId(messageJson.optString("message_id", ""));
            chatMessage.setRoomId(messageJson.optString("room_id", ""));
            chatMessage.setSenderUid(messageJson.optString("sender_uid", ""));
            chatMessage.setSenderEmail(messageJson.optString("sender_email", ""));
            chatMessage.setSenderName(messageJson.optString("sender_name", ""));
            chatMessage.setMessage(messageJson.optString("message", ""));
            chatMessage.setMessageType(messageJson.optString("message_type", "text"));
            chatMessage.setTimestamp(messageJson.optString("timestamp", ""));
            
            mainHandler.post(() -> listener.onMessageReceived(chatMessage));
            
        } catch (Exception e) {
            Log.e(TAG, "Error parsing new message", e);
        }
    }
    
    private void handleUserJoined(JSONObject messageJson) {
        String roomId = messageJson.optString("room_id", "");
        String userName = messageJson.optString("user_name", "");
        
        mainHandler.post(() -> listener.onUserJoinedRoom(roomId, userName));
    }
    
    private User parseUserFromJson(JSONObject userJson) throws JSONException {
        User user = new User();
        user.setUid(userJson.optString("uid", ""));
        user.setDisplayName(userJson.optString("name", ""));
        user.setEmail(userJson.optString("email", ""));
        user.setPhoneNumber(userJson.optString("phone_number", ""));
        user.setProfileImageUrl(userJson.optString("profile_image_url", ""));
        user.setBio(userJson.optString("bio", ""));
        user.setStatus(userJson.optString("status", "offline"));
        
        return user;
    }
    
    // ====================================================================================
    // HELPER METHODS
    // ====================================================================================
    
    private void sendMessage(JSONObject message) {
        // Enqueue for scheduled sending; fall back to immediate if scheduler unavailable
        if (transportScheduler != null) {
            JSONObject toSend = message;
            if (enablePadding) {
                // Optional: pad body minimally to defeat trivial compression cues
                toSend = PayloadPadder.padForJson(toSend, DEFAULT_PAD_MIN_CHARS);
                // Always add pad field to normalize total frame size
                toSend = PayloadPadder.padWithPadField(toSend, TARGET_FRAME_SIZE);
            }
            transportScheduler.enqueue(toSend);
        } else {
            sendNow(message);
        }
    }
    
    private void sendNow(JSONObject message) {
        if (webSocket != null && isConnected && message != null) {
            try {
                String messageText = message.toString();
                int len = messageText != null ? messageText.length() : 0;
                webSocket.send(messageText);
                Log.d(TAG, "📤 Sent message (" + len + " chars): " + messageText);
            } catch (Exception e) {
                Log.e(TAG, "Error sending message", e);
            }
        }
    }
    
    private void attemptReconnect() {
        if (isReconnecting || reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            return;
        }
        
        isReconnecting = true;
        reconnectAttempts++;
        
        Log.d(TAG, "🔄 Attempting reconnection " + reconnectAttempts + "/" + MAX_RECONNECT_ATTEMPTS);
        
        if (listener != null) {
            mainHandler.post(() -> listener.onReconnecting(reconnectAttempts));
        }
        
        // Exponential backoff
        long delay = Math.min(1000 * (long) Math.pow(2, reconnectAttempts - 1), 30000);
        
        mainHandler.postDelayed(() -> {
            isReconnecting = false;
            connect();
        }, delay);
    }
    
    public void cleanup() {
        disconnect();
        stopScheduler();
        if (client != null) {
            client.dispatcher().executorService().shutdown();
            client.connectionPool().evictAll();
        }
    }
    
    private void stopScheduler() {
        try {
            if (transportScheduler != null) {
                transportScheduler.stop();
            }
        } catch (Exception ignored) { }
    }
}
