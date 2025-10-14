#!/usr/bin/env python3
"""
Simple check of SQLite messages 
"""

import sqlite3
import os
from datetime import datetime

# Database path
DATABASE_PATH = os.path.join(os.path.dirname(__file__), 'bisto_chat.db')

def check_messages():
    """Check messages in SQLite"""
    try:
        conn = sqlite3.connect(DATABASE_PATH)
        cursor = conn.cursor()
        
        print("🔍 SQLite Database Messages")
        print("=" * 50)
        
        # Check table structure first
        cursor.execute("PRAGMA table_info(messages);")
        columns = cursor.fetchall()
        print("📋 Available columns:")
        for col in columns:
            print(f"  - {col[1]}: {col[2]}")
        print()
        
        # Get recent messages with available columns
        cursor.execute('''
            SELECT id, sender_uid, message_text, timestamp, chat_room_id
            FROM messages 
            ORDER BY timestamp DESC 
            LIMIT 10
        ''')
        
        messages = cursor.fetchall()
        
        if messages:
            print(f"📝 Recent messages ({len(messages)}):")
            print()
            for msg in messages:
                try:
                    dt = datetime.fromtimestamp(int(msg[3])) if msg[3] else datetime.now()
                    time_str = dt.strftime('%Y-%m-%d %H:%M:%S')
                except:
                    time_str = str(msg[3])
                
                print(f"ID {msg[0]}: [{msg[1]}] {msg[2]} (Room: {msg[4]}, Time: {time_str})")
        else:
            print("❌ No messages found")
        
        # Total count
        cursor.execute("SELECT COUNT(*) FROM messages")
        total = cursor.fetchone()[0]
        print(f"\n📊 Total messages: {total}")
        
        conn.close()
        
    except Exception as e:
        print(f"❌ Error: {e}")

if __name__ == "__main__":
    check_messages()