package com.sameetasadullah.i180479_180531;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.content.Intent;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

public class screen5 extends AppCompatActivity {

    RecyclerView recyclerView;
    EditText message;
    ImageView cameraImage, backButton, makeCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen5);

        // Initialize views with correct IDs from layout
        recyclerView = findViewById(R.id.rv_messages);
        message = findViewById(R.id.message);
        cameraImage = findViewById(R.id.camera_image);
        backButton = findViewById(R.id.back_button);
        makeCall = findViewById(R.id.make_call);

        // TODO: Replace Firebase functionality with Supabase
        // Previous Firebase message loading code has been removed for Supabase migration

        setupRecyclerView();
        setupClickListeners();
    }

    private void setupRecyclerView() {
        if (recyclerView != null) {
            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(screen5.this);
            recyclerView.setLayoutManager(layoutManager);
            // TODO: Set adapter with Supabase data
        }
    }

    private void setupClickListeners() {
        if (backButton != null) {
            backButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    finish();
                }
            });
        }

        if (makeCall != null) {
            makeCall.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // TODO: Implement call functionality
                    Toast.makeText(screen5.this, "Call functionality to be implemented", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (cameraImage != null) {
            cameraImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // TODO: Implement image selection with Supabase storage
                    Intent intent = new Intent();
                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    startActivityForResult(Intent.createChooser(intent, "Select Picture"), 1);
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            // TODO: Handle image upload with Supabase storage
            Toast.makeText(this, "Image upload to be implemented with Supabase", Toast.LENGTH_SHORT).show();
        }
    }
}

