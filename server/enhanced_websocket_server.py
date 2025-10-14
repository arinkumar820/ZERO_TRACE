#!/usr/bin/env python3
"""
Enhanced WebSocket Server for Bisto Chat
Handles both chat messages and user data operations through a single WebSocket connection

Features:
- User data storage (save_user, get_users, get_user_profile)
- Chat messaging (send_message, join_room, leave_room)
- Real-time communication
- MySQL database storage
- Multi-action support through JSON message types
"""

import asyncio
import websockets
import json
import mysql.connector
import logging
import time
from datetime import datetime
import uuid
from typing import Dict, Set, Optional

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# MySQL Database Configuration
DB_CONFIG = {
    "host": "localhost",
    "user": "root",
    "password": "rudra@69420",  # Your MySQL password
    "database": "message_database",
    "autocommit": True
}

# Global variables for client management
connected_clients: Dict[websockets.WebSocketServerProtocol, dict] = {}  # websocket -> client_info
room_clients: Dict[str, Set[websockets.WebSocketServerProtocol]] = {}   # room_id -> set of websockets
user_sessions: Dict[str, websockets.WebSocketServerProtocol] = {}       # user_uid -> websocket

class DatabaseManager:
    """Handles all database operations"""
    
    @staticmethod
    def get_connection():
        """Get MySQL database connection"""
        try:
            return mysql.connector.connect(**DB_CONFIG)
        except mysql.connector.Error as err:
            logger.error(f"Database connection error: {err}")
            return None
    
    @staticmethod
    def init_database():
        """Initialize database with required tables"""
        try:
            # First create database if not exists
            temp_config = DB_CONFIG.copy()
            temp_config.pop('database', None)
            
            temp_conn = mysql.connector.connect(**temp_config)
            temp_cursor = temp_conn.cursor()
            temp_cursor.execute("CREATE DATABASE IF NOT EXISTS message_database")
            temp_conn.commit()
            temp_conn.close()
            
            # Now create tables
            conn = DatabaseManager.get_connection()
            if not conn:
                return False
                
            cursor = conn.cursor()
            
            # Users table
            cursor.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    uid VARCHAR(255) PRIMARY KEY,
                    name VARCHAR(255) NOT NULL,
                    email VARCHAR(255) UNIQUE NOT NULL,
                    phone_number VARCHAR(20),
                    profile_image_url TEXT,
                    bio TEXT,
                    status ENUM('online', 'offline', 'away') DEFAULT 'offline',
                    last_seen DATETIME DEFAULT CURRENT_TIMESTAMP,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_email (email),
                    INDEX idx_name (name),
                    INDEX idx_status (status)
                )
            """)
            
            # Chat rooms table
            cursor.execute("""
                CREATE TABLE IF NOT EXISTS chat_rooms (
                    id VARCHAR(255) PRIMARY KEY,
                    name VARCHAR(255) NOT NULL,
                    description TEXT,
                    type ENUM('group', 'personal') DEFAULT 'personal',
                    created_by VARCHAR(255) NOT NULL,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_type (type),
                    INDEX idx_created_by (created_by),
                    FOREIGN KEY (created_by) REFERENCES users(uid) ON DELETE CASCADE
                )
            """)
            
            # Messages table
            cursor.execute("""
                CREATE TABLE IF NOT EXISTS messages (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    message_id VARCHAR(255) UNIQUE NOT NULL,
                    chat_room_id VARCHAR(255) NOT NULL,
                    sender_uid VARCHAR(255) NOT NULL,
                    sender_email VARCHAR(255) NOT NULL,
                    sender_name VARCHAR(255) NOT NULL,
                    message TEXT NOT NULL,
                    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                    message_type VARCHAR(50) DEFAULT 'text',
                    status VARCHAR(50) DEFAULT 'sent',
                    INDEX idx_timestamp (timestamp),
                    INDEX idx_sender (sender_uid),
                    INDEX idx_room (chat_room_id),
                    INDEX idx_room_time (chat_room_id, timestamp),
                    FOREIGN KEY (sender_uid) REFERENCES users(uid) ON DELETE CASCADE
                )
            """)
            
            # Room participants table
            cursor.execute("""
                CREATE TABLE IF NOT EXISTS room_participants (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    room_id VARCHAR(255) NOT NULL,
                    user_uid VARCHAR(255) NOT NULL,
                    joined_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    is_admin BOOLEAN DEFAULT FALSE,
                    UNIQUE KEY unique_participant (room_id, user_uid),
                    INDEX idx_room (room_id),
                    INDEX idx_user (user_uid),
                    FOREIGN KEY (room_id) REFERENCES chat_rooms(id) ON DELETE CASCADE,
                    FOREIGN KEY (user_uid) REFERENCES users(uid) ON DELETE CASCADE
                )
            """)
            
            conn.commit()
            conn.close()
            logger.info("✅ Database initialized successfully")
            return True
            
        except mysql.connector.Error as err:
            logger.error(f"❌ Database initialization error: {err}")
            return False
    
    @staticmethod
    def save_user(user_data):
        """Save or update user data"""
        try:
            conn = DatabaseManager.get_connection()
            if not conn:
                return {"status": "error", "message": "Database connection failed"}
            
            cursor = conn.cursor()
            
            # Insert or update user
            cursor.execute("""
                INSERT INTO users (uid, name, email, phone_number, profile_image_url, bio, status, last_seen)
                VALUES (%s, %s, %s, %s, %s, %s, 'online', CURRENT_TIMESTAMP)
                ON DUPLICATE KEY UPDATE 
                    name = VALUES(name),
                    email = VALUES(email),
                    phone_number = VALUES(phone_number),
                    profile_image_url = VALUES(profile_image_url),
                    bio = VALUES(bio),
                    status = 'online',
                    last_seen = CURRENT_TIMESTAMP,
                    updated_at = CURRENT_TIMESTAMP
            """, (
                user_data.get('uid'),
                user_data.get('name'),
                user_data.get('email'),
                user_data.get('phone_number', ''),
                user_data.get('profile_image_url', ''),
                user_data.get('bio', 'Hey there! I am using Bisto Chat.'),
            ))
            
            conn.commit()
            conn.close()
            
            logger.info(f"✅ User saved: {user_data.get('email')}")
            return {"status": "saved", "message": "User data saved successfully"}
            
        except mysql.connector.Error as err:
            logger.error(f"❌ Error saving user: {err}")
            return {"status": "error", "message": f"Database error: {err}"}
    
    @staticmethod
    def get_users(search_query=None, current_user_uid=None):
        """Get all users or search users"""
        try:
            conn = DatabaseManager.get_connection()
            if not conn:
                return {"status": "error", "message": "Database connection failed"}
            
            cursor = conn.cursor(dictionary=True)
            
            if search_query:
                # Search users by name or email
                search_pattern = f"%{search_query.lower()}%"
                cursor.execute("""
                    SELECT uid, name, email, phone_number, profile_image_url, bio, status, last_seen
                    FROM users 
                    WHERE (LOWER(name) LIKE %s OR LOWER(email) LIKE %s)
                      AND uid != %s
                    ORDER BY 
                        CASE 
                            WHEN LOWER(name) = %s THEN 1
                            WHEN LOWER(email) = %s THEN 2
                            WHEN LOWER(name) LIKE %s THEN 3
                            WHEN LOWER(email) LIKE %s THEN 4
                            ELSE 5
                        END
                    LIMIT 50
                """, (search_pattern, search_pattern, current_user_uid or '', 
                      search_query.lower(), search_query.lower(), search_pattern, search_pattern))
            else:
                # Get all users except current user
                cursor.execute("""
                    SELECT uid, name, email, phone_number, profile_image_url, bio, status, last_seen
                    FROM users 
                    WHERE uid != %s
                    ORDER BY name
                    LIMIT 100
                """, (current_user_uid or '',))
            
            users = cursor.fetchall()
            
            # Convert datetime to string for JSON serialization
            for user in users:
                if user.get('last_seen'):
                    user['last_seen'] = user['last_seen'].isoformat()
            
            conn.close()
            
            logger.info(f"🔍 Found {len(users)} users")
            return {"status": "success", "users": users, "count": len(users)}
            
        except mysql.connector.Error as err:
            logger.error(f"❌ Error getting users: {err}")
            return {"status": "error", "message": f"Database error: {err}"}
    
    @staticmethod
    def get_user_profile(uid):
        """Get specific user profile"""
        try:
            conn = DatabaseManager.get_connection()
            if not conn:
                return {"status": "error", "message": "Database connection failed"}
            
            cursor = conn.cursor(dictionary=True)
            cursor.execute("""
                SELECT uid, name, email, phone_number, profile_image_url, bio, status, last_seen, created_at
                FROM users 
                WHERE uid = %s
            """, (uid,))
            
            user = cursor.fetchone()
            conn.close()
            
            if user:
                # Convert datetime to string for JSON serialization
                if user.get('last_seen'):
                    user['last_seen'] = user['last_seen'].isoformat()
                if user.get('created_at'):
                    user['created_at'] = user['created_at'].isoformat()
                
                logger.info(f"👤 Found user profile: {user.get('email')}")
                return {"status": "success", "user": user}
            else:
                return {"status": "error", "message": "User not found"}
                
        except mysql.connector.Error as err:
            logger.error(f"❌ Error getting user profile: {err}")
            return {"status": "error", "message": f"Database error: {err}"}
    
    @staticmethod
    def save_message(message_data):
        """Save chat message to database"""
        try:
            conn = DatabaseManager.get_connection()
            if not conn:
                return {"status": "error", "message": "Database connection failed"}
            
            cursor = conn.cursor()
            
            message_id = str(uuid.uuid4())
            cursor.execute("""
                INSERT INTO messages (message_id, chat_room_id, sender_uid, sender_email, sender_name, message, message_type)
                VALUES (%s, %s, %s, %s, %s, %s, %s)
            """, (
                message_id,
                message_data.get('chat_room_id', 'general'),
                message_data.get('sender_uid'),
                message_data.get('sender_email'),
                message_data.get('sender_name'),
                message_data.get('message'),
                message_data.get('message_type', 'text')
            ))
            
            conn.commit()
            conn.close()
            
            logger.info(f"💬 Message saved: {message_data.get('sender_email')}")
            return {"status": "saved", "message_id": message_id}
            
        except mysql.connector.Error as err:
            logger.error(f"❌ Error saving message: {err}")
            return {"status": "error", "message": f"Database error: {err}"}

async def send_safe(websocket, message_dict):
    """Safely send JSON message to websocket"""
    try:
        if websocket.open:
            message_json = json.dumps(message_dict)
            await websocket.send(message_json)
            return True
    except websockets.exceptions.ConnectionClosed:
        await cleanup_client(websocket)
    except Exception as e:
        logger.error(f"Error sending message: {e}")
        await cleanup_client(websocket)
    return False

async def cleanup_client(websocket):
    """Clean up disconnected client"""
    # Remove from connected clients
    client_info = connected_clients.pop(websocket, {})
    
    # Remove from room clients
    room_id = client_info.get('room_id')
    if room_id and room_id in room_clients:
        room_clients[room_id].discard(websocket)
        if not room_clients[room_id]:
            del room_clients[room_id]
    
    # Remove from user sessions
    user_uid = client_info.get('uid')
    if user_uid and user_uid in user_sessions:
        if user_sessions[user_uid] == websocket:
            del user_sessions[user_uid]
    
    if client_info:
        logger.info(f"🔌 Client disconnected: {client_info.get('email', 'Unknown')}")

async def broadcast_to_room(message_dict, room_id, exclude_websocket=None):
    """Broadcast message to all clients in a room"""
    if room_id not in room_clients:
        logger.info(f"📭 No clients in room {room_id}")
        return
    
    disconnected = set()
    successful_sends = 0
    
    for client in room_clients[room_id].copy():
        if client != exclude_websocket:
            if await send_safe(client, message_dict):
                successful_sends += 1
            else:
                disconnected.add(client)
    
    # Clean up disconnected clients
    for client in disconnected:
        await cleanup_client(client)
    
    logger.info(f"📡 Broadcast to {successful_sends} clients in room {room_id}")

async def handle_save_user(websocket, data):
    """Handle save_user message"""
    try:
        required_fields = ['uid', 'name', 'email']
        for field in required_fields:
            if not data.get(field):
                await send_safe(websocket, {
                    "type": "save_user_response",
                    "status": "error",
                    "message": f"Missing required field: {field}"
                })
                return
        
        # Save user to database
        result = DatabaseManager.save_user(data)
        
        # Update client info
        client_info = connected_clients.get(websocket, {})
        client_info.update({
            'uid': data['uid'],
            'name': data['name'],
            'email': data['email'],
            'phone_number': data.get('phone_number', ''),
            'bio': data.get('bio', '')
        })
        connected_clients[websocket] = client_info
        
        # Update user sessions
        user_sessions[data['uid']] = websocket
        
        # Send response
        await send_safe(websocket, {
            "type": "save_user_response",
            "status": result["status"],
            "message": result["message"],
            "user_uid": data['uid']
        })
        
    except Exception as e:
        logger.error(f"Error handling save_user: {e}")
        await send_safe(websocket, {
            "type": "save_user_response",
            "status": "error",
            "message": "Internal server error"
        })

async def handle_get_users(websocket, data):
    """Handle get_users message"""
    try:
        search_query = data.get('search_query')
        current_user_uid = connected_clients.get(websocket, {}).get('uid')
        
        # Get users from database
        result = DatabaseManager.get_users(search_query, current_user_uid)
        
        # Send response
        await send_safe(websocket, {
            "type": "get_users_response",
            "status": result["status"],
            "users": result.get("users", []),
            "count": result.get("count", 0),
            "search_query": search_query
        })
        
    except Exception as e:
        logger.error(f"Error handling get_users: {e}")
        await send_safe(websocket, {
            "type": "get_users_response",
            "status": "error",
            "message": "Internal server error"
        })

async def handle_get_user_profile(websocket, data):
    """Handle get_user_profile message"""
    try:
        uid = data.get('uid')
        if not uid:
            await send_safe(websocket, {
                "type": "get_user_profile_response",
                "status": "error",
                "message": "Missing user UID"
            })
            return
        
        # Get user profile from database
        result = DatabaseManager.get_user_profile(uid)
        
        # Send response
        response = {
            "type": "get_user_profile_response",
            "status": result["status"]
        }
        
        if result["status"] == "success":
            response["user"] = result["user"]
        else:
            response["message"] = result["message"]
        
        await send_safe(websocket, response)
        
    except Exception as e:
        logger.error(f"Error handling get_user_profile: {e}")
        await send_safe(websocket, {
            "type": "get_user_profile_response",
            "status": "error",
            "message": "Internal server error"
        })

async def handle_join_room(websocket, data):
    """Handle join_room message"""
    try:
        room_id = data.get('room_id', 'general')
        
        # Leave previous room
        client_info = connected_clients.get(websocket, {})
        old_room = client_info.get('room_id')
        if old_room and old_room in room_clients:
            room_clients[old_room].discard(websocket)
        
        # Join new room
        if room_id not in room_clients:
            room_clients[room_id] = set()
        room_clients[room_id].add(websocket)
        
        # Update client info
        client_info['room_id'] = room_id
        connected_clients[websocket] = client_info
        
        # Send confirmation
        await send_safe(websocket, {
            "type": "join_room_response",
            "status": "success",
            "room_id": room_id,
            "message": f"Joined room {room_id}"
        })
        
        # Notify room about new member
        user_name = client_info.get('name', 'User')
        await broadcast_to_room({
            "type": "user_joined",
            "room_id": room_id,
            "user_name": user_name,
            "timestamp": datetime.now().isoformat()
        }, room_id, exclude_websocket=websocket)
        
        logger.info(f"👥 User {user_name} joined room {room_id}")
        
    except Exception as e:
        logger.error(f"Error handling join_room: {e}")
        await send_safe(websocket, {
            "type": "join_room_response",
            "status": "error",
            "message": "Failed to join room"
        })

async def handle_send_message(websocket, data):
    """Handle send_message message"""
    try:
        client_info = connected_clients.get(websocket, {})
        
        # Validate required fields
        if not data.get('message'):
            await send_safe(websocket, {
                "type": "send_message_response",
                "status": "error",
                "message": "Message content is required"
            })
            return
        
        # Prepare message data
        message_data = {
            'chat_room_id': data.get('room_id', client_info.get('room_id', 'general')),
            'sender_uid': client_info.get('uid', 'anonymous'),
            'sender_email': client_info.get('email', 'anonymous@example.com'),
            'sender_name': client_info.get('name', 'Anonymous'),
            'message': data['message'],
            'message_type': data.get('message_type', 'text')
        }
        
        # Save to database
        result = DatabaseManager.save_message(message_data)
        
        if result["status"] == "saved":
            # Prepare broadcast message
            broadcast_data = {
                "type": "new_message",
                "room_id": message_data['chat_room_id'],
                "message_id": result["message_id"],
                "sender_uid": message_data['sender_uid'],
                "sender_email": message_data['sender_email'],
                "sender_name": message_data['sender_name'],
                "message": message_data['message'],
                "message_type": message_data['message_type'],
                "timestamp": datetime.now().isoformat()
            }
            
            # Broadcast to room
            await broadcast_to_room(broadcast_data, message_data['chat_room_id'])
            
            # Send confirmation to sender
            await send_safe(websocket, {
                "type": "send_message_response",
                "status": "success",
                "message_id": result["message_id"]
            })
            
            logger.info(f"💬 Message sent by {message_data['sender_name']}: {message_data['message'][:50]}...")
        else:
            await send_safe(websocket, {
                "type": "send_message_response",
                "status": "error",
                "message": result["message"]
            })
        
    except Exception as e:
        logger.error(f"Error handling send_message: {e}")
        await send_safe(websocket, {
            "type": "send_message_response",
            "status": "error",
            "message": "Failed to send message"
        })

async def handle_message(websocket, message_data):
    """Handle incoming WebSocket messages based on type"""
    try:
        message_type = message_data.get('type', 'unknown')
        
        logger.info(f"📨 Received message type: {message_type}")
        
        if message_type == 'save_user':
            await handle_save_user(websocket, message_data)
            
        elif message_type == 'get_users':
            await handle_get_users(websocket, message_data)
            
        elif message_type == 'get_user_profile':
            await handle_get_user_profile(websocket, message_data)
            
        elif message_type == 'join_room':
            await handle_join_room(websocket, message_data)
            
        elif message_type == 'send_message':
            await handle_send_message(websocket, message_data)
            
        elif message_type == 'ping':
            await send_safe(websocket, {
                "type": "pong",
                "timestamp": datetime.now().isoformat()
            })
            
        else:
            logger.warning(f"Unknown message type: {message_type}")
            await send_safe(websocket, {
                "type": "error",
                "message": f"Unknown message type: {message_type}"
            })
    
    except Exception as e:
        logger.error(f"Error handling message: {e}")
        await send_safe(websocket, {
            "type": "error",
            "message": "Internal server error"
        })

async def handle_client(websocket, path):
    """Handle new WebSocket client connection"""
    client_ip = websocket.remote_address[0] if websocket.remote_address else "unknown"
    logger.info(f"🔌 New client connected from {client_ip}")
    
    # Initialize client info
    connected_clients[websocket] = {
        'ip': client_ip,
        'connected_at': datetime.now().isoformat()
    }
    
    try:
        # Send welcome message
        await send_safe(websocket, {
            "type": "welcome",
            "message": "Connected to Enhanced Bisto Chat Server",
            "timestamp": datetime.now().isoformat(),
            "supported_actions": [
                "save_user",
                "get_users", 
                "get_user_profile",
                "join_room",
                "send_message",
                "ping"
            ]
        })
        
        # Listen for messages
        async for message in websocket:
            try:
                message_data = json.loads(message)
                await handle_message(websocket, message_data)
            except json.JSONDecodeError:
                logger.error(f"Invalid JSON from {client_ip}: {message}")
                await send_safe(websocket, {
                    "type": "error",
                    "message": "Invalid JSON format"
                })
            except Exception as e:
                logger.error(f"Error processing message from {client_ip}: {e}")
    
    except websockets.exceptions.ConnectionClosed:
        logger.info(f"🔌 Client {client_ip} disconnected")
    except Exception as e:
        logger.error(f"Error with client {client_ip}: {e}")
    finally:
        await cleanup_client(websocket)

async def show_server_stats():
    """Show server statistics periodically"""
    while True:
        await asyncio.sleep(60)  # Every minute
        total_clients = len(connected_clients)
        total_rooms = len(room_clients)
        total_users = len(user_sessions)
        
        logger.info(f"📊 Server Stats - Clients: {total_clients}, Rooms: {total_rooms}, Users: {total_users}")

async def main():
    """Start the Enhanced WebSocket Server"""
    HOST = "0.0.0.0"
    PORT = 8080
    
    logger.info("=" * 60)
    logger.info("🚀 Starting Enhanced Bisto Chat WebSocket Server")
    logger.info(f"📡 Server will listen on: {HOST}:{PORT}")
    logger.info(f"🔗 WebSocket URL: ws://localhost:{PORT}")
    logger.info(f"🔗 External URL: ws://YOUR_IP:{PORT}")
    logger.info("=" * 60)
    
    # Initialize database
    if not DatabaseManager.init_database():
        logger.error("❌ Failed to initialize database")
        return
    
    # Start stats task
    asyncio.create_task(show_server_stats())
    
    # Start WebSocket server
    try:
        async with websockets.serve(
            handle_client, 
            HOST, 
            PORT,
            ping_interval=30,
            ping_timeout=15
        ):
            logger.info(f"✅ Enhanced WebSocket server running on ws://{HOST}:{PORT}")
            logger.info("📱 Supported message types:")
            logger.info("   - save_user: Store user profile data")
            logger.info("   - get_users: Search and retrieve users")
            logger.info("   - get_user_profile: Get specific user details")
            logger.info("   - join_room: Join a chat room")
            logger.info("   - send_message: Send chat messages")
            logger.info("   - ping: Keep connection alive")
            logger.info("")
            logger.info("🛑 Press Ctrl+C to stop the server")
            
            # Keep server running
            await asyncio.Future()
            
    except KeyboardInterrupt:
        logger.info("🛑 Server shutdown requested by user")
    except Exception as e:
        logger.error(f"❌ Server error: {e}")
    finally:
        logger.info("👋 Enhanced WebSocket server stopped")

if __name__ == "__main__":
    asyncio.run(main())