package com.sameetasadullah.i180479_180531;

import java.io.Serializable;

/**
 * ChatMessage data model class
 * 
 * Represents a chat message with all necessary information
 * for display and identification
 */
public class ChatMessage implements Serializable {
    
    private String messageId;
    private String roomId;
    private String senderUid;
    private String senderName;
    private String senderEmail;
    private String message;
    private String messageType;
    private String timestamp;
    private boolean isDelivered;
    private boolean isRead;
    
    // Default constructor
    public ChatMessage() {
        this.isDelivered = false;
        this.isRead = false;
        this.messageType = "text";
    }
    
    // Constructor with essential fields
    public ChatMessage(String messageId, String roomId, String senderUid, 
                      String senderName, String message, String timestamp) {
        this();
        this.messageId = messageId;
        this.roomId = roomId;
        this.senderUid = senderUid;
        this.senderName = senderName;
        this.message = message;
        this.timestamp = timestamp;
    }
    
    // Getters and Setters
    public String getMessageId() {
        return messageId;
    }
    
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
    
    public String getRoomId() {
        return roomId;
    }
    
    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }
    
    public String getSenderUid() {
        return senderUid;
    }
    
    public void setSenderUid(String senderUid) {
        this.senderUid = senderUid;
    }
    
    public String getSenderName() {
        return senderName;
    }
    
    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }
    
    public String getSenderEmail() {
        return senderEmail;
    }
    
    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getMessageType() {
        return messageType;
    }
    
    public void setMessageType(String messageType) {
        this.messageType = messageType != null ? messageType : "text";
    }
    
    public String getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    
    public boolean isDelivered() {
        return isDelivered;
    }
    
    public void setDelivered(boolean delivered) {
        isDelivered = delivered;
    }
    
    public boolean isRead() {
        return isRead;
    }
    
    public void setRead(boolean read) {
        isRead = read;
    }
    
    // Utility methods
    public boolean isSentByMe(String currentUserUid) {
        return senderUid != null && senderUid.equals(currentUserUid);
    }
    
    public boolean isTextMessage() {
        return "text".equals(messageType);
    }
    
    public boolean isImageMessage() {
        return "image".equals(messageType);
    }
    
    public boolean isFileMessage() {
        return "file".equals(messageType);
    }
    
    public boolean isSystemMessage() {
        return "system".equals(messageType);
    }
    
    public boolean isTemporaryMessage() {
        return messageId != null && messageId.startsWith("temp_");
    }
    
    @Override
    public String toString() {
        return "ChatMessage{" +
                "messageId='" + messageId + '\'' +
                ", roomId='" + roomId + '\'' +
                ", senderName='" + senderName + '\'' +
                ", message='" + message + '\'' +
                ", messageType='" + messageType + '\'' +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        ChatMessage that = (ChatMessage) o;
        
        return messageId != null ? messageId.equals(that.messageId) : that.messageId == null;
    }
    
    @Override
    public int hashCode() {
        return messageId != null ? messageId.hashCode() : 0;
    }
}