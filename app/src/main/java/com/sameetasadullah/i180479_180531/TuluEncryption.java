package com.sameetasadullah.i180479_180531;

import android.util.Base64;
import android.util.Log;
import java.util.HashMap;
import java.util.Map;


public class TuluEncryption {
    
    private static final String TAG = "TuluEncryption";
    
    // Tulu/Kannada character mapping for common syllables
    private static final Map<String, String> TULU_MAPPING = new HashMap<String, String>() {{
        put("a", "ಅ"); put("e", "ಎ"); put("i", "ಇ"); put("o", "ಒ"); put("u", "ಉ");
        put("ka", "ಕ"); put("ga", "ಗ"); put("cha", "ಚ"); put("ja", "ಜ"); put("ta", "ಟ");
        put("da", "ಡ"); put("na", "ನ"); put("pa", "ಪ"); put("ba", "ಬ"); put("ma", "ಮ");
        put("ya", "ಯ"); put("ra", "ರ"); put("la", "ಲ"); put("va", "ವ"); put("sa", "ಸ"); put("ha", "ಹ");
    }};
    
    // Reverse mapping for decryption
    private static final Map<String, String> REVERSE_TULU_MAPPING = new HashMap<>();
    
    // Number to symbol mapping
    private static final Map<String, String> NUMBER_MAPPING = new HashMap<String, String>() {{
        put("0", "⌖"); put("1", "⚙"); put("2", "☉"); put("3", "✪"); put("4", "⦿");
        put("5", "☬"); put("6", "⛁"); put("7", "♜"); put("8", "✩"); put("9", "⚶");
    }};
    private static final Map<String, String> REVERSE_NUMBER_MAPPING = new HashMap<>();
    
    // Symbol to special character mapping
    private static final Map<String, String> SYMBOL_MAPPING = new HashMap<String, String>() {{
        put(",", "⌁"); put(".", "⍕"); put("?", "⌬"); put("!", "✶"); put("-", "⛊");
        put("_", "⍟"); put(":", "⛎"); put(";", "✺"); put("(", "⏢"); put(")", "⏣");
        put(" ", "◦"); put("@", "⚡"); put("#", "⊗"); put("$", "⌘"); put("%", "◈");
        put("&", "⟐"); put("*", "✦"); put("+", "⊕"); put("=", "⚌"); put("/", "⟋");
    }};
    private static final Map<String, String> REVERSE_SYMBOL_MAPPING = new HashMap<>();
    
    // Golden ratio for mathematical scrambling
    private static final double PHI = (1 + Math.sqrt(5)) / 2;
    
    // Encryption marker to identify Tulu-encrypted messages
    private static final String TULU_MARKER = "ತುಳು:"; // "Tulu:" in Kannada
    
    // Initialize reverse mappings
    static {
        // Initialize reverse mappings
        for (Map.Entry<String, String> entry : TULU_MAPPING.entrySet()) {
            REVERSE_TULU_MAPPING.put(entry.getValue(), entry.getKey());
        }
        for (Map.Entry<String, String> entry : NUMBER_MAPPING.entrySet()) {
            REVERSE_NUMBER_MAPPING.put(entry.getValue(), entry.getKey());
        }
        for (Map.Entry<String, String> entry : SYMBOL_MAPPING.entrySet()) {
            REVERSE_SYMBOL_MAPPING.put(entry.getValue(), entry.getKey());
        }
    }
    

    private String scrambleText(String text) {
        try {
            StringBuilder result = new StringBuilder();
            for (char c : text.toCharArray()) {
                int transformed = (int)((int)(c * PHI) % 500) + 1000;
                result.append((char)transformed);
            }
            return result.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error in scrambleText: " + e.getMessage());
            return text; // Fallback to original
        }
    }
    

