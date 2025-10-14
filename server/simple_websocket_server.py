#!/usr/bin/env python3
"""
Simple Bisto Chat WebSocket Server
Lightweight server for testing chat functionality without database dependencies
"""

import asyncio
import websockets
import json
import logging
import time
from datetime import datetime
import uuid

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Global variables for client management
connected_clients = set()  # All connected clients
room_clients = {}  # Dict: room_id -> set of websockets in that room
client_info = {}   # Dict: websocket -> {uid, email, room_id, etc}

class SimpleMemoryStorage:
    """Simple in-memory storage for messages and rooms"""
    def __init__(self):
        self.messages = []
        self.rooms = {
            'general_chat': {
                'id': 'general_chat',
                'name': 'General Chat',
                'type': 'group',
                'created_at': datetime.now().isoformat()
            }
        }
        self.users = {}
    
    def add_message(self, room_id, sender_uid, sender_email, message, message_type='text'):
        """Add a message to memory storage"""
        message_data = {
            'id': len(self.messages) + 1,
            'chat_room_id': room_id,
            'sender_uid': sender_uid,
            'sender_email': sender_email,
            'message': message,
            'timestamp': datetime.now().isoformat(),
            'message_type': message_type
        }
        self.messages.append(message_data)
        logger.info(f"Message stored: {sender_email} in {room_id}: {message[:50]}...")
        return message_data
    
    def get_room_messages(self, room_id, limit=50):
        """Get recent messages for a room"""
        room_messages = [msg for msg in self.messages if msg['chat_room_id'] == room_id]
        return room_messages[-limit:] if room_messages else []
    
    def create_room(self, room_id, room_name, room_type='group'):
        """Create a new chat room"""
        if room_id not in self.rooms:
            self.rooms[room_id] = {
                'id': room_id,
                'name': room_name,
                'type': room_type,
                'created_at': datetime.now().isoformat()
            }
            logger.info(f"Room created: {room_id} - {room_name}")
        return self.rooms[room_id]

# Global storage instance
storage = SimpleMemoryStorage()

async def send_safe(websocket, message):
    """Safely send message to websocket"""
    try:
        if websocket.open:
            await websocket.send(message)
            return True
    except websockets.exceptions.ConnectionClosed:
        logger.warning("Attempted to send to closed connection")
        await cleanup_client(websocket)
    except Exception as e:
        logger.error(f"Error sending message: {e}")
        await cleanup_client(websocket)
    return False

async def cleanup_client(websocket):
    """Clean up disconnected client from all data structures"""
    # Remove from connected clients
    connected_clients.discard(websocket)
    
    # Remove from room clients
    client_room = None
    if websocket in client_info:
        client_room = client_info[websocket].get('room_id')
    
    if client_room and client_room in room_clients:
        room_clients[client_room].discard(websocket)
        if not room_clients[client_room]:  # Remove empty room
            del room_clients[client_room]
    
    # Remove client info
    if websocket in client_info:
        logger.info(f"Client disconnected: {client_info[websocket].get('email', 'Unknown')}")
        del client_info[websocket]

async def join_client_to_room(websocket, room_id):
    """Add client to a specific room"""
    if room_id not in room_clients:
        room_clients[room_id] = set()
    
    # Remove client from previous room if any
    if websocket in client_info:
        old_room = client_info[websocket].get('room_id')
        if old_room and old_room in room_clients:
            room_clients[old_room].discard(websocket)
    
    # Add to new room
    room_clients[room_id].add(websocket)
    if websocket in client_info:
        client_info[websocket]['room_id'] = room_id
    
    logger.info(f"Client joined room {room_id}. Room now has {len(room_clients[room_id])} clients")

async def broadcast_to_room(message_data, room_id, exclude_websocket=None):
    """Broadcast message to all clients in a specific room"""
    if room_id not in room_clients or not room_clients[room_id]:
        logger.info(f"No clients in room {room_id} to broadcast to")
        return
    
    message_json = json.dumps(message_data)
    disconnected = set()
    successful_sends = 0
    
    logger.info(f"Broadcasting to {len(room_clients[room_id])} clients in room {room_id}")
    
    for client in room_clients[room_id].copy():
        if client != exclude_websocket:
            if await send_safe(client, message_json):
                successful_sends += 1
            else:
                disconnected.add(client)
    
    # Clean up disconnected clients
    for client in disconnected:
        await cleanup_client(client)
    
    logger.info(f"Message broadcast successful to {successful_sends} clients in room {room_id}")

