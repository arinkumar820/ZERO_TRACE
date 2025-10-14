package com.sameetasadullah.i180479_180531;

/**
 * ChatRoom model class for Firebase database storage
 */
public class ChatRoom {
    private String roomId;
    private String roomName;
    private String roomType; // "private" or "group"
    private String roomDescription;
    private String roomImageUrl;
    private String createdBy;
    private long createdAt;
    private long lastActivity;
    private String lastMessage;
    private String lastMessageSender;
    private long lastMessageTime;
    private int memberCount;
    private boolean isActive;

    // Default constructor required for Firebase
    public ChatRoom() {
        this.isActive = true;
        this.memberCount = 0;
    }

    public ChatRoom(String roomId, String roomName, String roomType, String createdBy) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.roomType = roomType;
        this.createdBy = createdBy;
        this.createdAt = System.currentTimeMillis();
        this.lastActivity = System.currentTimeMillis();
        this.isActive = true;
        this.memberCount = 0;
    }

    // Getters
    public String getRoomId() {
        return roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public String getRoomType() {
        return roomType;
    }

    public String getRoomDescription() {
        return roomDescription;
    }

    public String getRoomImageUrl() {
        return roomImageUrl;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getLastActivity() {
        return lastActivity;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public String getLastMessageSender() {
        return lastMessageSender;
    }

    public long getLastMessageTime() {
        return lastMessageTime;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public boolean isActive() {
        return isActive;
    }

    // Setters
    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public void setRoomDescription(String roomDescription) {
        this.roomDescription = roomDescription;
    }

    public void setRoomImageUrl(String roomImageUrl) {
        this.roomImageUrl = roomImageUrl;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public void setLastActivity(long lastActivity) {
        this.lastActivity = lastActivity;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public void setLastMessageSender(String lastMessageSender) {
        this.lastMessageSender = lastMessageSender;
    }

    public void setLastMessageTime(long lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    // Utility methods
    public boolean isPrivateChat() {
        return "private".equals(roomType);
    }

    public boolean isGroupChat() {
        return "group".equals(roomType);
    }

    public String getDisplayName() {
        return roomName != null && !roomName.trim().isEmpty() ? 
               roomName : (isPrivateChat() ? "Private Chat" : "Group Chat");
    }

    @Override
    public String toString() {
        return "ChatRoom{" +
                "roomId='" + roomId + '\'' +
                ", roomName='" + roomName + '\'' +
                ", roomType='" + roomType + '\'' +
                ", createdBy='" + createdBy + '\'' +
                ", memberCount=" + memberCount +
                ", isActive=" + isActive +
                '}';
    }
}