    private String unscrambleText(String scrambledText) {
        try {
            StringBuilder result = new StringBuilder();
            for (char c : scrambledText.toCharArray()) {
                int target = (int) c;
                boolean found = false;
                
                // Try to find the original character
                for (int code = 32; code < 127; code++) { // Printable ASCII range
                    if ((int)(code * PHI) % 500 + 1000 == target) {
                        result.append((char)code);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    result.append("?"); // Placeholder for unrecoverable characters
                }
            }
            return result.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error in unscrambleText: " + e.getMessage());
            return scrambledText; // Fallback
        }
    }
    

    public String encryptMessage(String message) {
        try {
            if (message == null || message.isEmpty()) {
                return message;
            }
            
            StringBuilder result = new StringBuilder();
            String messageLower = message.toLowerCase();
            int i = 0;
            
            while (i < message.length()) {
                boolean charFound = false;
                
                // Check for two-character mappings first (like "ka", "ga")
                if (i < message.length() - 1) {
                    String twoChar = messageLower.substring(i, i + 2);
                    if (TULU_MAPPING.containsKey(twoChar)) {
                        result.append(TULU_MAPPING.get(twoChar));
                        i += 2;
                        charFound = true;
                        continue;
                    }
                }
                
                // Check single character mappings
                String singleChar = String.valueOf(messageLower.charAt(i));
                if (TULU_MAPPING.containsKey(singleChar)) {
                    result.append(TULU_MAPPING.get(singleChar));
                    charFound = true;
                } else if (NUMBER_MAPPING.containsKey(singleChar)) {
                    result.append(NUMBER_MAPPING.get(singleChar));
                    charFound = true;
                } else if (SYMBOL_MAPPING.containsKey(singleChar)) {
                    result.append(SYMBOL_MAPPING.get(singleChar));
                    charFound = true;
                }
                
                // If no mapping found, use golden ratio scrambling
                if (!charFound) {
                    result.append(scrambleText(String.valueOf(message.charAt(i)))); // Preserve case for scrambling
                }
                
                i++;
            }
            
            // Add Tulu marker prefix for identification
            String encryptedResult = TULU_MARKER + result.toString();
            
            // Base64 encode for safe storage/transmission
            String b64Result = Base64.encodeToString(encryptedResult.getBytes("UTF-8"), Base64.URL_SAFE | Base64.NO_WRAP);
            
            Log.d(TAG, "Tulu encryption successful");
            return b64Result;
            
        } catch (Exception e) {
            Log.e(TAG, "Tulu encryption error: " + e.getMessage());
            return message; // Fallback to original message
        }
    }
    

    public String decryptMessage(String encryptedMessage) {
        try {
            if (encryptedMessage == null || encryptedMessage.isEmpty()) {
                return encryptedMessage;
            }
            
            // Check if this is a Tulu-encrypted message
            if (!isTuluEncrypted(encryptedMessage)) {
                return encryptedMessage; // Not Tulu-encrypted, return as-is
            }
            
            // Base64 decode
            byte[] decodedBytes = Base64.decode(encryptedMessage, Base64.URL_SAFE);
            String decodedMessage = new String(decodedBytes, "UTF-8");
            
            // Remove Tulu marker
            String textToDecrypt = decodedMessage;
            if (decodedMessage.startsWith(TULU_MARKER)) {
                textToDecrypt = decodedMessage.substring(TULU_MARKER.length());
            }
            
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < textToDecrypt.length(); i++) {
                String singleChar = String.valueOf(textToDecrypt.charAt(i));
                boolean charFound = false;
                
                // Check reverse mappings
                if (REVERSE_NUMBER_MAPPING.containsKey(singleChar)) {
                    result.append(REVERSE_NUMBER_MAPPING.get(singleChar));
                    charFound = true;
                } else if (REVERSE_SYMBOL_MAPPING.containsKey(singleChar)) {
                    result.append(REVERSE_SYMBOL_MAPPING.get(singleChar));
                    charFound = true;
                } else if (REVERSE_TULU_MAPPING.containsKey(singleChar)) {
                    result.append(REVERSE_TULU_MAPPING.get(singleChar));
                    charFound = true;
                }
                
                // If no mapping found, it's a scrambled character
                if (!charFound) {
                    result.append(unscrambleText(singleChar));
                }
            }
            
            Log.d(TAG, "Tulu decryption successful");
            return result.toString();
            
        } catch (Exception e) {
            Log.e(TAG, "Tulu decryption error: " + e.getMessage());
            return encryptedMessage; // Fallback to original
        }
    }
    

    public boolean isTuluEncrypted(String message) {
        try {
            if (message == null || message.length() < 10) {
                return false;
            }
            
            // Try to base64 decode
            byte[] decodedBytes = Base64.decode(message, Base64.URL_SAFE);
            String decoded = new String(decodedBytes, "UTF-8");
            
            // Check for Tulu marker
            return decoded.startsWith(TULU_MARKER);
            
        } catch (Exception e) {
            return false;
        }
    }
    

    public static class HybridEncryption {
        private final TuluEncryption tuluEncryption;
        private final boolean useTulu;
        private final boolean useAes;
        
        public HybridEncryption(boolean useTulu, boolean useAes) {
            this.useTulu = useTulu;
            this.useAes = useAes;
            this.tuluEncryption = useTulu ? new TuluEncryption() : null;
        }
        
