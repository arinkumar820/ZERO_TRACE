#!/usr/bin/env python3
"""
Bisto Chat Database Server
A Flask-based API server for the Android chat application
Provides RESTful endpoints for user management, contacts, and messaging
"""

import os
import sqlite3
import json
from datetime import datetime, timedelta
from flask import Flask, request, jsonify
from flask_cors import CORS
from werkzeug.security import generate_password_hash, check_password_hash
import uuid
import logging
from threading import Lock
from encryption_utils import encrypt_message, decrypt_message

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Initialize Flask app
app = Flask(__name__)
app.config['SECRET_KEY'] = 'your-secret-key-change-this'
CORS(app)  # Enable CORS for Android app communication

# Database configuration
DATABASE_PATH = os.path.join(os.path.dirname(__file__), 'bisto_chat.db')
db_lock = Lock()

# Initialize database
def init_database():
    """Initialize SQLite database with required tables"""
    with sqlite3.connect(DATABASE_PATH) as conn:
        cursor = conn.cursor()
        
        # Users table
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS users (
                uid TEXT PRIMARY KEY,
                email TEXT UNIQUE NOT NULL,
                password_hash TEXT NOT NULL,
                display_name TEXT,
                phone_number TEXT,
                profile_image_url TEXT,
                bio TEXT DEFAULT 'Hey there! I''m using Bisto Chat.',
                status TEXT DEFAULT 'offline',
                last_seen INTEGER,
                created_at INTEGER DEFAULT (strftime('%s', 'now')),
                updated_at INTEGER DEFAULT (strftime('%s', 'now'))
            )
        ''')
        
        # Contacts table (for user relationships)
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS contacts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_uid TEXT NOT NULL,
                contact_uid TEXT NOT NULL,
                contact_name TEXT,
                added_at INTEGER DEFAULT (strftime('%s', 'now')),
                FOREIGN KEY (user_uid) REFERENCES users (uid),
                FOREIGN KEY (contact_uid) REFERENCES users (uid),
                UNIQUE(user_uid, contact_uid)
            )
        ''')
        
        # Messages table
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                message_id TEXT UNIQUE NOT NULL,
                sender_uid TEXT NOT NULL,
                receiver_uid TEXT NOT NULL,
                message_text TEXT NOT NULL,
                timestamp INTEGER DEFAULT (strftime('%s', 'now')),
                message_type TEXT DEFAULT 'text',
                status TEXT DEFAULT 'sent',
                FOREIGN KEY (sender_uid) REFERENCES users (uid),
                FOREIGN KEY (receiver_uid) REFERENCES users (uid)
            )
        ''')
        
        # Chat sessions table (for group chats or conversation tracking)
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS chat_sessions (
                session_id TEXT PRIMARY KEY,
                session_name TEXT,
                session_type TEXT DEFAULT 'private',
                created_by TEXT NOT NULL,
                created_at INTEGER DEFAULT (strftime('%s', 'now')),
                FOREIGN KEY (created_by) REFERENCES users (uid)
            )
        ''')
        
        # Session participants table
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS session_participants (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                session_id TEXT NOT NULL,
                user_uid TEXT NOT NULL,
                joined_at INTEGER DEFAULT (strftime('%s', 'now')),
                FOREIGN KEY (session_id) REFERENCES chat_sessions (session_id),
                FOREIGN KEY (user_uid) REFERENCES users (uid),
                UNIQUE(session_id, user_uid)
            )
        ''')
        
        conn.commit()
        logger.info("Database initialized successfully")

# Utility functions
def get_current_timestamp():
    """Get current timestamp in seconds"""
    return int(datetime.now().timestamp())

def generate_uid():
    """Generate unique identifier"""
    return str(uuid.uuid4())

def get_db_connection():
    """Get database connection with row factory"""
    conn = sqlite3.connect(DATABASE_PATH)
    conn.row_factory = sqlite3.Row
    return conn

# API Endpoints

@app.route('/')
def index():
    """Health check endpoint"""
    return jsonify({
        'status': 'success',
        'message': 'Bisto Chat Database Server is running',
        'timestamp': get_current_timestamp()
    })

# User Authentication Endpoints

@app.route('/api/auth/register', methods=['POST'])
def register_user():
    """Register a new user"""
    try:
        data = request.get_json()
        
        # Validate required fields
        required_fields = ['email', 'password', 'display_name']
        for field in required_fields:
            if not data.get(field):
                return jsonify({'error': f'{field} is required'}), 400
        
        email = data['email'].lower().strip()
        password = data['password']
        display_name = data['display_name'].strip()
        phone_number = data.get('phone_number', '')
        bio = data.get('bio', 'Hey there! I\'m using Bisto Chat.')
        
        # Generate user ID and hash password
        uid = generate_uid()
        password_hash = generate_password_hash(password)
        
        with db_lock:
            conn = get_db_connection()
            try:
                cursor = conn.cursor()
                cursor.execute('''
                    INSERT INTO users (uid, email, password_hash, display_name, 
                                     phone_number, bio, status, last_seen)
                    VALUES (?, ?, ?, ?, ?, ?, 'online', ?)
                ''', (uid, email, password_hash, display_name, phone_number, bio, get_current_timestamp()))
                
                conn.commit()
                
                # Return user data (without password)
                user_data = {
                    'uid': uid,
                    'email': email,
                    'display_name': display_name,
                    'phone_number': phone_number,
                    'bio': bio,
                    'status': 'online'
                }
                
                logger.info(f"New user registered: {email}")
                return jsonify({
                    'status': 'success',
                    'message': 'User registered successfully',
                    'user': user_data
                })
                
            except sqlite3.IntegrityError as e:
                if 'email' in str(e):
                    return jsonify({'error': 'Email already exists'}), 409
                else:
                    return jsonify({'error': 'Registration failed'}), 500
            finally:
                conn.close()
                
    except Exception as e:
        logger.error(f"Registration error: {str(e)}")
        return jsonify({'error': 'Internal server error'}), 500

@app.route('/api/auth/login', methods=['POST'])
def login_user():
    """Authenticate user login"""
    try:
        data = request.get_json()
        
        email = data.get('email', '').lower().strip()
        password = data.get('password', '')
        
        if not email or not password:
            return jsonify({'error': 'Email and password are required'}), 400
        
        with db_lock:
            conn = get_db_connection()
            try:
                cursor = conn.cursor()
                cursor.execute('SELECT * FROM users WHERE email = ?', (email,))
                user = cursor.fetchone()
                
                if user and check_password_hash(user['password_hash'], password):
                    # Update user status and last seen
                    cursor.execute('''
                        UPDATE users SET status = 'online', last_seen = ?, updated_at = ?
                        WHERE uid = ?
                    ''', (get_current_timestamp(), get_current_timestamp(), user['uid']))
                    conn.commit()
                    
                    user_data = {
                        'uid': user['uid'],
                        'email': user['email'],
                        'display_name': user['display_name'],
                        'phone_number': user['phone_number'],
                        'profile_image_url': user['profile_image_url'],
                        'bio': user['bio'],
                        'status': 'online'
                    }
                    
                    logger.info(f"User logged in: {email}")
                    return jsonify({
                        'status': 'success',
                        'message': 'Login successful',
                        'user': user_data
                    })
                else:
                    return jsonify({'error': 'Invalid email or password'}), 401
                    
            finally:
                conn.close()
                
    except Exception as e:
        logger.error(f"Login error: {str(e)}")
        return jsonify({'error': 'Internal server error'}), 500

# User Management Endpoints

@app.route('/api/users/search', methods=['GET'])
def search_users():
    """Search users by email or name"""
    try:
        query = request.args.get('q', '').strip()
        current_user_uid = request.args.get('current_uid', '')
        
        if len(query) < 2:
            return jsonify({
                'status': 'success',
                'users': []
            })
        
        with db_lock:
            conn = get_db_connection()
            try:
                cursor = conn.cursor()
                
                # Search by email (exact) or display name (partial)
                cursor.execute('''
                    SELECT uid, email, display_name, profile_image_url, bio, status
                    FROM users 
                    WHERE (email = ? OR display_name LIKE ?) 
                    AND uid != ?
                    ORDER BY 
                        CASE WHEN email = ? THEN 1 ELSE 2 END,
                        display_name
                    LIMIT 20
                ''', (query.lower(), f'%{query}%', current_user_uid, query.lower()))
                
                users = []
                for row in cursor.fetchall():
                    users.append({
                        'uid': row['uid'],
                        'email': row['email'],
                        'display_name': row['display_name'],
                        'profile_image_url': row['profile_image_url'],
                        'bio': row['bio'],
                        'status': row['status']
                    })
                
                return jsonify({
                    'status': 'success',
                    'users': users
                })
                
            finally:
                conn.close()
                
    except Exception as e:
        logger.error(f"User search error: {str(e)}")
        return jsonify({'error': 'Internal server error'}), 500

@app.route('/api/users/<uid>/status', methods=['PUT'])
def update_user_status():
    """Update user online status"""
    try:
        data = request.get_json()
        status = data.get('status', 'offline')
        
        with db_lock:
            conn = get_db_connection()
            try:
                cursor = conn.cursor()
                cursor.execute('''
                    UPDATE users SET status = ?, last_seen = ?, updated_at = ?
                    WHERE uid = ?
                ''', (status, get_current_timestamp(), get_current_timestamp(), uid))
                conn.commit()
                
                return jsonify({
                    'status': 'success',
                    'message': 'Status updated'
                })
                
            finally:
                conn.close()
                
    except Exception as e:
        logger.error(f"Status update error: {str(e)}")
        return jsonify({'error': 'Internal server error'}), 500

# Contact Management Endpoints

@app.route('/api/contacts', methods=['POST'])
def add_contact():
    """Add a user as contact"""
    try:
        data = request.get_json()
        user_uid = data.get('user_uid')
        contact_uid = data.get('contact_uid')
        contact_name = data.get('contact_name')
        
        if not user_uid or not contact_uid:
            return jsonify({'error': 'user_uid and contact_uid are required'}), 400
        
        if user_uid == contact_uid:
            return jsonify({'error': 'Cannot add yourself as contact'}), 400
        
        with db_lock:
            conn = get_db_connection()
            try:
                cursor = conn.cursor()
                
                # Check if users exist
                cursor.execute('SELECT uid FROM users WHERE uid IN (?, ?)', (user_uid, contact_uid))
                if len(cursor.fetchall()) != 2:
                    return jsonify({'error': 'One or both users not found'}), 404
                
                # Add mutual contact relationship
                cursor.execute('''
                    INSERT OR IGNORE INTO contacts (user_uid, contact_uid, contact_name)
                    VALUES (?, ?, ?)
                ''', (user_uid, contact_uid, contact_name))
                
                cursor.execute('''
                    INSERT OR IGNORE INTO contacts (user_uid, contact_uid, contact_name)
                    VALUES (?, ?, (SELECT display_name FROM users WHERE uid = ?))
                ''', (contact_uid, user_uid, user_uid))
                
                conn.commit()
                
                return jsonify({
                    'status': 'success',
                    'message': 'Contact added successfully'
                })
                
            except sqlite3.IntegrityError:
                return jsonify({'error': 'Contact already exists'}), 409
            finally:
                conn.close()
                
    except Exception as e:
        logger.error(f"Add contact error: {str(e)}")
        return jsonify({'error': 'Internal server error'}), 500

@app.route('/api/contacts/<user_uid>', methods=['GET'])
def get_user_contacts(user_uid):
    """Get user's contact list"""
    try:
        with db_lock:
            conn = get_db_connection()
            try:
                cursor = conn.cursor()
                cursor.execute('''
                    SELECT c.contact_uid, c.contact_name, c.added_at,
                           u.email, u.display_name, u.profile_image_url, u.status, u.last_seen
                    FROM contacts c
                    JOIN users u ON c.contact_uid = u.uid
                    WHERE c.user_uid = ?
                    ORDER BY c.added_at DESC
                ''', (user_uid,))
                
                contacts = []
                for row in cursor.fetchall():
                    contacts.append({
                        'uid': row['contact_uid'],
                        'name': row['contact_name'] or row['display_name'],
                        'email': row['email'],
                        'display_name': row['display_name'],
                        'profile_image_url': row['profile_image_url'],
                        'status': row['status'],
                        'last_seen': row['last_seen'],
                        'added_at': row['added_at']
                    })
                
                return jsonify({
                    'status': 'success',
                    'contacts': contacts
                })
                
            finally:
                conn.close()
                
    except Exception as e:
        logger.error(f"Get contacts error: {str(e)}")
        return jsonify({'error': 'Internal server error'}), 500

# Messaging Endpoints

@app.route('/api/messages', methods=['POST'])
def send_message():
    """Send a message"""
    try:
        data = request.get_json()
        sender_uid = data.get('sender_uid')
        receiver_uid = data.get('receiver_uid')
        message_text = data.get('message_text')
        message_type = data.get('message_type', 'text')
        
        if not all([sender_uid, receiver_uid, message_text]):
            return jsonify({'error': 'sender_uid, receiver_uid, and message_text are required'}), 400
        
        message_id = generate_uid()
        
        # Encrypt the message before storing
        encrypted_message_text = encrypt_message(message_text)
        logger.debug(f"Message encrypted for storage: {message_text[:50]}... -> {encrypted_message_text[:50]}...")
        
        with db_lock:
            conn = get_db_connection()
            try:
                cursor = conn.cursor()
                cursor.execute('''
                    INSERT INTO messages (message_id, sender_uid, receiver_uid, 
                                        message_text, message_type, status)
                    VALUES (?, ?, ?, ?, ?, 'sent')
                ''', (message_id, sender_uid, receiver_uid, encrypted_message_text, message_type))
                
                conn.commit()
                
                return jsonify({
                    'status': 'success',
                    'message_id': message_id,
                    'timestamp': get_current_timestamp()
                })
                
            finally:
                conn.close()
                
    except Exception as e:
        logger.error(f"Send message error: {str(e)}")
        return jsonify({'error': 'Internal server error'}), 500

@app.route('/api/messages/<user1_uid>/<user2_uid>', methods=['GET'])
def get_messages(user1_uid, user2_uid):
    """Get messages between two users"""
    try:
        limit = request.args.get('limit', 50, type=int)
        offset = request.args.get('offset', 0, type=int)
        
        with db_lock:
            conn = get_db_connection()
            try:
                cursor = conn.cursor()
                cursor.execute('''
                    SELECT m.message_id, m.sender_uid, m.receiver_uid, m.message_text,
                           m.timestamp, m.message_type, m.status,
                           u.display_name as sender_name
                    FROM messages m
                    JOIN users u ON m.sender_uid = u.uid
                    WHERE (m.sender_uid = ? AND m.receiver_uid = ?) 
                       OR (m.sender_uid = ? AND m.receiver_uid = ?)
                    ORDER BY m.timestamp DESC
                    LIMIT ? OFFSET ?
                ''', (user1_uid, user2_uid, user2_uid, user1_uid, limit, offset))
                
                messages = []
                for row in cursor.fetchall():
                    # Decrypt the message before sending
                    decrypted_message = decrypt_message(row['message_text'])
                    logger.debug(f"Message decrypted for retrieval: {row['message_text'][:50]}... -> {decrypted_message[:50]}...")
                    
                    messages.append({
                        'message_id': row['message_id'],
                        'sender_uid': row['sender_uid'],
                        'receiver_uid': row['receiver_uid'],
                        'message_text': decrypted_message,
                        'timestamp': row['timestamp'],
                        'message_type': row['message_type'],
                        'status': row['status'],
                        'sender_name': row['sender_name']
                    })
                
                return jsonify({
                    'status': 'success',
                    'messages': list(reversed(messages))  # Oldest first
                })
                
            finally:
                conn.close()
                
    except Exception as e:
        logger.error(f"Get messages error: {str(e)}")
        return jsonify({'error': 'Internal server error'}), 500

# Database management endpoints (for development/testing)

@app.route('/api/admin/reset-db', methods=['POST'])
def reset_database():
    """Reset database (development only)"""
    try:
        with db_lock:
            if os.path.exists(DATABASE_PATH):
                os.remove(DATABASE_PATH)
            init_database()
        
        logger.info("Database reset successfully")
        return jsonify({
            'status': 'success',
            'message': 'Database reset successfully'
        })
        
    except Exception as e:
        logger.error(f"Database reset error: {str(e)}")
        return jsonify({'error': 'Internal server error'}), 500

@app.route('/api/admin/stats', methods=['GET'])
def get_stats():
    """Get database statistics"""
    try:
        with db_lock:
            conn = get_db_connection()
            try:
                cursor = conn.cursor()
                
                # Get table counts
                stats = {}
                tables = ['users', 'contacts', 'messages', 'chat_sessions']
                
                for table in tables:
                    cursor.execute(f'SELECT COUNT(*) as count FROM {table}')
                    stats[table] = cursor.fetchone()['count']
                
                # Get online users count
                cursor.execute('SELECT COUNT(*) as count FROM users WHERE status = "online"')
                stats['online_users'] = cursor.fetchone()['count']
                
                return jsonify({
                    'status': 'success',
                    'stats': stats
                })
                
            finally:
                conn.close()
                
    except Exception as e:
        logger.error(f"Get stats error: {str(e)}")
        return jsonify({'error': 'Internal server error'}), 500

# Error handlers
@app.errorhandler(404)
def not_found(error):
    return jsonify({'error': 'Endpoint not found'}), 404

@app.errorhandler(500)
def internal_error(error):
    return jsonify({'error': 'Internal server error'}), 500

if __name__ == '__main__':
    # Initialize database
    init_database()
    
    # Run the server
    HOST = '0.0.0.0'  # Listen on all interfaces
    PORT = 8080       # Port for the API server
    DEBUG = True      # Set to False in production
    
    logger.info(f"Starting Bisto Chat Database Server on {HOST}:{PORT}")
    logger.info(f"Database path: {DATABASE_PATH}")
    
    app.run(host=HOST, port=PORT, debug=DEBUG, threaded=True)