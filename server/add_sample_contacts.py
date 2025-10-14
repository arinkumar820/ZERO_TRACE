#!/usr/bin/env python3
"""
Sample Contacts Inserter for Bisto Chat
Adds random contacts to the MySQL database for testing
"""

import mysql.connector
import uuid
import hashlib
import time
from datetime import datetime

# Database Configuration
DB_CONFIG = {
    'host': 'localhost',
    'user': 'root',  # Change if different
    'password': '',  # Add your MySQL password
    'database': 'message_database'
}

# Sample contacts data
SAMPLE_CONTACTS = [
    {
        'name': 'Arin',
        'email': 'arin@example.com',
        'phone': '+1-555-0101',
        'bio': 'Hey there! I love coding and technology. Always up for a chat about new frameworks!',
        'status': 'online'
    },
    {
        'name': 'Prachi',
        'email': 'prachi@example.com',
        'phone': '+1-555-0102',
        'bio': 'Designer by day, gamer by night. Let\'s create something amazing together!',
        'status': 'online'
    },
    {
        'name': 'John Smith',
        'email': 'john.smith@example.com',
        'phone': '+1-555-0103',
        'bio': 'Coffee enthusiast and software developer. Building the future, one line of code at a time.',
        'status': 'away'
    },
    {
        'name': 'Sarah Johnson',
        'email': 'sarah.j@example.com',
        'phone': '+1-555-0104',
        'bio': 'Digital marketing specialist. Love connecting people through great conversations!',
        'status': 'online'
    },
    {
        'name': 'Mike Chen',
        'email': 'mike.chen@example.com',
        'phone': '+1-555-0105',
        'bio': 'Full-stack developer and tech blogger. Always exploring new technologies.',
        'status': 'offline'
    },
    {
        'name': 'Emily Davis',
        'email': 'emily.davis@example.com',
        'phone': '+1-555-0106',
        'bio': 'UI/UX designer with a passion for creating beautiful user experiences.',
        'status': 'online'
    },
    {
        'name': 'Alex Rodriguez',
        'email': 'alex.r@example.com',
        'phone': '+1-555-0107',
        'bio': 'Mobile app developer. iOS and Android expert. Let\'s build the next big app!',
        'status': 'busy'
    },
    {
        'name': 'Lisa Wang',
        'email': 'lisa.wang@example.com',
        'phone': '+1-555-0108',
        'bio': 'Data scientist and AI researcher. Fascinated by machine learning and neural networks.',
        'status': 'online'
    },
    {
        'name': 'David Thompson',
        'email': 'david.t@example.com',
        'phone': '+1-555-0109',
        'bio': 'DevOps engineer. Cloud infrastructure and automation specialist.',
        'status': 'away'
    },
    {
        'name': 'Anna Kowalski',
        'email': 'anna.k@example.com',
        'phone': '+1-555-0110',
        'bio': 'Frontend developer specializing in React and Vue.js. Love creating interactive UIs!',
        'status': 'online'
    }
]

def generate_firebase_uid():
    """Generate a Firebase-like UID"""
    return str(uuid.uuid4()).replace('-', '')[:28]

def create_tables_if_not_exist(cursor):
    """Create the users table if it doesn't exist"""
    
    create_users_table = """
    CREATE TABLE IF NOT EXISTS users (
        id INT AUTO_INCREMENT PRIMARY KEY,
        uid VARCHAR(255) UNIQUE NOT NULL,
        firebase_uid VARCHAR(255) UNIQUE,
        email VARCHAR(255) UNIQUE NOT NULL,
        name VARCHAR(255) NOT NULL,
        phone_number VARCHAR(20),
        profile_image_url TEXT,
        bio TEXT,
        status ENUM('online', 'offline', 'away', 'busy') DEFAULT 'offline',
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
    """
    
    # Also create chat_rooms table for messaging
    create_chat_rooms_table = """
    CREATE TABLE IF NOT EXISTS chat_rooms (
        id INT AUTO_INCREMENT PRIMARY KEY,
        room_id VARCHAR(255) UNIQUE NOT NULL,
        room_name VARCHAR(255),
        room_type ENUM('personal', 'group') DEFAULT 'personal',
        created_by VARCHAR(255),
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (created_by) REFERENCES users(uid) ON DELETE SET NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
    """
    
    # Create messages table
    create_messages_table = """
    CREATE TABLE IF NOT EXISTS messages (
        id INT AUTO_INCREMENT PRIMARY KEY,
        message_id VARCHAR(255) UNIQUE NOT NULL,
        room_id VARCHAR(255) NOT NULL,
        sender_uid VARCHAR(255) NOT NULL,
        message TEXT NOT NULL,
        message_type ENUM('text', 'image', 'file', 'system') DEFAULT 'text',
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (room_id) REFERENCES chat_rooms(room_id) ON DELETE CASCADE,
        FOREIGN KEY (sender_uid) REFERENCES users(uid) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
    """
    
    # Create room_participants table
    create_participants_table = """
    CREATE TABLE IF NOT EXISTS room_participants (
        id INT AUTO_INCREMENT PRIMARY KEY,
        room_id VARCHAR(255) NOT NULL,
        user_uid VARCHAR(255) NOT NULL,
        joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (room_id) REFERENCES chat_rooms(room_id) ON DELETE CASCADE,
        FOREIGN KEY (user_uid) REFERENCES users(uid) ON DELETE CASCADE,
        UNIQUE KEY unique_room_user (room_id, user_uid)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
    """
    
    cursor.execute(create_users_table)
    cursor.execute(create_chat_rooms_table)
    cursor.execute(create_messages_table)
    cursor.execute(create_participants_table)
    print("✅ Database tables created/verified")

