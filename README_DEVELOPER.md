# Bisto Chat – Developer README (Full Architecture, Workflows, and Code Map)

This is the comprehensive, developer-focused README for the Bisto Chat project. It explains how the app is structured end-to-end, how to start it, where each feature lives, exactly where encryption and decryption occur, and how to test, debug, and extend the system.

Use this guide when you need to:
- Understand the complete architecture (Android + Python servers + DB)
- Start and run all services in development (Windows + Android Studio)
- Know exactly where encryption/decryption is performed
- Find source files by feature quickly
- Troubleshoot common issues

---

Table of Contents
- High-Level Architecture
- Project Structure (Key Paths)
- Quick Start (Windows + Android Studio)
- Runtime Workflows
  - App Startup and Auth
  - Contact Search and Inbox
  - Chat Flow (Real-time + History)
  - Where Encryption/Decryption Happens
- Configuration
- Backend Services (Python)
- Android App (Java) – Key Classes
- UI Layouts (XML)
- API Endpoints (Summary)
- WebSocket Events (Summary)
- Disappearing Messages
- Screenshot Protection
- Hybrid System (Firebase Auth + MySQL)
- Testing & Verification
- Troubleshooting
- Extensibility Tips
- File Index (Selected)

---

High-Level Architecture

Android App (Java)
- UI, authentication, contacts, chat screen, WebSocket client.

Flask REST API (Python)
- User search, contacts, message history, admin stats. Uses SQLite (dev) or MySQL (hybrid).

WebSocket Server (Python)
- Real-time messaging and presence. Persists messages to DB.

Databases
- SQLite (default dev) – server/bisto_chat.db auto-created
- MySQL (optional hybrid) – for user search/contacts/messaging in hybrid mode

Encryption
- AES-256 (Fernet, PBKDF2) – secure at-rest storage
- Tulu-language obfuscation – Kannada/Tulu mapping + golden-ratio scrambling
- Hybrid (Tulu + AES) – layered security and cultural obfuscation

---

Project Structure (Key Paths)

Android app (Java):
- app/src/main/java/com/sameetasadullah/i180479_180531/
  - BistoChatApplication.java – app initialization
  - screen1.java – launcher
  - fragmentsContainer.java, fragmentAdapter.java – main nav host
  - fragment_screen4.java – chat list (inbox)
  - ChatActivity.java – conversation UI
  - ChatMessageAdapter.java – message list, countdown timers
  - WebSocketClientManager.java – socket lifecycle
  - ApiClient.java – REST client (BASE_URL)
  - SearchContactsActivity.java – unified search UI
  - FirebaseUserManager.java – Firebase Authentication wrappers
  - TuluEncryption.java – Android-side Tulu encryption
  - ScreenshotProtection*.java – screenshot detection/protection
  - DisappearingMessage*.java – disappearing message settings/managers

