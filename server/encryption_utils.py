#!/usr/bin/env python3
"""
Encryption Utility Module for Bisto Chat
Provides AES encryption/decryption functionality with optional Tulu encryption layer
"""

import os
import base64
from cryptography.fernet import Fernet
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.kdf.pbkdf2 import PBKDF2HMAC
import logging

# Import Tulu encryption if available
try:
    from tulu_encryption import TuluEncryption, HybridEncryption
    TULU_AVAILABLE = True
except ImportError:
    TULU_AVAILABLE = False
    print("Tulu encryption not available. Using AES-only mode.")

logger = logging.getLogger(__name__)

class MessageEncryption:
    """Handles encryption and decryption of chat messages with optional Tulu layer"""
    
    def __init__(self, password=None, use_tulu=False, encryption_mode="aes"):
        """Initialize encryption with a password or use default
        
        Args:
            password (str, optional): Encryption password
            use_tulu (bool): Enable Tulu encryption layer
            encryption_mode (str): "aes", "tulu", or "hybrid"
        """
        # Use a default password if none provided (in production, use environment variable)
        self.password = password or "BistoChatSecureKey2024!@#"
        self.salt = b'stable_salt_for_bisto_chat_2024'  # In production, use random salt per app instance
        self.fernet = self._generate_key()
        self.use_tulu = use_tulu and TULU_AVAILABLE
        self.encryption_mode = encryption_mode.lower()
        
        # Initialize Tulu encryption if requested and available
        if self.use_tulu or self.encryption_mode in ["tulu", "hybrid"]:
            if TULU_AVAILABLE:
                self.tulu_encryptor = TuluEncryption()
                if self.encryption_mode == "hybrid":
                    self.hybrid_encryptor = HybridEncryption(use_tulu=True, use_aes=True)
                logger.info(f"Initialized with Tulu encryption mode: {self.encryption_mode}")
            else:
                logger.warning("Tulu encryption requested but not available. Falling back to AES.")
                self.encryption_mode = "aes"
    
    def _generate_key(self):
        """Generate encryption key from password using PBKDF2"""
        try:
            # Derive key from password
            kdf = PBKDF2HMAC(
                algorithm=hashes.SHA256(),
                length=32,
                salt=self.salt,
                iterations=100000,
            )
            key = base64.urlsafe_b64encode(kdf.derive(self.password.encode()))
            return Fernet(key)
        except Exception as e:
            logger.error(f"Error generating encryption key: {e}")
            raise
    
    def encrypt_message(self, message_text):
        """Encrypt a message text using the configured encryption method
        
        Args:
            message_text (str): Plain text message to encrypt
            
        Returns:
            str: Encrypted message (format depends on encryption mode)
        """
        try:
            if not message_text:
                return message_text
            
            # Choose encryption method based on mode
            if self.encryption_mode == "tulu" and hasattr(self, 'tulu_encryptor'):
                # Tulu-only encryption
                encrypted_result = self.tulu_encryptor.encrypt_message(message_text)
                logger.debug("Message encrypted with Tulu encryption")
                return encrypted_result
                
            elif self.encryption_mode == "hybrid" and hasattr(self, 'hybrid_encryptor'):
                # Hybrid encryption (Tulu + AES)
                encrypted_result = self.hybrid_encryptor.encrypt_message(message_text)
                logger.debug("Message encrypted with hybrid encryption (Tulu + AES)")
                return encrypted_result
                
            else:
                # Default AES encryption
                message_bytes = message_text.encode('utf-8')
                encrypted_bytes = self.fernet.encrypt(message_bytes)
                encrypted_b64 = base64.urlsafe_b64encode(encrypted_bytes).decode('utf-8')
                logger.debug("Message encrypted with AES encryption")
                return encrypted_b64
            
        except Exception as e:
            logger.error(f"Encryption error: {e}")
            # Return original message if encryption fails (fallback)
            return message_text
    
    def decrypt_message(self, encrypted_message):
        """Decrypt an encrypted message using the appropriate method
        
        Args:
            encrypted_message (str): Encrypted message
            
        Returns:
            str: Decrypted plain text message
        """
        try:
            if not encrypted_message:
                return encrypted_message
            
            # Check if it's a Tulu-encrypted message first
            if hasattr(self, 'tulu_encryptor') and self.tulu_encryptor.is_tulu_encrypted(encrypted_message):
                if self.encryption_mode == "hybrid" and hasattr(self, 'hybrid_encryptor'):
                    # Hybrid decryption
                    decrypted_result = self.hybrid_encryptor.decrypt_message(encrypted_message)
                    logger.debug("Message decrypted with hybrid decryption")
                    return decrypted_result
                else:
                    # Tulu-only decryption
                    decrypted_result = self.tulu_encryptor.decrypt_message(encrypted_message)
                    logger.debug("Message decrypted with Tulu decryption")
                    return decrypted_result
            
            # Check if message is AES encrypted
            if not self._is_encrypted(encrypted_message):
                # Return as-is if not encrypted (backward compatibility)
                logger.debug("Message appears to be plain text, returning as-is")
                return encrypted_message
            
            # Default AES decryption
            encrypted_bytes = base64.urlsafe_b64decode(encrypted_message.encode('utf-8'))
            decrypted_bytes = self.fernet.decrypt(encrypted_bytes)
            decrypted_text = decrypted_bytes.decode('utf-8')
            logger.debug("Message decrypted with AES decryption")
            return decrypted_text
            
        except Exception as e:
            logger.error(f"Decryption error: {e}")
            # Return original message if decryption fails (backward compatibility)
            return encrypted_message
    
    def _is_encrypted(self, message):
        """Check if a message appears to be encrypted
        
        Args:
            message (str): Message to check
            
        Returns:
            bool: True if message appears encrypted
        """
        try:
            # Fernet tokens are base64 encoded and have specific characteristics
            if len(message) < 20:  # Minimum length for Fernet token
                return False
            
            # Try to base64 decode - encrypted messages should be valid base64
            base64.urlsafe_b64decode(message.encode('utf-8'))
            
            # Additional check: Fernet tokens start with specific bytes after decoding
            decoded = base64.urlsafe_b64decode(message.encode('utf-8'))
            return len(decoded) > 20  # Minimum Fernet token length
            
        except Exception:
            return False
    
    def encrypt_bulk_messages(self, messages):
        """Encrypt multiple messages at once
        
        Args:
            messages (list): List of message dictionaries with 'message_text' field
            
        Returns:
            list: Messages with encrypted 'message_text' field
        """
        encrypted_messages = []
        for msg in messages:
            if isinstance(msg, dict) and 'message_text' in msg:
                msg_copy = msg.copy()
                msg_copy['message_text'] = self.encrypt_message(msg['message_text'])
                encrypted_messages.append(msg_copy)
            else:
                encrypted_messages.append(msg)
        return encrypted_messages
    
    def decrypt_bulk_messages(self, messages):
        """Decrypt multiple messages at once
        
        Args:
            messages (list): List of message dictionaries with encrypted 'message_text' field
            
        Returns:
            list: Messages with decrypted 'message_text' field
        """
        decrypted_messages = []
        for msg in messages:
            if isinstance(msg, dict) and 'message_text' in msg:
                msg_copy = msg.copy()
                msg_copy['message_text'] = self.decrypt_message(msg['message_text'])
                decrypted_messages.append(msg_copy)
            else:
                decrypted_messages.append(msg)
        return decrypted_messages

