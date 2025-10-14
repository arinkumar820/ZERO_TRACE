package com.sameetasadullah.i180479_180531;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Local SQLite database for storing user data
 * Uses Firebase Auth for authentication, but stores user profiles locally
 */
public class LocalUserDatabase extends SQLiteOpenHelper {
    
    private static final String TAG = "LocalUserDB";
    
    // Database Info
    private static final String DATABASE_NAME = "BistoChat.db";
    private static final int DATABASE_VERSION = 1;
    
    // Table Names
    private static final String TABLE_USERS = "users";
    private static final String TABLE_CONTACTS = "contacts";
    
    // User Table Columns
    private static final String KEY_UID = "uid";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_DISPLAY_NAME = "display_name";
    private static final String KEY_PHONE_NUMBER = "phone_number";
    private static final String KEY_BIO = "bio";
    private static final String KEY_PROFILE_IMAGE_URL = "profile_image_url";
    private static final String KEY_STATUS = "status";
    private static final String KEY_LAST_SEEN = "last_seen";
    private static final String KEY_CREATED_AT = "created_at";
    private static final String KEY_UPDATED_AT = "updated_at";
    
    // Contacts Table Columns (for storing user's contacts)
    private static final String KEY_CONTACT_ID = "contact_id";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_CONTACT_UID = "contact_uid";
    private static final String KEY_ADDED_AT = "added_at";
    
    private static LocalUserDatabase instance;
    
    public static synchronized LocalUserDatabase getInstance(Context context) {
        if (instance == null) {
            instance = new LocalUserDatabase(context.getApplicationContext());
        }
        return instance;
    }
    
    private LocalUserDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create Users Table
        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + "("
                + KEY_UID + " TEXT PRIMARY KEY,"
                + KEY_EMAIL + " TEXT NOT NULL,"
                + KEY_DISPLAY_NAME + " TEXT NOT NULL,"
                + KEY_PHONE_NUMBER + " TEXT,"
                + KEY_BIO + " TEXT,"
                + KEY_PROFILE_IMAGE_URL + " TEXT,"
                + KEY_STATUS + " TEXT DEFAULT 'offline',"
                + KEY_LAST_SEEN + " INTEGER,"
                + KEY_CREATED_AT + " INTEGER,"
                + KEY_UPDATED_AT + " INTEGER"
                + ")";
        
        // Create Contacts Table
        String CREATE_CONTACTS_TABLE = "CREATE TABLE " + TABLE_CONTACTS + "("
                + KEY_CONTACT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_USER_ID + " TEXT NOT NULL,"
                + KEY_CONTACT_UID + " TEXT NOT NULL,"
                + KEY_ADDED_AT + " INTEGER,"
                + "FOREIGN KEY(" + KEY_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + KEY_UID + "),"
                + "FOREIGN KEY(" + KEY_CONTACT_UID + ") REFERENCES " + TABLE_USERS + "(" + KEY_UID + "),"
                + "UNIQUE(" + KEY_USER_ID + ", " + KEY_CONTACT_UID + ")"
                + ")";
        
        db.execSQL(CREATE_USERS_TABLE);
        db.execSQL(CREATE_CONTACTS_TABLE);
        
