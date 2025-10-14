#!/usr/bin/env python3
"""
Bisto Chat Encryption Demonstration
Shows how message encryption and decryption works in your chat system
"""

from encryption_utils import encrypt_message, decrypt_message
import json

def main():
    print("🔐 Bisto Chat Message Encryption Demonstration")
    print("=" * 50)
    
    # Demo messages
    demo_messages = [
        "Hey! How are you doing?",
        "Want to grab coffee later? ☕",
        "This message contains sensitive info: My password is 123456",
        "Let's discuss the secret project 🤫",
        "Unicode test: 你好世界 🌍 مرحبا بالعالم"
    ]
    
    print("\n📝 Original Messages vs. Database Storage:")
    print("-" * 50)
    
    encrypted_messages = []
    for i, msg in enumerate(demo_messages, 1):
        # This is what happens in your server when sending messages
        encrypted = encrypt_message(msg)
        encrypted_messages.append(encrypted)
        
        print(f"\nMessage {i}:")
        print(f"  📱 User types: {msg}")
        print(f"  🔒 Stored in DB: {encrypted}")
        print(f"  📏 Storage size: {len(encrypted)} characters")
    
    print("\n" + "=" * 50)
    print("🗄️  Database View (what an attacker would see):")
    print("-" * 50)
    
    for i, encrypted in enumerate(encrypted_messages, 1):
        print(f"message_{i}: {encrypted}")
    
    print("\n" + "=" * 50)
    print("📱 Client View (what users actually see):")
    print("-" * 50)
    
    for i, encrypted in enumerate(encrypted_messages, 1):
        # This is what happens when retrieving messages for display
        decrypted = decrypt_message(encrypted)
        print(f"Message {i}: {decrypted}")
    
    print("\n" + "=" * 50)
    print("🔍 Security Features:")
    print("-" * 50)
    print("✅ Messages are encrypted using AES-256")
    print("✅ Each encryption uses a unique random IV (see different encrypted values)")
    print("✅ Database stores only encrypted data")
    print("✅ Users see original messages seamlessly")
    print("✅ Works with all text, emojis, and Unicode")
    print("✅ Backward compatible with existing plain text messages")
    
    # Show that same message encrypts differently each time
    print("\n🔄 Encryption Randomness Demo:")
    print("-" * 30)
    test_msg = "Same message, different encryption"
    for i in range(3):
        encrypted = encrypt_message(test_msg)
        print(f"Encryption {i+1}: {encrypted[:50]}...")
    
    print("\n✨ Your chat is now secure! Messages are protected in the database.")
    print("Users can chat normally while their data stays encrypted at rest.")

if __name__ == "__main__":
    main()