# Global encryption instance (singleton pattern)
_encryption_instance = None
_tulu_instance = None
_hybrid_instance = None

def get_encryption_instance(encryption_mode="aes"):
    """Get the global encryption instance
    
    Args:
        encryption_mode (str): "aes", "tulu", or "hybrid"
    """
    global _encryption_instance, _tulu_instance, _hybrid_instance
    
    if encryption_mode == "tulu":
        if _tulu_instance is None:
            _tulu_instance = MessageEncryption(encryption_mode="tulu")
        return _tulu_instance
    elif encryption_mode == "hybrid":
        if _hybrid_instance is None:
            _hybrid_instance = MessageEncryption(encryption_mode="hybrid")
        return _hybrid_instance
    else:
        if _encryption_instance is None:
            _encryption_instance = MessageEncryption(encryption_mode="aes")
        return _encryption_instance

def encrypt_message(message_text, encryption_mode="aes"):
    """Convenience function to encrypt a message
    
    Args:
        message_text (str): Message to encrypt
        encryption_mode (str): "aes", "tulu", or "hybrid"
    """
    return get_encryption_instance(encryption_mode).encrypt_message(message_text)

def decrypt_message(encrypted_message, encryption_mode="aes"):
    """Convenience function to decrypt a message
    
    Args:
        encrypted_message (str): Message to decrypt
        encryption_mode (str): "aes", "tulu", or "hybrid" (auto-detected if not specified)
    """
    # Auto-detect encryption type if using default AES mode
    if encryption_mode == "aes" and TULU_AVAILABLE:
        # Try to detect if it's Tulu encrypted
        tulu_enc = TuluEncryption()
        if tulu_enc.is_tulu_encrypted(encrypted_message):
            return get_encryption_instance("hybrid").decrypt_message(encrypted_message)
    
    return get_encryption_instance(encryption_mode).decrypt_message(encrypted_message)

# Test functions
if __name__ == "__main__":
    # Test the encryption functionality
    print("Testing Message Encryption...")
    
    encryptor = MessageEncryption()
    
    test_messages = [
        "Hello, this is a test message!",
        "This is another message with special characters: !@#$%^&*()",
        "Unicode test: 🚀 Hello 世界 🌟",
        ""  # Empty message test
    ]
    
    for msg in test_messages:
        print(f"\nOriginal: {msg}")
        
        # Encrypt
        encrypted = encryptor.encrypt_message(msg)
        print(f"Encrypted: {encrypted}")
        
        # Decrypt
        decrypted = encryptor.decrypt_message(encrypted)
        print(f"Decrypted: {decrypted}")
        
        # Verify
        print(f"Match: {msg == decrypted}")
        print(f"Is Encrypted: {encryptor._is_encrypted(encrypted)}")
    
    print("\n✅ Encryption test complete!")