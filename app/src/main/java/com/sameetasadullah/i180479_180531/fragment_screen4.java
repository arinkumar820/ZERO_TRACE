package com.sameetasadullah.i180479_180531;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.widget.ImageView;
import android.widget.EditText;
import android.widget.Toast;
import android.content.Intent;
import android.util.Log;
import android.text.Editable;
import android.text.TextWatcher;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class fragment_screen4 extends Fragment {

    private ImageView newMessageButton;
    private RecyclerView chatsRecyclerView;
    private EditText searchEditText;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private screen4RVAdaptor chatAdapter;
    private List<chat> chatList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_screen4, container, false);
        
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        
        // Initialize UI components
        newMessageButton = view.findViewById(R.id.new_message);
        chatsRecyclerView = view.findViewById(R.id.rv_chats);
        searchEditText = view.findViewById(R.id.search_edit_text);
        
        Log.d("Fragment4", "UI Components - newMessageButton: " + (newMessageButton != null) + 
              ", chatsRecyclerView: " + (chatsRecyclerView != null));
        
        // Initialize chat list and load contacts from Firebase
        initializeChatList();
        loadContactsIntoChatList();
        
        // Set up RecyclerView
        if (chatsRecyclerView != null) {
            setupRecyclerView();
        }
        
        // Set up search functionality
        if (searchEditText != null) {
            setupSearch();
        }
        
        // Set up New Message button
        if (newMessageButton != null) {
            newMessageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d("Fragment4", "New Message button clicked!");
                    handleNewMessageClick();
                }
            });
        } else {
            Log.e("Fragment4", "New Message button not found in layout!");
        }
        
        return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Refresh chat list from contacts when returning to this screen
        loadContactsIntoChatList();
    }
    
    public void applicationNotMinimized() {
        // TODO: Add Firebase functionality if needed
    }
    
    private void initializeChatList() {
        // Start with your original sample/pinned chats
        chatList = new ArrayList<>(buildStaticChats());
        Log.d("Fragment4", "Initialized chat list with static entries: " + chatList.size());
    }

    private List<chat> buildStaticChats() {
        List<chat> staticChats = new ArrayList<>();
        // Elite Teams (top 2)
        staticChats.add(new chat("team_alpha", "Team Alpha 🚀", "Elite development team ready for action!", "now", false,
                "https://via.placeholder.com/150/FF5722/FFFFFF?text=TA", "online", "now", "today"));

        staticChats.add(new chat("team_tiger", "Team Tiger 🐅", "Fierce warriors conquering new challenges!", "2m ago", false,
                "https://via.placeholder.com/150/FF9800/FFFFFF?text=TT", "online", "2m ago", "today"));

        // General Chat
        staticChats.add(new chat("general_chat", "General Chat 💬", "Open discussions for everyone", "5m ago", true,
                "https://via.placeholder.com/150/4CAF50/FFFFFF?text=GC", "online", "5m ago", "today"));

        // Person-to-Person Chats
        staticChats.add(new chat("user_kalpaditya", "Kalpaditya", "Hey! How's your project going?", "30m ago", false,
                "https://via.placeholder.com/150/795548/FFFFFF?text=KA", "online", "30m ago", "today"));

        staticChats.add(new chat("user_deepak", "Deepak", "Thanks for the help with the code!", "1h ago", true,
                "https://via.placeholder.com/150/E91E63/FFFFFF?text=DE", "offline", "1h ago", "today"));

        return staticChats;
    }

    private boolean containsId(List<chat> list, String id) {
        if (id == null) return false;
        for (chat c : list) {
            if (id.equals(c.getId())) return true;
        }
        return false;
    }

    private void loadContactsIntoChatList() {
        if (currentUser == null) {
            Log.w("Fragment4", "No current user; cannot load contacts");
            return;
        }

        DatabaseReference contactsRef = FirebaseDatabase.getInstance()
                .getReference("Contacts")
                .child(currentUser.getUid());

        Log.d("Fragment4", "Loading contacts from Firebase for uid: " + currentUser.getUid());

        contactsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<chat> dynamicContacts = new ArrayList<>();

                for (DataSnapshot child : snapshot.getChildren()) {
                    String contactUid = child.getKey();

                    // Flexible field extraction (handle various structures)
                    String name = getString(child, "name");
                    if (name == null) name = getString(child, "display_name");
                    String email = getString(child, "email");
                    String dp = getString(child, "profileImageUrl");
                    if (dp == null) dp = getString(child, "profile_image_url");

                    if ((name == null || name.trim().isEmpty()) && email != null) {
                        name = email.contains("@") ? email.substring(0, email.indexOf("@")) : email;
                    }
                    if (name == null) name = "Contact";

                    // Build a chat row with placeholder last message
                    chat c = new chat(
                            contactUid != null ? contactUid : name,
                            name,
                            "Tap to start chat",
                            "",
                            true,
                            dp != null ? dp : "https://via.placeholder.com/150/4CAF50/FFFFFF?text=C",
                            "offline",
                            "",
                            ""
                    );
                    dynamicContacts.add(c);
                }

                // Merge static and dynamic, avoiding duplicates by id
                List<chat> merged = new ArrayList<>(buildStaticChats());
                for (chat c : dynamicContacts) {
                    if (!containsId(merged, c.getId())) {
                        merged.add(c);
                    }
                }

                chatList.clear();
                chatList.addAll(merged);
                if (chatAdapter != null) {
                    chatAdapter.notifyDataSetChanged();
                }
                Log.d("Fragment4", "Merged static (" + buildStaticChats().size() + ") + dynamic (" + dynamicContacts.size() + ") => total " + merged.size());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Fragment4", "Failed to load contacts: " + error.getMessage());
                Toast.makeText(getContext(), "Failed to load contacts: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }

            private String getString(DataSnapshot snap, String key) {
                Object val = snap.child(key).getValue();
                return val != null ? String.valueOf(val) : null;
            }
        });
    }
    
    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        chatsRecyclerView.setLayoutManager(layoutManager);
        
        chatAdapter = new screen4RVAdaptor(getContext(), chatList, this);
        chatsRecyclerView.setAdapter(chatAdapter);
        
        Log.d("Fragment4", "RecyclerView set up with adapter");
    }
    
    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (chatAdapter != null) {
                    chatAdapter.getFilter().filter(s);
                }
            }
            
            @Override
            public void afterTextChanged(Editable s) {
                // Not needed
            }
        });
    }
    
    private void handleNewMessageClick() {
        Log.d("Fragment4", "Handling New Message click");
        
        // Check if user is authenticated
        if (currentUser == null) {
            Toast.makeText(getContext(), "Please login to send messages", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            // Navigate to general chat activity for real-time messaging
            Intent intent = new Intent(getActivity(), ChatActivity.class);
            intent.putExtra("chat_name", "New Chat");
            intent.putExtra("chat_id", "general");
            startActivity(intent);
        } catch (Exception e) {
            Log.e("Fragment4", "Error opening chat activity", e);
            Toast.makeText(getContext(), "Chat feature error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
