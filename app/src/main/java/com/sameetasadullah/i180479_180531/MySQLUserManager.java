package com.sameetasadullah.i180479_180531;

import android.content.Context;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * MySQL User Manager - Connects Firebase Auth with MySQL Database
 * 
 * Features:
 * - Firebase Authentication for login/register
 * - MySQL message_database for user search and messaging
 * - Auto-sync users between Firebase and MySQL
 */
public class MySQLUserManager {
    
    private static final String TAG = "MySQLUserManager";
    
    // API Configuration - Update this to your server IP
    private static final String BASE_URL = "http://10.48.121.125:5000"; // Hybrid user service on PC IPv4
    private static final String SYNC_ENDPOINT = "/api/user/sync";
    private static final String SEARCH_ENDPOINT = "/api/user/search";
    private static final String PROFILE_ENDPOINT = "/api/user/profile";
    private static final String STATUS_ENDPOINT = "/api/user/status";
    
    private OkHttpClient httpClient;
    private FirebaseAuth firebaseAuth;
    private ExecutorService executorService;
    private Context context;
    
    // Callback interfaces
    public interface UserSyncCallback {
        void onSuccess(User user);
        void onFailure(String error);
    }
    
    public interface UserSearchCallback {
        void onUsersFound(List<User> users);
        void onFailure(String error);
    }
    
    public interface UserProfileCallback {
        void onProfileLoaded(User user);
        void onFailure(String error);
    }
    
    public interface StatusUpdateCallback {
        void onSuccess();
        void onFailure(String error);
    }
    
    public MySQLUserManager(Context context) {
        this.context = context;
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .build();
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.executorService = Executors.newFixedThreadPool(3);
        
        Log.d(TAG, "🔥 MySQL User Manager initialized");
        Log.d(TAG, "🌐 Server URL: " + BASE_URL);
    }
    
