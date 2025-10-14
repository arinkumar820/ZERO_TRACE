package com.sameetasadullah.i180479_180531;

/**
 * User model class for Firebase database storage and retrieval
 */
public class User {
    private String uid;
    private String email;
    private String displayName;
    private String profileImageUrl;
    private String phoneNumber;
    private String status; // online, offline, etc.
    private long lastSeen;
    private String bio;

    // Default constructor required for Firebase
    public User() {}

    public User(String uid, String email, String displayName) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.status = "offline";
        this.lastSeen = System.currentTimeMillis();
        this.bio = "Hey there! I'm using Bisto Chat.";
    }

    public User(String uid, String email, String displayName, String profileImageUrl, String phoneNumber) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.profileImageUrl = profileImageUrl;
        this.phoneNumber = phoneNumber;
        this.status = "offline";
        this.lastSeen = System.currentTimeMillis();
        this.bio = "Hey there! I'm using Bisto Chat.";
    }

    // Getters
    public String getUid() {
        return uid;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getStatus() {
        return status;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public String getBio() {
        return bio;
    }

    // Setters
    public void setUid(String uid) {
        this.uid = uid;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    // Utility method to get display text (name or email)
    public String getDisplayText() {
        if (displayName != null && !displayName.trim().isEmpty()) {
            return displayName;
        } else if (email != null && email.contains("@")) {
            return email.substring(0, email.indexOf("@"));
        } else {
            return "Unknown User";
        }
    }

    // Method to check if user is currently online
    public boolean isOnline() {
        return "online".equals(status);
    }

    @Override
    public String toString() {
        return "User{" +
                "uid='" + uid + '\'' +
                ", email='" + email + '\'' +
                ", displayName='" + displayName + '\'' +
                ", profileImageUrl='" + profileImageUrl + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", status='" + status + '\'' +
                ", lastSeen=" + lastSeen +
                ", bio='" + bio + '\'' +
                '}';
    }
}