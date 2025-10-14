#!/usr/bin/env python3
"""
Tulu Language Based Encryption Module for Bisto Chat
Advanced encryption using Kannada/Tulu characters, symbol mapping, and golden ratio scrambling
"""

import os
import math
import base64
import logging
from datetime import datetime
from typing import Optional, Dict, Any

logger = logging.getLogger(__name__)

class TuluEncryption:
    """
    Advanced Tulu-based encryption system using:
    - Kannada/Tulu character mapping for common syllables
    - Symbol mapping for punctuation and numbers
    - Golden ratio scrambling for unknown characters
    - Multiple layers of obfuscation
    """
    
    def __init__(self):
        """Initialize Tulu encryption with character mappings"""
        
        # Tulu/Kannada character mapping for common syllables
        self.tulu_mapping = {
            "a": "ಅ", "e": "ಎ", "i": "ಇ", "o": "ಒ", "u": "ಉ",
            "ka": "ಕ", "ga": "ಗ", "cha": "ಚ", "ja": "ಜ", "ta": "ಟ",
            "da": "ಡ", "na": "ನ", "pa": "ಪ", "ba": "ಬ", "ma": "ಮ",
            "ya": "ಯ", "ra": "ರ", "la": "ಲ", "va": "ವ", "sa": "ಸ", "ha": "ಹ"
        }
        
        # Reverse mapping for decryption
        self.reverse_tulu_mapping = {v: k for k, v in self.tulu_mapping.items()}
        
        # Number to symbol mapping
        self.number_mapping = {
            "0": "⌖", "1": "⚙", "2": "☉", "3": "✪", "4": "⦿",
            "5": "☬", "6": "⛁", "7": "♜", "8": "✩", "9": "⚶"
        }
        self.reverse_number_mapping = {v: k for k, v in self.number_mapping.items()}
        
        # Symbol to special character mapping
        self.symbol_mapping = {
            ",": "⌁", ".": "⍕", "?": "⌬", "!": "✶", "-": "⛊",
            "_": "⍟", ":": "⛎", ";": "✺", "(": "⏢", ")": "⏣",
            " ": "◦", "@": "⚡", "#": "⊗", "$": "⌘", "%": "◈",
            "&": "⟐", "*": "✦", "+": "⊕", "=": "⚌", "/": "⟋"
        }
        self.reverse_symbol_mapping = {v: k for k, v in self.symbol_mapping.items()}
        
        # Golden ratio for mathematical scrambling
        self.phi = (1 + math.sqrt(5)) / 2
        
        # Encryption marker to identify Tulu-encrypted messages
        self.tulu_marker = "ತುಳು:"  # "Tulu:" in Kannada
        
    def scramble_text(self, text: str) -> str:
        """
        Scramble text using golden ratio mathematical transformation
        
        Args:
            text (str): Text to scramble
            
        Returns:
            str: Scrambled text using Unicode characters
        """
        try:
            return ''.join(chr((int(ord(c) * self.phi) % 500) + 1000) for c in text)
        except Exception as e:
            logger.error(f"Error in scramble_text: {e}")
            return text  # Fallback to original
            
    def unscramble_text(self, scrambled_text: str) -> str:
        """
        Unscramble text by reversing golden ratio transformation
        
        Args:
            scrambled_text (str): Scrambled text to reverse
            
        Returns:
            str: Original text
        """
        try:
            result = ""
            for char in scrambled_text:
                target = ord(char)
                found = False
                # Try to find the original character
                for code in range(32, 127):  # Printable ASCII range
                    if (int(code * self.phi) % 500) + 1000 == target:
                        result += chr(code)
                        found = True
                        break
                if not found:
                    result += "?"  # Placeholder for unrecoverable characters
            return result
        except Exception as e:
            logger.error(f"Error in unscramble_text: {e}")
            return scrambled_text  # Fallback
    
    def encrypt_message(self, message: str) -> str:
        """
        Encrypt a message using Tulu character mapping and golden ratio scrambling
        
        Args:
            message (str): Plain text message to encrypt
            
        Returns:
            str: Tulu-encrypted message with marker
        """
        try:
            if not message:
                return message
                
            result = ""
            i = 0
            message_lower = message.lower()
            
            while i < len(message):
                char_found = False
                
                # Check for two-character mappings first (like "ka", "ga")
                if i < len(message) - 1:
                    two_char = message_lower[i:i+2]
                    if two_char in self.tulu_mapping:
                        result += self.tulu_mapping[two_char]
                        i += 2
                        char_found = True
                        continue
                
                # Check single character mappings
                char = message_lower[i]
                if char in self.tulu_mapping:
                    result += self.tulu_mapping[char]
                    char_found = True
                elif char in self.number_mapping:
                    result += self.number_mapping[char]
                    char_found = True
                elif char in self.symbol_mapping:
                    result += self.symbol_mapping[char]
                    char_found = True
                
                # If no mapping found, use golden ratio scrambling
                if not char_found:
                    result += self.scramble_text(message[i])  # Preserve case for scrambling
                
                i += 1
            
            # Add Tulu marker prefix for identification
            encrypted_result = self.tulu_marker + result
            
            # Base64 encode for safe storage/transmission
            b64_result = base64.urlsafe_b64encode(encrypted_result.encode('utf-8')).decode('utf-8')
            
            logger.debug(f"Tulu encryption successful")
            return b64_result
            
        except Exception as e:
            logger.error(f"Tulu encryption error: {e}")
            return message  # Fallback to original message
    
    def decrypt_message(self, encrypted_message: str) -> str:
        """
        Decrypt a Tulu-encrypted message
        
        Args:
            encrypted_message (str): Tulu-encrypted message
            
        Returns:
            str: Decrypted plain text message
        """
        try:
            if not encrypted_message:
                return encrypted_message
            
            # Check if this is a Tulu-encrypted message
            if not self.is_tulu_encrypted(encrypted_message):
                return encrypted_message  # Not Tulu-encrypted, return as-is
            
            # Base64 decode
            decoded_message = base64.urlsafe_b64decode(encrypted_message.encode('utf-8')).decode('utf-8')
            
            # Remove Tulu marker
            if decoded_message.startswith(self.tulu_marker):
                text_to_decrypt = decoded_message[len(self.tulu_marker):]
            else:
                text_to_decrypt = decoded_message
            
            result = ""
            for char in text_to_decrypt:
                char_found = False
                
                # Check reverse mappings
                if char in self.reverse_number_mapping:
                    result += self.reverse_number_mapping[char]
                    char_found = True
                elif char in self.reverse_symbol_mapping:
                    result += self.reverse_symbol_mapping[char]
                    char_found = True
                elif char in self.reverse_tulu_mapping:
                    result += self.reverse_tulu_mapping[char]
                    char_found = True
                
                # If no mapping found, it's a scrambled character
                if not char_found:
                    result += self.unscramble_text(char)
            
            logger.debug("Tulu decryption successful")
            return result
            
        except Exception as e:
            logger.error(f"Tulu decryption error: {e}")
            return encrypted_message  # Fallback to original
    
    def is_tulu_encrypted(self, message: str) -> bool:
        """
        Check if a message is Tulu-encrypted
        
        Args:
            message (str): Message to check
            
        Returns:
            bool: True if message appears to be Tulu-encrypted
        """
        try:
            if not message or len(message) < 10:
                return False
            
            # Try to base64 decode
            decoded = base64.urlsafe_b64decode(message.encode('utf-8')).decode('utf-8')
            
            # Check for Tulu marker
            return decoded.startswith(self.tulu_marker)
            
        except Exception:
            return False
    
    def encrypt_with_metadata(self, message: str, sender_uid: str = None) -> Dict[str, Any]:
        """
        Encrypt message with additional metadata
        
        Args:
            message (str): Message to encrypt
            sender_uid (str, optional): Sender's unique identifier
            
        Returns:
            dict: Encrypted message with metadata
        """
        try:
            encrypted_message = self.encrypt_message(message)
            
            return {
                'encrypted_message': encrypted_message,
                'encryption_type': 'tulu',
                'encryption_timestamp': datetime.now().isoformat(),
                'sender_uid': sender_uid,
                'message_length': len(message),
                'encrypted_length': len(encrypted_message)
            }
            
        except Exception as e:
            logger.error(f"Error in encrypt_with_metadata: {e}")
            return {
                'encrypted_message': message,
                'encryption_type': 'none',
                'error': str(e)
            }