async def handle_client_message(websocket, message_data):
    """Process incoming message from client"""
    try:
        message_type = message_data.get('type', 'unknown')
        
        if message_type == 'join_room':
            # Client joining a room
            room_id = message_data.get('room_id', 'general_chat')
            user_uid = message_data.get('user_uid', 'anonymous')
            user_email = message_data.get('user_email', 'anonymous@example.com')
            
            # Store client info
            client_info[websocket] = {
                'uid': user_uid,
                'email': user_email,
                'room_id': room_id,
                'joined_at': datetime.now().isoformat()
            }
            
            # Join the room
            await join_client_to_room(websocket, room_id)
            
            # Send room history
            recent_messages = storage.get_room_messages(room_id, 20)
            await send_safe(websocket, json.dumps({
                'type': 'room_history',
                'room_id': room_id,
                'messages': recent_messages
            }))
            
            # Notify room about new member
            await broadcast_to_room({
                'type': 'user_joined',
                'room_id': room_id,
                'user_email': user_email,
                'timestamp': datetime.now().isoformat()
            }, room_id, exclude_websocket=websocket)
            
            logger.info(f"User {user_email} joined room {room_id}")
        
        elif message_type == 'chat_message':
            # Regular chat message
            if websocket not in client_info:
                await send_safe(websocket, json.dumps({
                    'type': 'error',
                    'message': 'You must join a room first'
                }))
                return
            
            client = client_info[websocket]
            room_id = client.get('room_id', 'general_chat')
            message_text = message_data.get('message', '')
            
            if not message_text.strip():
                return  # Ignore empty messages
            
            # Store the message
            stored_message = storage.add_message(
                room_id=room_id,
                sender_uid=client['uid'],
                sender_email=client['email'],
                message=message_text,
                message_type='text'
            )
            
            # Broadcast to room
            broadcast_data = {
                'type': 'new_message',
                'room_id': room_id,
                'message_id': stored_message['id'],
                'sender_uid': client['uid'],
                'sender_email': client['email'],
                'message': message_text,
                'timestamp': stored_message['timestamp'],
                'message_type': 'text'
            }
            
            await broadcast_to_room(broadcast_data, room_id)
            
        elif message_type == 'ping':
            # Ping/pong for connection keep-alive
            await send_safe(websocket, json.dumps({
                'type': 'pong',
                'timestamp': datetime.now().isoformat()
            }))
        
        else:
            logger.warning(f"Unknown message type: {message_type}")
            await send_safe(websocket, json.dumps({
                'type': 'error',
                'message': f'Unknown message type: {message_type}'
            }))
    
    except Exception as e:
        logger.error(f"Error handling message: {e}")
        await send_safe(websocket, json.dumps({
            'type': 'error',
            'message': 'Internal server error'
        }))

async def handle_client(websocket, path):
    """Handle new client connection"""
    client_ip = websocket.remote_address[0] if websocket.remote_address else "unknown"
    logger.info(f"New client connected from {client_ip}")
    
    # Add to connected clients
    connected_clients.add(websocket)
    
    try:
        # Send welcome message
        await send_safe(websocket, json.dumps({
            'type': 'welcome',
            'message': 'Connected to Bisto Chat Server',
            'timestamp': datetime.now().isoformat(),
            'server_info': {
                'version': '1.0',
                'features': ['chat', 'rooms', 'history']
            }
        }))
        
        # Listen for messages
        async for message in websocket:
            try:
                message_data = json.loads(message)
                await handle_client_message(websocket, message_data)
            except json.JSONDecodeError:
                logger.error(f"Invalid JSON received from {client_ip}: {message}")
                await send_safe(websocket, json.dumps({
                    'type': 'error',
                    'message': 'Invalid JSON format'
                }))
            except Exception as e:
                logger.error(f"Error processing message from {client_ip}: {e}")
    
    except websockets.exceptions.ConnectionClosed:
        logger.info(f"Client {client_ip} disconnected")
    except Exception as e:
        logger.error(f"Error with client {client_ip}: {e}")
    finally:
        await cleanup_client(websocket)

async def show_server_stats():
    """Periodically show server statistics"""
    while True:
        await asyncio.sleep(30)  # Every 30 seconds
        total_clients = len(connected_clients)
        total_rooms = len(room_clients)
        total_messages = len(storage.messages)
        
        logger.info(f"Server Stats - Clients: {total_clients}, Rooms: {total_rooms}, Messages: {total_messages}")
        
        # Show room details
        for room_id, clients in room_clients.items():
            logger.info(f"Room '{room_id}': {len(clients)} clients")

async def main():
    """Start the WebSocket server"""
    # Server configuration
    HOST = "0.0.0.0"  # Listen on all interfaces
    PORT = 8080
    
    logger.info("="*50)
    logger.info("🚀 Starting Simple Bisto Chat WebSocket Server")
    logger.info(f"📡 Server will listen on: {HOST}:{PORT}")
    logger.info(f"🔗 WebSocket URL: ws://localhost:{PORT}")
    logger.info(f"🔗 External URL: ws://YOUR_IP:{PORT}")
    logger.info("="*50)
    
    # Create default room
    storage.create_room('general_chat', 'General Chat', 'group')
    logger.info("✅ Default 'general_chat' room created")
    
    # Start stats task
    asyncio.create_task(show_server_stats())
    
    # Start WebSocket server
    try:
        async with websockets.serve(
            handle_client, 
            HOST, 
            PORT,
            ping_interval=20,  # Send ping every 20 seconds
            ping_timeout=10,   # Wait 10 seconds for pong
            close_timeout=10   # Close timeout
        ):
            logger.info(f"✅ WebSocket server running on ws://{HOST}:{PORT}")
            logger.info("📱 Your Android app can now connect!")
            logger.info("🛑 Press Ctrl+C to stop the server")
            
            # Keep server running
            await asyncio.Future()  # Run forever
            
    except KeyboardInterrupt:
        logger.info("🛑 Server shutdown requested by user")
    except Exception as e:
        logger.error(f"❌ Server error: {e}")
    finally:
        logger.info("👋 Server stopped")

if __name__ == "__main__":
    # Check if we can import websockets
    try:
        import websockets
        logger.info("✅ websockets library found")
    except ImportError:
        logger.error("❌ websockets library not found!")
        logger.error("📦 Install it with: pip install websockets")
        exit(1)
    
    # Start the server
    asyncio.run(main())