package com.sameetasadullah.i180479_180531;

import androidx.recyclerview.widget.RecyclerView;
import androidx.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.RelativeLayout;
import android.content.Context;
import android.content.Intent;
import java.util.List;
import de.hdodenhof.circleimageview.CircleImageView;
import com.squareup.picasso.Picasso;

public class screen6RVAdaptor extends RecyclerView.Adapter<screen6RVAdaptor.screen6ViewHolder> {

    private List<contact> contactList;
    private Context context;
    private fragment_screen6 fragment;

    public screen6RVAdaptor(Context context, List<contact> contactList, fragment_screen6 fragment) {
        this.context = context;
        this.contactList = contactList;
        this.fragment = fragment;
        
        // TODO: Replace Firebase functionality with Supabase
        // Previous Firebase contact loading code has been removed for Supabase migration
    }

    @NonNull
    @Override
    public screen6ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.contact_row, parent, false);
        return new screen6ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull screen6ViewHolder holder, int position) {
        // TODO: Bind contact data from Supabase
        holder.number.setText(contactList.get(position).getNumber());
        Picasso.get().load(contactList.get(position).getDp()).into(holder.dp);

        holder.rl_contact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: Navigate to chat screen with Supabase data
                Intent intent = new Intent(context, screen5.class);
                intent.putExtra("contactInfo", "TODO: Pass contact data");
                fragment.applicationNotMinimized();
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return contactList.size();
    }

    public static class screen6ViewHolder extends RecyclerView.ViewHolder {
        TextView number;
        CircleImageView dp;
        RelativeLayout rl_contact;

        public screen6ViewHolder(@NonNull View itemView) {
            super(itemView);
            number = itemView.findViewById(R.id.number);
            dp = itemView.findViewById(R.id.dp);
            rl_contact = itemView.findViewById(R.id.rl_contact);
        }
    }
}
