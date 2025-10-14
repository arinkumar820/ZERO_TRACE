package com.sameetasadullah.i180479_180531;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import java.util.HashMap;
import java.util.Map;

/**
 * User profile model for Firebase Realtime Database
 * Handles user approval system with pending/approved/rejected status
 * 
 * Supports both new field names (displayName) and existing field names (display_name)
 */
@IgnoreExtraProperties
public class UserProfile {
    
    // User approval status constants
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_APPROVED = "approved";
    public static final String STATUS_REJECTED = "rejected";
    
    // Activity status constants
    public static final String ACTIVITY_ONLINE = "online";
    public static final String ACTIVITY_OFFLINE = "offline";
    public static final String ACTIVITY_AWAY = "away";
    
    private String uid;
    private String email;
    
    // Support both field names for backwards compatibility
    private String displayName;  // New field name
    private String display_name; // Existing field name in database
    
    private String status;        // Approval status (pending/approved/rejected)
    private String activityStatus; // Activity status (online/offline/away) 
    private long registrationTimestamp;
    private long lastLoginTimestamp;
    private String profileImageUrl;
    private String phoneNumber;
    
    // Additional fields from existing database
    private String email_lower;
    private String name_lower;
    private long lastSeen;
    private long last_seen;
    
    // Required empty constructor for Firebase
    public UserProfile() {}
    
    /**
     * Constructor for new user registration
     */
    public UserProfile(String uid, String email, String displayName) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.status = STATUS_PENDING; // New users start as pending
        this.registrationTimestamp = System.currentTimeMillis();
        this.lastLoginTimestamp = 0;
        this.profileImageUrl = "";
        this.phoneNumber = "";
    }
    
    /**
     * Convert to Map for Firebase storage
     */
    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("uid", uid);
        result.put("email", email);
        result.put("displayName", displayName);
        result.put("status", status);
        result.put("registrationTimestamp", registrationTimestamp);
        result.put("lastLoginTimestamp", lastLoginTimestamp);
        result.put("profileImageUrl", profileImageUrl);
        result.put("phoneNumber", phoneNumber);
        return result;
    }
    
    /**
     * Check if user is approved for login
     */
    public boolean isApproved() {
        return STATUS_APPROVED.equals(status);
    }
    
    /**
     * Check if user is pending approval
     */
    public boolean isPending() {
        return STATUS_PENDING.equals(status);
    }
    
    /**
     * Check if user is rejected
     */
    public boolean isRejected() {
        return STATUS_REJECTED.equals(status);
    }
    
    /**
     * Update last login timestamp
     */
    public void updateLastLogin() {
        this.lastLoginTimestamp = System.currentTimeMillis();
    }
    
    // Getters and Setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    /**
     * Get display name with backward compatibility
     * Checks both displayName and display_name fields
     */
    public String getDisplayName() { 
        // Return displayName if available, otherwise display_name
        if (displayName != null && !displayName.trim().isEmpty()) {
            return displayName;
        } else if (display_name != null && !display_name.trim().isEmpty()) {
            return display_name;
        }
        return "";
    }
    
    public void setDisplayName(String displayName) { 
        this.displayName = displayName;
        this.display_name = displayName; // Keep both fields in sync
    }
    
    // Getter/Setter for display_name field (existing database)
    public String getDisplay_name() { return display_name; }
    public void setDisplay_name(String display_name) { 
        this.display_name = display_name;
        if (displayName == null || displayName.trim().isEmpty()) {
            this.displayName = display_name; // Sync to new field if empty
        }
    }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public long getRegistrationTimestamp() { return registrationTimestamp; }
    public void setRegistrationTimestamp(long registrationTimestamp) { this.registrationTimestamp = registrationTimestamp; }
    
    public long getLastLoginTimestamp() { return lastLoginTimestamp; }
    public void setLastLoginTimestamp(long lastLoginTimestamp) { this.lastLoginTimestamp = lastLoginTimestamp; }
    
    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
    
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    
    // Additional field getters/setters for existing database compatibility
    public String getEmail_lower() { return email_lower; }
    public void setEmail_lower(String email_lower) { this.email_lower = email_lower; }
    
    public String getName_lower() { return name_lower; }
    public void setName_lower(String name_lower) { this.name_lower = name_lower; }
    
    public long getLastSeen() { return lastSeen; }
    public void setLastSeen(long lastSeen) { this.lastSeen = lastSeen; }
    
    public long getLast_seen() { return last_seen; }
    public void setLast_seen(long last_seen) { this.last_seen = last_seen; }
    
    public String getActivityStatus() { return activityStatus; }
    public void setActivityStatus(String activityStatus) { this.activityStatus = activityStatus; }
    
    /**
     * Get formatted display name for UI
     */
    public String getFormattedDisplayName() {
        if (displayName != null && !displayName.trim().isEmpty()) {
            return displayName;
        } else if (email != null && email.contains("@")) {
            return email.substring(0, email.indexOf("@"));
        } else {
            return "User";
        }
    }
    
    /**
     * Get status display text for UI
     */
    public String getStatusDisplayText() {
        switch (status) {
            case STATUS_PENDING:
                return "Pending Approval";
            case STATUS_APPROVED:
                return "Approved";
            case STATUS_REJECTED:
                return "Rejected";
            default:
                return "Unknown Status";
        }
    }
    
    @Override
    public String toString() {
        return "UserProfile{" +
                "uid='" + uid + '\'' +
                ", email='" + email + '\'' +
                ", displayName='" + displayName + '\'' +
                ", status='" + status + '\'' +
                ", registrationTimestamp=" + registrationTimestamp +
                '}';
    }
}