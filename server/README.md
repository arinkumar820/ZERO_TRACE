# Bisto Chat Database Server

A Python Flask-based database server for the Android Bisto Chat application.

## 🎯 Overview

This server provides a RESTful API backend for the Android chat app, replacing Firebase with a local SQLite database solution. It handles user authentication, contacts management, and messaging.

## 🛠️ Setup Instructions

### Prerequisites
- Python 3.7 or higher
- pip (Python package installer)
- Windows computer (for .bat script)

### Quick Start

1. **Install Python** (if not already installed):
   - Download from https://python.org
   - Make sure to check "Add Python to PATH" during installation

2. **Start the Server**:
   ```bash
   # Navigate to the server folder
   cd server
   
   # Run the startup script (Windows)
   start_server.bat
   
   # OR manually install dependencies and run
   pip install -r requirements.txt
   python server.py
   ```

3. **Verify Server is Running**:
   - Server will start on `http://localhost:8080`
   - You should see "Starting Bisto Chat Database Server" in the console
   - Test with: http://localhost:8080 in your browser

## 📱 Android Integration

The Android app includes `ApiClient.java` for communicating with the server.

### Server URLs:
- **Android Emulator**: `http://10.0.2.2:8080` (automatically configured)
- **Real Device**: `http://YOUR_COMPUTER_IP:8080` (update in ApiClient.java)

### Testing Connectivity:
1. Open the app and navigate to the Server Test screen
2. Click "Test Connection" to verify server connectivity
3. Use "Get Server Stats" to see database information

## 🔗 API Endpoints

### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - User login

### User Management
- `GET /api/users/search?q={query}` - Search users
- `PUT /api/users/{uid}/status` - Update user status

### Contacts
- `POST /api/contacts` - Add contact
- `GET /api/contacts/{uid}` - Get user contacts

### Messages
- `POST /api/messages` - Send message
- `GET /api/messages/{user1}/{user2}` - Get conversation

### Admin (Development)
- `GET /api/admin/stats` - Database statistics
- `POST /api/admin/reset-db` - Reset database

## 🗄️ Database Schema

The server uses SQLite with the following tables:

### Users
```sql
uid TEXT PRIMARY KEY
email TEXT UNIQUE
password_hash TEXT
display_name TEXT
phone_number TEXT
profile_image_url TEXT
bio TEXT
status TEXT (online/offline)
last_seen INTEGER
```

### Contacts
```sql
user_uid TEXT
contact_uid TEXT
contact_name TEXT
added_at INTEGER
```

### Messages
```sql
message_id TEXT UNIQUE
sender_uid TEXT
receiver_uid TEXT
message_text TEXT
timestamp INTEGER
message_type TEXT
status TEXT
```

## 🚀 Usage Examples

### Register User (Android)
```java
ApiClient.registerUser("user@example.com", "password123", "John Doe", 
    "1234567890", "Hello!", new ApiClient.ApiCallback() {
    @Override
    public void onSuccess(JSONObject response) {
        // Handle success
    }
    
    @Override
    public void onError(String error) {
        // Handle error
    }
});
```

### Search Users
```java
ApiClient.searchUsers("john", currentUserId, callback);
```

### Send Message
```java
ApiClient.sendMessage(senderUid, receiverUid, "Hello!", "text", callback);
```

## 🔧 Configuration

### Server Settings (server.py)
```python
HOST = '0.0.0.0'  # Listen on all interfaces
PORT = 8080       # Server port
DEBUG = True      # Development mode
```

### Android Settings (ApiClient.java)
```java
// For emulator
private static final String BASE_URL = "http://10.0.2.2:8080";

// For real device (replace with your computer's IP)
private static final String BASE_URL = "http://192.168.1.100:8080";
```

## 🛡️ Security Notes

⚠️ **This is for development/testing only!**

- Passwords are hashed using Werkzeug's security functions
- No HTTPS in development (use HTTP only)
- CORS is enabled for all origins
- Database reset endpoint is available (remove in production)

## 📝 Development Features

- **Auto-reload**: Server restarts when code changes (DEBUG=True)
- **Logging**: All API calls are logged to console
- **Error Handling**: Comprehensive error responses
- **Thread Safety**: Database operations are thread-safe

## 🐛 Troubleshooting

### Common Issues:

1. **Connection Failed**:
   - Ensure Python server is running
   - Check firewall isn't blocking port 8080
   - Verify correct IP address for real devices

2. **Python/Flask Errors**:
   - Install missing packages: `pip install -r requirements.txt`
   - Check Python version: `python --version`

3. **Database Issues**:
   - Delete `bisto_chat.db` to reset
   - Use `/api/admin/reset-db` endpoint

### Getting Your Computer's IP (for real devices):
```bash
# Windows
ipconfig

# Look for IPv4 Address under your network adapter
# Example: 192.168.1.100
```

## 📊 Monitoring

Access these URLs in your browser while server is running:

- Health Check: http://localhost:8080
- Database Stats: http://localhost:8080/api/admin/stats

## 🔄 Integration with Android App

The server is fully integrated with your Android app:

1. **Registration**: Users registered in Android are stored in server database
2. **Authentication**: Login/logout updates user status
3. **Contact Search**: Real-time search through server database
4. **Messaging**: All messages stored and retrieved from server
5. **Status Updates**: Online/offline status managed by server

## 🚦 Next Steps

1. Start the Python server using `start_server.bat`
2. Build and run your Android app
3. Test server connectivity using the built-in Server Test screen
4. Register test users and try the contact search functionality
5. Send messages between users to test the full system

The server will create a `bisto_chat.db` SQLite file to store all data locally.