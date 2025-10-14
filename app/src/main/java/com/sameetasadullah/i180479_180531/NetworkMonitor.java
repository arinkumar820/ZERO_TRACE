package com.sameetasadullah.i180479_180531;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Monitors network connectivity and notifies registered listeners.
 */
public final class NetworkMonitor {
    private static final String TAG = "NetworkMonitor";

    public interface Listener {
        void onNetworkAvailable();
        void onNetworkLost();
    }

    private static ConnectivityManager cm;
    private static final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static ConnectivityManager.NetworkCallback callback;
    private static volatile boolean initialized = false;

    private NetworkMonitor() {}

    public static synchronized void init(Context context) {
        if (initialized) return;
        Context app = context.getApplicationContext();
        cm = (ConnectivityManager) app.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            Log.w(TAG, "ConnectivityManager not available");
            initialized = true;
            return;
        }
        callback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                Log.d(TAG, "Network available");
                notifyAvailable();
            }

            @Override
            public void onLost(Network network) {
                Log.d(TAG, "Network lost");
                notifyLost();
            }
        };
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                cm.registerDefaultNetworkCallback(callback);
            } else {
                NetworkRequest req = new NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build();
                cm.registerNetworkCallback(req, callback);
            }
            initialized = true;
            Log.d(TAG, "NetworkMonitor initialized");
        } catch (Exception e) {
            Log.e(TAG, "Failed to register network callback", e);
            initialized = true; // Avoid re-trying endlessly
        }
    }

    public static void addListener(Listener l) {
        if (l == null) return;
        listeners.add(l);
        // Immediately dispatch current state to new listener
        if (isNetworkAvailable()) {
            mainHandler.post(l::onNetworkAvailable);
        } else {
            mainHandler.post(l::onNetworkLost);
        }
    }

    public static void removeListener(Listener l) {
        if (l == null) return;
        listeners.remove(l);
    }

    public static boolean isNetworkAvailable() {
        if (cm == null) return false;
        try {
            Network active = cm.getActiveNetwork();
            if (active == null) return false;
            NetworkCapabilities caps = cm.getNetworkCapabilities(active);
            return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } catch (Exception e) {
            return false;
        }
    }

    private static void notifyAvailable() {
        for (Listener l : listeners) {
            mainHandler.post(l::onNetworkAvailable);
        }
    }

    private static void notifyLost() {
        for (Listener l : listeners) {
            mainHandler.post(l::onNetworkLost);
        }
    }
}
