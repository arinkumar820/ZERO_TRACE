#!/usr/bin/env python3
"""
Clean database setup - removes all foreign key constraints for reliable messaging
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

def clean_database():
    """Drop and recreate all tables without foreign key constraints"""
    try:
        db = mysql.connector.connect(**DB_CONFIG)
        cursor = db.cursor()
        
        print("🧹 Cleaning database...")
        print("=" * 40)
        
        # Drop all tables to remove constraints
        print("1️⃣ Dropping existing tables...")
        cursor.execute("DROP TABLE IF EXISTS room_participants")
        cursor.execute("DROP TABLE IF EXISTS contacts") 
        cursor.execute("DROP TABLE IF EXISTS messages")
        cursor.execute("DROP TABLE IF EXISTS chat_rooms")
        cursor.execute("DROP TABLE IF EXISTS users")
        print("✅ Tables dropped")
        
        # Create users table
        print("2️⃣ Creating users table...")
        cursor.execute("""
            CREATE TABLE users (
                uid VARCHAR(255) PRIMARY KEY,
                email VARCHAR(255) UNIQUE NOT NULL,
                display_name VARCHAR(255),
                status VARCHAR(50) DEFAULT 'offline',
                last_seen DATETIME DEFAULT CURRENT_TIMESTAMP,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                INDEX idx_email (email),
                INDEX idx_status (status)
            )
        """)
        
        # Create chat_rooms table
        print("3️⃣ Creating chat_rooms table...")
        cursor.execute("""
            CREATE TABLE chat_rooms (
                id VARCHAR(255) PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                description TEXT,
                type ENUM('group', 'personal') DEFAULT 'group',
                created_by VARCHAR(255) DEFAULT 'system',
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                INDEX idx_type (type)
            )
        """)
        
        # Create messages table WITHOUT foreign key constraints
        print("4️⃣ Creating messages table...")
        cursor.execute("""
            CREATE TABLE messages (
                id INT AUTO_INCREMENT PRIMARY KEY,
                chat_room_id VARCHAR(255) NOT NULL DEFAULT 'general_chat',
                sender_uid VARCHAR(255) NOT NULL,
                sender_email VARCHAR(255) NOT NULL,
                sender_name VARCHAR(255),
                message TEXT NOT NULL,
                message_type VARCHAR(50) DEFAULT 'text',
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                is_edited BOOLEAN DEFAULT FALSE,
                edited_at DATETIME NULL,
                reply_to_message_id INT NULL,
                INDEX idx_timestamp (timestamp),
                INDEX idx_sender (sender_uid),
                INDEX idx_room (chat_room_id),
                INDEX idx_room_time (chat_room_id, timestamp)
            )
        """)
        
        # Create room_participants table
        print("5️⃣ Creating room_participants table...")
        cursor.execute("""
            CREATE TABLE room_participants (
                id INT AUTO_INCREMENT PRIMARY KEY,
                room_id VARCHAR(255) NOT NULL,
                user_uid VARCHAR(255) NOT NULL,
                joined_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                is_admin BOOLEAN DEFAULT FALSE,
                UNIQUE KEY unique_participant (room_id, user_uid),
                INDEX idx_room (room_id),
                INDEX idx_user (user_uid)
            )
        """)
        
        # Create contacts table
        print("6️⃣ Creating contacts table...")
        cursor.execute("""
            CREATE TABLE contacts (
                id INT AUTO_INCREMENT PRIMARY KEY,
                user_uid VARCHAR(255) NOT NULL,
                contact_uid VARCHAR(255) NOT NULL,
                contact_name VARCHAR(255),
                added_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                UNIQUE KEY unique_contact (user_uid, contact_uid),
                INDEX idx_user (user_uid),
                INDEX idx_contact (contact_uid)
            )
        """)
        
        # Insert your 5 chat rooms
        print("7️⃣ Creating your 5 awesome chat rooms...")
        chat_rooms = [
            ('team_alpha', 'Team Alpha 🚀', 'Elite development team ready for action!', 'group'),
            ('team_tiger', 'Team Tiger 🐅', 'Fierce warriors conquering new challenges!', 'group'),
            ('general_chat', 'General Chat 💬', 'Open discussions for everyone', 'group'),
            ('user_kalpaditya', 'Kalpaditya', 'Personal chat with Kalpaditya', 'personal'),
            ('user_deepak', 'Deepak', 'Personal chat with Deepak', 'personal')
        ]
        
        for room_id, name, description, room_type in chat_rooms:
            cursor.execute("""
                INSERT INTO chat_rooms (id, name, description, type, created_by)
                VALUES (%s, %s, %s, %s, 'system')
            """, (room_id, name, description, room_type))
        
        print("✅ Chat rooms created!")
        
        # Insert some welcome messages
        print("8️⃣ Adding welcome messages...")
        welcome_messages = [
            ('team_alpha', 'system', 'system@alpha.com', 'System', '🚀 Welcome to Team Alpha! Elite developers unite!', 'text'),
            ('team_tiger', 'system', 'system@tiger.com', 'System', '🐅 Team Tiger ready to conquer challenges!', 'text'),
            ('general_chat', 'system', 'system@chat.com', 'System', '💬 Welcome to the general chat! Feel free to discuss anything.', 'text'),
            ('user_kalpaditya', 'system', 'system@personal.com', 'System', '👋 This is your private chat with Kalpaditya', 'text'),
            ('user_deepak', 'system', 'system@personal.com', 'System', '👋 This is your private chat with Deepak', 'text')
        ]
        
        for room_id, uid, email, name, msg, msg_type in welcome_messages:
            cursor.execute("""
                INSERT INTO messages (chat_room_id, sender_uid, sender_email, sender_name, message, message_type)
                VALUES (%s, %s, %s, %s, %s, %s)
            """, (room_id, uid, email, name, msg, msg_type))
        
        db.commit()
        db.close()
        
        print("✅ Database cleaned and recreated successfully!")
        return True
        
    except mysql.connector.Error as err:
        logger.error(f"Database error: {err}")
        return False
    except Exception as e:
        logger.error(f"General error: {e}")
        return False

if __name__ == "__main__":
    print("🧹 Database Clean & Setup")
    print("=" * 50)
    
    success = clean_database()
    
    if success:
        print("\n🎉 Database setup completed successfully!")
        print("\n📋 Available Chat Rooms:")
        print("   🚀 Team Alpha - Elite development team")
        print("   🐅 Team Tiger - Fierce warriors")  
        print("   💬 General Chat - Open discussions")
        print("   👤 Kalpaditya - Personal chat")
        print("   👤 Deepak - Personal chat")
        print("\n🚀 No more foreign key constraint issues!")
        print("🎯 Your WebSocket server should work perfectly now!")
    else:
        print("\n❌ Database setup failed!")
        print("Check your MySQL connection and try again.")