        Log.d(TAG, "Database tables created successfully");
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older tables if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONTACTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        
        // Create tables again
        onCreate(db);
    }
    
    /**
     * Add or update a user in local database
     */
    public long addOrUpdateUser(User user) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put(KEY_UID, user.getUid());
        values.put(KEY_EMAIL, user.getEmail());
        values.put(KEY_DISPLAY_NAME, user.getDisplayName());
        values.put(KEY_PHONE_NUMBER, user.getPhoneNumber());
        values.put(KEY_BIO, user.getBio());
        values.put(KEY_PROFILE_IMAGE_URL, user.getProfileImageUrl());
        values.put(KEY_STATUS, user.getStatus());
        values.put(KEY_LAST_SEEN, user.getLastSeen());
        values.put(KEY_UPDATED_AT, System.currentTimeMillis());
        
        // Check if user exists
        if (getUserByUid(user.getUid()) != null) {
            // Update existing user
            int result = db.update(TABLE_USERS, values, KEY_UID + " = ?", new String[]{user.getUid()});
            Log.d(TAG, "Updated user: " + user.getEmail() + ", rows affected: " + result);
            return result;
        } else {
            // Insert new user
            values.put(KEY_CREATED_AT, System.currentTimeMillis());
            long id = db.insert(TABLE_USERS, null, values);
            Log.d(TAG, "Added new user: " + user.getEmail() + ", id: " + id);
            return id;
        }
    }
    
    /**
     * Get user by UID
     */
    public User getUserByUid(String uid) {
        SQLiteDatabase db = this.getReadableDatabase();
        
        Cursor cursor = db.query(TABLE_USERS, null, KEY_UID + " = ?", 
                new String[]{uid}, null, null, null);
        
        if (cursor != null && cursor.moveToFirst()) {
            User user = cursorToUser(cursor);
            cursor.close();
            return user;
        }
        
        if (cursor != null) {
            cursor.close();
        }
        return null;
    }
    
    /**
     * Search users by email or display name (case-insensitive)
     */
    public List<User> searchUsers(String query, String currentUserId) {
        List<User> userList = new ArrayList<>();
        
        if (query == null || query.trim().length() < 2) {
            return userList;
        }
        
        SQLiteDatabase db = this.getReadableDatabase();
        String searchQuery = "%" + query.toLowerCase() + "%";
        
        String sql = "SELECT * FROM " + TABLE_USERS + " WHERE " +
                "(" + KEY_EMAIL + " LIKE ? OR " + KEY_DISPLAY_NAME + " LIKE ?) " +
                "AND " + KEY_UID + " != ? " +
                "ORDER BY " + KEY_DISPLAY_NAME + " ASC";
        
        Cursor cursor = db.rawQuery(sql, new String[]{searchQuery, searchQuery, currentUserId});
        
        if (cursor.moveToFirst()) {
            do {
                User user = cursorToUser(cursor);
                userList.add(user);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        Log.d(TAG, "Found " + userList.size() + " users matching: " + query);
        
        return userList;
    }
    
    /**
     * Get all users except current user
     */
    public List<User> getAllUsers(String currentUserId) {
        List<User> userList = new ArrayList<>();
        
        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT * FROM " + TABLE_USERS + " WHERE " + KEY_UID + " != ? ORDER BY " + KEY_DISPLAY_NAME + " ASC";
        
        Cursor cursor = db.rawQuery(sql, new String[]{currentUserId});
        
        if (cursor.moveToFirst()) {
            do {
                User user = cursorToUser(cursor);
                userList.add(user);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        return userList;
    }
    
    /**
     * Add contact relationship
     */
    public boolean addContact(String userId, String contactUid) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put(KEY_USER_ID, userId);
        values.put(KEY_CONTACT_UID, contactUid);
        values.put(KEY_ADDED_AT, System.currentTimeMillis());
        
        try {
            long id = db.insert(TABLE_CONTACTS, null, values);
            Log.d(TAG, "Added contact relationship: " + userId + " -> " + contactUid);
            return id != -1;
        } catch (Exception e) {
            Log.e(TAG, "Error adding contact", e);
            return false;
        }
    }
    
    /**
     * Get user's contacts
     */
    public List<User> getUserContacts(String userId) {
        List<User> contacts = new ArrayList<>();
        
        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT u.* FROM " + TABLE_USERS + " u " +
                "INNER JOIN " + TABLE_CONTACTS + " c ON u." + KEY_UID + " = c." + KEY_CONTACT_UID + " " +
                "WHERE c." + KEY_USER_ID + " = ? " +
                "ORDER BY u." + KEY_DISPLAY_NAME + " ASC";
        
        Cursor cursor = db.rawQuery(sql, new String[]{userId});
        
        if (cursor.moveToFirst()) {
            do {
                User user = cursorToUser(cursor);
                contacts.add(user);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        return contacts;
    }
    
    /**
     * Delete user
     */
    public boolean deleteUser(String uid) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        // Delete user's contact relationships
        db.delete(TABLE_CONTACTS, KEY_USER_ID + " = ? OR " + KEY_CONTACT_UID + " = ?", 
                new String[]{uid, uid});
        
        // Delete user
        int result = db.delete(TABLE_USERS, KEY_UID + " = ?", new String[]{uid});
        
        Log.d(TAG, "Deleted user: " + uid + ", rows affected: " + result);
        return result > 0;
    }
    
    /**
     * Get total user count
     */
    public int getUserCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_USERS, null);
        
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }
    
    /**
     * Convert cursor to User object
     */
    private User cursorToUser(Cursor cursor) {
        String uid = cursor.getString(cursor.getColumnIndexOrThrow(KEY_UID));
        String email = cursor.getString(cursor.getColumnIndexOrThrow(KEY_EMAIL));
        String displayName = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DISPLAY_NAME));
        
        User user = new User(uid, email, displayName);
        user.setPhoneNumber(cursor.getString(cursor.getColumnIndexOrThrow(KEY_PHONE_NUMBER)));
        user.setBio(cursor.getString(cursor.getColumnIndexOrThrow(KEY_BIO)));
        user.setProfileImageUrl(cursor.getString(cursor.getColumnIndexOrThrow(KEY_PROFILE_IMAGE_URL)));
        user.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(KEY_STATUS)));
        user.setLastSeen(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_LAST_SEEN)));
        
        return user;
    }
    
    /**
     * Clear all data (for testing)
     */
    public void clearAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_CONTACTS, null, null);
        db.delete(TABLE_USERS, null, null);
        Log.d(TAG, "All data cleared");
    }
}