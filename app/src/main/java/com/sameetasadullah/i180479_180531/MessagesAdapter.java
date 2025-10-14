package com.sameetasadullah.i180479_180531;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * MessagesAdapter for displaying chat messages in RecyclerView
 * 
 * Handles both sent and received messages with different layouts
 */
public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.MessageViewHolder> {
    
    private static final int VIEW_TYPE_MESSAGE_SENT = 1;
    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;
    
    private List<ChatMessage> messagesList;
    private String currentUserUid;
    
    private SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    
    public MessagesAdapter(List<ChatMessage> messagesList, String currentUserUid) {
        this.messagesList = messagesList;
        this.currentUserUid = currentUserUid;
    }
    
    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messagesList.get(position);
        
        if (message.isSentByMe(currentUserUid)) {
            return VIEW_TYPE_MESSAGE_SENT;
        } else {
            return VIEW_TYPE_MESSAGE_RECEIVED;
        }
    }
    
    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        
        if (viewType == VIEW_TYPE_MESSAGE_SENT) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_received, parent, false);
        }
        
        return new MessageViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatMessage message = messagesList.get(position);
        holder.bind(message, getItemViewType(position));
    }
    
    @Override
    public int getItemCount() {
        return messagesList != null ? messagesList.size() : 0;
    }
    
    public void addMessage(ChatMessage message) {
        if (messagesList != null) {
            messagesList.add(message);
            notifyItemInserted(messagesList.size() - 1);
        }
    }
    
    public void removeMessage(int position) {
        if (messagesList != null && position >= 0 && position < messagesList.size()) {
            messagesList.remove(position);
            notifyItemRemoved(position);
        }
    }
    
    public void updateMessage(int position, ChatMessage message) {
        if (messagesList != null && position >= 0 && position < messagesList.size()) {
            messagesList.set(position, message);
            notifyItemChanged(position);
        }
    }
    
    /**
     * ViewHolder class for message items
     */
    public class MessageViewHolder extends RecyclerView.ViewHolder {
        
        private TextView tvMessage;
        private TextView tvTime;
        private TextView tvSenderName;
        private LinearLayout messageContainer;
        
        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            
            tvMessage = itemView.findViewById(R.id.message_text);
            tvTime = itemView.findViewById(R.id.time_text);
            tvSenderName = itemView.findViewById(R.id.sender_text);
            messageContainer = null; // Not available in existing layout
        }
        
        public void bind(ChatMessage message, int viewType) {
            // Set message text
            tvMessage.setText(message.getMessage());
            
            // Set timestamp
            String formattedTime = formatTime(message.getTimestamp());
            tvTime.setText(formattedTime);
            
            // Set sender name (only for received messages)
            if (viewType == VIEW_TYPE_MESSAGE_RECEIVED && tvSenderName != null) {
                String senderName = message.getSenderName();
                if (senderName == null || senderName.trim().isEmpty()) {
                    senderName = message.getSenderEmail();
                    if (senderName != null && senderName.contains("@")) {
                        senderName = senderName.substring(0, senderName.indexOf("@"));
                    }
                }
                tvSenderName.setText(senderName);
                tvSenderName.setVisibility(View.VISIBLE);
            } else if (tvSenderName != null) {
                tvSenderName.setVisibility(View.GONE);
            }
            
            // Style temporary/optimistic messages differently
            if (message.isTemporaryMessage()) {
                tvMessage.setAlpha(0.7f);
                tvTime.setAlpha(0.7f);
            } else {
                tvMessage.setAlpha(1.0f);
                tvTime.setAlpha(1.0f);
            }
            
            // Handle different message types
            if (message.isSystemMessage()) {
                // Style system messages (like "User joined the chat")
                tvMessage.setTextColor(itemView.getContext().getResources().getColor(android.R.color.darker_gray));
                tvMessage.setTextSize(14f);
                
                // Center system messages - skip since messageContainer is not available
            }
        }
    }
    
    /**
     * Format timestamp for display
     */
    private String formatTime(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            return "";
        }
        
        try {
            Date date = inputFormat.parse(timestamp);
            return outputFormat.format(date);
        } catch (ParseException e) {
            // If parsing fails, try to extract time from timestamp
            if (timestamp.contains(" ")) {
                String[] parts = timestamp.split(" ");
                if (parts.length >= 2) {
                    String timePart = parts[1];
                    if (timePart.length() >= 5) {
                        return timePart.substring(0, 5); // HH:mm
                    }
                }
            }
            
            // Fallback: return timestamp as-is or just the time part
            return timestamp.length() > 10 ? timestamp.substring(timestamp.length() - 8, timestamp.length() - 3) : timestamp;
        }
    }
    
    /**
     * Update the current user UID if needed
     */
    public void setCurrentUserUid(String currentUserUid) {
        this.currentUserUid = currentUserUid;
        notifyDataSetChanged();
    }
    
    /**
     * Clear all messages
     */
    public void clearMessages() {
        if (messagesList != null) {
            int size = messagesList.size();
            messagesList.clear();
            notifyItemRangeRemoved(0, size);
        }
    }
    
    /**
     * Get message at position
     */
    public ChatMessage getMessageAt(int position) {
        if (messagesList != null && position >= 0 && position < messagesList.size()) {
            return messagesList.get(position);
        }
        return null;
    }
    
    /**
     * Find message by ID
     */
    public int findMessagePositionById(String messageId) {
        if (messagesList != null && messageId != null) {
            for (int i = 0; i < messagesList.size(); i++) {
                ChatMessage message = messagesList.get(i);
                if (messageId.equals(message.getMessageId())) {
                    return i;
                }
            }
        }
        return -1;
    }
    
    /**
     * Remove temporary messages (used for optimistic updates)
     */
    public void removeTemporaryMessages() {
        if (messagesList != null) {
            for (int i = messagesList.size() - 1; i >= 0; i--) {
                ChatMessage message = messagesList.get(i);
                if (message.isTemporaryMessage()) {
                    messagesList.remove(i);
                    notifyItemRemoved(i);
                }
            }
        }
    }
}