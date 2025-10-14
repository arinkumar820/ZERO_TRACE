package com.sameetasadullah.i180479_180531;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import android.content.Intent;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Adapter for displaying user search results in RecyclerView
 */
public class UserSearchAdapter extends RecyclerView.Adapter<UserSearchAdapter.UserSearchViewHolder> {

    private List<User> userList;
    private Context context;
    private FirebaseAuth firebaseAuth;
    private OnUserAddedListener onUserAddedListener;

    // Interface for callback when user is added as contact
    public interface OnUserAddedListener {
        void onUserAdded(User user);
        void onChatStarted(String roomId, User user);
    }

    public UserSearchAdapter(Context context) {
        this.context = context;
        this.userList = new ArrayList<>();
        this.firebaseAuth = FirebaseAuth.getInstance();
    }

    public void setOnUserAddedListener(OnUserAddedListener listener) {
        this.onUserAddedListener = listener;
    }

    // Update the user list and notify adapter
    public void updateUsers(List<User> newUsers) {
        if (newUsers == null) {
            this.userList = new ArrayList<>();
        } else {
            this.userList = new ArrayList<>(newUsers);
        }
        notifyDataSetChanged();
    }

    // Add a single user to the list
    public void addUser(User user) {
        if (user != null && !userList.contains(user)) {
            userList.add(user);
            notifyItemInserted(userList.size() - 1);
        }
    }

    // Clear all users
    public void clearUsers() {
        int size = userList.size();
        userList.clear();
        notifyItemRangeRemoved(0, size);
    }

