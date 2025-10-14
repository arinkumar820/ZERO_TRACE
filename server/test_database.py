#!/usr/bin/env python3
"""
Test SQLite database connection and operations
"""

import sqlite3
import os
from datetime import datetime

# Database path
DATABASE_PATH = os.path.join(os.path.dirname(__file__), 'bisto_chat.db')

def test_database():
    """Test database operations"""
    try:
        print(f"Testing database: {DATABASE_PATH}")
        print(f"Database exists: {os.path.exists(DATABASE_PATH)}")
        
        # Test connection
        conn = sqlite3.connect(DATABASE_PATH)
        conn.row_factory = sqlite3.Row
        cursor = conn.cursor()
        
        print("✅ Database connection successful")
        
        # Test table structure
        cursor.execute("SELECT name FROM sqlite_master WHERE type='table';")
        tables = cursor.fetchall()
        print(f"📋 Tables found: {[table[0] for table in tables]}")
        
        # Test messages table structure
        cursor.execute("PRAGMA table_info(messages);")
        columns = cursor.fetchall()
        print(f"📊 Messages table columns: {[col[1] for col in columns]}")
        
        # Test inserting a message
        test_message = {
            'chat_room_id': 'test_room',
            'sender_uid': 'test_uid_123',
            'sender_email': 'test@example.com',
            'message': 'Test message from database test',
            'message_type': 'text',
            'timestamp': datetime.now().isoformat()
        }
        
        cursor.execute('''
            INSERT INTO messages (chat_room_id, sender_uid, sender_email, message, message_type, timestamp)
            VALUES (?, ?, ?, ?, ?, ?)
        ''', (test_message['chat_room_id'], test_message['sender_uid'], 
              test_message['sender_email'], test_message['message'],
              test_message['message_type'], test_message['timestamp']))
        
        message_id = cursor.lastrowid
        conn.commit()
        
        print(f"✅ Test message inserted with ID: {message_id}")
        
        # Test reading messages
        cursor.execute("SELECT COUNT(*) FROM messages")
        count = cursor.fetchone()[0]
        print(f"📈 Total messages in database: {count}")
        
        # Test reading recent messages
        cursor.execute('''
            SELECT id, chat_room_id, sender_email, message, timestamp 
            FROM messages 
            ORDER BY timestamp DESC 
            LIMIT 5
        ''')
        recent_messages = cursor.fetchall()
        
        print("📝 Recent messages:")
        for msg in recent_messages:
            print(f"  ID {msg[0]}: {msg[2]} in {msg[1]}: {msg[3][:50]}...")
        
        conn.close()
        print("✅ Database test completed successfully!")
        return True
        
    except sqlite3.Error as e:
        print(f"❌ SQLite Error: {e}")
        return False
    except Exception as e:
        print(f"❌ General Error: {e}")
        return False

if __name__ == "__main__":
    test_database()