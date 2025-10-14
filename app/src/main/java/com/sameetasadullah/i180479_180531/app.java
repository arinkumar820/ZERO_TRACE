package com.sameetasadullah.i180479_180531;

import android.app.Application;

// // import com.google.firebase.database.FirebaseDatabase; // Removed for Supabase // Removed for Supabase migration
import com.squareup.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

public class app extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        //         // FirebaseDatabase.getInstance().setPersistenceEnabled(true); // TODO: Replace with Supabase initialization.Trim() // TODO: Replace with Supabase database call

        Picasso.Builder builder = new Picasso.Builder(this);
        builder.downloader(new OkHttp3Downloader(this, Integer.MAX_VALUE));
        Picasso built = builder.build();
        built.setIndicatorsEnabled(false);
        built.setLoggingEnabled(true);
        Picasso.setSingletonInstance(built);
    }
}