    @NonNull
    @Override
    public UserSearchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user_search_enhanced, parent, false);
        return new UserSearchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserSearchViewHolder holder, int position) {
        if (userList == null || position < 0 || position >= userList.size()) {
            return;
        }

        User user = userList.get(position);
        if (user == null) {
            return;
        }

        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        return userList != null ? userList.size() : 0;
    }

    public class UserSearchViewHolder extends RecyclerView.ViewHolder {
        private CircleImageView profileImageView;
        private TextView nameTextView;
        private TextView emailTextView;
        private TextView statusTextView;
        private TextView addButton;
        private TextView chatButton;
        private View onlineIndicator;
        private FirebaseUserManager userManager;

        public UserSearchViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImageView = itemView.findViewById(R.id.iv_profile);
            nameTextView = itemView.findViewById(R.id.tv_name);
            emailTextView = itemView.findViewById(R.id.tv_email);
            statusTextView = itemView.findViewById(R.id.tv_status);
            addButton = itemView.findViewById(R.id.btn_add);
            chatButton = itemView.findViewById(R.id.btn_chat);
            onlineIndicator = itemView.findViewById(R.id.v_online_indicator);
            userManager = new FirebaseUserManager();
        }

        public void bind(User user) {
            // Set user name
            if (nameTextView != null) {
                String displayName = user.getDisplayText();
                nameTextView.setText(displayName);
            }

            // Set email
            if (emailTextView != null) {
                String email = user.getEmail() != null ? user.getEmail() : "No email";
                emailTextView.setText(email);
            }

            // Set status/bio (optional)
            if (statusTextView != null) {
                String bio = user.getBio();
                if (bio != null && !bio.trim().isEmpty()) {
                    statusTextView.setText(bio);
                    statusTextView.setVisibility(View.VISIBLE);
                } else {
                    statusTextView.setVisibility(View.GONE);
                }
            }

            // Load profile image
            if (profileImageView != null) {
                String imageUrl = user.getProfileImageUrl();
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Picasso.get()
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_launcher_background)
                            .error(R.drawable.ic_launcher_background)
                            .into(profileImageView);
                } else {
                    // Set default profile image
                    profileImageView.setImageResource(R.drawable.ic_launcher_background);
                }
            }

            // Show online status
            if (onlineIndicator != null) {
                if (user.isOnline()) {
                    onlineIndicator.setVisibility(View.VISIBLE);
                } else {
                    onlineIndicator.setVisibility(View.GONE);
                }
            }

            // Set add button click listener
            if (addButton != null) {
                addButton.setOnClickListener(v -> {
                    addUserAsContact(user);
                });
            }
            
            // Set chat button click listener
            if (chatButton != null) {
                chatButton.setOnClickListener(v -> {
                    startChatWithUser(user);
                });
            }

            // Set item click listener for profile view
            itemView.setOnClickListener(v -> {
                // Optional: Show user profile details
                Toast.makeText(context, "User: " + user.getDisplayText(), Toast.LENGTH_SHORT).show();
            });
        }

        private void addUserAsContact(User user) {
            if (firebaseAuth.getCurrentUser() == null) {
                Toast.makeText(context, "Please login to add contacts", Toast.LENGTH_SHORT).show();
                return;
            }

            String currentUserId = firebaseAuth.getCurrentUser().getUid();
            
            // Don't allow adding self as contact
            if (currentUserId.equals(user.getUid())) {
                Toast.makeText(context, "You cannot add yourself as a contact", Toast.LENGTH_SHORT).show();
                return;
            }

            // Add user to current user's contacts in Firebase
            DatabaseReference contactsRef = FirebaseDatabase.getInstance()
                    .getReference("Contacts")
                    .child(currentUserId)
                    .child(user.getUid());

            // Create contact object with timestamp
            Contact contact = new Contact(
                    user.getUid(),
                    user.getDisplayText(),
                    user.getEmail(),
                    user.getProfileImageUrl(),
                    System.currentTimeMillis()
            );

            contactsRef.setValue(contact)
                    .addOnSuccessListener(aVoid -> {
                        // Also add current user to the other user's contacts (mutual contact)
                        if (firebaseAuth.getCurrentUser() != null) {
                            String currentUserEmail = firebaseAuth.getCurrentUser().getEmail();
                            String currentUserName = firebaseAuth.getCurrentUser().getDisplayName();
                            
                            DatabaseReference mutualContactRef = FirebaseDatabase.getInstance()
                                    .getReference("Contacts")
                                    .child(user.getUid())
                                    .child(currentUserId);

                            Contact mutualContact = new Contact(
                                    currentUserId,
                                    currentUserName != null ? currentUserName : currentUserEmail,
                                    currentUserEmail,
                                    null,
                                    System.currentTimeMillis()
                            );

                            mutualContactRef.setValue(mutualContact);
                        }

                        // Update UI
                        addButton.setText("Added");
                        addButton.setEnabled(false);
                        addButton.setBackgroundTintList(
                                context.getResources().getColorStateList(android.R.color.darker_gray));

                        Toast.makeText(context, user.getDisplayText() + " added to contacts!", 
                                Toast.LENGTH_SHORT).show();

                        // Notify listener
                        if (onUserAddedListener != null) {
                            onUserAddedListener.onUserAdded(user);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Failed to add contact: " + e.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                    });
        }
        
        private void startChatWithUser(User user) {
            if (firebaseAuth.getCurrentUser() == null) {
                Toast.makeText(context, "Please login to start chat", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String currentUserId = firebaseAuth.getCurrentUser().getUid();
            
            // Don't allow chatting with self
            if (currentUserId.equals(user.getUid())) {
                Toast.makeText(context, "You cannot chat with yourself", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Show loading state
            if (chatButton != null) {
                chatButton.setText("Creating...");
                chatButton.setEnabled(false);
            }
            
            // Create private chat room with the user
            userManager.createPrivateChatRoom(user.getUid(), new FirebaseUserManager.ChatRoomCallback() {
                @Override
                public void onChatRoomCreated(String roomId, ChatRoom chatRoom) {
                    // Reset button state
                    if (chatButton != null) {
                        chatButton.setText("Chat");
                        chatButton.setEnabled(true);
                    }
                    
                    // Start ChatActivity with the room
                    Intent chatIntent = new Intent(context, ChatActivity.class);
                    chatIntent.putExtra("chat_id", roomId);
                    chatIntent.putExtra("chat_name", user.getDisplayText());
                    chatIntent.putExtra("chat_type", "private");
                    context.startActivity(chatIntent);
                    
                    // Notify listener
                    if (onUserAddedListener != null) {
                        onUserAddedListener.onChatStarted(roomId, user);
                    }
                    
                    Toast.makeText(context, "Chat started with " + user.getDisplayText(), 
                            Toast.LENGTH_SHORT).show();
                }
                
                @Override
                public void onFailure(String error) {
                    // Reset button state
                    if (chatButton != null) {
                        chatButton.setText("Chat");
                        chatButton.setEnabled(true);
                    }
                    
                    Toast.makeText(context, "Failed to start chat: " + error, 
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // Contact model for Firebase storage
    public static class Contact {
        private String uid;
        private String name;
        private String email;
        private String profileImageUrl;
        private long addedTimestamp;

        public Contact() {} // Required for Firebase

        public Contact(String uid, String name, String email, String profileImageUrl, long addedTimestamp) {
            this.uid = uid;
            this.name = name;
            this.email = email;
            this.profileImageUrl = profileImageUrl;
            this.addedTimestamp = addedTimestamp;
        }

        // Getters and setters
        public String getUid() { return uid; }
        public void setUid(String uid) { this.uid = uid; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getProfileImageUrl() { return profileImageUrl; }
        public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
        public long getAddedTimestamp() { return addedTimestamp; }
        public void setAddedTimestamp(long addedTimestamp) { this.addedTimestamp = addedTimestamp; }
    }
}