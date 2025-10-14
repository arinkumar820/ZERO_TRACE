#!/usr/bin/env python3
"""
Debug database connection and message insertion
"""

import mysql.connector
import logging

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# MySQL connection configuration
DB_CONFIG = {
    "host": "localhost",
    "user": "root",
    "password": "rudra@69420",  # Change to your MySQL password
    "database": "message_database",
    "autocommit": True
}

def test_db_operations():
    """Test database operations step by step"""
    try:
        print("🔍 Testing Database Operations...")
        print("=" * 40)
        
        # Test connection
        print("1️⃣ Testing database connection...")
        db = mysql.connector.connect(**DB_CONFIG)
        print("✅ Database connection successful!")
        
        cursor = db.cursor()
        
        # Test chat rooms exist
        print("2️⃣ Checking chat rooms...")
        cursor.execute("SELECT id, name FROM chat_rooms")
        rooms = cursor.fetchall()
        print(f"✅ Found {len(rooms)} chat rooms:")
        for room in rooms:
            print(f"   🏠 {room[0]} - {room[1]}")
        
        # Test message insertion directly
        print("3️⃣ Testing message insertion...")
        test_data = (
            "team_alpha",           # chat_room_id 
            "debug_user",          # sender_uid
            "debug@test.com",      # sender_email
            "Debug test message!", # message
            "text"                 # message_type
        )
        
        insert_query = """
            INSERT INTO messages (chat_room_id, sender_uid, sender_email, message, message_type)
            VALUES (%s, %s, %s, %s, %s)
        """
        
        cursor.execute(insert_query, test_data)
        message_id = cursor.lastrowid
        print(f"✅ Message inserted with ID: {message_id}")
        
        # Verify message was saved
        print("4️⃣ Verifying message was saved...")
        cursor.execute("SELECT * FROM messages WHERE id = %s", (message_id,))
        saved_msg = cursor.fetchone()
        if saved_msg:
            print(f"✅ Message verified: {saved_msg}")
        else:
            print("❌ Message not found!")
        
        # Test user insertion
        print("5️⃣ Testing user insertion...")
        user_data = ("debug_user", "debug@test.com", "Debug User", "online")
        cursor.execute("""
            INSERT INTO users (uid, email, display_name, status, last_seen)
            VALUES (%s, %s, %s, %s, NOW())
            ON DUPLICATE KEY UPDATE 
                status = VALUES(status), 
                last_seen = NOW()
        """, user_data)
        print("✅ User inserted/updated successfully!")
        
        db.close()
        return True
        
    except mysql.connector.Error as err:
        print(f"❌ Database error: {err}")
        print(f"Error code: {err.errno}")
        print(f"SQL state: {err.sqlstate}")
        return False
    except Exception as e:
        print(f"❌ General error: {e}")
        return False

if __name__ == "__main__":
    success = test_db_operations()
    
    if success:
        print("\n🎉 All database operations successful!")
        print("The database is working correctly.")
    else:
        print("\n💥 Database operations failed!")
        print("Check your MySQL server and credentials.")