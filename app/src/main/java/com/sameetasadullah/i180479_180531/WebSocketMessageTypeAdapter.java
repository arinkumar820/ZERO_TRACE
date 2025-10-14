package com.sameetasadullah.i180479_180531;

import android.util.Log;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

/**
 * Custom TypeAdapter for WebSocketMessage to ensure createdAt is properly serialized and deserialized
 */
public class WebSocketMessageTypeAdapter extends TypeAdapter<WebSocketMessage> {
    private static final String TAG = "WebSocketMsgAdapter";

    @Override
    public void write(JsonWriter out, WebSocketMessage message) throws IOException {
        if (message == null) {
            out.nullValue();
            return;
        }

        out.beginObject();

        // Write all standard fields
        out.name("type").value(message.getType());
        
        if (message.getSender_uid() != null) {
            out.name("sender_uid").value(message.getSender_uid());
        }
        
        if (message.getSender_email() != null) {
            out.name("sender_email").value(message.getSender_email());
        }
        
        if (message.getMessage() != null) {
            out.name("message").value(message.getMessage());
        }
        
        if (message.getMessage_type() != null) {
            out.name("message_type").value(message.getMessage_type());
        }
        
        if (message.getDisplay_name() != null) {
            out.name("display_name").value(message.getDisplay_name());
        }
        
        if (message.getChat_room_id() != null) {
            out.name("chat_room_id").value(message.getChat_room_id());
        }
        
        if (message.getRoom_id() != null) {
            out.name("room_id").value(message.getRoom_id());
        }
        
        // Write timestamp
        out.name("timestamp").value(message.getTimestamp());
        
        // Always write createdAt to ensure it's preserved
        out.name("createdAt").value(message.getCreatedAt());
        
        out.name("message_id").value(message.getMessage_id());
        out.name("isDisappearing").value(message.isDisappearing());
        out.name("disappearAfterMs").value(message.getDisappearAfterMs());
        out.name("isExpired").value(message.isExpired());
        out.name("seen").value(message.isSeen());
        
        // Server compatibility fields
        if (message.getSender_uid() != null) {
            out.name("user_uid").value(message.getSender_uid());
        }
        
        if (message.getSender_email() != null) {
            out.name("user_email").value(message.getSender_email());
        }

        out.endObject();
    }

    @Override
    public WebSocketMessage read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }

        WebSocketMessage message = new WebSocketMessage();
        in.beginObject();

        while (in.hasNext()) {
            String name = in.nextName();
            
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                continue;
            }

            switch (name) {
                case "type":
                    message.setType(in.nextString());
                    break;
                case "sender_uid":
                    message.setSender_uid(in.nextString());
                    break;
                case "sender_email":
                    message.setSender_email(in.nextString());
                    break;
                case "message":
                    message.setMessage(in.nextString());
                    break;
                case "message_type":
                    message.setMessage_type(in.nextString());
                    break;
                case "display_name":
                    message.setDisplay_name(in.nextString());
                    break;
                case "chat_room_id":
                    message.setChat_room_id(in.nextString());
                    break;
                case "room_id":
                    message.setRoom_id(in.nextString());
                    break;
                case "timestamp":
                    if (in.peek() == JsonToken.NUMBER) {
                        message.setTimestamp(in.nextLong());
                    } else {
                        // Handle string timestamp
                        String timestampStr = in.nextString();
                        try {
                            // Use FlexibleTimestampDeserializer logic
                            long timestamp = parseTimestamp(timestampStr);
                            message.setTimestamp(timestamp);
                        } catch (Exception e) {
                            Log.w(TAG, "Failed to parse timestamp: " + timestampStr, e);
                            message.setTimestamp(System.currentTimeMillis());
                        }
                    }
                    break;
                case "createdAt":
                    if (in.peek() == JsonToken.NUMBER) {
                        message.setCreatedAt(in.nextLong());
                    } else {
                        // Handle string timestamp
                        String createdAtStr = in.nextString();
                        try {
                            // Use FlexibleTimestampDeserializer logic
                            long createdAt = parseTimestamp(createdAtStr);
                            message.setCreatedAt(createdAt);
                        } catch (Exception e) {
                            Log.w(TAG, "Failed to parse createdAt: " + createdAtStr, e);
                            // Don't set current time here, keep the value from constructor
                        }
                    }
                    break;
                case "message_id":
                    message.setMessage_id(in.nextInt());
                    break;
                case "isDisappearing":
                    message.setDisappearing(in.nextBoolean());
                    break;
                case "disappearAfterMs":
                    message.setDisappearAfterMs(in.nextLong());
                    break;
                case "isExpired":
                    message.setExpired(in.nextBoolean());
                    break;
                case "seen":
                    message.setSeen(in.nextBoolean());
                    break;
                default:
                    // Skip unknown fields
                    in.skipValue();
                    break;
            }
        }

        in.endObject();
        
        // Normalize timestamps to avoid timer resets:
        // 1) If timestamp missing/invalid, set to now
        if (message.getTimestamp() <= 0) {
            long now = System.currentTimeMillis();
            Log.d(TAG, "Timestamp missing; setting to now: " + now);
            message.setTimestamp(now);
        }
        // 2) If createdAt missing/invalid, set to timestamp (now if we just set it)
        if (message.getCreatedAt() <= 0) {
            Log.d(TAG, "createdAt missing; setting to timestamp: " + message.getTimestamp());
            message.setCreatedAt(message.getTimestamp());
        }
        
        return message;
    }
    
    /**
     * Parse timestamp string to long using similar logic to FlexibleTimestampDeserializer
     */
    private long parseTimestamp(String timestampStr) {
        if (timestampStr == null || timestampStr.isEmpty()) {
            return System.currentTimeMillis();
        }
        // 1) Try as long milliseconds
        try {
            return Long.parseLong(timestampStr);
        } catch (NumberFormatException ignore) { }

        // 2) Try common ISO-8601 patterns in UTC (mirrors FlexibleTimestampDeserializer)
        java.text.SimpleDateFormat[] formats = new java.text.SimpleDateFormat[] {
            new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", java.util.Locale.US), // microseconds
            new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", java.util.Locale.US),    // milliseconds
            new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US),        // basic
            new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US),     // Z suffix
            new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)  // ms + Z
        };
        for (java.text.SimpleDateFormat f : formats) {
            f.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        }
        for (java.text.SimpleDateFormat f : formats) {
            try {
                java.util.Date d = f.parse(timestampStr);
                if (d != null) return d.getTime();
            } catch (Exception ignore) { }
        }

        // 3) Fallback to now
        return System.currentTimeMillis();
    }
}
