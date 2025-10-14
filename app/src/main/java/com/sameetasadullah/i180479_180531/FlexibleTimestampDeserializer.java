package com.sameetasadullah.i180479_180531;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Custom Gson deserializer to handle both Unix timestamp (long) and ISO 8601 string timestamps
 */
public class FlexibleTimestampDeserializer implements JsonDeserializer<Long> {
    
    @Override
    public Long deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        
        if (json.isJsonPrimitive()) {
            // Check if it's already a number (Unix timestamp)
            if (json.getAsJsonPrimitive().isNumber()) {
                return json.getAsLong();
            }
            
            // If it's a string, try to parse as ISO 8601 timestamp
            if (json.getAsJsonPrimitive().isString()) {
                String timestampStr = json.getAsString();
                return parseIsoTimestamp(timestampStr);
            }
        }
        
        // Fallback: return current time
        return System.currentTimeMillis();
    }
    
    /**
     * Parse ISO 8601 timestamp string to Unix timestamp (milliseconds)
     */
    private long parseIsoTimestamp(String isoTimestamp) {
        try {
            // Handle various ISO 8601 formats
            SimpleDateFormat[] formats = {
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.US), // With microseconds
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US),    // With milliseconds
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US),        // Basic format
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US),     // With Z suffix
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)  // With milliseconds and Z
            };
            
            // Set UTC timezone for all formatters
            for (SimpleDateFormat format : formats) {
                format.setTimeZone(TimeZone.getTimeZone("UTC"));
            }
            
            // Try each format
            for (SimpleDateFormat format : formats) {
                try {
                    Date date = format.parse(isoTimestamp);
                    return date.getTime();
                } catch (Exception ignored) {
                    // Try next format
                }
            }
            
        } catch (Exception e) {
            // If all parsing fails, log and return current time
            android.util.Log.w("FlexibleTimestamp", "Failed to parse timestamp: " + isoTimestamp, e);
        }
        
        // Fallback: return current time
        return System.currentTimeMillis();
    }
}