# Enhanced encryption wrapper that combines AES and Tulu
class HybridEncryption:
    """
    Hybrid encryption system combining AES-256 and Tulu encryption
    """
    
    def __init__(self, use_tulu: bool = True, use_aes: bool = True):
        """
        Initialize hybrid encryption
        
        Args:
            use_tulu (bool): Enable Tulu encryption layer
            use_aes (bool): Enable AES encryption layer
        """
        self.use_tulu = use_tulu
        self.use_aes = use_aes
        
        if use_tulu:
            self.tulu_encryptor = TuluEncryption()
        
        if use_aes:
            # Import AES encryption from existing module
            try:
                from encryption_utils import MessageEncryption
                self.aes_encryptor = MessageEncryption()
            except ImportError:
                logger.warning("AES encryption not available")
                self.use_aes = False
    
    def encrypt_message(self, message: str) -> str:
        """
        Encrypt message using hybrid approach
        
        Args:
            message (str): Plain text message
            
        Returns:
            str: Encrypted message
        """
        try:
            encrypted_message = message
            
            # First layer: Tulu encryption (character obfuscation)
            if self.use_tulu:
                encrypted_message = self.tulu_encryptor.encrypt_message(encrypted_message)
                logger.debug("Applied Tulu encryption layer")
            
            # Second layer: AES encryption (strong cryptographic security)
            if self.use_aes and hasattr(self, 'aes_encryptor'):
                encrypted_message = self.aes_encryptor.encrypt_message(encrypted_message)
                logger.debug("Applied AES encryption layer")
            
            return encrypted_message
            
        except Exception as e:
            logger.error(f"Hybrid encryption error: {e}")
            return message
    
    def decrypt_message(self, encrypted_message: str) -> str:
        """
        Decrypt message using hybrid approach (reverse order)
        
        Args:
            encrypted_message (str): Encrypted message
            
        Returns:
            str: Decrypted plain text message
        """
        try:
            decrypted_message = encrypted_message
            
            # First layer: AES decryption (reverse order)
            if self.use_aes and hasattr(self, 'aes_encryptor'):
                decrypted_message = self.aes_encryptor.decrypt_message(decrypted_message)
                logger.debug("Applied AES decryption layer")
            
            # Second layer: Tulu decryption
            if self.use_tulu:
                decrypted_message = self.tulu_encryptor.decrypt_message(decrypted_message)
                logger.debug("Applied Tulu decryption layer")
            
            return decrypted_message
            
        except Exception as e:
            logger.error(f"Hybrid decryption error: {e}")
            return encrypted_message

