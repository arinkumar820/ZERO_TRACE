#!/usr/bin/env python3
"""
Hybrid User Service for Bisto Chat
- Uses Firebase for authentication
- Uses MySQL message_database for user data, search, and messaging
- Syncs users between Firebase Auth and MySQL database
"""

import mysql.connector
import logging
from flask import Flask, request, jsonify
from flask_cors import CORS
import json
from datetime import datetime

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Initialize Flask app
app = Flask(__name__)
CORS(app)

# MySQL Database Configuration
DB_CONFIG = {
    "host": "localhost",
    "user": "root",
    "password": "rudra@69420",  # Your MySQL password
    "database": "message_database",
    "autocommit": True
}

def get_db_connection():
    """Get MySQL database connection"""
    try:
        return mysql.connector.connect(**DB_CONFIG)
    except mysql.connector.Error as err:
        logger.error(f"Database connection error: {err}")
        return None

def init_message_database():
    """Initialize message_database with required tables"""
    try:
        # First, create database if it doesn't exist
        temp_config = DB_CONFIG.copy()
        temp_config.pop('database', None)  # Remove database from config temporarily
        
        temp_conn = mysql.connector.connect(**temp_config)
        temp_cursor = temp_conn.cursor()
        temp_cursor.execute("CREATE DATABASE IF NOT EXISTS message_database")
        temp_conn.commit()
        temp_conn.close()
        logger.info("✅ Database 'message_database' ensured to exist")
        
        # Now connect to the specific database
        conn = get_db_connection()
        if not conn:
            return False
            
        cursor = conn.cursor()
        
        # Users table for search and profile data
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS users (
                uid VARCHAR(255) PRIMARY KEY,
                firebase_uid VARCHAR(255) UNIQUE NOT NULL,
                email VARCHAR(255) UNIQUE NOT NULL,
                display_name VARCHAR(255) NOT NULL,
                phone_number VARCHAR(20),
                profile_image_url TEXT,
                bio TEXT,
                status ENUM('online', 'offline', 'away') DEFAULT 'offline',
                last_seen DATETIME DEFAULT CURRENT_TIMESTAMP,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                INDEX idx_email (email),
                INDEX idx_display_name (display_name),
                INDEX idx_status (status)
            )
        """)
        
        # Contacts/Friends table
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS contacts (
                id INT AUTO_INCREMENT PRIMARY KEY,
                user_uid VARCHAR(255) NOT NULL,
                contact_uid VARCHAR(255) NOT NULL,
                contact_name VARCHAR(255),
                added_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                status ENUM('pending', 'accepted', 'blocked') DEFAULT 'accepted',
                FOREIGN KEY (user_uid) REFERENCES users(uid) ON DELETE CASCADE,
                FOREIGN KEY (contact_uid) REFERENCES users(uid) ON DELETE CASCADE,
                UNIQUE KEY unique_contact (user_uid, contact_uid),
                INDEX idx_user (user_uid),
                INDEX idx_contact (contact_uid)
            )
        """)
        
        # Chat rooms table (already exists from your WebSocket server)
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS chat_rooms (
                id VARCHAR(255) PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                description TEXT,
                type ENUM('group', 'personal') DEFAULT 'personal',
                created_by VARCHAR(255) NOT NULL,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                INDEX idx_type (type),
                INDEX idx_created_by (created_by)
            )
        """)
        
        conn.commit()
        conn.close()
        logger.info("✅ message_database initialized successfully")
        return True
        
    except mysql.connector.Error as err:
        logger.error(f"❌ Database initialization error: {err}")
        return False

# API Endpoints

@app.route('/')
def health_check():
    """Health check endpoint"""
    return jsonify({
        'status': 'success',
        'message': 'Hybrid User Service is running',
        'services': ['Firebase Auth', 'MySQL Database'],
        'timestamp': datetime.now().isoformat()
    })

@app.route('/api/user/sync', methods=['POST'])
def sync_firebase_user():
    """
    Sync a Firebase authenticated user to MySQL database
    Call this after successful Firebase Auth login/register
    """
    try:
        data = request.get_json()
        
        # Validate required fields from Firebase Auth
        required_fields = ['firebase_uid', 'email']
        for field in required_fields:
            if not data.get(field):
                return jsonify({'error': f'{field} is required'}), 400
        
        firebase_uid = data['firebase_uid']
        email = data['email'].lower().strip()
        display_name = data.get('display_name', email.split('@')[0])
        phone_number = data.get('phone_number', '')
        profile_image_url = data.get('profile_image_url', '')
        bio = data.get('bio', 'Hey there! I am using Bisto Chat.')
        if not bio or bio.strip() == '':
            bio = 'Hey there! I am using Bisto Chat.'
        
        conn = get_db_connection()
        if not conn:
            return jsonify({'error': 'Database connection failed'}), 500
        
        cursor = conn.cursor()
        
        # Check if user already exists
        cursor.execute("SELECT uid FROM users WHERE firebase_uid = %s OR email = %s", 
                      (firebase_uid, email))
        existing_user = cursor.fetchone()
        
        if existing_user:
            # Update existing user
            cursor.execute("""
                UPDATE users SET 
                    email = %s,
                    display_name = %s,
                    phone_number = %s,
                    profile_image_url = %s,
                    bio = %s,
                    status = 'online',
                    last_seen = CURRENT_TIMESTAMP,
                    updated_at = CURRENT_TIMESTAMP
                WHERE firebase_uid = %s
            """, (email, display_name, phone_number, profile_image_url, bio, firebase_uid))
            
            user_uid = existing_user[0]
            logger.info(f"✅ User updated in MySQL: {email}")
            
        else:
            # Create new user in MySQL
            user_uid = f"user_{datetime.now().strftime('%Y%m%d_%H%M%S')}_{firebase_uid[:8]}"
            
            cursor.execute("""
                INSERT INTO users (uid, firebase_uid, email, display_name, phone_number, 
                                 profile_image_url, bio, status, last_seen)
                VALUES (%s, %s, %s, %s, %s, %s, %s, 'online', CURRENT_TIMESTAMP)
            """, (user_uid, firebase_uid, email, display_name, phone_number, 
                  profile_image_url, bio))
            
            logger.info(f"✅ New user added to MySQL: {email}")
        
        conn.commit()
        
        # Get complete user data to return
        cursor.execute("SELECT * FROM users WHERE firebase_uid = %s", (firebase_uid,))
        user_data = cursor.fetchone()
        
        conn.close()
        
        if user_data:
            user_dict = {
                'uid': user_data[0],
                'firebase_uid': user_data[1],
                'email': user_data[2],
                'display_name': user_data[3],
                'phone_number': user_data[4],
                'profile_image_url': user_data[5],
                'bio': user_data[6],
                'status': user_data[7],
                'last_seen': user_data[8].isoformat() if user_data[8] else None
            }
            
            return jsonify({
                'status': 'success',
                'message': 'User synced successfully',
                'user': user_dict
            })
        else:
            return jsonify({'error': 'Failed to retrieve user data'}), 500
            
    except mysql.connector.Error as e:
        logger.error(f"❌ Database error in sync_firebase_user: {e}")
        return jsonify({'error': 'Database operation failed'}), 500
    except Exception as e:
        logger.error(f"❌ Error in sync_firebase_user: {e}")
        return jsonify({'error': 'Internal server error'}), 500

@app.route('/api/user/search', methods=['GET'])
def search_users():
    """
    Search for users by email or display name
    Used by SearchContactsActivity
    """
    try:
        query = request.args.get('q', '').strip()
        current_user_uid = request.args.get('current_user', '')
        
        if len(query) < 2:
            return jsonify({'error': 'Query must be at least 2 characters long'}), 400
        
        conn = get_db_connection()
        if not conn:
            return jsonify({'error': 'Database connection failed'}), 500
        
        cursor = conn.cursor(dictionary=True)
        
        # Search by email or display name (exclude current user)
        search_query = f"%{query.lower()}%"
        cursor.execute("""
            SELECT uid, firebase_uid, email, display_name, phone_number, 
                   profile_image_url, bio, status, last_seen
            FROM users 
            WHERE (LOWER(email) LIKE %s OR LOWER(display_name) LIKE %s)
              AND uid != %s
              AND status != 'deleted'
            ORDER BY 
                CASE 
                    WHEN LOWER(email) = %s THEN 1
                    WHEN LOWER(display_name) = %s THEN 2
                    WHEN LOWER(email) LIKE %s THEN 3
                    WHEN LOWER(display_name) LIKE %s THEN 4
                    ELSE 5
                END
            LIMIT 50
        """, (search_query, search_query, current_user_uid, 
              query.lower(), query.lower(), search_query, search_query))
        
        users = cursor.fetchall()
        conn.close()
        
        # Format response
        user_list = []
        for user in users:
            user_dict = {
                'uid': user['uid'],
                'firebase_uid': user['firebase_uid'],
                'email': user['email'],
                'display_name': user['display_name'],
                'phone_number': user['phone_number'],
                'profile_image_url': user['profile_image_url'],
                'bio': user['bio'],
                'status': user['status'],
                'last_seen': user['last_seen'].isoformat() if user['last_seen'] else None
            }
            user_list.append(user_dict)
        
        logger.info(f"🔍 Search for '{query}' returned {len(user_list)} users")
        
        return jsonify({
            'status': 'success',
            'query': query,
            'count': len(user_list),
            'users': user_list
        })
        
    except mysql.connector.Error as e:
        logger.error(f"❌ Database error in search_users: {e}")
        return jsonify({'error': 'Database operation failed'}), 500
    except Exception as e:
        logger.error(f"❌ Error in search_users: {e}")
        return jsonify({'error': 'Internal server error'}), 500

@app.route('/api/user/profile/<user_uid>', methods=['GET'])
def get_user_profile(user_uid):
    """Get user profile by UID"""
    try:
        conn = get_db_connection()
        if not conn:
            return jsonify({'error': 'Database connection failed'}), 500
        
        cursor = conn.cursor(dictionary=True)
        cursor.execute("""
            SELECT uid, firebase_uid, email, display_name, phone_number, 
                   profile_image_url, bio, status, last_seen, created_at
            FROM users 
            WHERE uid = %s
        """, (user_uid,))
        
        user = cursor.fetchone()
        conn.close()
        
        if user:
            user_dict = {
                'uid': user['uid'],
                'firebase_uid': user['firebase_uid'],
                'email': user['email'],
                'display_name': user['display_name'],
                'phone_number': user['phone_number'],
                'profile_image_url': user['profile_image_url'],
                'bio': user['bio'],
                'status': user['status'],
                'last_seen': user['last_seen'].isoformat() if user['last_seen'] else None,
                'created_at': user['created_at'].isoformat() if user['created_at'] else None
            }
            
            return jsonify({
                'status': 'success',
                'user': user_dict
            })
        else:
            return jsonify({'error': 'User not found'}), 404
            
    except mysql.connector.Error as e:
        logger.error(f"❌ Database error in get_user_profile: {e}")
        return jsonify({'error': 'Database operation failed'}), 500
    except Exception as e:
        logger.error(f"❌ Error in get_user_profile: {e}")
        return jsonify({'error': 'Internal server error'}), 500

@app.route('/api/user/status', methods=['PUT'])
def update_user_status():
    """Update user online/offline status"""
    try:
        data = request.get_json()
        firebase_uid = data.get('firebase_uid')
        status = data.get('status', 'online')
        
        if not firebase_uid:
            return jsonify({'error': 'firebase_uid is required'}), 400
        
        if status not in ['online', 'offline', 'away']:
            return jsonify({'error': 'Invalid status'}), 400
        
        conn = get_db_connection()
        if not conn:
            return jsonify({'error': 'Database connection failed'}), 500
        
        cursor = conn.cursor()
        cursor.execute("""
            UPDATE users SET 
                status = %s,
                last_seen = CURRENT_TIMESTAMP,
                updated_at = CURRENT_TIMESTAMP
            WHERE firebase_uid = %s
        """, (status, firebase_uid))
        
        conn.commit()
        affected_rows = cursor.rowcount
        conn.close()
        
        if affected_rows > 0:
            return jsonify({
                'status': 'success',
                'message': f'Status updated to {status}'
            })
        else:
            return jsonify({'error': 'User not found'}), 404
            
    except mysql.connector.Error as e:
        logger.error(f"❌ Database error in update_user_status: {e}")
        return jsonify({'error': 'Database operation failed'}), 500
    except Exception as e:
        logger.error(f"❌ Error in update_user_status: {e}")
        return jsonify({'error': 'Internal server error'}), 500

if __name__ == '__main__':
    print("🚀 Initializing Hybrid User Service...")
    print("🔥 Firebase Auth + 🗄️ MySQL Database")
    
    # Initialize database
    if init_message_database():
        print("✅ Database initialized successfully")
        print("🌐 Starting API server on port 5000...")
        app.run(debug=True, host='0.0.0.0', port=5000)
    else:
        print("❌ Failed to initialize database")
        exit(1)