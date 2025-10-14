#!/usr/bin/env python3
"""
Bisto Chat WebSocket Server with MySQL Database
Real-time messaging server for Android chat application
"""

import asyncio
import websockets
import json
import mysql.connector
import logging
import time
from datetime import datetime
import uuid
from encryption_utils import encrypt_message, decrypt_message

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

# Global variables for room-based client management
connected_clients = set()  # All clients
room_clients = {}  # Dict: room_id -> set of websockets in that room
db_pool = None

def get_db_connection():
    """Get a new database connection"""
    try:
        return mysql.connector.connect(**DB_CONFIG)
    except mysql.connector.Error as err:
        logger.error(f"Database connection error: {err}")
        return None

def init_database():
    """Initialize database with required tables"""
    try:
        db = get_db_connection()
        if not db:
            return False
            
        cursor = db.cursor()
        
        # Create chat_rooms table
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
        
        # Create messages table with room association
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
        
        # Create users table for authentication
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
        
        # Create contacts table
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS contacts (
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
        
        db.commit()
        db.close()
        logger.info("Database initialized successfully")
        return True
        
    except mysql.connector.Error as err:
        logger.error(f"Database initialization error: {err}")
        return False

async def send_safe(websocket, message):
    """Safely send message to websocket"""
    try:
        if websocket.open:
            await websocket.send(message)
    except websockets.exceptions.ConnectionClosed:
        logger.warning("Attempted to send to closed connection")
        if websocket in connected_clients:
            connected_clients.remove(websocket)
    except Exception as e:
        logger.error(f"Error sending message: {e}")

async def broadcast_message_to_room(message_data, room_id, exclude_websocket=None):
    """Broadcast message only to clients in the specified room"""
    if room_id not in room_clients or not room_clients[room_id]:
        logger.info(f"No clients in room {room_id} to broadcast to")
        return
    
    message_json = json.dumps(message_data)
    disconnected = set()
    
    logger.info(f"Broadcasting to {len(room_clients[room_id])} clients in room {room_id}")
    
    for client in room_clients[room_id].copy():  # Use copy to avoid modification during iteration
        if client != exclude_websocket:
            try:
                if client.open:
                    await client.send(message_json)
                    logger.debug(f"Message sent to client in room {room_id}")
                else:
                    disconnected.add(client)
            except websockets.exceptions.ConnectionClosed:
                disconnected.add(client)
            except Exception as e:
                logger.error(f"Error broadcasting to client in room {room_id}: {e}")
                disconnected.add(client)
    
    # Remove disconnected clients from room and global sets
    for client in disconnected:
        room_clients[room_id].discard(client)
        connected_clients.discard(client)
    
    if disconnected:
        logger.info(f"Removed {len(disconnected)} disconnected clients from room {room_id}")

async def broadcast_message(message_data, exclude_websocket=None):
    """Legacy function - broadcasts to all clients (for backwards compatibility)"""
    if not connected_clients:
        return
    
    message_json = json.dumps(message_data)
    disconnected = set()
    
    for client in connected_clients:
        if client != exclude_websocket:
            try:
                if client.open:
                    await client.send(message_json)
                else:
                    disconnected.add(client)
            except websockets.exceptions.ConnectionClosed:
                disconnected.add(client)
            except Exception as e:
                logger.error(f"Error broadcasting to client: {e}")
                disconnected.add(client)
    
    # Remove disconnected clients
    connected_clients.difference_update(disconnected)

async def join_client_to_room(websocket, room_id):
    """Add client to a specific chat room"""
    global room_clients
    
    if room_id not in room_clients:
        room_clients[room_id] = set()
    
    room_clients[room_id].add(websocket)
    websocket.current_room = room_id
    
    logger.info(f"Client joined room {room_id}. Room now has {len(room_clients[room_id])} clients.")

async def leave_client_from_room(websocket, room_id):
    """Remove client from a specific chat room"""
    global room_clients
    
    if room_id in room_clients:
        room_clients[room_id].discard(websocket)
        if not room_clients[room_id]:  # Remove empty room
            del room_clients[room_id]
            logger.info(f"Room {room_id} is now empty and removed")
        else:
            logger.info(f"Client left room {room_id}. Room now has {len(room_clients[room_id])} clients.")

async def handle_room_join(websocket, data):
    """Handle client joining a specific room"""
    try:
        room_id = data.get('room_id', 'general_chat')
        
        # Leave previous room if exists
        if hasattr(websocket, 'current_room') and websocket.current_room:
            await leave_client_from_room(websocket, websocket.current_room)
        
        # Join new room
        await join_client_to_room(websocket, room_id)
        
        # Send room-specific message history
        await send_room_message_history(websocket, room_id)
        
        # Confirm room join
        await send_safe(websocket, json.dumps({
            'type': 'room_joined',
            'room_id': room_id,
            'message': f'Joined room {room_id}'
        }))
        
        logger.info(f"User {websocket.user_uid if hasattr(websocket, 'user_uid') else 'unknown'} joined room {room_id}")
        
    except Exception as e:
        logger.error(f"Error handling room join: {e}")
        await send_safe(websocket, json.dumps({
            'type': 'error',
            'message': 'Failed to join room'
        }))

async def handle_user_join(websocket, data):
    """Handle user joining/authentication"""
    try:
        # Accept both old and new field names for compatibility
        user_uid = data.get('sender_uid') or data.get('user_uid')
        user_email = data.get('sender_email') or data.get('user_email', '')
        display_name = data.get('display_name', user_email.split('@')[0] if '@' in user_email else 'Unknown')
        
        if not user_uid:
            await send_safe(websocket, json.dumps({
                'type': 'error',
                'message': 'User ID required'
            }))
            return
        
        db = get_db_connection()
        if not db:
            await send_safe(websocket, json.dumps({
                'type': 'error', 
                'message': 'Database connection failed'
            }))
            return
            
        cursor = db.cursor()
        
        # Update or insert user
        cursor.execute("""
            INSERT INTO users (uid, email, display_name, status, last_seen)
            VALUES (%s, %s, %s, 'online', NOW())
            ON DUPLICATE KEY UPDATE 
                status = 'online', 
                last_seen = NOW(),
                display_name = COALESCE(display_name, VALUES(display_name))
        """, (user_uid, user_email, display_name))
        
        db.commit()
        db.close()
        
        # Store user info with websocket
        websocket.user_uid = user_uid
        websocket.user_email = user_email
        
        # Send join confirmation
        await send_safe(websocket, json.dumps({
            'type': 'join_success',
            'message': 'Connected successfully',
            'user_uid': user_uid
        }))
        
        logger.info(f"User {user_email} ({user_uid}) joined")
        
    except Exception as e:
        logger.error(f"Error handling user join: {e}")
        await send_safe(websocket, json.dumps({
            'type': 'error',
            'message': 'Join failed'
        }))

async def handle_message(websocket, data):
    """Handle incoming chat message"""
    try:
        sender_uid = data.get('sender_uid')
        sender_email = data.get('sender_email', '')
        message_text = data.get('message', '')
        message_type = data.get('message_type', 'text')
        chat_room_id = data.get('chat_room_id', 'general')
        
        if not all([sender_uid, message_text]):
            await send_safe(websocket, json.dumps({
                'type': 'error',
                'message': 'Sender UID and message are required'
            }))
            return
        
        # Auto-join client to room if not already in it
        if not hasattr(websocket, 'current_room') or websocket.current_room != chat_room_id:
            logger.info(f"Auto-joining client to room {chat_room_id}")
            await join_client_to_room(websocket, chat_room_id)
        
        # Encrypt the message before storing in database
        encrypted_message_text = encrypt_message(message_text)
        logger.debug(f"Message encrypted for WebSocket storage: {message_text[:50]}... -> {encrypted_message_text[:50]}...")
        
        # Store encrypted message in database
        db = get_db_connection()
        if not db:
            logger.error("Database connection failed for message storage")
            return
            
        cursor = db.cursor()
        # Get sender name from display_name or generate from email
        sender_name = data.get('display_name', '')
        if not sender_name and sender_email:
            sender_name = sender_email.split('@')[0] if '@' in sender_email else 'User'
        
        cursor.execute("""
            INSERT INTO messages (chat_room_id, sender_uid, sender_email, sender_name, message, message_type)
            VALUES (%s, %s, %s, %s, %s, %s)
        """, (chat_room_id, sender_uid, sender_email, sender_name, encrypted_message_text, message_type))
        
        message_id = cursor.lastrowid
        db.commit()
        
        # Get timestamp
        cursor.execute("SELECT timestamp FROM messages WHERE id = %s", (message_id,))
        timestamp_result = cursor.fetchone()
        timestamp = timestamp_result[0] if timestamp_result else datetime.now()
        
        db.close()
        
        # Prepare broadcast message (send decrypted message to clients)
        # Convert timestamp to milliseconds for consistency with history messages
        timestamp_ms = int(timestamp.timestamp() * 1000) if hasattr(timestamp, 'timestamp') else int(time.time() * 1000)
        
        broadcast_data = {
            'type': 'new_message',  # Android app expects 'new_message' type
            'message_id': message_id,
            'sender_uid': sender_uid,
            'sender_email': sender_email,
            'sender_name': sender_name,
            'message': message_text,  # Send original plain text to clients
            'message_type': message_type,
            'room_id': chat_room_id,  # Android app expects 'room_id'
            'chat_room_id': chat_room_id,  # Keep for compatibility
            'timestamp': timestamp_ms  # Milliseconds timestamp for consistency
        }
        
        logger.debug(f"Broadcasting decrypted message to room {chat_room_id}: {message_text[:50]}...")
        
        # Broadcast only to clients in the same room
        await broadcast_message_to_room(broadcast_data, chat_room_id)
        
        logger.info(f"Message from {sender_email}: {message_text[:50]}...")
        
    except Exception as e:
        logger.error(f"Error handling message: {e}")
        await send_safe(websocket, json.dumps({
            'type': 'error',
            'message': 'Message send failed'
        }))

async def send_room_message_history(websocket, room_id, limit=20):
    """Send recent messages from a specific room to client"""
    try:
        db = get_db_connection()
        if not db:
            return
            
        cursor = db.cursor()
        cursor.execute("""
            SELECT id, sender_uid, sender_email, sender_name, message, timestamp, message_type, chat_room_id
            FROM messages 
            WHERE chat_room_id = %s
            ORDER BY timestamp DESC 
            LIMIT %s
        """, (room_id, limit))
        
        messages = cursor.fetchall()
        db.close()
        
        # Send messages in chronological order (oldest first)
        for msg in reversed(messages):
            # Convert datetime to milliseconds timestamp
            timestamp_ms = int(msg[5].timestamp() * 1000) if msg[5] else int(time.time() * 1000)
            
            # Decrypt the message before sending to client
            decrypted_message = decrypt_message(msg[4])
            logger.debug(f"Room history message decrypted: {msg[4][:50]}... -> {decrypted_message[:50]}...")
            
            message_data = {
                'type': 'history',
                'sender_uid': msg[1],
                'sender_email': msg[2],
                'sender_name': msg[3] if msg[3] else (msg[2].split('@')[0] if '@' in str(msg[2]) else 'User'),
                'message': decrypted_message,  # Send decrypted message
                'timestamp': timestamp_ms,
                'message_type': msg[6] if msg[6] else 'text',
                'chat_room_id': msg[7] if len(msg) > 7 else room_id
            }
            await send_safe(websocket, json.dumps(message_data))
        
        logger.info(f"Sent {len(messages)} messages from room {room_id} to client")
        
    except Exception as e:
        logger.error(f"Error sending room message history: {e}")

async def send_message_history(websocket, limit=20):
    """Send recent messages from all rooms to newly connected client (legacy)"""
    try:
        db = get_db_connection()
        if not db:
            return
            
        cursor = db.cursor()
        cursor.execute("""
            SELECT id, sender_uid, sender_email, sender_name, message, timestamp, message_type, chat_room_id
            FROM messages 
            ORDER BY timestamp DESC 
            LIMIT %s
        """, (limit,))
        
        messages = cursor.fetchall()
        db.close()
        
        # Send messages in chronological order (oldest first)
        for msg in reversed(messages):
            # Convert datetime to milliseconds timestamp
            timestamp_ms = int(msg[5].timestamp() * 1000) if msg[5] else int(time.time() * 1000)
            
            # Decrypt the message before sending to client
            decrypted_message = decrypt_message(msg[4])
            logger.debug(f"General history message decrypted: {msg[4][:50]}... -> {decrypted_message[:50]}...")
            
            message_data = {
                'type': 'history',
                'sender_uid': msg[1],
                'sender_email': msg[2],
                'sender_name': msg[3] if msg[3] else (msg[2].split('@')[0] if '@' in str(msg[2]) else 'User'),
                'message': decrypted_message,  # Send decrypted message
                'timestamp': timestamp_ms,
                'message_type': msg[6] if msg[6] else 'text',
                'chat_room_id': msg[7] if len(msg) > 7 else 'general'
            }
            await send_safe(websocket, json.dumps(message_data))
        
        logger.info(f"Sent {len(messages)} historical messages to client")
        
    except Exception as e:
        logger.error(f"Error sending message history: {e}")

async def handle_search_users(websocket, data):
    """Handle user search request"""
    try:
        query = data.get('query', '').strip()
        limit = data.get('limit', 20)
        
        if not query or len(query) < 2:
            await send_safe(websocket, json.dumps({
                'type': 'search_users_response',
                'success': False,
                'message': 'Query must be at least 2 characters long'
            }))
            return
        
        db = get_db_connection()
        if not db:
            await send_safe(websocket, json.dumps({
                'type': 'search_users_response',
                'success': False,
                'message': 'Database connection failed'
            }))
            return
        
        cursor = db.cursor(dictionary=True)
        
        # Search users by email or display_name
        search_pattern = f"%{query}%"
        cursor.execute("""
            SELECT DISTINCT uid, email, display_name, status, last_seen, created_at
            FROM users 
            WHERE (email LIKE %s OR display_name LIKE %s)
            AND email IS NOT NULL
            ORDER BY 
                CASE WHEN status = 'online' THEN 0 ELSE 1 END,
                last_seen DESC,
                email
            LIMIT %s
        """, (search_pattern, search_pattern, limit))
        
        users = cursor.fetchall()
        db.close()
        
        # Format users for response
        formatted_users = []
        for user in users:
            formatted_user = {
                "uid": user["uid"],
                "email": user["email"],
                "display_name": user["display_name"] or user["email"].split('@')[0],
                "status": user["status"] or "offline",
                "last_seen": user["last_seen"].isoformat() if user["last_seen"] else None,
                "created_at": user["created_at"].isoformat() if user["created_at"] else None
            }
            formatted_users.append(formatted_user)
        
        await send_safe(websocket, json.dumps({
            'type': 'search_users_response',
            'success': True,
            'users': formatted_users,
            'count': len(formatted_users),
            'query': query
        }))
        
        logger.info(f"User search for '{query}' returned {len(formatted_users)} results")
        
    except Exception as e:
        logger.error(f"Error searching users: {e}")
        await send_safe(websocket, json.dumps({
            'type': 'search_users_response',
            'success': False,
            'message': 'Search failed'
        }))

async def handle_get_contacts(websocket, data):
    """Handle get contacts request"""
    try:
        user_uid = data.get('user_uid')
        
        if not user_uid:
            await send_safe(websocket, json.dumps({
                'type': 'get_contacts_response',
                'success': False,
                'message': 'user_uid is required'
            }))
            return
        
        db = get_db_connection()
        if not db:
            await send_safe(websocket, json.dumps({
                'type': 'get_contacts_response',
                'success': False,
                'message': 'Database connection failed'
            }))
            return
        
        cursor = db.cursor(dictionary=True)
        
        # Get contacts for the user
        cursor.execute("""
            SELECT c.contact_uid, c.contact_name, c.added_at,
                   u.email, u.display_name, u.status, u.last_seen
            FROM contacts c
            LEFT JOIN users u ON c.contact_uid = u.uid
            WHERE c.user_uid = %s
            ORDER BY c.added_at DESC
        """, (user_uid,))
        
        contacts = cursor.fetchall()
        db.close()
        
        # Format contacts for response
        formatted_contacts = []
        for contact in contacts:
            formatted_contact = {
                "uid": contact["contact_uid"],
                "email": contact["email"],
                "display_name": contact["display_name"] or contact["contact_name"] or (contact["email"].split('@')[0] if contact["email"] else "Unknown"),
                "status": contact["status"] or "offline",
                "last_seen": contact["last_seen"].isoformat() if contact["last_seen"] else None,
                "added_at": contact["added_at"].isoformat() if contact["added_at"] else None
            }
            formatted_contacts.append(formatted_contact)
        
        await send_safe(websocket, json.dumps({
            'type': 'get_contacts_response',
            'success': True,
            'contacts': formatted_contacts,
            'count': len(formatted_contacts)
        }))
        
        logger.info(f"Sent {len(formatted_contacts)} contacts to user {user_uid}")
        
    except Exception as e:
        logger.error(f"Error getting contacts: {e}")
        await send_safe(websocket, json.dumps({
            'type': 'get_contacts_response',
            'success': False,
            'message': 'Failed to get contacts'
        }))

async def handle_add_contact(websocket, data):
    """Handle add contact request"""
    try:
        user_uid = data.get('user_uid')
        contact_uid = data.get('contact_uid')
        contact_name = data.get('contact_name', '')
        
        if not user_uid or not contact_uid:
            await send_safe(websocket, json.dumps({
                'type': 'add_contact_response',
                'success': False,
                'message': 'user_uid and contact_uid are required'
            }))
            return
        
        if user_uid == contact_uid:
            await send_safe(websocket, json.dumps({
                'type': 'add_contact_response',
                'success': False,
                'message': 'Cannot add yourself as a contact'
            }))
            return
        
        db = get_db_connection()
        if not db:
            await send_safe(websocket, json.dumps({
                'type': 'add_contact_response',
                'success': False,
                'message': 'Database connection failed'
            }))
            return
        
        cursor = db.cursor()
        
        # Check if contact already exists
        cursor.execute("""
            SELECT id FROM contacts 
            WHERE user_uid = %s AND contact_uid = %s
        """, (user_uid, contact_uid))
        
        if cursor.fetchone():
            db.close()
            await send_safe(websocket, json.dumps({
                'type': 'add_contact_response',
                'success': False,
                'message': 'Contact already exists'
            }))
            return
        
        # Add contact
        cursor.execute("""
            INSERT INTO contacts (user_uid, contact_uid, contact_name)
            VALUES (%s, %s, %s)
        """, (user_uid, contact_uid, contact_name))
        
        db.commit()
        db.close()
        
        await send_safe(websocket, json.dumps({
            'type': 'add_contact_response',
            'success': True,
            'message': 'Contact added successfully'
        }))
        
        logger.info(f"User {user_uid} added contact {contact_uid}")
        
    except Exception as e:
        logger.error(f"Error adding contact: {e}")
        await send_safe(websocket, json.dumps({
            'type': 'add_contact_response',
            'success': False,
            'message': 'Failed to add contact'
        }))

async def handle_create_chat(websocket, data):
    """Handle create chat room request"""
    try:
        user1_uid = data.get('user1_uid')
        user2_uid = data.get('user2_uid')
        room_name = data.get('room_name', '')
        
        if not user1_uid or not user2_uid:
            await send_safe(websocket, json.dumps({
                'type': 'create_chat_response',
                'success': False,
                'message': 'user1_uid and user2_uid are required'
            }))
            return
        
        if user1_uid == user2_uid:
            await send_safe(websocket, json.dumps({
                'type': 'create_chat_response',
                'success': False,
                'message': 'Cannot create chat with yourself'
            }))
            return
        
        db = get_db_connection()
        if not db:
            await send_safe(websocket, json.dumps({
                'type': 'create_chat_response',
                'success': False,
                'message': 'Database connection failed'
            }))
            return
        
        cursor = db.cursor()
        
        # Generate unique room ID for private chat
        room_id = f"private_{min(user1_uid, user2_uid)}_{max(user1_uid, user2_uid)}"
        
        # Check if chat room already exists
        cursor.execute("""
            SELECT id FROM chat_rooms WHERE id = %s
        """, (room_id,))
        
        if cursor.fetchone():
            db.close()
            await send_safe(websocket, json.dumps({
                'type': 'create_chat_response',
                'success': True,
                'room_id': room_id,
                'message': 'Chat room already exists'
            }))
            return
        
        # Create chat room
        if not room_name:
            room_name = f"Private Chat"
        
        cursor.execute("""
            INSERT INTO chat_rooms (id, name, type, created_by)
            VALUES (%s, %s, 'personal', %s)
        """, (room_id, room_name, user1_uid))
        
        # Add both users as participants
        cursor.execute("""
            INSERT INTO room_participants (room_id, user_uid, is_admin)
            VALUES (%s, %s, TRUE), (%s, %s, FALSE)
        """, (room_id, user1_uid, room_id, user2_uid))
        
        db.commit()
        db.close()
        
        await send_safe(websocket, json.dumps({
            'type': 'create_chat_response',
            'success': True,
            'room_id': room_id,
            'message': 'Chat room created successfully'
        }))
        
        logger.info(f"Created private chat room {room_id} between {user1_uid} and {user2_uid}")
        
    except Exception as e:
        logger.error(f"Error creating chat room: {e}")
        await send_safe(websocket, json.dumps({
            'type': 'create_chat_response',
            'success': False,
            'message': 'Failed to create chat room'
        }))

async def handle_get_chat_rooms(websocket, data):
    """Handle get chat rooms request"""
    try:
        user_uid = data.get('user_uid')
        
        if not user_uid:
            await send_safe(websocket, json.dumps({
                'type': 'get_chat_rooms_response',
                'success': False,
                'message': 'user_uid is required'
            }))
            return
        
        db = get_db_connection()
        if not db:
            await send_safe(websocket, json.dumps({
                'type': 'get_chat_rooms_response',
                'success': False,
                'message': 'Database connection failed'
            }))
            return
        
        cursor = db.cursor(dictionary=True)
        
        # Get user's chat rooms
        cursor.execute("""
            SELECT cr.id, cr.name, cr.type, cr.created_at,
                   rp.joined_at, rp.is_admin
            FROM chat_rooms cr
            INNER JOIN room_participants rp ON cr.id = rp.room_id
            WHERE rp.user_uid = %s
            ORDER BY rp.joined_at DESC
        """, (user_uid,))
        
        rooms = cursor.fetchall()
        db.close()
        
        # Format rooms for response
        formatted_rooms = []
        for room in rooms:
            formatted_room = {
                "room_id": room["id"],
                "name": room["name"],
                "type": room["type"],
                "is_admin": bool(room["is_admin"]),
                "joined_at": room["joined_at"].isoformat() if room["joined_at"] else None,
                "created_at": room["created_at"].isoformat() if room["created_at"] else None
            }
            formatted_rooms.append(formatted_room)
        
        await send_safe(websocket, json.dumps({
            'type': 'get_chat_rooms_response',
            'success': True,
            'rooms': formatted_rooms,
            'count': len(formatted_rooms)
        }))
        
        logger.info(f"Sent {len(formatted_rooms)} chat rooms to user {user_uid}")
        
    except Exception as e:
        logger.error(f"Error getting chat rooms: {e}")
        await send_safe(websocket, json.dumps({
            'type': 'get_chat_rooms_response',
            'success': False,
            'message': 'Failed to get chat rooms'
        }))

async def handle_get_users(websocket, data):
    """Handle get_users request"""
    try:
        search_query = data.get("search_query", "").strip()
        
        db = get_db_connection()
        if not db:
            await send_safe(websocket, json.dumps({
                "type": "get_users_response",
                "status": "error",
                "message": "Database connection failed"
            }))
            return
        
        cursor = db.cursor(dictionary=True)
        
        if search_query:
            # Search users by email or display_name
            search_pattern = f"%{search_query}%"
            cursor.execute("""
                SELECT uid, email, display_name, status, last_seen, created_at
                FROM users 
                WHERE (email LIKE %s OR display_name LIKE %s)
                AND email IS NOT NULL
                ORDER BY 
                    CASE WHEN status = 'online' THEN 0 ELSE 1 END,
                    last_seen DESC,
                    email
                LIMIT 20
            """, (search_pattern, search_pattern))
        else:
            # Get all users
            cursor.execute("""
                SELECT uid, email, display_name, status, last_seen, created_at
                FROM users 
                WHERE email IS NOT NULL
                ORDER BY 
                    CASE WHEN status = 'online' THEN 0 ELSE 1 END,
                    last_seen DESC,
                    email
                LIMIT 50
            """)
        
        users = cursor.fetchall()
        db.close()
        
        # Format users for response
        formatted_users = []
        for user in users:
            formatted_user = {
                "uid": user["uid"],
                "email": user["email"],
                "name": user["display_name"] or user["email"].split('@')[0] if user["email"] else "Unknown",
                "phone_number": "",  # Not stored in current schema
                "profile_image_url": "",  # Not stored in current schema
                "bio": "Hey there! I'm using Bisto Chat.",  # Default bio
                "status": user["status"] or "offline"
            }
            formatted_users.append(formatted_user)
        
        response = {
            "type": "get_users_response",
            "status": "success",
            "users": formatted_users,
            "search_query": search_query or None
        }
        
        await send_safe(websocket, json.dumps(response))
        logger.info(f"Sent {len(formatted_users)} users for query: '{search_query}'")
        
    except Exception as e:
        logger.error(f"Error getting users: {e}")
        await send_safe(websocket, json.dumps({
            "type": "get_users_response",
            "status": "error",
            "message": f"Failed to get users: {str(e)}"
        }))

async def handle_save_user(websocket, data):
    """Handle save_user request"""
    try:
        uid = data.get("uid")
        name = data.get("name")
        email = data.get("email")
        phone_number = data.get("phone_number", "")
        bio = data.get("bio", "Hey there! I'm using Bisto Chat.")
        
        if not uid or not email or not name:
            await send_safe(websocket, json.dumps({
                "type": "save_user_response",
                "status": "error",
                "message": "uid, name, and email are required"
            }))
            return
        
        db = get_db_connection()
        if not db:
            await send_safe(websocket, json.dumps({
                "type": "save_user_response",
                "status": "error",
                "message": "Database connection failed"
            }))
            return
        
        cursor = db.cursor()
        
        # Insert or update user
        cursor.execute("""
            INSERT INTO users (uid, email, display_name, status, last_seen, created_at)
            VALUES (%s, %s, %s, 'online', NOW(), NOW())
            ON DUPLICATE KEY UPDATE
            display_name = VALUES(display_name),
            status = 'online',
            last_seen = NOW()
        """, (uid, email, name))
        
        db.commit()
        db.close()
        
        response = {
            "type": "save_user_response",
            "status": "saved",
            "user_uid": uid,
            "message": "User profile saved successfully"
        }
        
        await send_safe(websocket, json.dumps(response))
        logger.info(f"Saved user: {email}")
        
    except mysql.connector.IntegrityError as e:
        if "Duplicate entry" in str(e) and "email" in str(e):
            await send_safe(websocket, json.dumps({
                "type": "save_user_response",
                "status": "error",
                "message": "Email already exists"
            }))
        else:
            await send_safe(websocket, json.dumps({
                "type": "save_user_response",
                "status": "error",
                "message": f"Database error: {str(e)}"
            }))
    except Exception as e:
        logger.error(f"Error saving user: {e}")
        await send_safe(websocket, json.dumps({
            "type": "save_user_response",
            "status": "error",
            "message": f"Failed to save user: {str(e)}"
        }))

async def handle_get_user_profile(websocket, data):
    """Handle get_user_profile request"""
    try:
        uid = data.get("uid")
        
        if not uid:
            await send_safe(websocket, json.dumps({
                "type": "get_user_profile_response",
                "status": "error",
                "message": "uid is required"
            }))
            return
        
        db = get_db_connection()
        if not db:
            await send_safe(websocket, json.dumps({
                "type": "get_user_profile_response",
                "status": "error",
                "message": "Database connection failed"
            }))
            return
        
        cursor = db.cursor(dictionary=True)
        cursor.execute("""
            SELECT uid, email, display_name, status, last_seen, created_at
            FROM users 
            WHERE uid = %s
        """, (uid,))
        
        user = cursor.fetchone()
        db.close()
        
        if not user:
            await send_safe(websocket, json.dumps({
                "type": "get_user_profile_response",
                "status": "error",
                "message": "User not found"
            }))
            return
        
        # Format user for response
        formatted_user = {
            "uid": user["uid"],
            "email": user["email"],
            "name": user["display_name"] or user["email"].split('@')[0] if user["email"] else "Unknown",
            "phone_number": "",  # Not stored in current schema
            "profile_image_url": "",  # Not stored in current schema
            "bio": "Hey there! I'm using Bisto Chat.",  # Default bio
            "status": user["status"] or "offline"
        }
        
        response = {
            "type": "get_user_profile_response",
            "status": "success",
            "user": formatted_user
        }
        
        await send_safe(websocket, json.dumps(response))
        logger.info(f"Sent user profile: {user['email']}")
        
    except Exception as e:
        logger.error(f"Error getting user profile: {e}")
        await send_safe(websocket, json.dumps({
            "type": "get_user_profile_response",
            "status": "error",
            "message": f"Failed to get user profile: {str(e)}"
        }))

async def handle_user_disconnect(websocket):
    """Handle user disconnection"""
    try:
        if hasattr(websocket, 'user_uid') and websocket.user_uid:
            # Update user status to offline
            db = get_db_connection()
            if db:
                cursor = db.cursor()
                cursor.execute("""
                    UPDATE users 
                    SET status = 'offline', last_seen = NOW()
                    WHERE uid = %s
                """, (websocket.user_uid,))
                db.commit()
                db.close()
                
                logger.info(f"User {websocket.user_uid} disconnected")
        
        # Remove from current room
        if hasattr(websocket, 'current_room') and websocket.current_room:
            await leave_client_from_room(websocket, websocket.current_room)
        
        # Remove from global connected clients
        if websocket in connected_clients:
            connected_clients.remove(websocket)
            
    except Exception as e:
        logger.error(f"Error handling disconnect: {e}")

async def handler(websocket, path):
    """Main WebSocket handler"""
    client_address = websocket.remote_address
    logger.info(f"New client connected from {client_address}")
    
    connected_clients.add(websocket)
    
    try:
        # Send connection confirmation
        await send_safe(websocket, json.dumps({
            'type': 'connected',
            'message': 'Connected to Bisto Chat Server'
        }))
        
        async for message in websocket:
            try:
                data = json.loads(message)
                message_type = data.get('type', 'message')
                
                if message_type == 'join' or message_type == 'join_room':
                    await handle_user_join(websocket, data)
                    # Send message history after successful join
                    await send_message_history(websocket)
                    
                elif message_type == 'message' or message_type == 'chat_message':
                    await handle_message(websocket, data)
                    
                elif message_type == 'room_join':
                    await handle_room_join(websocket, data)
                    
                elif message_type == 'ping':
                    await send_safe(websocket, json.dumps({'type': 'pong'}))
                    
                elif message_type == 'search_users':
                    await handle_search_users(websocket, data)
                    
                elif message_type == 'get_contacts':
                    await handle_get_contacts(websocket, data)
                    
                elif message_type == 'add_contact':
                    await handle_add_contact(websocket, data)
                    
                elif message_type == 'create_chat':
                    await handle_create_chat(websocket, data)
                    
                elif message_type == 'get_chat_rooms':
                    await handle_get_chat_rooms(websocket, data)
                    
                elif message_type == 'get_users':
                    await handle_get_users(websocket, data)
                    
                elif message_type == 'save_user':
                    await handle_save_user(websocket, data)
                    
                elif message_type == 'get_user_profile':
                    await handle_get_user_profile(websocket, data)
                    
                else:
                    logger.warning(f"Unknown message type: {message_type}")
                    
            except json.JSONDecodeError as e:
                logger.error(f"Invalid JSON received: {e}")
                await send_safe(websocket, json.dumps({
                    'type': 'error',
                    'message': 'Invalid JSON format'
                }))
            except Exception as e:
                logger.error(f"Error processing message: {e}")
                
    except websockets.exceptions.ConnectionClosed:
        logger.info(f"Client {client_address} disconnected")
    except Exception as e:
        logger.error(f"Handler error: {e}")
    finally:
        await handle_user_disconnect(websocket)

async def cleanup_offline_users():
    """Periodic cleanup of old offline users and expired messages"""
    while True:
        try:
            await asyncio.sleep(60)  # Run every 1 minute for message cleanup
            
            db = get_db_connection()
            if db:
                cursor = db.cursor()
                
                # Delete messages older than 2 minutes
                cursor.execute("""
                    DELETE FROM messages 
                    WHERE timestamp < DATE_SUB(NOW(), INTERVAL 2 MINUTE)
                """)
                
                deleted_messages = cursor.rowcount
                if deleted_messages > 0:
                    logger.info(f"Deleted {deleted_messages} expired messages (older than 2 minutes)")
                
                # Mark users as offline if they haven't been seen for 10 minutes
                cursor.execute("""
                    UPDATE users 
                    SET status = 'offline' 
                    WHERE status = 'online' 
                    AND last_seen < DATE_SUB(NOW(), INTERVAL 10 MINUTE)
                """)
                
                affected_rows = cursor.rowcount
                if affected_rows > 0:
                    logger.info(f"Marked {affected_rows} users as offline")
                
                db.commit()
                db.close()
                
        except Exception as e:
            logger.error(f"Cleanup error: {e}")

async def main():
    """Main server function"""
    logger.info("Starting Bisto Chat WebSocket Server...")
    
    # Initialize database
    if not init_database():
        logger.error("Failed to initialize database. Exiting.")
        return
    
    # Start cleanup task
    cleanup_task = asyncio.create_task(cleanup_offline_users())
    
    # Start WebSocket server
    try:
        async with websockets.serve(
            handler, 
            "0.0.0.0", 
            8081,
            ping_interval=20,
            ping_timeout=10
        ):
            logger.info("WebSocket server running on ws://0.0.0.0:8081")
            logger.info("Database: MySQL on localhost:3306")
            logger.info("Server is ready to accept connections...")
            
            # Keep server running
            await asyncio.Future()
            
    except Exception as e:
        logger.error(f"Server startup error: {e}")
    finally:
        cleanup_task.cancel()

if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        logger.info("Server stopped by user")
    except Exception as e:
        logger.error(f"Server error: {e}")