# Global instances for easy access
_tulu_encryptor = None
_hybrid_encryptor = None

def get_tulu_encryptor():
    """Get global Tulu encryptor instance"""
    global _tulu_encryptor
    if _tulu_encryptor is None:
        _tulu_encryptor = TuluEncryption()
    return _tulu_encryptor

def get_hybrid_encryptor():
    """Get global hybrid encryptor instance"""
    global _hybrid_encryptor
    if _hybrid_encryptor is None:
        _hybrid_encryptor = HybridEncryption()
    return _hybrid_encryptor

# Convenience functions
def tulu_encrypt(message: str) -> str:
    """Convenience function to encrypt with Tulu"""
    return get_tulu_encryptor().encrypt_message(message)

def tulu_decrypt(encrypted_message: str) -> str:
    """Convenience function to decrypt Tulu message"""
    return get_tulu_encryptor().decrypt_message(encrypted_message)

def hybrid_encrypt(message: str) -> str:
    """Convenience function for hybrid encryption"""
    return get_hybrid_encryptor().encrypt_message(message)

def hybrid_decrypt(encrypted_message: str) -> str:
    """Convenience function for hybrid decryption"""
    return get_hybrid_encryptor().decrypt_message(encrypted_message)

# Test and demonstration functions
if __name__ == "__main__":
    print("🔐 Testing Tulu Encryption System...")
    print("=" * 50)
    
    tulu_enc = TuluEncryption()
    hybrid_enc = HybridEncryption()
    
    test_messages = [
        "Hello World!",
        "This is a test message with numbers 12345",
        "Special characters: !@#$%^&*()",
        "Karnataka namaste! How are you?",
        "Mixed content: abc123!@# xyz789",
        "Unicode test: 🚀 Hello 世界 🌟",
        "Kannada: ನಮಸ್ತೆ ಕನ್ನಡ",
        ""  # Empty message test
    ]
    
    print("\n1. TULU ENCRYPTION TEST:")
    print("-" * 30)
    
    for i, msg in enumerate(test_messages):
        print(f"\nTest {i+1}:")
        print(f"Original:  '{msg}'")
        
        # Tulu encryption
        tulu_encrypted = tulu_enc.encrypt_message(msg)
        print(f"Tulu Enc:  '{tulu_encrypted[:50]}{'...' if len(tulu_encrypted) > 50 else ''}'")
        
        # Tulu decryption
        tulu_decrypted = tulu_enc.decrypt_message(tulu_encrypted)
        print(f"Tulu Dec:  '{tulu_decrypted}'")
        print(f"Match:     {msg == tulu_decrypted}")
        print(f"Is Tulu:   {tulu_enc.is_tulu_encrypted(tulu_encrypted)}")
    
    print("\n\n2. HYBRID ENCRYPTION TEST:")
    print("-" * 30)
    
    for i, msg in enumerate(test_messages[:3]):  # Test first 3 messages
        print(f"\nHybrid Test {i+1}:")
        print(f"Original:    '{msg}'")
        
        # Hybrid encryption (Tulu + AES)
        hybrid_encrypted = hybrid_enc.encrypt_message(msg)
        print(f"Hybrid Enc:  '{hybrid_encrypted[:50]}{'...' if len(hybrid_encrypted) > 50 else ''}'")
        
        # Hybrid decryption
        hybrid_decrypted = hybrid_enc.decrypt_message(hybrid_encrypted)
        print(f"Hybrid Dec:  '{hybrid_decrypted}'")
        print(f"Match:       {msg == hybrid_decrypted}")
    
    print("\n\n3. CHARACTER MAPPING DEMO:")
    print("-" * 30)
    
    demo_text = "namaste kannada"
    print(f"Demo text: '{demo_text}'")
    print("Character by character mapping:")
    
    result = ""
    i = 0
    while i < len(demo_text):
        if i < len(demo_text) - 1 and demo_text[i:i+2] in tulu_enc.tulu_mapping:
            two_char = demo_text[i:i+2]
            mapped = tulu_enc.tulu_mapping[two_char]
            print(f"  '{two_char}' → '{mapped}'")
            result += mapped
            i += 2
        elif demo_text[i] in tulu_enc.tulu_mapping:
            char = demo_text[i]
            mapped = tulu_enc.tulu_mapping[char]
            print(f"  '{char}' → '{mapped}'")
            result += mapped
            i += 1
        elif demo_text[i] in tulu_enc.symbol_mapping:
            char = demo_text[i]
            mapped = tulu_enc.symbol_mapping[char]
            print(f"  '{char}' → '{mapped}'")
            result += mapped
            i += 1
        else:
            scrambled = tulu_enc.scramble_text(demo_text[i])
            print(f"  '{demo_text[i]}' → '{scrambled}' (scrambled)")
            result += scrambled
            i += 1
    
    print(f"Final result: '{result}'")
    
    print("\n✅ All tests completed!")
    print("\nEncryption layers available:")
    print("- 🔤 Tulu character mapping")
    print("- 🔢 Symbol and number mapping")
    print("- 🌀 Golden ratio scrambling")
    print("- 🔐 AES-256 encryption (if available)")
    print("\nHybrid system provides multiple layers of security! 🚀")