def insert_sample_contacts(cursor, connection):
    """Insert sample contacts into the database"""
    
    insert_query = """
    INSERT INTO users (uid, firebase_uid, email, name, phone_number, bio, status, last_seen)
    VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
    ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    phone_number = VALUES(phone_number),
    bio = VALUES(bio),
    status = VALUES(status),
    updated_at = CURRENT_TIMESTAMP
    """
    
    contacts_added = 0
    
    for contact in SAMPLE_CONTACTS:
        try:
            uid = generate_firebase_uid()
            firebase_uid = generate_firebase_uid()
            
            cursor.execute(insert_query, (
                uid,
                firebase_uid,
                contact['email'],
                contact['name'],
                contact['phone'],
                contact['bio'],
                contact['status'],
                datetime.now()
            ))
            
            contacts_added += 1
            print(f"✅ Added: {contact['name']} ({contact['email']})")
            
        except mysql.connector.IntegrityError as e:
            if "Duplicate entry" in str(e):
                print(f"ℹ️  Skipped: {contact['name']} (already exists)")
            else:
                print(f"❌ Error adding {contact['name']}: {e}")
        except Exception as e:
            print(f"❌ Error adding {contact['name']}: {e}")
    
    connection.commit()
    return contacts_added

def create_sample_chat_rooms(cursor, connection):
    """Create some sample chat rooms for testing"""
    
    # Get some users for creating rooms
    cursor.execute("SELECT uid, name FROM users LIMIT 5")
    users = cursor.fetchall()
    
    if len(users) < 2:
        print("⚠️  Not enough users to create chat rooms")
        return
    
    # Create a few sample rooms
    rooms_data = [
        {
            'room_id': f"{users[0][0]}_{users[1][0]}",
            'room_name': f"{users[0][1]} & {users[1][1]}",
            'room_type': 'personal',
            'created_by': users[0][0],
            'participants': [users[0][0], users[1][0]]
        },
        {
            'room_id': f"{users[0][0]}_{users[2][0]}",
            'room_name': f"{users[0][1]} & {users[2][1]}",
            'room_type': 'personal',
            'created_by': users[0][0],
            'participants': [users[0][0], users[2][0]]
        }
    ]
    
    room_query = """
    INSERT INTO chat_rooms (room_id, room_name, room_type, created_by)
    VALUES (%s, %s, %s, %s)
    ON DUPLICATE KEY UPDATE room_name = VALUES(room_name)
    """
    
    participant_query = """
    INSERT INTO room_participants (room_id, user_uid)
    VALUES (%s, %s)
    ON DUPLICATE KEY UPDATE joined_at = VALUES(joined_at)
    """
    
    for room in rooms_data:
        try:
            # Create room
            cursor.execute(room_query, (
                room['room_id'],
                room['room_name'],
                room['room_type'],
                room['created_by']
            ))
            
            # Add participants
            for participant in room['participants']:
                cursor.execute(participant_query, (room['room_id'], participant))
            
            print(f"✅ Created chat room: {room['room_name']}")
            
        except Exception as e:
            print(f"❌ Error creating room {room['room_name']}: {e}")
    
    connection.commit()

