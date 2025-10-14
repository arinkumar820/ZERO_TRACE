#!/usr/bin/env python3
"""
Test the fixed WebSocket database functions
"""

import sys
import os
sys.path.append(os.path.dirname(__file__))

from websocket_sqlite_server import SQLiteStorage

def test_websocket_db():
    """Test the WebSocket database functions"""
    try:
        # Create storage instance
        db_path = os.path.join(os.path.dirname(__file__), 'bisto_chat.db')
        storage = SQLiteStorage(db_path)
        
        print("🧪 Testing WebSocket Database Functions")
        print("=" * 50)
        
        # Test adding a message
        print("📝 Testing add_message...")
        result = storage.add_message(
            room_id="general_chat",
            sender_uid="test_user_123", 
            sender_email="test@example.com",
            message="Hello from WebSocket test!",
            message_type="text"
        )
        
        if result:
            print(f"✅ Message added successfully: ID {result['id']}")
        else:
            print("❌ Failed to add message")
            return False
        
        # Test getting room messages
        print("\n📚 Testing get_room_messages...")
        messages = storage.get_room_messages("general_chat", 5)
        
        print(f"✅ Retrieved {len(messages)} messages")
        for i, msg in enumerate(messages, 1):
            print(f"  {i}. [{msg['sender_uid']}] {msg['message'][:50]}...")
        
        print("\n✅ All WebSocket database tests passed!")
        return True
        
    except Exception as e:
        print(f"❌ Test failed: {e}")
        return False

if __name__ == "__main__":
    test_websocket_db()