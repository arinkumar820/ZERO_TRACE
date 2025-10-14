package com.sameetasadullah.i180479_180531;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Manages Firebase user registration, profile creation, and chat room management
 */
public class FirebaseUserManager {
    
    private static final String TAG = "FirebaseUserManager";
    
    // Firebase Database References
    private static final String USERS_NODE = "Users";
    private static final String CONTACTS_NODE = "Contacts";
    private static final String CHAT_ROOMS_NODE = "ChatRooms";
    private static final String ROOM_MEMBERS_NODE = "RoomMembers";
    private static final String ROOM_MESSAGES_NODE = "RoomMessages";
    
    private DatabaseReference databaseRef;
    private FirebaseAuth firebaseAuth;
    
    // Interfaces for callbacks
    public interface UserCreationCallback {
        void onSuccess(String userId);
        void onFailure(String error);
    }
    
    public interface ChatRoomCallback {
        void onChatRoomCreated(String roomId, ChatRoom chatRoom);
        void onFailure(String error);
    }
    
    public interface UserSearchCallback {
        void onUsersFound(java.util.List<User> users);
        void onFailure(String error);
    }
    
    public FirebaseUserManager() {
        // Fix locale for Firebase
        ensureLocaleConfiguration();
        
        databaseRef = FirebaseDatabase.getInstance().getReference();
        firebaseAuth = FirebaseAuth.getInstance();
    }
    
    /**
     * Create or update user profile in Firebase when user registers/logs in
     */
    public void createOrUpdateUserProfile(FirebaseUser firebaseUser, UserCreationCallback callback) {
        if (firebaseUser == null) {
            callback.onFailure("User not authenticated");
            return;
        }
        
        String uid = firebaseUser.getUid();
        String email = firebaseUser.getEmail();
        String displayName = firebaseUser.getDisplayName();
        
        // If display name is null, extract from email
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = email != null && email.contains("@") ? 
                        email.substring(0, email.indexOf("@")) : "Unknown User";
        }
        
        // Create User object
        User user = new User(uid, email, displayName);
        user.setStatus("online");
        user.setLastSeen(System.currentTimeMillis());
        
