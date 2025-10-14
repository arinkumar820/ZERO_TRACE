package com.sameetasadullah.i180479_180531;

import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.content.Intent;
import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class fragment_screen6 extends Fragment {

    private RelativeLayout newContactButton, newGroupButton;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_screen6, container, false);
        
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        
        // Initialize UI components
        newContactButton = view.findViewById(R.id.rl_new_contact);
        newGroupButton = view.findViewById(R.id.rl_new_group);
        
        Log.d("Fragment6", "UI Components - newContactButton: " + (newContactButton != null) + 
              ", newGroupButton: " + (newGroupButton != null));
        
        // Set up New Contact button
        if (newContactButton != null) {
            newContactButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d("Fragment6", "New Contact button clicked!");
                    handleNewContactClick();
                }
            });
        } else {
            Log.e("Fragment6", "New Contact button not found in layout!");
        }
        
        // Set up New Group button
        if (newGroupButton != null) {
            newGroupButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d("Fragment6", "New Group button clicked!");
                    handleNewGroupClick();
                }
            });
        } else {
            Log.e("Fragment6", "New Group button not found in layout!");
        }
        
        return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // TODO: Add Supabase functionality
    }
    
    public void applicationNotMinimized() {
        // TODO: Add Firebase functionality if needed
    }
    
    private void handleNewContactClick() {
        Log.d("Fragment6", "Handling New Contact click");
        
        // Check if user is authenticated
        if (currentUser == null) {
            Toast.makeText(getContext(), "Please login to add contacts", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            // Navigate to search contacts activity
            Intent intent = new Intent(getActivity(), SearchContactsActivity.class);
            startActivity(intent);
            Log.d("Fragment6", "Launched SearchContactsActivity");
        } catch (Exception e) {
            Log.e("Fragment6", "Error opening search contacts activity", e);
            Toast.makeText(getContext(), "Unable to open contact search: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void handleNewGroupClick() {
        Log.d("Fragment6", "Handling New Group click");
        
        // Check if user is authenticated
        if (currentUser == null) {
            Toast.makeText(getContext(), "Please login to create groups", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            // Navigate to create group activity
            Intent intent = new Intent(getActivity(), createGroup.class);
            startActivity(intent);
        } catch (Exception e) {
            Log.e("Fragment6", "Error opening create group activity", e);
            Toast.makeText(getContext(), "Group creation feature coming soon!", Toast.LENGTH_SHORT).show();
        }
    }
}
