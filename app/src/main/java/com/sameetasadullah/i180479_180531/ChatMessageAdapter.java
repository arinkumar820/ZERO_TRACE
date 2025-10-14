package com.sameetasadullah.i180479_180531;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import android.content.Context;

public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.MessageViewHolder> {
    
    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;
    
    private List<WebSocketMessage> messageList;
    private String currentUserId;
    private SimpleDateFormat timeFormat;
    private Context context;
    private DisappearingMessageManager disappearingManager;
    
    public ChatMessageAdapter(List<WebSocketMessage> messageList, String currentUserId, Context context) {
        this.messageList = messageList;
        this.currentUserId = currentUserId;
        this.context = context;
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        
        // Initialize disappearing message manager for UI countdown
        this.disappearingManager = new DisappearingMessageManager(context, new DisappearingMessageManager.MessageUpdateListener() {
            @Override
            public void onMessageExpired(int position) {
                // Hide message from UI (but keep in database)
                if (position >= 0 && position < messageList.size()) {
                    WebSocketMessage message = messageList.get(position);
                    message.setExpired(true);
                    notifyItemChanged(position);
                }
            }
            
            @Override
            public void onMessageTimerUpdate(int position, long remainingMs) {
                // Update countdown display
                if (position >= 0 && position < messageList.size()) {
                    notifyItemChanged(position);
                }
            }
        });
    }
    
    // Backward compatibility constructor
    public ChatMessageAdapter(List<WebSocketMessage> messageList, String currentUserId) {
        this(messageList, currentUserId, null);
    }
    
    @Override
    public int getItemViewType(int position) {
        WebSocketMessage message = messageList.get(position);
        if (message.getSender_uid().equals(currentUserId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }
    
    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_SENT) {
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
        WebSocketMessage message = messageList.get(position);
        holder.bind(message);
    }
    
    @Override
    public int getItemCount() {
        return messageList.size();
    }
    
    class MessageViewHolder extends RecyclerView.ViewHolder {
        private TextView messageText;
        private TextView timeText;
        private TextView senderText;
        private TextView seenText;
        private TextView messageIdText;
        private TextView countdownText;
        
        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.message_text);
            timeText = itemView.findViewById(R.id.time_text);
            senderText = itemView.findViewById(R.id.sender_text);
            // Only present in sent layout
            seenText = itemView.findViewById(R.id.seen_text);
            // Present in both layouts
            messageIdText = itemView.findViewById(R.id.message_id_text);
            countdownText = itemView.findViewById(R.id.countdown_text);
        }
        
        public void bind(WebSocketMessage message) {
            int position = getAdapterPosition();
            
            // Handle expired/disappeared messages
            if (message.isExpired() || message.shouldDisappear()) {
                messageText.setText("💨 This message has disappeared");
                messageText.setAlpha(0.6f);
                messageText.setTextColor(itemView.getResources().getColor(android.R.color.darker_gray));
            } else if (message.getMessage() != null) {
                messageText.setText(message.getMessage());
                messageText.setAlpha(1.0f);
                messageText.setTextColor(itemView.getResources().getColor(android.R.color.primary_text_light));
            } else {
                messageText.setText("[No message text]");
            }
            
            // Format time - handle potential timestamp issues
            if (timeText != null) {
                try {
                    long timestamp = message.getTimestamp();
                    if (timestamp == 0) {
                        timestamp = System.currentTimeMillis();
                    }
                    String time = timeFormat.format(new Date(timestamp));
                    timeText.setText(time);
                } catch (Exception e) {
                    timeText.setText("--:--");
                }
            }
            
            
            // Show sender email for received messages
            if (senderText != null && message.getSender_uid() != null && 
                !message.getSender_uid().equals(currentUserId)) {
                String senderEmail = message.getSender_email();
                if (senderEmail != null && senderEmail.contains("@")) {
                    String displayName = senderEmail.substring(0, senderEmail.indexOf("@"));
                    senderText.setText(displayName);
                } else {
                    senderText.setText("Unknown");
                }
            }
            
            // Display message ID for all messages
            if (messageIdText != null) {
                int messageId = message.getMessage_id();
                if (messageId > 0) {
                    messageIdText.setText("#" + messageId);
                    messageIdText.setVisibility(View.VISIBLE);
                } else {
                    // For messages without ID, show position as fallback
                    messageIdText.setText("#" + (position + 1));
                    messageIdText.setVisibility(View.VISIBLE);
                }
            }

            // Show enhanced seen indicator for sent messages
            if (seenText != null) {
                boolean isSentByMe = message.getSender_uid() != null && message.getSender_uid().equals(currentUserId);
                if (isSentByMe) {
                    if (message.isSeen()) {
                        // Show checkmarks with seen timestamp if available
                        if (message.getSeenAt() > 0) {
                            String seenTime = new SimpleDateFormat("HH:mm", Locale.getDefault())
                                    .format(new Date(message.getSeenAt()));
                            seenText.setText("✓✓ " + seenTime);
                        } else {
                            seenText.setText("✓✓ Seen");
                        }
                        seenText.setTextColor(itemView.getResources().getColor(android.R.color.holo_blue_light));
                        seenText.setVisibility(View.VISIBLE);
                    } else {
                        // Show single checkmark for delivered but not seen
                        seenText.setText("✓");
                        seenText.setTextColor(itemView.getResources().getColor(android.R.color.white));
                        seenText.setVisibility(View.VISIBLE);
                    }
                } else {
                    seenText.setVisibility(View.GONE);
                }
            }
            
            // Display countdown timer for disappearing messages
            if (countdownText != null) {
                if (message.isDisappearing() && !message.isExpired()) {
                    long remainingMs = message.getRemainingTimeMs();
                    if (remainingMs > 0) {
                        String countdown = DisappearingMessageSettings.formatRemainingTime(remainingMs);
                        String icon = DisappearingMessageManager.getDisappearingIcon(remainingMs);
                        countdownText.setText(icon + " " + countdown);
                        countdownText.setTextColor(itemView.getResources().getColor(
                            DisappearingMessageManager.getCountdownColorHint(remainingMs)));
                        countdownText.setVisibility(View.VISIBLE);
                        
                        // Start timer for this message if not already started
                        disappearingManager.startMessageTimer(message, position);
                    } else {
                        countdownText.setText("💨 Expired");
                        countdownText.setTextColor(itemView.getResources().getColor(android.R.color.darker_gray));
                        countdownText.setVisibility(View.VISIBLE);
                    }
                } else {
                    countdownText.setVisibility(View.GONE);
                }
            }
        }
    }
    
    
    /**
     * Mark a message as seen and notify the adapter
     */
    public void markMessageAsSeen(int position, String seenByUid) {
        if (position >= 0 && position < messageList.size()) {
            WebSocketMessage message = messageList.get(position);
            if (!message.isSeen()) {
                message.markAsSeen(seenByUid);
                notifyItemChanged(position);
            }
        }
    }
    
    /**
     * Mark messages from a specific sender as seen
     */
    public void markMessagesFromSenderAsSeen(String senderUid, String seenByUid) {
        for (int i = 0; i < messageList.size(); i++) {
            WebSocketMessage message = messageList.get(i);
            if (message.getSender_uid().equals(senderUid) && !message.isSeen()) {
                message.markAsSeen(seenByUid);
                notifyItemChanged(i);
            }
        }
    }
    
    /**
     * Get interface for read receipt callbacks
     */
    public interface ReadReceiptListener {
        void onMessageSeen(WebSocketMessage message, int position);
    }
    
    private ReadReceiptListener readReceiptListener;
    
    public void setReadReceiptListener(ReadReceiptListener listener) {
        this.readReceiptListener = listener;
    }
    
    public void cleanup() {
        // Cleanup disappearing message timers
        if (disappearingManager != null) {
            disappearingManager.cleanup();
        }
    }
}
