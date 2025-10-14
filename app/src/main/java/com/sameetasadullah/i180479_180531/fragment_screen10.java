package com.sameetasadullah.i180479_180531;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

// import com.google.firebase.auth.FirebaseAuth; // Removed for Supabase migration
// import com.google.firebase.database.DatabaseReference; // Removed for Supabase migration
// import com.google.firebase.database.FirebaseDatabase; // Removed for Supabase migration

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class fragment_screen10 extends Fragment {
    RecyclerView recyclerView;
    screen10RVAdaptor adaptor;
    List<call> callList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_screen10, container, false);

        recyclerView = view.findViewById(R.id.rv_calls);

        callList = new ArrayList<>();
        // Updated names per request
        callList.add(new call("Arin", "inbound", "10:21", false));
        callList.add(new call("Deepak", "lost", "22:14", true));
        callList.add(new call("Kalpa", "inbound", "monday", false));
        callList.add(new call("Prachi", "lost", "friday", true));

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        adaptor = new screen10RVAdaptor(getActivity(), callList, fragment_screen10.this);
        recyclerView.setAdapter(adaptor);
        recyclerView.addItemDecoration(new VerticalSpaceItemDecoration(70));

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        ((fragmentsContainer)getActivity()).changeImageColorToBlue(2);
    }

    public void applicationNotMinimized() {
        ((fragmentsContainer)getActivity()).minimized = false;
    }
}
