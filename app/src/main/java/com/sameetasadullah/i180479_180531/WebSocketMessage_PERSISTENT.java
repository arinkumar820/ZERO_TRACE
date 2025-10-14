package com.sameetasadullah.i180479_180531;

/**
 * PERSISTENT WebSocket Message - NO DISAPPEARING FUNCTIONALITY
 * This class is guaranteed to have NO disappearing message features
 */
public class WebSocketMessage_PERSISTENT {
    // Static counter for generating unique message IDs
    private static int nextMessageId = 1;
    
    private String type; // message, join, ping, error, connected, etc.
    private String sender_uid;
    private String sender_email;
    private String message;
    private String message_type = "text";
    private String display_name;
    private String chat_room_id = "general_chat";
    private long timestamp;
    private int message_id;
    private long createdAt;
    private boolean seen = false;
    private long seenAt = 0;
    private String seenBy = null;
    
    // Server-compatible fields
    private String user_uid;
    private String user_email;
    private String room_id;

    /**
     * Generate a unique message ID
     */
    private static synchronized int generateUniqueMessageId() {
        return nextMessageId++;
    }
    
    public static synchronized void setNextMessageId(int nextId) {
        if (nextId > nextMessageId) {
            nextMessageId = nextId;
        }
    }
    
    // Default constructor
    public WebSocketMessage_PERSISTENT() {
        long currentTime = System.currentTimeMillis();
        this.timestamp = currentTime;
        this.createdAt = currentTime;
        this.type = "chat_message";
        if (this.message_id <= 0) {
            this.message_id = generateUniqueMessageId();
        }
    }

    public WebSocketMessage_PERSISTENT(String senderUid, String senderEmail, String message) {
        this.sender_uid = senderUid;
        this.sender_email = senderEmail;
        this.message = message;
        long currentTime = System.currentTimeMillis();
        this.timestamp = currentTime;
        this.createdAt = currentTime;
        this.type = "chat_message";
        this.message_id = generateUniqueMessageId();
    }
    
    // Static factory methods
    public static WebSocketMessage_PERSISTENT createJoinMessage(String senderUid, String senderEmail, String displayName) {
        return createJoinMessage(senderUid, senderEmail, displayName, "general_chat");
    }
    
    public static WebSocketMessage_PERSISTENT createJoinMessage(String senderUid, String senderEmail, String displayName, String roomId) {
        WebSocketMessage_PERSISTENT msg = new WebSocketMessage_PERSISTENT();
        msg.type = "join_room";
        msg.sender_uid = senderUid;
        msg.sender_email = senderEmail;
        msg.display_name = displayName;
        msg.chat_room_id = roomId;
        msg.user_uid = senderUid;
        msg.user_email = senderEmail;
        msg.room_id = roomId;
        return msg;
    }
    
    public static WebSocketMessage_PERSISTENT createMessage(String senderUid, String senderEmail, String message) {
        WebSocketMessage_PERSISTENT msg = new WebSocketMessage_PERSISTENT(senderUid, senderEmail, message);
        msg.setCreatedAt(System.currentTimeMillis());
        return msg;
    }

    // Getters
    public String getType() { return type; }
    public String getSender_uid() { return sender_uid; }
    public String getSender_email() { return sender_email; }
    public String getMessage() { return message; }
    public String getMessage_type() { return message_type; }
    public String getDisplay_name() { return display_name; }
    public int getMessage_id() { return message_id; }
    public long getTimestamp() { return timestamp; }
    public String getChat_room_id() { return chat_room_id; }
    public String getRoom_id() { return room_id != null ? room_id : chat_room_id; }
    
    public long getCreatedAt() {
        if (createdAt <= 0) {
            createdAt = timestamp > 0 ? timestamp : System.currentTimeMillis();
        }
        return createdAt;
    }
    
    public boolean isSeen() { return seen; }
    public long getSeenAt() { return seenAt; }
    public String getSeenBy() { return seenBy; }

    // Setters
    public void setType(String type) { this.type = type; }
    public void setSender_uid(String sender_uid) { this.sender_uid = sender_uid; }
    public void setSender_email(String sender_email) { this.sender_email = sender_email; }
    public void setMessage(String message) { this.message = message; }
    public void setMessage_type(String message_type) { this.message_type = message_type; }
    public void setDisplay_name(String display_name) { this.display_name = display_name; }
    public void setMessage_id(int message_id) { this.message_id = message_id; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setChat_room_id(String chat_room_id) { 
        this.chat_room_id = chat_room_id; 
        this.room_id = chat_room_id;
    }
    public void setRoom_id(String room_id) { 
        this.room_id = room_id; 
        this.chat_room_id = room_id;
    }
    
    public void setCreatedAt(long createdAt) {
        if (createdAt > 0) {
            this.createdAt = createdAt;
        }
    }
    
    public void setSeen(boolean seen) { 
        this.seen = seen;
        if (seen && seenAt == 0) {
            this.seenAt = System.currentTimeMillis();
        }
    }
    
    public void setSeenAt(long seenAt) { this.seenAt = seenAt; }
    public void setSeenBy(String seenBy) { this.seenBy = seenBy; }
    
    public void markAsSeen(String userUid) {
        this.seen = true;
        this.seenBy = userUid;
        this.seenAt = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "WebSocketMessage_PERSISTENT{" +
                "sender_uid='" + sender_uid + '\'' +
                ", sender_email='" + sender_email + '\'' +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}