def add_sample_messages(cursor, connection):
    """Add some sample messages for testing"""
    
    # Get existing rooms
    cursor.execute("SELECT room_id FROM chat_rooms LIMIT 2")
    rooms = cursor.fetchall()
    
    if not rooms:
        print("⚠️  No chat rooms found for adding messages")
        return
    
    # Get users in the first room
    room_id = rooms[0][0]
    cursor.execute("""
        SELECT u.uid, u.name 
        FROM users u 
        JOIN room_participants rp ON u.uid = rp.user_uid 
        WHERE rp.room_id = %s
    """, (room_id,))
    
    room_users = cursor.fetchall()
    
    if len(room_users) < 2:
        return
    
    # Sample messages
    sample_messages = [
        (room_users[0][0], "Hey! How are you doing?"),
        (room_users[1][0], "I'm great! How about you?"),
        (room_users[0][0], "Pretty good! Working on some new features for our chat app."),
        (room_users[1][0], "That sounds exciting! I'd love to hear more about it."),
        (room_users[0][0], "Sure! We've added real-time messaging with WebSocket support."),
        (room_users[1][0], "Wow, that's awesome! The app keeps getting better! 🚀"),
    ]
    
    message_query = """
    INSERT INTO messages (message_id, room_id, sender_uid, message, message_type)
    VALUES (%s, %s, %s, %s, %s)
    """
    
    for i, (sender_uid, message_text) in enumerate(sample_messages):
        try:
            message_id = f"msg_{int(time.time())}_{i}"
            cursor.execute(message_query, (
                message_id,
                room_id,
                sender_uid,
                message_text,
                'text'
            ))
            print(f"✅ Added message from {room_users[0][1] if sender_uid == room_users[0][0] else room_users[1][1]}")
            
        except Exception as e:
            print(f"❌ Error adding message: {e}")
    
    connection.commit()

def main():
    """Main function to set up sample data"""
    
    print("🚀 Bisto Chat - Sample Contacts Inserter")
    print("=" * 50)
    
    try:
        # Connect to MySQL
        print("📡 Connecting to MySQL database...")
        connection = mysql.connector.connect(**DB_CONFIG)
        cursor = connection.cursor()
        
        print(f"✅ Connected to database: {DB_CONFIG['database']}")
        
        # Create tables if they don't exist
        print("\n📋 Creating/verifying database tables...")
        create_tables_if_not_exist(cursor)
        
        # Insert sample contacts
        print("\n👥 Adding sample contacts...")
        contacts_added = insert_sample_contacts(cursor, connection)
        
        # Create sample chat rooms
        print(f"\n💬 Creating sample chat rooms...")
        create_sample_chat_rooms(cursor, connection)
        
        # Add sample messages
        print(f"\n📝 Adding sample messages...")
        add_sample_messages(cursor, connection)
        
        # Show summary
        print("\n" + "=" * 50)
        print("📊 SUMMARY")
        print("=" * 50)
        
        # Count total users
        cursor.execute("SELECT COUNT(*) FROM users")
        total_users = cursor.fetchone()[0]
        
        cursor.execute("SELECT COUNT(*) FROM chat_rooms")
        total_rooms = cursor.fetchone()[0]
        
        cursor.execute("SELECT COUNT(*) FROM messages")
        total_messages = cursor.fetchone()[0]
        
        print(f"👥 Total Users: {total_users}")
        print(f"💬 Total Chat Rooms: {total_rooms}")
        print(f"📝 Total Messages: {total_messages}")
        
        # Show some sample users
        print(f"\n📋 Sample users added:")
        cursor.execute("SELECT name, email, status FROM users ORDER BY created_at DESC LIMIT 5")
        users = cursor.fetchall()
        
        for name, email, status in users:
            status_emoji = {
                'online': '🟢', 'offline': '🔴', 
                'away': '🟡', 'busy': '🔴'
            }.get(status, '⚫')
            print(f"  {status_emoji} {name} ({email}) - {status}")
        
        print(f"\n✅ Sample data setup completed successfully!")
        print(f"🎯 You can now test your Bisto Chat app with {total_users} users")
        
    except mysql.connector.Error as e:
        print(f"❌ MySQL Error: {e}")
        print("\n💡 Tips:")
        print("  - Make sure MySQL is running")
        print("  - Check database credentials in DB_CONFIG")
        print("  - Ensure 'message_database' exists")
        
    except Exception as e:
        print(f"❌ Error: {e}")
        
    finally:
        if 'connection' in locals() and connection.is_connected():
            cursor.close()
            connection.close()
            print("📡 Database connection closed")

if __name__ == "__main__":
    main()