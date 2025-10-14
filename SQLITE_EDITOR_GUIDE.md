# 🗄️ SQLite Editor Guide - Complete Database Access

## 📊 **Your App's SQLite Database:**

### **Database Information:**
- **Name:** `chats.db`
- **Version:** 1
- **Location:** `/data/data/com.sameetasadullah.i180479_180531/databases/chats.db` (on Android device)
- **Table:** `chatsTable`

### **Table Schema:**
```sql
CREATE TABLE chatsTable (
    _id INTEGER PRIMARY KEY AUTOINCREMENT,
    sender_name TEXT NOT NULL,
    receiver_name TEXT NOT NULL,
    message TEXT NOT NULL,
    time TEXT NOT NULL
);
```

---

## 🛠️ **SQLite Editor Options**

### **1. 🏆 RECOMMENDED: Android Studio Database Inspector**

#### **How to Access:**
1. **Open Android Studio**
2. **Run your app** on device/emulator
3. **Go to:** View → Tool Windows → Database Inspector
4. **Select your app** from the running processes
5. **Navigate to:** `chats.db` → `chatsTable`

#### **Features:**
- ✅ Real-time data viewing
- ✅ Execute SQL queries
- ✅ Edit data directly
- ✅ Export data
- ✅ No setup required

#### **Screenshot Location:**
```
View → Tool Windows → Database Inspector
```

---

### **2. 🔧 DB Browser for SQLite (Desktop App)**

#### **Download & Install:**
1. **Go to:** [https://sqlitebrowser.org/dl/](https://sqlitebrowser.org/dl/)
2. **Download:** Windows version
3. **Install:** Follow setup wizard

#### **How to Use:**
1. **First, extract database from device:**
   ```bash
   adb pull /data/data/com.sameetasadullah.i180479_180531/databases/chats.db C:\temp\chats.db
   ```
2. **Open DB Browser**
3. **File → Open Database**
4. **Select:** `C:\temp\chats.db`
5. **Browse Data tab** to view tables

#### **Features:**
- ✅ Full SQL editor
- ✅ Visual table editor
- ✅ Database structure viewer
- ✅ Import/Export capabilities
- ✅ Query execution

---

### **3. 💻 Command Line SQLite (Built-in)**

#### **For Windows (if SQLite installed):**
1. **Extract database first:**
   ```powershell
   adb pull /data/data/com.sameetasadullah.i180479_180531/databases/chats.db C:\temp\chats.db
   ```

2. **Open PowerShell and run:**
   ```powershell
   # If SQLite is installed
   sqlite3 C:\temp\chats.db
   
   # Basic commands:
   .tables                    # List all tables
   .schema chatsTable         # Show table structure
   SELECT * FROM chatsTable;  # View all data
   .quit                      # Exit
   ```

#### **Install SQLite (if needed):**
1. **Download:** [https://sqlite.org/download.html](https://sqlite.org/download.html)
2. **Choose:** Precompiled Binaries for Windows
3. **Extract:** To `C:\sqlite\`
4. **Add to PATH:** System Environment Variables

---

### **4. 🌐 Online SQLite Editors**

#### **SQLiteViewer.app:**
1. **Go to:** [https://sqliteviewer.app/](https://sqliteviewer.app/)
2. **Extract database** from device first
3. **Upload:** `chats.db` file
4. **View/Edit** data in browser

#### **Features:**
- ✅ No installation required
- ✅ Works in any browser
- ✅ Basic editing capabilities
- ⚠️ Upload required (privacy consideration)

---

### **5. 📱 Device File Explorer (Root Required)**

#### **Using ADB:**
```bash
# Connect device with USB debugging enabled
adb shell
cd /data/data/com.sameetasadullah.i180479_180531/databases/
ls -la
# You'll see: chats.db, chats.db-shm, chats.db-wal
```

#### **Using Root Explorer Apps:**
- **Solid Explorer**
- **Root Explorer** 
- **ES File Explorer**

---

## 🚀 **Quick Start: Android Studio Method**

### **Step-by-Step:**

1. **Open Android Studio**
2. **Build and run your app** on device/emulator
3. **Open Database Inspector:**
   ```
   View → Tool Windows → Database Inspector
   ```
4. **Select your app** from running processes list
5. **Expand databases** → `chats.db` → `chatsTable`
6. **View data** in the table view
7. **Execute queries** in the query tab

### **Sample Queries to Try:**
```sql
-- View all messages
SELECT * FROM chatsTable;

-- Count total messages
SELECT COUNT(*) FROM chatsTable;

-- Find messages from specific sender
SELECT * FROM chatsTable WHERE sender_name = 'John';

-- Insert test message
INSERT INTO chatsTable (sender_name, receiver_name, message, time) 
VALUES ('TestUser', 'TestReceiver', 'Hello from SQLite!', '2025-01-15 10:30:00');

-- Update a message
UPDATE chatsTable SET message = 'Updated message' WHERE _id = 1;

-- Delete a message
DELETE FROM chatsTable WHERE _id = 1;
```

---

## 🔍 **Database Location by Platform**

### **Android Device/Emulator:**
```
/data/data/com.sameetasadullah.i180479_180531/databases/chats.db
```

### **Extracted to PC:**
```
C:\temp\chats.db (after using adb pull)
```

### **Android Studio Cache:**
```
C:\Users\{username}\.android\avd\{emulator-name}.avd\data\data\com.sameetasadullah.i180479_180531\databases\
```

---

## ⚡ **Quick Commands Reference**

### **ADB Commands:**
```bash
# List all databases
adb shell "ls -la /data/data/com.sameetasadullah.i180479_180531/databases/"

# Pull database to PC
adb pull /data/data/com.sameetasadullah.i180479_180531/databases/chats.db C:\temp\chats.db

# Push updated database back
adb push C:\temp\chats.db /data/data/com.sameetasadullah.i180479_180531/databases/chats.db
```

### **SQLite Commands:**
```sql
-- Basic operations
.tables                          -- List tables
.schema tablename                -- Show table structure
SELECT * FROM chatsTable;        -- View all data
.mode column                     -- Better formatting
.headers on                      -- Show column headers
.quit                           -- Exit
```

---

## 🎯 **Recommended Workflow**

### **For Development (Best):**
1. **Use Android Studio Database Inspector** for real-time viewing
2. **Use DB Browser for SQLite** for complex operations
3. **Keep both open** during development

### **For Quick Checks:**
1. **Android Studio Database Inspector** - instant access
2. **Command line** - quick queries

### **For Data Analysis:**
1. **Extract database** with ADB
2. **Open in DB Browser** for full analysis
3. **Export to Excel/CSV** if needed

---

## 🔧 **Troubleshooting**

### **Can't See Database:**
- **Ensure app is running** on device/emulator
- **Check Database Inspector** is connected to right process
- **Restart Android Studio** if not showing

### **Permission Denied:**
- **Enable USB debugging** on device
- **Grant ADB access** when prompted
- **Use emulator** if device is locked down

### **Database is Empty:**
- **Use the app first** to create some chat messages
- **Database is created** only when app first runs
- **Check if DBHelper is being called** in your code

---

## 📋 **Summary**

**Best SQLite Editor for Android Development:**
1. 🥇 **Android Studio Database Inspector** - Built-in, real-time
2. 🥈 **DB Browser for SQLite** - Full-featured desktop app
3. 🥉 **Command Line SQLite** - Quick and powerful

**Your database file:** `chats.db` with table `chatsTable`
**Location:** Inside your app's private data directory
**Access:** Via Android Studio (easiest) or ADB extraction