Android resources (XML):
- app/src/main/res/layout/*.xml – activities, fragments, rows, message items

Servers (Python):
- server/server.py – Flask REST API (default port 8080)
- server/websocket_server.py – real-time WebSocket server
- server/encryption_utils.py – AES/Tulu/Hybrid encryption wrapper
- server/tulu_encryption.py – Tulu encryption engine + HybridEncryption
- server/requirements.txt – Python deps
- server/*.py – tools, tests, demos (see File Index)

Documentation:
- README.md – user-facing overview
- TULU_ENCRYPTION_GUIDE.md – Tulu encryption details and examples
- server/README.md – server-focused quickstart

---

Quick Start (Windows + Android Studio)

1) Start REST API (SQLite dev)
- Open PowerShell:
  - cd i180479_180531/server
  - pip install -r requirements.txt
  - python .\server.py
- REST base URL (local browser): http://localhost:8080
- Android emulator base URL: http://10.0.2.2:8080

2) Start WebSocket server (if running separately)
- In another PowerShell window:
  - cd i180479_180531/server
  - python .\websocket_server.py

3) Open Android project & configure URL
- Open the i180479_180531 folder in Android Studio
- Place app/google-services.json
- Edit ApiClient.java BASE_URL:
  - Emulator: http://10.0.2.2:8080
  - Real device: http://YOUR_PC_IPV4:8080 (find using ipconfig)

4) Build and run the app
- Run on emulator or device from Android Studio
- Use Server Test screen to validate connectivity

---

Runtime Workflows

App Startup and Auth
- screen1.java launches → inputCredentials.java for login/register
- FirebaseUserManager.java handles auth with Firebase
- Hybrid option (optional): HybridUserManager.java/MySQLUserManager.java sync Firebase user to MySQL via server/hybrid_user_service.py

Contact Search and Inbox
- SearchContactsActivity.java chooses Firebase search or REST-based search (server/api)
- fragment_screen4.java shows the chat list (inbox) with last messages and statuses

Chat Flow (Real-time + History)
- Open ChatActivity.java via inbox
- WebSocketClientManager.java connects to WS server for real-time exchange
- ApiClient.java fetches message history via REST when needed
- ChatMessageAdapter.java renders messages, timestamps, disappearing countdowns

Where Encryption/Decryption Happens

Modes supported by the server (encryption_utils.py):
- "aes" (default), "tulu", or "hybrid"

REST Path (History & Post)
- Send (Android → REST /api/messages): Server encrypts before storing
- Retrieve (Android → REST /api/messages/...): Server decrypts before responding

Server code (Python):
- server/encryption_utils.py
  - MessageEncryption.encrypt_message(text, mode)
  - MessageEncryption.decrypt_message(text, mode)
- server/tulu_encryption.py
  - TuluEncryption.encrypt_message/decrypt_message
  - HybridEncryption for layered Tulu + AES
- server/server.py (endpoints):
  - POST /api/messages: Encrypt incoming message_text before insert
  - GET /api/messages/...: Decrypt stored message_text before returning JSON

WebSocket Path (Real-time)
- Send (Android → WebSocket): Server can encrypt message_text before DB persist
- Broadcast: Often plaintext broadcast to clients for immediate display; storage is encrypted. You can switch to encrypted broadcast + client-side decrypt if desired.

Client-Side (Optional Tulu)
- app/.../TuluEncryption.java
  - TuluEncryption.Helper.encrypt(plain)
  - TuluEncryption.Helper.decrypt(cipher)
  - TuluEncryption.Helper.isEncrypted(s)
- If enabling client-side Tulu:
  - Encrypt before send in ChatActivity
  - Decrypt on receive in WebSocket listener or adapter bind

Recommended defaults:
- Server-only AES (fast, secure at-rest, minimal payload growth)
- Use Tulu or Hybrid on server for themed security or layered protection
- Client-side Tulu is optional for a culturally themed E2E obfuscation

---

Configuration

Android
- ApiClient.java BASE_URL
  - Emulator: http://10.0.2.2:8080
  - Device: http://YOUR_PC_IPV4:8080 (ipconfig → IPv4 Address)
- Manifest adds INTERNET permission and cleartext traffic for dev

Server
- server/encryption_utils.py modes
  - encrypt_message(plain, encryption_mode="aes")
  - decrypt_message(cipher, encryption_mode="aes")
- Use "tulu" or "hybrid" to switch behaviors

Hybrid
- server/hybrid_user_service.py (port 5000)
- MySQL credentials inside script

---

Backend Services (Python)

Core entry points
- server.py – Flask REST API (port 8080). Endpoints for auth (dev), user search, contacts, messages, admin stats
- websocket_server.py – Real-time WebSocket server
- hybrid_user_service.py – Firebase → MySQL bridge (port 5000)

Encryption
- encryption_utils.py – AES/Tulu/Hybrid wrapper and helpers
- tulu_encryption.py – Tulu mapping engine + HybridEncryption
- demo_tulu_encryption.py – demo and performance benchmarks

DB Utilities
- clean_database.py – reset local DB
- examine_db_schema.py – inspect schema
- init_chat_rooms.py – seed default chat rooms

---

Android App (Java) – Key Classes

Initialization & Navigation
- BistoChatApplication.java – global init (logging, locale, toggles)
- screen1.java – launcher; routes to fragmentsContainer or auth
- fragmentsContainer.java + fragmentAdapter.java – main nav host

Auth & Users
- inputCredentials.java – login/registration UI
- FirebaseUserManager.java – Firebase Auth operations
- HybridUserManager.java / MySQLUserManager.java – hybrid integration
- User.java – profile data model

Search & Contacts
- SearchContactsActivity.java – unified contact search
- FirebaseSearchContactsActivity.java / LocalSearchContactsActivity.java – alt search modes
- UserSearchAdapter.java, ContactsAdapter.java – Recycler adapters

Chat & Real-time
- ChatActivity.java – conversation UI, message input
- WebSocketClientManager.java – connect, send, receive, listeners
- WebSocketClientListener.java – callbacks to UI
- WebSocketMessage.java – message model (incl. disappearing flags)
- ChatMessageAdapter.java – binds messages and countdowns

Privacy & Security
- TuluEncryption.java – client-side Tulu encryption
- ScreenshotProtection.java / *Detector / *Delegate – screenshot defense
- DisappearingMessageManager.java / Settings* – disappearing message behavior

---

UI Layouts (XML)

Key layouts in app/src/main/res/layout/
- activity_chat.xml – chat screen
- item_message_sent.xml, item_message_received.xml – message bubbles + countdown TextView
- activity_search_contacts.xml – search UI
- fragment_screen4.xml – chat list (inbox)
- activity_input_credentials.xml – auth UI
- activity_server_test.xml – server diagnostics

---

API Endpoints (Summary)

Auth (dev)
- POST /api/auth/register
- POST /api/auth/login

Users
- GET /api/users/search?q=term&uid=currentUser
- PUT /api/users/{uid}/status

Contacts
- POST /api/contacts
- GET /api/contacts/{uid}

Messages
- POST /api/messages (server encrypts before storing)
- GET /api/messages/{user1}/{user2} (server decrypts before returning)

Admin
- GET /api/admin/stats
- POST /api/admin/reset-db

---

WebSocket Events (Summary)

Connect
- Client connects and may authenticate (if required by your WS flavor)

Message
- Client sends {senderUid, receiverUid, messageText, ...}
- Server stores (encrypted) and broadcasts to room/users

History
- Use REST to fetch history (server decrypts before returning)

---

Disappearing Messages

Behavior
- Messages expire after a time window (default ~2 minutes)
- UI shows per-message countdown timers
- On expiry: UI hides/marks message as disappeared (server may keep data for audit)

Client code
- WebSocketMessage.java – isDisappearing, disappearAfterMs
- ChatMessageAdapter.java – countdown TextView updates
- DisappearingMessageManager.java – settings and lifecycle

---

Screenshot Protection

Components
- ScreenshotDetector.java – detect OS screenshot events
- ScreenshotDetectionDelegate.java – callbacks
- ScreenshotProtection.java – apply blur/overlay/warning
- ScreenshotProtectionTestActivity.java – manual testing

Note: Screenshot APIs vary by Android version/OEM; behavior may differ by device.

---

Hybrid System (Firebase Auth + MySQL)

Server – server/hybrid_user_service.py (port 5000)
- Sync Firebase users into MySQL
- REST endpoints: /api/user/sync, /api/user/search, /api/user/profile/{uid}, /api/user/status

Android – HybridUserManager.java / MySQLUserManager.java
- Configure BASE_URL to http://YOUR_PC_IPV4:5000
- Use for user search and profile retrieval

---

Testing & Verification

Servers
- PowerShell (from i180479_180531/server):
  - python .\server.py
  - python .\websocket_server.py
  - python .\demo_tulu_encryption.py (encryption demo & benchmarks)

App
- ServerTestActivity – verify REST reachability and /api/admin/stats
- WebSocketTestActivity – connect and send/receive
- FirebaseTestActivity – auth checks

CLI Snippets (PowerShell)
- Invoke-RestMethod http://localhost:8080/api/admin/stats | ConvertTo-Json
- Invoke-RestMethod "http://localhost:8080/api/users/search?q=john&uid=test" | ConvertTo-Json

---

Troubleshooting

Cannot connect from device
- Use your PC IPv4 (ipconfig) in ApiClient BASE_URL
- Ensure firewall allows 8080/5000

Build issues (Android)
- Sync Gradle, Clean Project, Invalidate Caches & Restart
- Verify google-services.json exists and package name matches Firebase project

Database stuck/corrupt (dev)
- Stop servers; delete server/bisto_chat.db
- Or POST /api/admin/reset-db

Encryption mismatch
- Ensure both encrypt and decrypt use the same mode ("aes" vs "tulu" vs "hybrid")
- For Tulu content rendering, confirm UTF-8 and fonts

WebSocket not receiving
- Start websocket_server.py
- Check logs for connections

---

Extensibility Tips

Add new API endpoints
- Edit server/server.py; add Flask routes; update README

Add a new encryption scheme
- Create a new module (e.g., hindi_encryption.py)
- Mirror minimal decrypt on Android if client-side needed
- Plug into encryption_utils.py and switch via encryption_mode

Enhance reliability
- Improve reconnection/backoff in WebSocketClientManager
- Queue outgoing messages for offline send

Scale out
- Move to production DB (MySQL/Postgres)
- Run Flask/WS behind a reverse proxy
- Use Redis/pub-sub for WS broadcast across instances

---

File Index (Selected)

Android (Java)
- BistoChatApplication.java – app init
- screen1.java – launcher and first navigation step
- fragmentsContainer.java – hosts bottom navigation & fragments
- fragment_screen4.java – inbox (chat list)
- ChatActivity.java – chat screen
- ChatMessageAdapter.java – bind messages + countdown timers
- WebSocketClientManager.java – websocket lifecycle
- ApiClient.java – REST client (BASE_URL)
- SearchContactsActivity.java – contact search
- FirebaseUserManager.java – Firebase auth
- TuluEncryption.java – client-side Tulu encryption
- ScreenshotProtection*.java – screenshot defense
- DisappearingMessage*.java – disappearing message feature

Server (Python)
- server.py – REST API (Flask)
- websocket_server.py – real-time chat
- hybrid_user_service.py – Firebase → MySQL bridge
- encryption_utils.py – AES/Tulu/Hybrid modes
- tulu_encryption.py – Tulu engine + HybridEncryption
- demo_tulu_encryption.py – demo & benchmarks
- init_chat_rooms.py – seed chat rooms
- clean_database.py – DB maintenance

---

Typical Workflows (Step-by-step)

Start everything (dev)
1) PowerShell – REST API:
   - cd i180479_180531/server
   - pip install -r requirements.txt
   - python .\server.py
2) PowerShell – WebSocket:
   - python .\websocket_server.py
3) Android Studio:
   - Update ApiClient BASE_URL
   - Run on emulator (10.0.2.2) or device (YOUR_PC_IPV4)

Send a message (server-only AES)
- Android ChatActivity → WS send plaintext
- WebSocket server persists message_text = AES-encrypted
- REST history returns decrypted messages to Android

Send a message (server Hybrid Tulu+AES)
- Android → WS send plaintext
- WebSocket server persists Tulu→AES encrypted text
- REST history decrypts AES then Tulu → returns plaintext

Send a message (client Tulu, server AES at rest)
- Android ChatActivity → encrypt with TuluEncryption → send
- Server may store as-is or apply AES before storing
- On receive (real-time): client Tulu-decrypt → show
- On history (REST): server returns stored (if AES) → client Tulu-decrypt

This README is intended to be your single point of truth for the system’s structure and workflows. For feature-specific details (like Tulu encryption internals), see TULU_ENCRYPTION_GUIDE.md. For server-only quick start, see server/README.md.
