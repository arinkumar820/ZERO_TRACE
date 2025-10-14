#!/usr/bin/env python3
"""
Test script to verify if encryption is working in the live system
This will send a test message and check if it gets encrypted in the database
"""

import requests
import mysql.connector
import json
import time
import uuid

# Database configuration (same as websocket_server.py)
DB_CONFIG = {
    "host": "localhost",
    "user": "root",
    "password": "rudra@69420",
    "database": "message_database",
    "autocommit": True
}

def test_flask_api_encryption():
    """Test if Flask API is encrypting messages"""
    print("🧪 Testing Flask API Encryption...")
    
    try:
        # Test message
        test_message = f"TEST_ENCRYPTION_{int(time.time())}"
        
        # Send message via Flask API
        api_url = "http://localhost:8080/api/messages"
        message_data = {
            "sender_uid": "test_user_1",
            "receiver_uid": "test_user_2", 
            "message_text": test_message,
            "message_type": "text"
        }
        
        print(f"Sending test message: {test_message}")
        response = requests.post(api_url, json=message_data)
        
        if response.status_code == 200:
            print("✅ Message sent successfully via Flask API")
            return test_message
        else:
            print(f"❌ Flask API error: {response.status_code} - {response.text}")
            return None
            
    except Exception as e:
        print(f"❌ Flask API test failed: {e}")
        return None

def check_database_encryption():
    """Check if messages are encrypted in the database"""
    print("\n🔍 Checking database for encryption...")
    
    try:
        db = mysql.connector.connect(**DB_CONFIG)
        cursor = db.cursor()
        
        # Get the latest messages
        cursor.execute("""
            SELECT message, sender_email, timestamp 
            FROM messages 
            ORDER BY timestamp DESC 
            LIMIT 10
        """)
        
        messages = cursor.fetchall()
        
        print(f"Found {len(messages)} recent messages:")
        
        encrypted_count = 0
        for i, (message, sender, timestamp) in enumerate(messages, 1):
            print(f"\n{i}. From: {sender} | Time: {timestamp}")
            print(f"   Message: {message}")
            
            # Check if message looks encrypted (base64 and long)
            if len(message) > 50 and message.replace('=', '').replace('+', '').replace('/', '').replace('-', '').replace('_', '').isalnum():
                print("   🔒 ENCRYPTED ✅")
                encrypted_count += 1
            else:
                print("   📝 PLAIN TEXT ❌")
        
        print(f"\n📊 Summary: {encrypted_count}/{len(messages)} messages are encrypted")
        
        if encrypted_count == 0:
            print("⚠️  NO ENCRYPTION DETECTED - Messages are stored as plain text!")
            print("   This means the updated server code is not running.")
        elif encrypted_count == len(messages):
            print("🎉 ALL MESSAGES ENCRYPTED - System working correctly!")
        else:
            print("⚡ PARTIAL ENCRYPTION - Some old messages are plain text, new ones should be encrypted")
        
        db.close()
        return encrypted_count > 0
        
    except Exception as e:
        print(f"❌ Database check failed: {e}")
        return False

def check_server_status():
    """Check if servers are running"""
    print("\n🌐 Checking server status...")
    
    # Test Flask API
    try:
        response = requests.get("http://localhost:8080/", timeout=5)
        if response.status_code == 200:
            print("✅ Flask API server is running on port 8080")
        else:
            print("❌ Flask API server responded with error")
    except Exception as e:
        print(f"❌ Flask API server not accessible: {e}")
    
    # Test WebSocket server (we can't easily test WebSocket, but we can check if it's using the database)
    print("📡 WebSocket server status: Check if it's running in another terminal")

def main():
    print("🔐 Live Encryption Verification Test")
    print("=" * 50)
    
    # Check server status
    check_server_status()
    
    # Check current database state
    encryption_detected = check_database_encryption()
    
    # Test Flask API if it's running
    test_message = test_flask_api_encryption()
    
    if test_message:
        print("\n⏳ Waiting 2 seconds for message to be processed...")
        time.sleep(2)
        
        # Check if the test message was encrypted
        print("\n🔍 Checking if test message was encrypted...")
        check_database_encryption()
    
    print("\n" + "=" * 50)
    print("📝 DIAGNOSIS:")
    
    if not encryption_detected:
        print("❌ ENCRYPTION IS NOT WORKING")
        print("\nPossible reasons:")
        print("1. You're running the OLD server code (without encryption)")
        print("2. Your client is connecting to a different server")
        print("3. The encryption code has an error")
        print("\n🔧 SOLUTION:")
        print("1. Stop all running servers")
        print("2. Start the UPDATED servers:")
        print("   python server.py")
        print("   python websocket_server.py")
        print("3. Send a new message and check again")
    else:
        print("✅ ENCRYPTION IS WORKING")
        print("New messages should be encrypted automatically")

if __name__ == "__main__":
    main()