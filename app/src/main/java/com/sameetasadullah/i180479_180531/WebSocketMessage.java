package com.sameetasadullah.i180479_180531;

import android.util.Log;

/**
 * Data model for WebSocket messages - PERSISTENT MESSAGES ONLY
 * NO DISAPPEARING MESSAGE FUNCTIONALITY
 */
public class WebSocketMessage {
    // Static counter for generating unique message IDs
    private static int nextMessageId = 1;
    
    private String type; // message, join, ping, error, connected, etc.
    private String sender_uid;
    private String sender_email;
    private String message;
    private String message_type = "text";
    private String display_name;
    private String chat_room_id = "general_chat"; // Default chat room
    private long timestamp;
    private int message_id;
    private long createdAt; // Track when the message was created
    private boolean seen = false; // Read receipt flag for outgoing messages
    private long seenAt = 0; // Timestamp when message was seen
    private String seenBy = null; // UID of user who marked as seen
    
    // Disappearing message fields for UI functionality (does NOT affect database storage)
    private boolean isDisappearing = true; // Enable disappearing in UI
    private long disappearAfterMs = 60000; // 1 minute default (UI only)
    private boolean isExpired = false; // UI expiration state
    
    // Server-compatible fields for join_room messages
    private String user_uid;    // Server expects this for join_room
    private String user_email;  // Server expects this for join_room
    private String room_id;     // Server expects this for join_room

    /**
     * Generate a unique message ID
     */
    private static synchronized int generateUniqueMessageId() {
        return nextMessageId++;
    }
    
    /**
     * Set the next message ID counter (useful for consistency with server)
     */
    public static synchronized void setNextMessageId(int nextId) {
        if (nextId > nextMessageId) {
            nextMessageId = nextId;
        }
    }
    
    // Default constructor for Gson
    public WebSocketMessage() {
        long currentTime = System.currentTimeMillis();
        this.timestamp = currentTime;
        this.createdAt = currentTime;
        this.type = "chat_message";  // Server expects 'chat_message', not 'message'
        // Generate unique ID only if not set by server
        if (this.message_id <= 0) {
            this.message_id = generateUniqueMessageId();
        }
    }

    public WebSocketMessage(String senderUid, String senderEmail, String message) {
        this.sender_uid = senderUid;
        this.sender_email = senderEmail;
        this.message = message;
        long currentTime = System.currentTimeMillis();
        this.timestamp = currentTime;
        this.createdAt = currentTime;
        this.type = "chat_message";  // Server expects 'chat_message', not 'message'
        this.message_id = generateUniqueMessageId();
    }
    
    // Constructor for join message
    public static WebSocketMessage createJoinMessage(String senderUid, String senderEmail, String displayName) {
        return createJoinMessage(senderUid, senderEmail, displayName, "general_chat");
    }
    
    // Constructor for join message with specific room ID
    public static WebSocketMessage createJoinMessage(String senderUid, String senderEmail, String displayName, String roomId) {
        WebSocketMessage msg = new WebSocketMessage();
        msg.type = "join_room";  // Server expects 'join_room', not 'join'
        msg.sender_uid = senderUid;
        msg.sender_email = senderEmail;
        msg.display_name = displayName;
        msg.chat_room_id = roomId;  // Set the actual room ID
        // Set server-compatible fields that the server actually looks for
        msg.user_uid = senderUid;
        msg.user_email = senderEmail;
        msg.room_id = roomId;  // Server expects this field for join_room
        return msg;
    }
    
    // Constructor for read receipt message
    public static WebSocketMessage createReadReceipt(String senderUid, String senderEmail, int originalMessageId, String roomId) {
        WebSocketMessage msg = new WebSocketMessage();
        msg.type = "read_receipt";
        msg.sender_uid = senderUid;
        msg.sender_email = senderEmail;
        msg.message_id = originalMessageId; // Reference to the original message
        msg.chat_room_id = roomId;
        msg.room_id = roomId;
        msg.message = null; // No message content for read receipts
        return msg;
    }

    // Getters
    public String getType() {
        return type;
    }
    
    public String getSender_uid() {
        return sender_uid;
    }

    public String getSender_email() {
        return sender_email;
    }

    public String getMessage() {
        return message;
    }
    
    public String getMessage_type() {
        return message_type;
    }
    
    public String getDisplay_name() {
        return display_name;
    }
    
