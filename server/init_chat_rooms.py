#!/usr/bin/env python3
"""
Initialize chat rooms with your awesome team data
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

def get_db_connection():
    """Get a new database connection"""
    try:
        return mysql.connector.connect(**DB_CONFIG)
    except mysql.connector.Error as err:
        logger.error(f"Database connection error: {err}")
        return None

def initialize_chat_rooms():
    """Initialize chat rooms with your awesome teams"""
    try:
        db = get_db_connection()
        if not db:
            logger.error("Failed to connect to database")
            return False
            
        cursor = db.cursor()
        
        # First, remove foreign key constraints temporarily
        cursor.execute("SET foreign_key_checks = 0")
        
        # Create chat_rooms table without foreign key constraints
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS chat_rooms (
                id VARCHAR(255) PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                description TEXT,
                type ENUM('group', 'personal') DEFAULT 'group',
                created_by VARCHAR(255) DEFAULT 'system',
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                INDEX idx_type (type)
            )
        """)
        
        # Insert your awesome chat rooms
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
                ON DUPLICATE KEY UPDATE
                    name = VALUES(name),
                    description = VALUES(description),
                    type = VALUES(type)
            """, (room_id, name, description, room_type))
        
        # Create messages table 
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS messages (
                id INT AUTO_INCREMENT PRIMARY KEY,
                chat_room_id VARCHAR(255) NOT NULL DEFAULT 'general_chat',
                sender_uid VARCHAR(255) NOT NULL,
                sender_email VARCHAR(255) NOT NULL,
                message TEXT NOT NULL,
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                message_type VARCHAR(50) DEFAULT 'text',
                INDEX idx_timestamp (timestamp),
                INDEX idx_sender (sender_uid),
                INDEX idx_room (chat_room_id),
                INDEX idx_room_time (chat_room_id, timestamp)
            )
        """)
        
        # Create users table
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS users (
                uid VARCHAR(255) PRIMARY KEY,
                email VARCHAR(255) UNIQUE NOT NULL,
                display_name VARCHAR(255),
                status VARCHAR(50) DEFAULT 'offline',
                last_seen DATETIME DEFAULT CURRENT_TIMESTAMP,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """)
        
        # Create room_participants table
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS room_participants (
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
        
        # Insert some sample messages for testing
        sample_messages = [
            ('team_alpha', 'sample_user_1', 'alpha@team.com', '🚀 Team Alpha ready for deployment!'),
            ('team_tiger', 'sample_user_2', 'tiger@team.com', '🐅 Tiger team on the hunt for success!'),
            ('general_chat', 'sample_user_3', 'user@general.com', '💬 Welcome to the enhanced chat system!'),
        ]
        
        for room_id, uid, email, message in sample_messages:
            cursor.execute("""
                INSERT INTO messages (chat_room_id, sender_uid, sender_email, message)
                VALUES (%s, %s, %s, %s)
                ON DUPLICATE KEY UPDATE message = VALUES(message)
            """, (room_id, uid, email, message))
        
        # Re-enable foreign key checks
        cursor.execute("SET foreign_key_checks = 1")
        
        db.commit()
        db.close()
        
        logger.info("✅ Chat rooms initialized successfully!")
        logger.info("🚀 Team Alpha, Team Tiger, General Chat, Kalpaditya, and Deepak rooms created")
        return True
        
    except mysql.connector.Error as err:
        logger.error(f"Database initialization error: {err}")
        return False

if __name__ == "__main__":
    print("🏗️  Initializing Chat Rooms Database...")
    print("=" * 50)
    
    success = initialize_chat_rooms()
    
    if success:
        print("✅ Database initialization completed!")
        print("\n📋 Available Chat Rooms:")
        print("   🚀 Team Alpha - Elite development team")
        print("   🐅 Team Tiger - Fierce warriors")  
        print("   💬 General Chat - Open discussions")
        print("   👤 Kalpaditya - Personal chat")
        print("   👤 Deepak - Personal chat")
        print("\n🎯 Server ready to handle messages!")
    else:
        print("❌ Database initialization failed!")