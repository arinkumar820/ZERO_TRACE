package com.sameetasadullah.i180479_180531;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * ContactsAdapter for displaying users/contacts in RecyclerView
 * 
 * Used in search functionality to display search results
 */
public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ContactViewHolder> {
    
    private List<User> contactsList;
    private OnContactClickListener clickListener;
    
    /**
     * Interface for handling contact click events
     */
    public interface OnContactClickListener {
        void onContactClick(User user);
        void onContactLongClick(User user);
    }
    
    public ContactsAdapter(List<User> contactsList, OnContactClickListener clickListener) {
        this.contactsList = contactsList;
        this.clickListener = clickListener;
    }
    
    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.contact_row, parent, false);
        return new ContactViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        User user = contactsList.get(position);
        holder.bind(user);
    }
    
    @Override
    public int getItemCount() {
        return contactsList != null ? contactsList.size() : 0;
    }
    
    public void updateContacts(List<User> newContacts) {
        this.contactsList = newContacts;
        notifyDataSetChanged();
    }
    
    public void addContact(User user) {
        if (contactsList != null) {
            contactsList.add(user);
            notifyItemInserted(contactsList.size() - 1);
        }
    }
    
    public void removeContact(int position) {
        if (contactsList != null && position >= 0 && position < contactsList.size()) {
            contactsList.remove(position);
            notifyItemRemoved(position);
        }
    }
    
    public void clearContacts() {
        if (contactsList != null) {
            int size = contactsList.size();
            contactsList.clear();
            notifyItemRangeRemoved(0, size);
        }
    }
    
    public User getContactAt(int position) {
        if (contactsList != null && position >= 0 && position < contactsList.size()) {
            return contactsList.get(position);
        }
        return null;
    }
    
    /**
     * ViewHolder class for contact items
     */
    public class ContactViewHolder extends RecyclerView.ViewHolder {
        
        private ImageView ivProfileImage;
        private TextView tvContactName;
        private TextView tvContactEmail;
        private TextView tvContactStatus;
        private View statusIndicator;
        
        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            
            ivProfileImage = itemView.findViewById(R.id.dp);
            tvContactName = itemView.findViewById(R.id.name);
            tvContactEmail = itemView.findViewById(R.id.number);
            tvContactStatus = null; // Not available in existing layout
            statusIndicator = null; // Not available in existing layout
            
            // Set click listeners
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && clickListener != null) {
                    clickListener.onContactClick(contactsList.get(position));
                }
            });
            
            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && clickListener != null) {
                    clickListener.onContactLongClick(contactsList.get(position));
                    return true;
                }
                return false;
            });
        }
        
        public void bind(User user) {
            // Set contact name
            String displayName = user.getDisplayName();
            if (displayName == null || displayName.trim().isEmpty()) {
                displayName = user.getEmail();
                if (displayName != null && displayName.contains("@")) {
                    displayName = displayName.substring(0, displayName.indexOf("@"));
                }
            }
            tvContactName.setText(displayName);
            
            // Set contact email
            tvContactEmail.setText(user.getEmail());
            
            // Set status if available
            if (tvContactStatus != null) {
                String status = user.getStatus();
                if (status != null && !status.trim().isEmpty()) {
                    tvContactStatus.setText(status);
                    tvContactStatus.setVisibility(View.VISIBLE);
                } else {
                    tvContactStatus.setVisibility(View.GONE);
                }
            }
            
            // Set status indicator color
            if (statusIndicator != null) {
                String userStatus = user.getStatus();
                int statusColor;
                
                if ("online".equalsIgnoreCase(userStatus)) {
                    statusColor = itemView.getContext().getResources().getColor(android.R.color.holo_green_light);
                } else if ("away".equalsIgnoreCase(userStatus)) {
                    statusColor = itemView.getContext().getResources().getColor(android.R.color.holo_orange_light);
                } else if ("busy".equalsIgnoreCase(userStatus)) {
                    statusColor = itemView.getContext().getResources().getColor(android.R.color.holo_red_light);
                } else {
                    // offline or unknown
                    statusColor = itemView.getContext().getResources().getColor(android.R.color.darker_gray);
                }
                
                statusIndicator.setBackgroundColor(statusColor);
                statusIndicator.setVisibility(View.VISIBLE);
            }
            
            // Set profile image
            if (ivProfileImage != null) {
                // TODO: Load profile image using image loading library
                // For now, set a default avatar or first letter of name
                String profileImageUrl = user.getProfileImageUrl();
                
                if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                    // Use image loading library like Glide or Picasso
                    // Glide.with(itemView.getContext())
                    //     .load(profileImageUrl)
                    //     .placeholder(R.drawable.default_avatar)
                    //     .error(R.drawable.default_avatar)
                    //     .circleCrop()
                    //     .into(ivProfileImage);
                    
                    // For now, use default avatar
                    ivProfileImage.setImageResource(android.R.drawable.ic_menu_gallery);
                } else {
                    // Set default avatar
                    ivProfileImage.setImageResource(android.R.drawable.ic_menu_gallery);
                }
            }
            
            // Set bio as subtitle if available
            if (user.getBio() != null && !user.getBio().trim().isEmpty() && tvContactStatus != null) {
                // If bio is short, show it instead of status
                String bio = user.getBio();
                if (bio.length() <= 50) {
                    tvContactStatus.setText(bio);
                    tvContactStatus.setVisibility(View.VISIBLE);
                }
            }
        }
    }
    
    /**
     * Filter contacts based on search query
     */
    public void filter(String query) {
        // This method can be used for client-side filtering if needed
        // For server-side search (recommended), this might not be necessary
        notifyDataSetChanged();
    }
    
    /**
     * Update click listener
     */
    public void setOnContactClickListener(OnContactClickListener listener) {
        this.clickListener = listener;
    }
}