    public int getMessage_id() {
        return message_id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    // Setters
    public void setType(String type) {
        this.type = type;
    }
    
    public void setSender_uid(String sender_uid) {
        this.sender_uid = sender_uid;
    }

    public void setSender_email(String sender_email) {
        this.sender_email = sender_email;
    }

    public void setMessage(String message) {
        this.message = message;
    }
    
    public void setMessage_type(String message_type) {
        this.message_type = message_type;
    }
    
    public void setDisplay_name(String display_name) {
        this.display_name = display_name;
    }
    
    public void setMessage_id(int message_id) {
        this.message_id = message_id;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getChat_room_id() {
        return chat_room_id;
    }
    
    public void setChat_room_id(String chat_room_id) {
        this.chat_room_id = chat_room_id;
        this.room_id = chat_room_id; // Keep both fields in sync
    }
    
    // Getter for server compatibility (server sends 'room_id')
    public String getRoom_id() {
        return room_id != null ? room_id : chat_room_id;
    }
    
    public void setRoom_id(String room_id) {
        this.room_id = room_id;
        this.chat_room_id = room_id; // Keep both fields in sync
    }
    
    /**
     * Get when the message was created
     */
    public long getCreatedAt() {
        // If createdAt is not set, use timestamp as fallback
        if (createdAt <= 0) {
            createdAt = timestamp > 0 ? timestamp : System.currentTimeMillis();
            Log.d("WebSocketMessage", "getCreatedAt: Using fallback timestamp because createdAt was invalid");
        }
        return createdAt;
    }
    
    /**
     * Set when the message was created
     */
    public void setCreatedAt(long createdAt) {
        // Validate createdAt before setting
        if (createdAt <= 0) {
            Log.w("WebSocketMessage", "setCreatedAt: Attempted to set invalid createdAt value: " + createdAt);
            // Don't set invalid values
            return;
        }
        this.createdAt = createdAt;
        Log.d("WebSocketMessage", "setCreatedAt: Set to " + createdAt);
    }
    
    
    public boolean isSeen() { return seen; }
    
    public void setSeen(boolean seen) { 
        this.seen = seen;
        if (seen && seenAt == 0) {
            this.seenAt = System.currentTimeMillis();
        }
    }
    
    public long getSeenAt() { return seenAt; }
    
    public void setSeenAt(long seenAt) { this.seenAt = seenAt; }
    
    public String getSeenBy() { return seenBy; }
    
    public void setSeenBy(String seenBy) { this.seenBy = seenBy; }
    
    /**
     * Mark message as seen by a specific user
     */
    public void markAsSeen(String userUid) {
        this.seen = true;
        this.seenBy = userUid;
        this.seenAt = System.currentTimeMillis();
    }
    
    /**
     * Create a regular persistent message (stored in DB, disappears from UI)
     */
    public static WebSocketMessage createMessage(String senderUid, String senderEmail, String message) {
        WebSocketMessage msg = new WebSocketMessage(senderUid, senderEmail, message);
        // Ensure createdAt is set to current time when message is created
        msg.setCreatedAt(System.currentTimeMillis());
        return msg;
    }
    
    // Disappearing message methods for UI functionality
    public boolean isDisappearing() { return isDisappearing; }
    public void setDisappearing(boolean disappearing) { this.isDisappearing = disappearing; }
    
    public long getDisappearAfterMs() { return disappearAfterMs; }
    public void setDisappearAfterMs(long disappearAfterMs) { this.disappearAfterMs = disappearAfterMs; }
    
    public boolean isExpired() { return isExpired; }
    public void setExpired(boolean expired) { this.isExpired = expired; }
    
    /**
     * Check if message should disappear from UI based on creation time
     */
    public boolean shouldDisappear() {
        if (!isDisappearing || isExpired) {
            return isExpired; // Return current expiration state
        }
        
        long age = System.currentTimeMillis() - getCreatedAt();
        boolean shouldExpire = age >= disappearAfterMs;
        
        if (shouldExpire && !isExpired) {
            isExpired = true; // Mark as expired for future calls
        }
        
        return shouldExpire;
    }
    
    /**
     * Get remaining time before UI disappearance
     */
    public long getRemainingTimeMs() {
        if (!isDisappearing || isExpired) {
            return 0;
        }
        
        long age = System.currentTimeMillis() - getCreatedAt();
        long remaining = disappearAfterMs - age;
        return Math.max(0, remaining);
    }

    @Override
    public String toString() {
        return "WebSocketMessage{" +
                "sender_uid='" + sender_uid + '\'' +
                ", sender_email='" + sender_email + '\'' +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}