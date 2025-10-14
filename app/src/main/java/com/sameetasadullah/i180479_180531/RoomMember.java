package com.sameetasadullah.i180479_180531;

/**
 * RoomMember model class for Firebase database storage
 * Represents a user's membership in a chat room
 */
public class RoomMember {
    private String userId;
    private String roomId;
    private String role; // "member", "admin", "owner"
    private long joinedAt;
    private long lastSeen;
    private boolean isMuted;
    private boolean isBlocked;

    // Default constructor required for Firebase
    public RoomMember() {
        this.role = "member";
        this.isMuted = false;
        this.isBlocked = false;
    }

    public RoomMember(String userId, String roomId, String role) {
        this.userId = userId;
        this.roomId = roomId;
        this.role = role != null ? role : "member";
        this.joinedAt = System.currentTimeMillis();
        this.lastSeen = System.currentTimeMillis();
        this.isMuted = false;
        this.isBlocked = false;
    }

    // Getters
    public String getUserId() {
        return userId;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getRole() {
        return role;
    }

    public long getJoinedAt() {
        return joinedAt;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public boolean isMuted() {
        return isMuted;
    }

    public boolean isBlocked() {
        return isBlocked;
    }

    // Setters
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setJoinedAt(long joinedAt) {
        this.joinedAt = joinedAt;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    public void setMuted(boolean muted) {
        isMuted = muted;
    }

    public void setBlocked(boolean blocked) {
        isBlocked = blocked;
    }

    // Utility methods
    public boolean isAdmin() {
        return "admin".equals(role) || "owner".equals(role);
    }

    public boolean isOwner() {
        return "owner".equals(role);
    }

    public boolean canSendMessages() {
        return !isMuted && !isBlocked;
    }

    @Override
    public String toString() {
        return "RoomMember{" +
                "userId='" + userId + '\'' +
                ", roomId='" + roomId + '\'' +
                ", role='" + role + '\'' +
                ", joinedAt=" + joinedAt +
                ", isMuted=" + isMuted +
                ", isBlocked=" + isBlocked +
                '}';
    }
}