        // Store user in Firebase
        databaseRef.child(USERS_NODE).child(uid).setValue(user)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "User profile created/updated successfully: " + email);
                        callback.onSuccess(uid);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Failed to create/update user profile", e);
                        callback.onFailure("Failed to create user profile: " + e.getMessage());
                    }
                });
    }
    
    /**
     * Search for users by email or display name
     */
    public void searchUsers(String query, UserSearchCallback callback) {
        Log.d(TAG, "=== FirebaseUserManager.searchUsers CALLED ===");
        Log.d(TAG, "Query: '" + query + "'");
        
        if (query == null || query.trim().length() < 2) {
            Log.w(TAG, "Query too short: '" + query + "'");
            callback.onFailure("Query too short");
            return;
        }
        
        String lowerQuery = query.toLowerCase().trim();
        Log.d(TAG, "Processed query: '" + lowerQuery + "'");
        
        // Check Firebase Auth state
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        String currentUserId = currentUser != null ? currentUser.getUid() : null;
        Log.d(TAG, "Current user ID: " + (currentUserId != null ? currentUserId : "null"));
        
        // Check database reference
        Log.d(TAG, "Database reference: " + (databaseRef != null ? "valid" : "null"));
        Log.d(TAG, "Users node path: " + USERS_NODE);
        
        // Add the database listener
        Log.d(TAG, "Adding database listener to Users node...");
        databaseRef.child(USERS_NODE).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "=== DATABASE RESPONSE RECEIVED ===");
                Log.d(TAG, "DataSnapshot exists: " + dataSnapshot.exists());
                Log.d(TAG, "Total children in Users node: " + dataSnapshot.getChildrenCount());
                
                java.util.List<User> foundUsers = new java.util.ArrayList<>();
                java.util.List<User> allUsers = new java.util.ArrayList<>();
                
                int processedCount = 0;
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    processedCount++;
                    String userKey = userSnapshot.getKey();
                    Log.d(TAG, "Processing user " + processedCount + " with key: " + userKey);
                    
                    try {
                        User user = userSnapshot.getValue(User.class);
                        if (user != null) {
                            // Validate user has required fields
                            String userUid = user.getUid();
                            String userEmail = user.getEmail();
                            String userDisplayName = user.getDisplayName();
                            
                            // If UID is null, use the Firebase key as UID
                            if (userUid == null || userUid.trim().isEmpty()) {
                                userUid = userKey;
                                user.setUid(userUid); // Update the user object
                                Log.w(TAG, "User had null UID, using Firebase key: " + userKey);
                            }
                            
                            allUsers.add(user);
                            Log.d(TAG, "User data: UID=" + userUid + ", Email=" + userEmail + ", DisplayName=" + userDisplayName);
                            
                            // Skip current user (safe null check)
                            if (userUid != null && userUid.equals(currentUserId)) {
                                Log.d(TAG, "Skipping current user");
                                continue;
                            }
                            
                            // Safe null checks for search matching
                            String email = userEmail != null && !userEmail.equals("null") ? userEmail.toLowerCase() : "";
                            String displayName = userDisplayName != null && !userDisplayName.equals("null") && !userDisplayName.equals("unknown user") ? userDisplayName.toLowerCase() : "";
                            
                            Log.d(TAG, "Checking match - Email: '" + email + "', DisplayName: '" + displayName + "', Query: '" + lowerQuery + "'");
                            
                            // Skip users with no searchable data
                            if (email.isEmpty() && displayName.isEmpty()) {
                                Log.w(TAG, "Skipping user with no searchable data: UID=" + userUid);
                                continue;
                            }
                            
                            // Check if query matches email or display name
                            boolean emailMatch = !email.isEmpty() && email.contains(lowerQuery);
                            boolean nameMatch = !displayName.isEmpty() && displayName.contains(lowerQuery);
                            
                            Log.d(TAG, "Match results - Email: " + emailMatch + ", Name: " + nameMatch);
                            
                            if (emailMatch || nameMatch) {
                                foundUsers.add(user);
                                Log.d(TAG, "*** MATCH FOUND: " + user.getDisplayText());
                            } else {
                                Log.d(TAG, "No match - User has valid data but doesn't contain '" + lowerQuery + "'");
                            }
                        } else {
                            Log.w(TAG, "Null user object for key: " + userKey);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing user snapshot: " + userKey, e);
                    }
                }
                
                Log.d(TAG, "=== SEARCH SUMMARY ===");
                Log.d(TAG, "Total users processed: " + processedCount);
                Log.d(TAG, "Valid user objects: " + allUsers.size());
                Log.d(TAG, "Matching users found: " + foundUsers.size());
                
                if (foundUsers.size() > 0) {
                    Log.d(TAG, "Returning " + foundUsers.size() + " matching users");
                } else {
                    Log.w(TAG, "No users matched the search query '" + query + "'");
                }
                
                callback.onUsersFound(foundUsers);
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "=== DATABASE ERROR ===");
                Log.e(TAG, "Error code: " + databaseError.getCode());
                Log.e(TAG, "Error message: " + databaseError.getMessage());
                Log.e(TAG, "Error details: " + databaseError.getDetails());
                
                callback.onFailure("Search failed: " + databaseError.getMessage());
            }
        });
        
        Log.d(TAG, "Database listener added successfully");
    }
    
    /**
     * Create a private chat room between current user and another user
     */
    public void createPrivateChatRoom(String otherUserId, ChatRoomCallback callback) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            callback.onFailure("User not authenticated");
            return;
        }
        
        String currentUserId = currentUser.getUid();
        
        // Create unique room ID for private chat (sorted user IDs to ensure consistency)
        String roomId = currentUserId.compareTo(otherUserId) < 0 ? 
                       currentUserId + "_" + otherUserId : 
                       otherUserId + "_" + currentUserId;
        
        // Check if chat room already exists
        databaseRef.child(CHAT_ROOMS_NODE).child(roomId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            // Chat room already exists
                            ChatRoom existingRoom = dataSnapshot.getValue(ChatRoom.class);
                            Log.d(TAG, "Private chat room already exists: " + roomId);
                            callback.onChatRoomCreated(roomId, existingRoom);
                        } else {
                            // Create new chat room
                            createNewPrivateChatRoom(roomId, currentUserId, otherUserId, callback);
                        }
                    }
                    
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        callback.onFailure("Failed to check existing chat room: " + databaseError.getMessage());
                    }
                });
    }
    
    /**
     * Create a group chat room with multiple users
     */
    public void createGroupChatRoom(String roomName, java.util.List<String> memberIds, ChatRoomCallback callback) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            callback.onFailure("User not authenticated");
            return;
        }
        
        String currentUserId = currentUser.getUid();
        String roomId = "group_" + System.currentTimeMillis();
        
        // Add current user to members if not already included
        if (!memberIds.contains(currentUserId)) {
            memberIds.add(currentUserId);
        }
        
        // Create ChatRoom object
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setRoomId(roomId);
        chatRoom.setRoomName(roomName);
        chatRoom.setRoomType("group");
        chatRoom.setCreatedBy(currentUserId);
        chatRoom.setCreatedAt(System.currentTimeMillis());
        chatRoom.setLastActivity(System.currentTimeMillis());
        
        // Save chat room
        databaseRef.child(CHAT_ROOMS_NODE).child(roomId).setValue(chatRoom)
                .addOnSuccessListener(aVoid -> {
                    // Add members to the chat room
                    addMembersToRoom(roomId, memberIds, new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "Group chat room created successfully: " + roomId);
                                callback.onChatRoomCreated(roomId, chatRoom);
                            } else {
                                callback.onFailure("Failed to add members to room");
                            }
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create group chat room", e);
                    callback.onFailure("Failed to create chat room: " + e.getMessage());
                });
    }
    
    /**
     * Create new private chat room
     */
    private void createNewPrivateChatRoom(String roomId, String user1Id, String user2Id, 
                                        ChatRoomCallback callback) {
        // Get user information to create room name
        databaseRef.child(USERS_NODE).child(user2Id)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        User otherUser = dataSnapshot.getValue(User.class);
                        String roomName = otherUser != null ? otherUser.getDisplayText() : "Private Chat";
                        
                        // Create ChatRoom object
                        ChatRoom chatRoom = new ChatRoom();
                        chatRoom.setRoomId(roomId);
                        chatRoom.setRoomName(roomName);
                        chatRoom.setRoomType("private");
                        chatRoom.setCreatedBy(user1Id);
                        chatRoom.setCreatedAt(System.currentTimeMillis());
                        chatRoom.setLastActivity(System.currentTimeMillis());
                        
                        // Save chat room
                        databaseRef.child(CHAT_ROOMS_NODE).child(roomId).setValue(chatRoom)
                                .addOnSuccessListener(aVoid -> {
                                    // Add both users as members
                                    java.util.List<String> members = new java.util.ArrayList<>();
                                    members.add(user1Id);
                                    members.add(user2Id);
                                    
                                    addMembersToRoom(roomId, members, new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Log.d(TAG, "Private chat room created successfully: " + roomId);
                                                callback.onChatRoomCreated(roomId, chatRoom);
                                            } else {
                                                callback.onFailure("Failed to add members to private chat");
                                            }
                                        }
                                    });
                                })
                                .addOnFailureListener(e -> {
                                    callback.onFailure("Failed to create private chat room: " + e.getMessage());
                                });
                    }
                    
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        callback.onFailure("Failed to get user information: " + databaseError.getMessage());
                    }
                });
    }
    
    /**
     * Add members to a chat room
     */
    private void addMembersToRoom(String roomId, java.util.List<String> memberIds, 
                                OnCompleteListener<Void> callback) {
        Map<String, Object> updates = new HashMap<>();
        long joinTime = System.currentTimeMillis();
        
        for (String memberId : memberIds) {
            RoomMember member = new RoomMember();
            member.setUserId(memberId);
            member.setRoomId(roomId);
            member.setJoinedAt(joinTime);
            member.setRole("member");
            
            updates.put(ROOM_MEMBERS_NODE + "/" + roomId + "/" + memberId, member);
        }
        
        databaseRef.updateChildren(updates).addOnCompleteListener(callback);
    }
    
    /**
     * Update user online status
     */
    public void updateUserStatus(String userId, String status) {
        if (userId == null || status == null) return;
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("lastSeen", System.currentTimeMillis());
        
        databaseRef.child(USERS_NODE).child(userId).updateChildren(updates);
    }
    
    /**
     * Get user's chat rooms
     */
    public void getUserChatRooms(String userId, ValueEventListener callback) {
        databaseRef.child(ROOM_MEMBERS_NODE)
                .orderByChild("userId")
                .equalTo(userId)
                .addValueEventListener(callback);
    }
    
    /**
     * Ensure locale configuration to prevent Firebase warnings
     */
    private void ensureLocaleConfiguration() {
        try {
            Locale currentLocale = Locale.getDefault();
            if (currentLocale == null) {
                currentLocale = new Locale("en", "US");
                Locale.setDefault(currentLocale);
            }
            
            // Set system properties for Firebase
            System.setProperty("user.language", currentLocale.getLanguage());
            System.setProperty("user.country", currentLocale.getCountry());
            
            Log.d(TAG, "Firebase locale configured: " + currentLocale.toString());
            
        } catch (Exception e) {
            Log.w(TAG, "Error configuring Firebase locale", e);
            try {
                Locale.setDefault(Locale.ENGLISH);
                System.setProperty("user.language", "en");
                System.setProperty("user.country", "US");
            } catch (Exception ex) {
                Log.e(TAG, "Failed to set fallback locale", ex);
            }
        }
    }
}