        public HybridEncryption() {
            this(true, true); // Default: use both layers
        }
        

        public String encryptMessage(String message) {
            try {
                String encryptedMessage = message;
                
                // First layer: Tulu encryption (character obfuscation)
                if (useTulu && tuluEncryption != null) {
                    encryptedMessage = tuluEncryption.encryptMessage(encryptedMessage);
                    Log.d(TAG, "Applied Tulu encryption layer");
                }
                
                // Second layer: Could add AES encryption here if needed
                // For now, we'll keep it simple with just Tulu
                
                return encryptedMessage;
                
            } catch (Exception e) {
                Log.e(TAG, "Hybrid encryption error: " + e.getMessage());
                return message;
            }
        }
        

        public String decryptMessage(String encryptedMessage) {
            try {
                String decryptedMessage = encryptedMessage;
                
                // Reverse order: AES decryption first (if implemented), then Tulu
                
                // Tulu decryption
                if (useTulu && tuluEncryption != null) {
                    decryptedMessage = tuluEncryption.decryptMessage(decryptedMessage);
                    Log.d(TAG, "Applied Tulu decryption layer");
                }
                
                return decryptedMessage;
                
            } catch (Exception e) {
                Log.e(TAG, "Hybrid decryption error: " + e.getMessage());
                return encryptedMessage;
            }
        }
        
        /**
         * Check if message is encrypted by this hybrid system
         * @param message Message to check
         * @return True if encrypted
         */
        public boolean isHybridEncrypted(String message) {
            return tuluEncryption != null && tuluEncryption.isTuluEncrypted(message);
        }
    }
    
    /**
     * Utility class for easy access to Tulu encryption
     */
    public static class Helper {
        private static TuluEncryption instance;
        private static HybridEncryption hybridInstance;
        
        public static synchronized TuluEncryption getInstance() {
            if (instance == null) {
                instance = new TuluEncryption();
            }
            return instance;
        }
        
        public static synchronized HybridEncryption getHybridInstance() {
            if (hybridInstance == null) {
                hybridInstance = new HybridEncryption();
            }
            return hybridInstance;
        }
        
        // Convenience methods
        public static String encrypt(String message) {
            return getInstance().encryptMessage(message);
        }
        
        public static String decrypt(String encryptedMessage) {
            return getInstance().decryptMessage(encryptedMessage);
        }
        
        public static String hybridEncrypt(String message) {
            return getHybridInstance().encryptMessage(message);
        }
        
        public static String hybridDecrypt(String encryptedMessage) {
            return getHybridInstance().decryptMessage(encryptedMessage);
        }
        
        public static boolean isEncrypted(String message) {
            return getInstance().isTuluEncrypted(message);
        }
    }
    
    /**
     * Demo method to show character mapping
     * @param text Text to demonstrate mapping for
     * @return Mapping demonstration string
     */
    public static String demonstrateMapping(String text) {
        StringBuilder demo = new StringBuilder();
        demo.append("Tulu Character Mapping Demo:\n");
        demo.append("Original: '").append(text).append("'\n");
        demo.append("Character mappings:\n");
        
        String textLower = text.toLowerCase();
        int i = 0;
        while (i < text.length()) {
            // Check two-character mappings first
            if (i < text.length() - 1) {
                String twoChar = textLower.substring(i, i + 2);
                if (TULU_MAPPING.containsKey(twoChar)) {
                    demo.append("  '").append(twoChar).append("' → '").append(TULU_MAPPING.get(twoChar)).append("'\n");
                    i += 2;
                    continue;
                }
            }
            
            // Check single character mappings
            String singleChar = String.valueOf(textLower.charAt(i));
            if (TULU_MAPPING.containsKey(singleChar)) {
                demo.append("  '").append(singleChar).append("' → '").append(TULU_MAPPING.get(singleChar)).append("'\n");
            } else if (NUMBER_MAPPING.containsKey(singleChar)) {
                demo.append("  '").append(singleChar).append("' → '").append(NUMBER_MAPPING.get(singleChar)).append("'\n");
            } else if (SYMBOL_MAPPING.containsKey(singleChar)) {
                demo.append("  '").append(singleChar).append("' → '").append(SYMBOL_MAPPING.get(singleChar)).append("'\n");
            } else {
                demo.append("  '").append(text.charAt(i)).append("' → (scrambled)\n");
            }
            i++;
        }
        
        return demo.toString();
    }
}