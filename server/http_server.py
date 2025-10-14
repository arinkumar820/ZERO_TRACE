#!/usr/bin/env python3
"""
HTTP Server for Bisto Chat Android App
Provides REST API endpoints for MySQLUserManager compatibility
Runs alongside the WebSocket server
"""

from flask import Flask, request, jsonify
from flask_cors import CORS
import mysql.connector
import logging
from datetime import datetime
import uuid

app = Flask(__name__)
CORS(app)  # Enable CORS for Android app

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# MySQL connection configuration
DB_CONFIG = {
    "host": "localhost",
    "user": "root",
    "password": "rudra@69420",  # Your MySQL password
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

@app.route('/')
def health_check():
    """Health check endpoint"""
    return jsonify({
        "status": "ok",
        "message": "Bisto Chat HTTP API Server is running",
        "database": "MySQL message_database",
        "timestamp": datetime.now().isoformat()
    })

@app.route('/api/search/users', methods=['GET'])
def search_users():
    """Search for users by email or display name"""
    try:
        query = request.args.get('q', '').strip()
        limit = int(request.args.get('limit', 20))
        
        if not query or len(query) < 2:
            return jsonify({
                "success": False,
                "message": "Query must be at least 2 characters long"
            }), 400
        
        db = get_db_connection()
        if not db:
            return jsonify({
                "success": False,
                "message": "Database connection failed"
            }), 500
        
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
        
        return jsonify({
            "success": True,
            "users": formatted_users,
            "count": len(formatted_users),
            "query": query
        })
        
    except Exception as e:
        logger.error(f"Error searching users: {e}")
        return jsonify({
            "success": False,
            "message": "Search failed"
        }), 500

@app.route('/api/contacts', methods=['GET'])
def get_contacts():
    """Get user's contact list"""
    try:
        user_uid = request.args.get('user_uid')
        
        if not user_uid:
            return jsonify({
                "success": False,
                "message": "user_uid parameter is required"
            }), 400
        
        db = get_db_connection()
        if not db:
            return jsonify({
                "success": False,
                "message": "Database connection failed"
            }), 500
        
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
        
        return jsonify({
            "success": True,
            "contacts": formatted_contacts,
            "count": len(formatted_contacts)
        })
        
    except Exception as e:
        logger.error(f"Error getting contacts: {e}")
        return jsonify({
            "success": False,
            "message": "Failed to get contacts"
        }), 500

@app.route('/api/contacts/add', methods=['POST'])
def add_contact():
    """Add a new contact"""
    try:
        data = request.get_json()
        user_uid = data.get('user_uid')
        contact_uid = data.get('contact_uid')
        contact_name = data.get('contact_name', '')
        
        if not user_uid or not contact_uid:
            return jsonify({
                "success": False,
                "message": "user_uid and contact_uid are required"
            }), 400
        
        if user_uid == contact_uid:
            return jsonify({
                "success": False,
                "message": "Cannot add yourself as a contact"
            }), 400
        
        db = get_db_connection()
        if not db:
            return jsonify({
                "success": False,
                "message": "Database connection failed"
            }), 500
        
        cursor = db.cursor()
        
        # Check if contact already exists
        cursor.execute("""
            SELECT id FROM contacts 
            WHERE user_uid = %s AND contact_uid = %s
        """, (user_uid, contact_uid))
        
        if cursor.fetchone():
            db.close()
            return jsonify({
                "success": False,
                "message": "Contact already exists"
            }), 409
        
        # Add contact
        cursor.execute("""
            INSERT INTO contacts (user_uid, contact_uid, contact_name)
            VALUES (%s, %s, %s)
        """, (user_uid, contact_uid, contact_name))
        
        db.commit()
        db.close()
        
        return jsonify({
            "success": True,
            "message": "Contact added successfully"
        })
        
    except Exception as e:
        logger.error(f"Error adding contact: {e}")
        return jsonify({
            "success": False,
            "message": "Failed to add contact"
        }), 500

if __name__ == '__main__':
    logger.info("🚀 Starting Bisto Chat HTTP API Server...")
    logger.info("📡 Database: MySQL message_database")
    logger.info("🌐 Server will run on http://0.0.0.0:8080")
    logger.info("📱 Android emulator should use: http://10.0.2.2:8080")
    
    try:
        # Test database connection
        db = get_db_connection()
        if db:
            logger.info("✅ Database connection successful")
            db.close()
        else:
            logger.error("❌ Database connection failed")
            exit(1)
            
        # Start Flask server
        app.run(host='0.0.0.0', port=8080, debug=False)
        
    except Exception as e:
        logger.error(f"❌ Server startup error: {e}")