    /**
     * Sync Firebase authenticated user to MySQL database
     * Call this after successful Firebase login/register
     */
    public void syncFirebaseUser(UserSyncCallback callback) {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        
        if (firebaseUser == null) {
            callback.onFailure("No Firebase user authenticated");
            return;
        }
        
        Log.d(TAG, "🔄 Syncing Firebase user to MySQL: " + firebaseUser.getEmail());
        
        executorService.execute(() -> {
            try {
                // Prepare user data for sync
                JSONObject userJson = new JSONObject();
                userJson.put("firebase_uid", firebaseUser.getUid());
                userJson.put("email", firebaseUser.getEmail());
                
                String displayName = firebaseUser.getDisplayName();
                if (displayName == null || displayName.trim().isEmpty()) {
                    String email = firebaseUser.getEmail();
                    displayName = (email != null && email.contains("@")) ? 
                                 email.substring(0, email.indexOf("@")) : "User";
                }
                userJson.put("display_name", displayName);
                
                userJson.put("phone_number", firebaseUser.getPhoneNumber() != null ? 
                            firebaseUser.getPhoneNumber() : "");
                userJson.put("profile_image_url", firebaseUser.getPhotoUrl() != null ? 
                            firebaseUser.getPhotoUrl().toString() : "");
                userJson.put("bio", "Hey there! I am using Bisto Chat.");
                
                Log.d(TAG, "📤 Sending user data: " + userJson.toString());
                
                RequestBody body = RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    userJson.toString()
                );
                
                Request request = new Request.Builder()
                    .url(BASE_URL + SYNC_ENDPOINT)
                    .post(body)
                    .build();
                
                httpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, java.io.IOException e) {
                        Log.e(TAG, "❌ Sync request failed", e);
                        runOnUiThread(() -> callback.onFailure("Network error: " + e.getMessage()));
                    }
                    
                    @Override
                    public void onResponse(Call call, Response response) {
                        try {
                            String responseBody = response.body().string();
                            Log.d(TAG, "📡 Sync response: " + responseBody);
                            
                            if (response.isSuccessful()) {
                                JSONObject jsonResponse = new JSONObject(responseBody);
                                
                                if ("success".equals(jsonResponse.getString("status"))) {
                                    JSONObject userObject = jsonResponse.getJSONObject("user");
                                    User user = parseUserFromJson(userObject);
                                    
                                    Log.d(TAG, "✅ User synced successfully: " + user.getEmail());
                                    runOnUiThread(() -> callback.onSuccess(user));
                                } else {
                                    String error = jsonResponse.optString("error", "Unknown error");
                                    Log.e(TAG, "❌ Sync failed: " + error);
                                    runOnUiThread(() -> callback.onFailure(error));
                                }
                            } else {
                                Log.e(TAG, "❌ Sync request failed with code: " + response.code());
                                runOnUiThread(() -> callback.onFailure("Server error: " + response.code()));
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "❌ Error parsing sync response", e);
                            runOnUiThread(() -> callback.onFailure("Response parsing error: " + e.getMessage()));
                        }
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "❌ Error creating sync request", e);
                callback.onFailure("Request creation error: " + e.getMessage());
            }
        });
    }
    
    /**
     * Search for users in MySQL database
     * Used by SearchContactsActivity
     */
    public void searchUsers(String query, UserSearchCallback callback) {
        if (query == null || query.trim().length() < 2) {
            callback.onFailure("Query must be at least 2 characters long");
            return;
        }
        
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        String currentUserUid = currentUser != null ? currentUser.getUid() : "";
        
        Log.d(TAG, "🔍 Searching for users: '" + query + "'");
        
        executorService.execute(() -> {
            try {
                String encodedQuery = java.net.URLEncoder.encode(query, "UTF-8");
                String encodedCurrentUser = java.net.URLEncoder.encode(currentUserUid, "UTF-8");
                String url = BASE_URL + SEARCH_ENDPOINT + "?q=" + encodedQuery + "&current_user=" + encodedCurrentUser;
                
                Log.d(TAG, "📤 Search URL: " + url);
                
                Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();
                
                httpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, java.io.IOException e) {
                        Log.e(TAG, "❌ Search request failed", e);
                        runOnUiThread(() -> callback.onFailure("Network error: " + e.getMessage()));
                    }
                    
                    @Override
                    public void onResponse(Call call, Response response) {
                        try {
                            String responseBody = response.body().string();
                            Log.d(TAG, "📡 Search response: " + responseBody);
                            
                            if (response.isSuccessful()) {
                                JSONObject jsonResponse = new JSONObject(responseBody);
                                
                                if ("success".equals(jsonResponse.getString("status"))) {
                                    JSONArray usersArray = jsonResponse.getJSONArray("users");
                                    List<User> users = new ArrayList<>();
                                    
                                    for (int i = 0; i < usersArray.length(); i++) {
                                        JSONObject userObject = usersArray.getJSONObject(i);
                                        User user = parseUserFromJson(userObject);
                                        users.add(user);
                                    }
                                    
                                    Log.d(TAG, "✅ Search found " + users.size() + " users");
                                    runOnUiThread(() -> callback.onUsersFound(users));
                                } else {
                                    String error = jsonResponse.optString("error", "Unknown error");
                                    Log.e(TAG, "❌ Search failed: " + error);
                                    runOnUiThread(() -> callback.onFailure(error));
                                }
                            } else {
                                Log.e(TAG, "❌ Search request failed with code: " + response.code());
                                runOnUiThread(() -> callback.onFailure("Server error: " + response.code()));
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "❌ Error parsing search response", e);
                            runOnUiThread(() -> callback.onFailure("Response parsing error: " + e.getMessage()));
                        }
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "❌ Error creating search request", e);
                callback.onFailure("Request creation error: " + e.getMessage());
            }
        });
    }
    
    /**
     * Update user online/offline status
     */
    public void updateUserStatus(String status, StatusUpdateCallback callback) {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        
        if (firebaseUser == null) {
            callback.onFailure("No Firebase user authenticated");
            return;
        }
        
        if (!status.equals("online") && !status.equals("offline") && !status.equals("away")) {
            callback.onFailure("Invalid status. Must be: online, offline, or away");
            return;
        }
        
        Log.d(TAG, "📱 Updating user status to: " + status);
        
        executorService.execute(() -> {
            try {
                JSONObject statusJson = new JSONObject();
                statusJson.put("firebase_uid", firebaseUser.getUid());
                statusJson.put("status", status);
                
                RequestBody body = RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    statusJson.toString()
                );
                
                Request request = new Request.Builder()
                    .url(BASE_URL + STATUS_ENDPOINT)
                    .put(body)
                    .build();
                
                httpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, java.io.IOException e) {
                        Log.e(TAG, "❌ Status update request failed", e);
                        runOnUiThread(() -> callback.onFailure("Network error: " + e.getMessage()));
                    }
                    
                    @Override
                    public void onResponse(Call call, Response response) {
                        try {
                            String responseBody = response.body().string();
                            Log.d(TAG, "📡 Status update response: " + responseBody);
                            
                            if (response.isSuccessful()) {
                                JSONObject jsonResponse = new JSONObject(responseBody);
                                
                                if ("success".equals(jsonResponse.getString("status"))) {
                                    Log.d(TAG, "✅ Status updated to: " + status);
                                    runOnUiThread(() -> callback.onSuccess());
                                } else {
                                    String error = jsonResponse.optString("error", "Unknown error");
                                    Log.e(TAG, "❌ Status update failed: " + error);
                                    runOnUiThread(() -> callback.onFailure(error));
                                }
                            } else {
                                Log.e(TAG, "❌ Status update request failed with code: " + response.code());
                                runOnUiThread(() -> callback.onFailure("Server error: " + response.code()));
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "❌ Error parsing status response", e);
                            runOnUiThread(() -> callback.onFailure("Response parsing error: " + e.getMessage()));
                        }
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "❌ Error creating status request", e);
                callback.onFailure("Request creation error: " + e.getMessage());
            }
        });
    }
    
    /**
     * Parse User object from JSON response
     */
    private User parseUserFromJson(JSONObject userJson) throws Exception {
        User user = new User();
        user.setUid(userJson.getString("uid"));
        user.setEmail(userJson.getString("email"));
        user.setDisplayName(userJson.getString("display_name"));
        user.setPhoneNumber(userJson.optString("phone_number", ""));
        user.setProfileImageUrl(userJson.optString("profile_image_url", ""));
        user.setBio(userJson.optString("bio", ""));
        user.setStatus(userJson.optString("status", "offline"));
        
        return user;
    }
    
    /**
     * Run code on UI thread
     */
    private void runOnUiThread(Runnable runnable) {
        if (context instanceof android.app.Activity) {
            ((android.app.Activity) context).runOnUiThread(runnable);
        } else {
            // Fallback for non-Activity contexts
            android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            mainHandler.post(runnable);
        }
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}