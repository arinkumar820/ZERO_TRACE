package com.sameetasadullah.i180479_180531;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * HTTP client for communicating with the Python database server
 */
public class ApiClient {
    
    private static final String TAG = "ApiClient";
    private static final String BASE_URL = "http://10.48.121.125:8080"; // PC IPv4 for physical device
    // Emulator: "http://10.0.2.2:8080" | Real device: replace with your PC IPv4
    
    private static final int TIMEOUT_MS = 10000; // 10 seconds
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    
    // Response callback interface
    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String error);
    }
    
    // List response callback interface
    public interface ApiListCallback {
        void onSuccess(JSONArray response);
        void onError(String error);
    }
    
    /**
     * Make HTTP GET request
     */
    public static void get(String endpoint, ApiCallback callback) {
        executor.execute(() -> {
            try {
                String response = makeHttpRequest("GET", endpoint, null);
                JSONObject jsonResponse = new JSONObject(response);
                callback.onSuccess(jsonResponse);
            } catch (Exception e) {
                Log.e(TAG, "GET request failed: " + e.getMessage());
                callback.onError(e.getMessage());
            }
        });
    }
    
    /**
     * Make HTTP POST request
     */
    public static void post(String endpoint, JSONObject data, ApiCallback callback) {
        executor.execute(() -> {
            try {
                String response = makeHttpRequest("POST", endpoint, data);
                JSONObject jsonResponse = new JSONObject(response);
                callback.onSuccess(jsonResponse);
            } catch (Exception e) {
                Log.e(TAG, "POST request failed: " + e.getMessage());
                callback.onError(e.getMessage());
            }
        });
    }
    
    /**
     * Make HTTP PUT request
     */
    public static void put(String endpoint, JSONObject data, ApiCallback callback) {
        executor.execute(() -> {
            try {
                String response = makeHttpRequest("PUT", endpoint, data);
                JSONObject jsonResponse = new JSONObject(response);
                callback.onSuccess(jsonResponse);
            } catch (Exception e) {
                Log.e(TAG, "PUT request failed: " + e.getMessage());
                callback.onError(e.getMessage());
            }
        });
    }
    
    /**
     * Core HTTP request method
     */
    private static String makeHttpRequest(String method, String endpoint, JSONObject data) 
            throws IOException, JSONException {
        
        URL url = new URL(BASE_URL + endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        try {
            // Configure connection
            connection.setRequestMethod(method);
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            
            // Add request body for POST/PUT
            if (data != null && ("POST".equals(method) || "PUT".equals(method))) {
                connection.setDoOutput(true);
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = data.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }
            }
            
            // Get response
            int responseCode = connection.getResponseCode();
            InputStream inputStream;
            
            if (responseCode >= 200 && responseCode < 300) {
                inputStream = connection.getInputStream();
            } else {
                inputStream = connection.getErrorStream();
            }
            
            // Read response
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }
            
            String responseString = response.toString();
            Log.d(TAG, method + " " + endpoint + " -> " + responseCode + ": " + responseString);
            
            if (responseCode >= 400) {
                throw new IOException("HTTP " + responseCode + ": " + responseString);
            }
            
            return responseString;
            
        } finally {
            connection.disconnect();
        }
    }
    
    // API Methods for specific endpoints
    
    /**
     * Register a new user
     */
    public static void registerUser(String email, String password, String displayName, 
                                  String phoneNumber, String bio, ApiCallback callback) {
        try {
            JSONObject data = new JSONObject();
            data.put("email", email);
            data.put("password", password);
            data.put("display_name", displayName);
            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                data.put("phone_number", phoneNumber);
            }
            if (bio != null && !bio.isEmpty()) {
                data.put("bio", bio);
            }
            
            post("/api/auth/register", data, callback);
            
        } catch (JSONException e) {
            callback.onError("Failed to create request: " + e.getMessage());
        }
    }
    
    /**
     * Login user
     */
    public static void loginUser(String email, String password, ApiCallback callback) {
        try {
            JSONObject data = new JSONObject();
            data.put("email", email);
            data.put("password", password);
            
            post("/api/auth/login", data, callback);
            
        } catch (JSONException e) {
            callback.onError("Failed to create request: " + e.getMessage());
        }
    }
    
    /**
     * Search users
     */
    public static void searchUsers(String query, String currentUserId, ApiCallback callback) {
        String endpoint = "/api/users/search?q=" + query;
        if (currentUserId != null && !currentUserId.isEmpty()) {
            endpoint += "&current_uid=" + currentUserId;
        }
        
        get(endpoint, callback);
    }
    
    /**
     * Add contact
     */
    public static void addContact(String userUid, String contactUid, String contactName, ApiCallback callback) {
        try {
            JSONObject data = new JSONObject();
            data.put("user_uid", userUid);
            data.put("contact_uid", contactUid);
            if (contactName != null && !contactName.isEmpty()) {
                data.put("contact_name", contactName);
            }
            
            post("/api/contacts", data, callback);
            
        } catch (JSONException e) {
            callback.onError("Failed to create request: " + e.getMessage());
        }
    }
    
    /**
     * Get user contacts
     */
    public static void getUserContacts(String userUid, ApiCallback callback) {
        get("/api/contacts/" + userUid, callback);
    }
    
    /**
     * Send message
     */
    public static void sendMessage(String senderUid, String receiverUid, String messageText, 
                                 String messageType, ApiCallback callback) {
        try {
            JSONObject data = new JSONObject();
            data.put("sender_uid", senderUid);
            data.put("receiver_uid", receiverUid);
            data.put("message_text", messageText);
            if (messageType != null && !messageType.isEmpty()) {
                data.put("message_type", messageType);
            }
            
            post("/api/messages", data, callback);
            
        } catch (JSONException e) {
            callback.onError("Failed to create request: " + e.getMessage());
        }
    }
    
    /**
     * Get messages between two users
     */
    public static void getMessages(String user1Uid, String user2Uid, int limit, int offset, ApiCallback callback) {
        String endpoint = String.format("/api/messages/%s/%s?limit=%d&offset=%d", 
                                      user1Uid, user2Uid, limit, offset);
        get(endpoint, callback);
    }
    
    /**
     * Update user status
     */
    public static void updateUserStatus(String userUid, String status, ApiCallback callback) {
        try {
            JSONObject data = new JSONObject();
            data.put("status", status);
            
            put("/api/users/" + userUid + "/status", data, callback);
            
        } catch (JSONException e) {
            callback.onError("Failed to create request: " + e.getMessage());
        }
    }
    
    /**
     * Test server connectivity
     */
    public static void testConnection(ApiCallback callback) {
        get("/", callback);
    }
    
    /**
     * Get server statistics (admin)
     */
    public static void getServerStats(ApiCallback callback) {
        get("/api/admin/stats", callback);
    }
    
    /**
     * Reset database (admin/development)
     */
    public static void resetDatabase(ApiCallback callback) {
        post("/api/admin/reset-db", new JSONObject(), callback);
    }
}