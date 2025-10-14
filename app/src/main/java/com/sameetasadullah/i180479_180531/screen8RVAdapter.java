package com.sameetasadullah.i180479_180531;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class screen8RVAdapter extends RecyclerView.Adapter<screen8RVAdapter.screen8ViewHolder> {
    private List<message> messageList;
    private Context context;
    private String currentUserId;
    private SimpleDateFormat timeFormat;
    private Handler mainHandler;

    // Safe constructor that initializes the list to prevent null pointer exceptions
    public screen8RVAdapter(List<message> messageList, Context context, String currentUserId) {
        this.context = context;
        this.currentUserId = currentUserId;
        this.messageList = messageList != null ? messageList : new ArrayList<>();
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    // Constructor with just context (for backwards compatibility)
    public screen8RVAdapter(Context context, String currentUserId) {
        this(null, context, currentUserId);
    }

    @NonNull
    @Override
    public screen8ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context).inflate(R.layout.recent_contact_row, parent, false);
        return new screen8ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull screen8ViewHolder holder, int position) {
        if (messageList == null || position < 0 || position >= messageList.size()) {
            return; // Safety check
        }

        message msg = messageList.get(position);
        if (msg == null) {
            return; // Safety check
        }

        // Display message content
        if (holder.messageText != null) {
            holder.messageText.setText(msg.getMessage() != null ? msg.getMessage() : "No message");
        }

        // Display time
        if (holder.timeText != null && msg.getTime() != null) {
            holder.timeText.setText(msg.getTime());
        }

        // Display sender info (for group chats or contact name)
        if (holder.name != null) {
            String displayName = "Unknown";
            if (msg.getSenderID() != null && msg.getSenderID().equals(currentUserId)) {
                displayName = "You";
            } else if (msg.getSenderID() != null) {
                displayName = msg.getSenderID(); // Could be replaced with actual name lookup
            }
            holder.name.setText(displayName);
        }

        // Load profile image if available
        if (holder.dp != null && msg.getImage() != null && !msg.getImage().isEmpty()) {
            Picasso.get()
                    .load(msg.getImage())
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .into(holder.dp);
        }

        // Set click listener for opening chat
        if (holder.itemView != null) {
            holder.itemView.setOnClickListener(v -> {
                if (msg.getReceiverID() != null || msg.getSenderID() != null) {
                    Intent intent = new Intent(context, screen5.class);
                    intent.putExtra("receiverID", msg.getReceiverID());
                    intent.putExtra("senderID", msg.getSenderID());
                    intent.putExtra("messageKey", msg.getKey());
                    context.startActivity(intent);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        // Safe method that never throws null pointer exception
        return messageList != null ? messageList.size() : 0;
    }

    // Method to safely add a new message (thread-safe for real-time updates)
    public void addMessage(message newMessage) {
        if (newMessage == null) {
            return;
        }

        // Ensure we're on the main thread for UI updates
        mainHandler.post(() -> {
            if (messageList == null) {
                messageList = new ArrayList<>();
            }
            
            messageList.add(newMessage);
            notifyItemInserted(messageList.size() - 1);
        });
    }

    // Method to add a message at specific position
    public void addMessageAt(int position, message newMessage) {
        if (newMessage == null || position < 0) {
            return;
        }

        mainHandler.post(() -> {
            if (messageList == null) {
                messageList = new ArrayList<>();
            }
            
            int safePosition = Math.min(position, messageList.size());
            messageList.add(safePosition, newMessage);
            notifyItemInserted(safePosition);
        });
    }

    // Method to update the entire message list safely
    public void updateMessages(List<message> newMessages) {
        mainHandler.post(() -> {
            if (newMessages == null) {
                messageList = new ArrayList<>();
            } else {
                messageList = new ArrayList<>(newMessages);
            }
            notifyDataSetChanged();
        });
    }

    // Method to clear all messages
    public void clearMessages() {
        mainHandler.post(() -> {
            if (messageList != null) {
                int size = messageList.size();
                messageList.clear();
                notifyItemRangeRemoved(0, size);
            }
        });
    }

    // Method to remove a message at specific position
    public void removeMessageAt(int position) {
        mainHandler.post(() -> {
            if (messageList != null && position >= 0 && position < messageList.size()) {
                messageList.remove(position);
                notifyItemRemoved(position);
            }
        });
    }

    // Method to get message at position safely
    public message getMessageAt(int position) {
        if (messageList == null || position < 0 || position >= messageList.size()) {
            return null;
        }
        return messageList.get(position);
    }

    // Method to check if adapter is empty
    public boolean isEmpty() {
        return getItemCount() == 0;
    }

    // ViewHolder class
    public static class screen8ViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        TextView messageText;
        TextView timeText;
        CircleImageView dp;
        ImageView statusIcon;

        public screen8ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.name);
            messageText = itemView.findViewById(R.id.name); // Using existing name field for message preview
            timeText = null; // Will be handled safely with null checks
            dp = itemView.findViewById(R.id.dp);
            statusIcon = null; // Will be handled safely with null checks
            
            // If some views don't exist in the layout, they'll be null and handled safely
        }
    }
}

