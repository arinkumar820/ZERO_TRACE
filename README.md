# 📱 Zero Trace - Complete Android Chat Application

> **Advanced Android Chat Application with Firebase Authentication, Real-time Messaging, Disappearing Messages, End-to-End Encryption, and Multiple Backend Options**

## 🎯 Project Overview

Zero Trace is a feature-rich Android chat application built with Java that combines modern messaging capabilities with enterprise-level security. The application supports multiple backend configurations and provides a WhatsApp-like user experience with advanced features.

### 🌟 Key Highlights
- **Hybrid Backend System**: Firebase Auth + MySQL Database + Python Flask API
- **Real-time Messaging**: WebSocket implementation for instant communication
- **Disappearing Messages**: 2-minute auto-delete with live countdown timers
- **End-to-End Encryption**: AES-256 encryption for database storage
- **Contact Search**: Advanced user discovery and contact management
- **Screenshot Protection**: Security features for sensitive conversations
- **Multiple Chat Rooms**: Support for different conversation topics
- **Cross-Platform Testing**: Works on emulators and real devices

## 📋 Table of Contents

1. [Architecture Overview](#-architecture-overview)
2. [Features](#-features)
3. [System Requirements](#-system-requirements)
4. [Quick Start Guide](#-quick-start-guide)
5. [Detailed Setup Instructions](#-detailed-setup-instructions)
6. [Backend Configuration](#-backend-configuration)
7. [Android App Configuration](#-android-app-configuration)
8. [Testing Guide](#-testing-guide)
9. [Feature Documentation](#-feature-documentation)
10. [Development Workflow](#-development-workflow)
11. [Troubleshooting](#-troubleshooting)
12. [API Documentation](#-api-documentation)
13. [Security Features](#-security-features)
14. [Contributing](#-contributing)

## 🏗️ Architecture Overview

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Android App   │◄──►│  Flask API      │◄──►│   MySQL DB      │
│   (Java)        │    │  (Python)       │    │   (Messages)    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │              ┌─────────────────┐              │
         └─────────────►│  WebSocket      │◄─────────────┘
                        │  Server         │
                        │  (Real-time)    │
                        └─────────────────┘
                                │
                        ┌─────────────────┐
                        │  Firebase Auth  │
                        │  (Authentication│
                        └─────────────────┘
```

### Technology Stack
- **Frontend**: Android (Java), Material Design
- **Backend**: Python Flask, WebSocket Server
- **Database**: MySQL + SQLite (hybrid approach)
- **Authentication**: Firebase Authentication
- **Real-time**: WebSocket connections
- **Encryption**: AES-256 with PBKDF2 key derivation
- **Testing**: Android Instrumented Tests, Python unit tests

## ✨ Features

### 🚀 Core Features
- [x] **User Registration & Authentication** (Firebase Auth)
- [x] **Real-time Messaging** (WebSocket)
- [x] **Contact Search & Management**
- [x] **Chat List Interface** (WhatsApp-like)
- [x] **Cross-device Synchronization**
- [x] **Online/Offline Status**
- [x] **Message History**

### 🔒 Security Features
- [x] **End-to-End Encryption** (AES-256)
- [x] **Screenshot Protection**
- [x] **Disappearing Messages** (2-minute timer)
- [x] **Secure API Communication**
- [x] **Password Hashing** (PBKDF2)

### 🎨 Advanced Features
- [x] **Multiple Chat Rooms**
- [x] **Live Message Countdown**
- [x] **Profile Picture Management**
- [x] **Dark Theme UI**
- [x] **Search Functionality**
- [x] **Unread Message Indicators**
- [x] **Status Indicators**

### 🧪 Testing & Development
- [x] **Server Connectivity Tests**
- [x] **Database Test Activities**
- [x] **WebSocket Test Interface**
- [x] **Firebase Debug Tools**
- [x] **Automated Android Tests**

## 🔧 System Requirements

### Android Development
- **Android Studio**: Arctic Fox or later
- **Java**: JDK 8 or higher
- **Android SDK**: API level 26-34
- **Gradle**: 7.0+

### Server Development
- **Python**: 3.7 or higher
- **MySQL**: 8.0+ (optional, SQLite also supported)
- **Git**: For version control

### Device Requirements
- **Android**: 8.0 (API level 26) or higher
- **RAM**: 2GB minimum, 4GB recommended
- **Storage**: 100MB for app + database

## 🚀 Quick Start Guide

### Option 1: APK Installation (Fastest)
```bash
# Install pre-built APK
adb install server/BistoChat-ChatList.apk
# OR for disappearing messages version
adb install server/BistoChat-DisappearingMessages.apk
# OR for real device testing
adb install server/BistoChat-RealDevices.apk
```

### Option 2: Full Development Setup

1. **Clone the Repository**
```bash
git clone <repository-url>
cd i180479_180531
```

2. **Set up Python Server**
```bash
cd server
pip install -r requirements.txt
python server.py
```

3. **Open Android Project**
```bash
# Open in Android Studio
code . # or studio64.exe .
```

4. **Build and Run**
```bash
./gradlew assembleDebug
./gradlew installDebug
```

## 📖 Detailed Setup Instructions

### 🔥 Firebase Setup

1. **Create Firebase Project**
   - Go to [Firebase Console](https://console.firebase.google.com)
   - Click "Create Project"
   - Enable Authentication with Email/Password

2. **Download Configuration**
   - Download `google-services.json`
   - Place in `app/` directory

3. **Enable Services**
   - Authentication: Email/Password
   - Realtime Database: Test mode
   - (Optional) Firestore for advanced features

### 🗄️ Database Setup

#### Option A: SQLite (Recommended for Development)
```bash
cd server
python server.py  # Automatically creates bisto_chat.db
```

#### Option B: MySQL (Production)
```bash
# Install MySQL
# Create database
mysql -u root -p
CREATE DATABASE message_database;

# Update connection in server files
# Run setup script
python setup_mysql.sql
```

#### Option C: Hybrid System
```bash
# Start hybrid service
python hybrid_user_service.py  # Port 5000

# Start WebSocket server
python websocket_server.py     # Port 8080
```

### 🐍 Python Server Configuration

1. **Install Dependencies**
```bash
pip install -r requirements.txt
```

2. **Configure Server Settings** (`server/server.py`)
```python
HOST = '0.0.0.0'        # Listen on all interfaces
PORT = 8080             # Server port
DEBUG = True            # Development mode
DATABASE = 'sqlite'     # or 'mysql'
```

3. **Start Server**
```bash
# Quick start (Windows)
start_server.bat

# Manual start
python server.py
```

### 📱 Android Configuration

1. **Update Server URLs** (`ApiClient.java`)
```java
// For Android Emulator
private static final String BASE_URL = "http://10.0.2.2:8080";

// For Real Device (update with your computer's IP)
private static final String BASE_URL = "http://192.168.1.100:8080";
```

2. **Build Configuration** (`app/build.gradle`)
```gradle
android {
    compileSdk 36
    defaultConfig {
        applicationId "com.sameetasadullah.i180479_180531"
        minSdk 26
        targetSdk 34
    }
}
```

3. **Permissions** (`AndroidManifest.xml`)
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

## 🔧 Backend Configuration

### 🌐 WebSocket Server
```bash
cd server
python websocket_server.py
```
- **Port**: 8080
- **Protocol**: WebSocket
- **Features**: Real-time messaging, user presence, chat rooms

### 🔌 REST API Server
```bash
cd server
python server.py
```
- **Port**: 8080
- **Protocol**: HTTP/REST
- **Features**: User management, contact search, message history

### 🔄 Hybrid User Service
```bash
cd server
python hybrid_user_service.py
```
- **Port**: 5000
- **Features**: Firebase auth sync, MySQL user management

## 📱 Android App Configuration

### 🎨 UI Components
- **Material Design**: Modern Android UI components
- **Dark Theme**: Consistent dark mode throughout
- **Custom Layouts**: Chat bubbles, contact lists, profile screens
- **Navigation**: Bottom navigation with 4 tabs

### 🏗️ Architecture Pattern
```
├── Activities/
│   ├── MainActivity (screen1.java)
│   ├── ChatActivity.java
│   ├── SearchContactsActivity.java
│   └── Account.java
├── Adapters/
│   ├── ChatMessageAdapter.java
│   ├── ContactsAdapter.java
│   └── screen4RVAdaptor.java
├── Models/
│   ├── User.java
│   ├── ChatMessage.java
│   └── WebSocketMessage.java
├── Services/
│   ├── WebSocketClientManager.java
│   ├── ApiClient.java
│   └── DatabaseManager.java
└── Utils/
    ├── ScreenshotProtection.java
    └── UserDataRepairTool.java
```

## 🧪 Testing Guide

### 🔍 Pre-flight Checks

1. **Server Connectivity Test**
```bash
# Test server is running
curl http://localhost:8080
# Expected: Server status page
```

2. **Database Test**
```bash
# Check database creation
python check_sqlite_simple.py
# Expected: User and message count
```

### 📲 Android Testing

#### Manual Testing Steps:

1. **Launch App**
   - Open Zero Trace
   - Should see login/register screen

2. **User Registration**
   - Create new account
   - Verify Firebase auth works
   - Check user sync to database

3. **Contact Search**
   - Navigate to "Add Contact" 
   - Search for other users
   - Add contacts to your list

4. **Chat Testing**
   - Open Messages tab
   - Select a chat room
   - Send test messages
   - Verify real-time delivery

5. **Disappearing Messages**
   - Send message
   - Watch 2-minute countdown
   - Verify message disappears

#### Automated Tests:
```bash
# Run Android instrumented tests
./gradlew connectedAndroidTest

# Specific test classes
./gradlew connectedAndroidTest -P android.testInstrumentationRunnerArguments.class=com.sameetasadullah.i180479_180531.registerTest
```

### 🐛 Debug Tools

1. **Server Test Activity**
   - Built-in server connectivity tester
   - Database statistics viewer
   - API endpoint testing

2. **WebSocket Test Activity**
   - Real-time connection testing
   - Message sending verification
   - Connection status monitoring

3. **Firebase Debug Activity**
   - Authentication state debugging
   - User data verification
   - Database read/write testing

## 📚 Feature Documentation

### 💬 Chat List Feature
**File**: `server/CHAT_LIST_FEATURE.md`

- **WhatsApp-like interface** with clickable chat boxes
- **4 default chat rooms**: Global, Tech Discussion, Random, Study Group
- **Visual indicators**: Online status, unread messages, timestamps
- **Search functionality** within chat list
- **Real-time updates** when new messages arrive

**Usage:**
1. Open Messages tab
2. Browse available chat rooms
3. Click any chat box to start conversation
4. Messages support disappearing timers

### 🕒 Disappearing Messages
**File**: `server/DISAPPEARING_MESSAGES_FEATURE.md`

- **2-minute auto-delete** with live countdown
- **Visual countdown timer**: Shows remaining time in real-time
- **Message expiration**: Replaced with disappear placeholder
- **Database retention**: Messages kept for backup/audit

**Technical Details:**
- `WebSocketMessage.java`: Added disappearing fields
- `ChatMessageAdapter.java`: Real-time countdown logic
- UI updates every second until expiration

### 🔐 End-to-End Encryption
**File**: `server/ENCRYPTION_GUIDE.md`

- **AES-256 encryption** using Fernet symmetric encryption
- **PBKDF2 key derivation** with 100,000 iterations
- **Automatic encryption** of all messages before database storage
- **Backward compatibility** with existing plain text messages

**Security Features:**
- Random IV for each message
- Base64 encoding for database storage
- Transparent to users (encryption is invisible)
- Unicode and emoji support

### 🔄 Hybrid System Integration
**File**: `server/HYBRID_SYSTEM_SETUP.md`

- **Firebase Authentication** for user login/register
- **MySQL Database** for user search, contacts, messaging
- **Automatic user sync** from Firebase to MySQL
- **WebSocket messaging** with MySQL storage

**Benefits:**
- Secure Firebase authentication
- Fast MySQL search capabilities
- Real-time WebSocket messaging
- Best of both worlds approach

### 🔍 Contact Search System

Advanced contact search with multiple search modes:
- **Firebase search**: Search Firebase users
- **Local search**: Search local database
- **Hybrid search**: Search MySQL database
- **Real-time filtering**: Instant search results

### 🛡️ Screenshot Protection

Security feature that:
- Detects screenshot attempts
- Shows warning to users
- Protects sensitive conversations
- Configurable protection levels

## 🔄 Development Workflow

### 🏗️ Building the Project

1. **Clean Build**
```bash
./gradlew clean
./gradlew build
```

2. **Debug Build**
```bash
./gradlew assembleDebug
```

3. **Release Build**
```bash
./gradlew assembleRelease
```

### 🔄 Development Cycle

1. **Start Backend Services**
```bash
# Terminal 1: Start main server
cd server
python server.py

# Terminal 2: Start WebSocket server (if using separate)
python websocket_server.py
```

2. **Android Development**
```bash
# Open Android Studio
# Make changes to Java/XML files
# Build and test on emulator/device
```

3. **Testing Changes**
```bash
# Install updated APK
./gradlew installDebug

# Test specific features
# Check server logs for issues
```

### 📁 Project Structure
```
i180479_180531/
├── app/                          # Android application
│   ├── src/main/java/...        # Java source code
│   ├── src/main/res/            # Android resources
│   ├── build.gradle             # App build configuration
│   └── google-services.json     # Firebase configuration
├── server/                       # Python backend
│   ├── server.py                # Main Flask server
│   ├── websocket_server.py      # WebSocket server
│   ├── hybrid_user_service.py   # Hybrid auth service
│   ├── encryption_utils.py      # Message encryption
│   ├── requirements.txt         # Python dependencies
│   └── *.md                     # Feature documentation
├── build.gradle                 # Project build configuration
└── README.md                    # This file
```

## 🔧 Troubleshooting

### 🚫 Common Issues & Solutions

#### 1. **Connection Failed**
```
❌ Error: "Failed to connect to server"
✅ Solutions:
   - Ensure Python server is running (python server.py)
   - Check firewall isn't blocking port 8080
   - Verify correct IP address in ApiClient.java
   - For real devices: Use computer's IP, not localhost
```

#### 2. **Python/Flask Errors**
```
❌ Error: "Module not found" or Flask import errors
✅ Solutions:
   - Install missing packages: pip install -r requirements.txt
   - Check Python version: python --version (needs 3.7+)
   - Use virtual environment: python -m venv venv
```

#### 3. **Database Issues**
```
❌ Error: "Database locked" or connection issues
✅ Solutions:
   - Delete bisto_chat.db to reset SQLite
   - Use /api/admin/reset-db endpoint
   - Check MySQL service is running (if using MySQL)
   - Verify database permissions
```

#### 4. **Android Build Errors**
```
❌ Error: "Unable to resolve dependency" or build failures
✅ Solutions:
   - Sync Project with Gradle Files
   - Clean Project (Build → Clean Project)
   - Invalidate Caches (File → Invalidate Caches and Restart)
   - Check internet connection for dependency downloads
```

#### 5. **Firebase Authentication Issues**
```
❌ Error: "Authentication failed" or Firebase errors
✅ Solutions:
   - Verify google-services.json is in app/ directory
   - Check Firebase project configuration
   - Enable Email/Password authentication in Firebase Console
   - Verify package name matches Firebase project
```

#### 6. **WebSocket Connection Issues**
```
❌ Error: "WebSocket connection failed"
✅ Solutions:
   - Start WebSocket server: python websocket_server.py
   - Check WebSocket server is running on port 8080
   - Verify network connectivity
   - Check server logs for connection errors
```

### 🔍 Debug Information

#### Getting Your Computer's IP (for real devices):
```bash
# Windows
ipconfig
# Look for IPv4 Address under your network adapter
# Example: 192.168.1.100

# macOS/Linux
ifconfig
# Look for inet address under your network interface
```

#### Server Health Checks:
```bash
# Check if server is running
curl http://localhost:8080
# Expected: Server status response

# Check database stats
curl http://localhost:8080/api/admin/stats
# Expected: Database statistics JSON

# Test WebSocket connection (requires wscat)
wscat -c ws://localhost:8080
```

#### Android Debugging:
```bash
# View app logs
adb logcat | grep "BistoChat"

# Install and run with debugging
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.sameetasadullah.i180479_180531/.screen1

# Check app permissions
adb shell dumpsys package com.sameetasadullah.i180479_180531
```

## 📖 API Documentation

### 🔗 REST API Endpoints

#### Authentication
```http
POST /api/auth/register
Content-Type: application/json

{
    "email": "user@example.com",
    "password": "password123",
    "displayName": "John Doe",
    "phoneNumber": "1234567890",
    "bio": "Hello there!"
}
```

```http
POST /api/auth/login
Content-Type: application/json

{
    "email": "user@example.com",
    "password": "password123"
}
```

#### User Management
```http
GET /api/users/search?q=john&uid=current_user_id
Authorization: Bearer <token>
```

```http
PUT /api/users/{uid}/status
Content-Type: application/json

{
    "status": "online",
    "lastSeen": 1640995200
}
```

#### Contacts
```http
POST /api/contacts
Content-Type: application/json

{
    "userUid": "user123",
    "contactUid": "contact456",
    "contactName": "John Doe"
}
```

```http
GET /api/contacts/{uid}
Authorization: Bearer <token>
```

#### Messages
```http
POST /api/messages
Content-Type: application/json

{
    "senderUid": "user123",
    "receiverUid": "user456",
    "messageText": "Hello!",
    "messageType": "text"
}
```

```http
GET /api/messages/{user1}/{user2}
Authorization: Bearer <token>
```

#### Admin (Development)
```http
GET /api/admin/stats
```

```http
POST /api/admin/reset-db
```

### 🔌 WebSocket Events

#### Connection
```javascript
// Connect to WebSocket
const ws = new WebSocket('ws://localhost:8080');

// Authentication after connection
ws.send(JSON.stringify({
    type: 'auth',
    token: 'firebase_token_here'
}));
```

#### Sending Messages
```javascript
ws.send(JSON.stringify({
    type: 'message',
    senderUid: 'user123',
    receiverUid: 'user456',
    messageText: 'Hello!',
    chatId: 'room_1'
}));
```

#### Receiving Messages
```javascript
ws.onmessage = function(event) {
    const data = JSON.parse(event.data);
    if (data.type === 'message') {
        // Handle incoming message
        displayMessage(data);
    }
};
```

## 🔒 Security Features

### 🛡️ Data Protection
- **AES-256 Encryption**: All messages encrypted before database storage
- **PBKDF2 Key Derivation**: 100,000 iterations for password hashing
- **Secure Communication**: HTTPS recommended for production
- **Input Validation**: Server-side validation for all API inputs
- **SQL Injection Protection**: Parameterized queries used throughout

### 🔐 Authentication Security
- **Firebase Auth**: Industry-standard authentication service
- **Token-based**: JWT tokens for API authentication
- **Session Management**: Automatic token refresh and validation
- **Password Security**: Minimum requirements and hashing

### 🚨 Privacy Features
- **Screenshot Protection**: Detects and warns on screenshot attempts
- **Disappearing Messages**: Automatic message deletion after timer
- **Data Retention**: Configurable message retention policies
- **User Privacy**: Optional profile visibility controls

### 🔒 Network Security
- **CORS Configuration**: Controlled cross-origin requests
- **Rate Limiting**: Protection against API abuse
- **Input Sanitization**: XSS and injection attack prevention
- **Error Handling**: No sensitive data in error messages

## 🤝 Contributing

### 🚀 Getting Started
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes
4. Test thoroughly
5. Commit changes (`git commit -m 'Add amazing feature'`)
6. Push to branch (`git push origin feature/amazing-feature`)
7. Open a Pull Request

### 📝 Development Guidelines
- Follow Java coding standards
- Add comments for complex logic
- Include tests for new features
- Update documentation as needed
- Test on both emulator and real devices

### 🧪 Testing Requirements
- All new features must include tests
- Existing tests must pass
- Manual testing on Android devices
- Server endpoint testing

## 📋 Version History

### v1.0.0 - Initial Release
- Basic chat functionality
- Firebase authentication
- SQLite database

### v1.1.0 - Real-time Features
- WebSocket implementation
- Real-time message sync
- Online/offline status

### v1.2.0 - Advanced Features
- Contact search system
- Chat list interface
- Multiple chat rooms

### v1.3.0 - Security Update
- End-to-end encryption
- Screenshot protection
- Disappearing messages

### v1.4.0 - Hybrid System
- MySQL database option
- Hybrid authentication
- Performance improvements

## 📞 Support & Contact

For questions, issues, or contributions:

- **GitHub Issues**: Use the issue tracker for bug reports
- **Documentation**: Check feature-specific .md files in `/server/`
- **Email**: Contact project maintainers
- **Discord/Slack**: Join development community

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🙏 Acknowledgments

- **Firebase**: Authentication and real-time database services
- **Flask**: Python web framework for API server
- **WebSocket**: Real-time communication protocol
- **Material Design**: Android UI components
- **cryptography**: Python encryption library
- **MySQL**: Database management system

---

**🎉 Happy Chatting with Bisto Chat!**

*This README provides comprehensive documentation for the entire Bisto Chat project. For feature-specific details, check the individual .md files in the `/server/` directory.*
