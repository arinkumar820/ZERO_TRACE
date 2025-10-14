#!/usr/bin/env python3
"""
Check messages in the SQLite database
"""

import sqlite3
import os
from datetime import datetime

# Database path
DATABASE_PATH = os.path.join(os.path.dirname(__file__), 'bisto_chat.db')

def check_messages():
    """Check recent messages in SQLite database"""
    try:
        conn = sqlite3.connect(DATABASE_PATH)
        conn.row_factory = sqlite3.Row
        cursor = conn.cursor()
        
        print("🔍 Checking SQLite Database Messages")
        print("=" * 50)
        
        # Get recent messages
        cursor.execute('''
            SELECT id, message_id, sender_uid, sender_email, message_text, 
                   timestamp, message_type, status, chat_room_id
            FROM messages 
            ORDER BY timestamp DESC 
            LIMIT 10
        ''')
        
        messages = cursor.fetchall()
        
        if messages:
            print(f"📝 Found {len(messages)} recent messages:")
            print()
            
            for i, msg in enumerate(messages, 1):
                # Convert timestamp if it's unix timestamp
                if msg['timestamp']:
                    try:
                        dt = datetime.fromtimestamp(int(msg['timestamp']))
                        time_str = dt.strftime('%Y-%m-%d %H:%M:%S')
                    except:
                        time_str = str(msg['timestamp'])
                else:
                    time_str = "No timestamp"
                
                print(f"{i:2d}. ID: {msg['id']}")
                print(f"    📧 From: {msg['sender_email'] or msg['sender_uid']}")
                print(f"    💬 Message: {msg['message_text']}")
                print(f"    🏠 Room: {msg['chat_room_id'] or 'N/A'}")
                print(f"    ⏰ Time: {time_str}")
                print(f"    📊 Status: {msg['status']}")
                print()
        else:
            print("❌ No messages found in SQLite database")
        
        # Check total count
        cursor.execute("SELECT COUNT(*) FROM messages")
        total = cursor.fetchone()[0]
        print(f"📊 Total messages in SQLite database: {total}")
        
        conn.close()
        
    except sqlite3.Error as e:
        print(f"❌ SQLite Error: {e}")
    except Exception as e:
        print(f"❌ Error: {e}")

if __name__ == "__main__":
    check_messages()