package com.sameetasadullah.i180479_180531;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * Sends security alerts to the WebSocket server (one-off, short-lived connection).
 */
public class SecurityAlertSender {
    private static final String TAG = "SecurityAlertSender";
    // Use the same server as chat (adjust if needed)
    private static final String SERVER_URL = "ws://10.48.121.125:8081";

    private static OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build();

    public static void sendVerificationFailure(Context context) {
        try {
            String androidId = "unknown";
            try {
                androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            } catch (Exception ignored) { }

            JSONObject payload = new JSONObject();
            payload.put("type", "danger");
            payload.put("event", "verification_failed");
            payload.put("device_id", androidId != null ? androidId : "unknown");
            payload.put("timestamp", System.currentTimeMillis());

            Request request = new Request.Builder().url(SERVER_URL).build();
            final String message = payload.toString();

            client.newWebSocket(request, new WebSocketListener() {
                @Override
                public void onOpen(WebSocket webSocket, Response response) {
                    try {
                        webSocket.send(message);
                        Log.d(TAG, "Sent security alert: " + message);
                        // Close shortly after send
                        webSocket.close(1000, "done");
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to send security alert", e);
                    }
                }

                @Override
                public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                    Log.e(TAG, "Security alert WS failure", t);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error constructing security alert", e);
        }
    }
}