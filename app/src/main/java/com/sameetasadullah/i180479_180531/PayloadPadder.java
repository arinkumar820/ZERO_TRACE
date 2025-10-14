package com.sameetasadullah.i180479_180531;

import org.json.JSONObject;

import java.util.concurrent.ThreadLocalRandom;

/**
 * PayloadPadder provides conservative padding for JSON messages
 * to reduce size-based metadata leakage without adding unknown fields
 * that a server might reject.
 */
public final class PayloadPadder {

    private static final String PAD_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";

    private PayloadPadder() {}

    /**
     * Add a "pad" field so that the JSON string length is at least targetLength characters.
     * If the message is already >= targetLength, it is returned unchanged.
     * This assumes the server ignores unknown fields like "pad".
     */
    public static JSONObject padWithPadField(JSONObject message, int targetLength) {
        try {
            if (message == null) return null;
            if (targetLength <= 0) return message;
            // Clone first so we don't mutate caller's instance
            JSONObject clone = new JSONObject(message.toString());
            // Start with empty pad; we'll grow it until we meet or exceed target
            StringBuilder padBuilder = new StringBuilder();
            clone.put("pad", padBuilder.toString());

            // Iteratively adjust pad to reach target length
            int safety = 0;
            while (clone.toString().length() < targetLength && safety < 10_000) {
                int deficit = targetLength - clone.toString().length();
                // Add at least 1 character each iteration, up to deficit
                int toAdd = Math.max(1, deficit);
                appendRandom(padBuilder, toAdd);
                clone.put("pad", padBuilder.toString());
                safety++;
            }
            return clone;
        } catch (Exception e) {
            return message;
        }
    }

    /**
     * If the message is a chat message (type == "send_message"), pad its "message" field
     * to at least minLength characters. Otherwise, return the message unchanged.
     * This does NOT add new JSON fields, to minimize server incompatibility risk.
     */
    public static JSONObject padForJson(JSONObject message, int minLength) {
        try {
            if (message == null) return null;
            String type = message.optString("type", "");
            if (!"send_message".equals(type)) {
                return message; // Only pad chat payloads by default
            }
            String body = message.optString("message", "");
            String padded = padToLength(body, Math.max(0, minLength));
            // Clone to avoid mutating caller's instance if shared
            JSONObject clone = new JSONObject(message.toString());
            clone.put("message", padded);
            return clone;
        } catch (Exception e) {
            // On any error, return as-is
            return message;
        }
    }

    public static String padToLength(String input, int targetLen) {
        if (input == null) input = "";
        if (targetLen <= 0 || input.length() >= targetLen) return input;
        StringBuilder sb = new StringBuilder(input);
        int toAdd = targetLen - sb.length();
        appendRandom(sb, toAdd);
        return sb.toString();
    }

    private static void appendRandom(StringBuilder sb, int count) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < count; i++) {
            int idx = r.nextInt(PAD_CHARS.length());
            sb.append(PAD_CHARS.charAt(idx));
        }
    }
}
