# 🚀 Quick Start: SQLite Database Access

## 🎯 **Fastest Way to View Your SQLite Database**

### **Method 1: Android Studio Database Inspector (RECOMMENDED)**

#### **Steps:**
1. **Open Android Studio**
2. **Build and run** your app on device/emulator
3. **Navigate to database test screen** in your app
4. **Add sample data** using the test buttons
5. **In Android Studio**: View → Tool Windows → **Database Inspector**
6. **Select your app** from running processes
7. **Expand**: `chats.db` → `chatsTable`
8. **View your data** in real-time!

---

### **Method 2: Using Your Database Test Activity**

#### **How to Access:**
1. **Run your app**
2. **Navigate to** `DatabaseTestActivity` (you may need to add a button in your main activity)
3. **Or launch directly** using ADB:
   ```bash
   adb shell am start -n com.sameetasadullah.i180479_180531/.DatabaseTestActivity
   ```

#### **Test Buttons:**
- ✅ **Add Sample Chat Data** - Creates 10 test messages
- 📊 **View All Chat Data** - Shows data in Logcat
- 🗑️ **Clear All Data** - Empties the database
- ℹ️ **Show Database Info** - Shows database location and stats

---

### **Method 3: Extract Database File**

#### **Using ADB:**
```bash
# Connect your device/emulator
adb devices

# Extract the database
adb pull /data/data/com.sameetasadullah.i180479_180531/databases/chats.db C:\temp\chats.db

# Open with DB Browser for SQLite (download from sqlitebrowser.org)
```

---

## 📊 **Your Database Details**

### **Database Info:**
- **File Name:** `chats.db`
- **Table:** `chatsTable` 
- **Location:** `/data/data/com.sameetasadullah.i180479_180531/databases/`

### **Table Structure:**
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

## 🔧 **Troubleshooting**

### **Can't See Database in Android Studio:**
1. **Make sure app is running** on device/emulator
2. **Use the test activity** to add some data first
3. **Refresh Database Inspector** (refresh button in the tool window)
4. **Check Logcat** for database creation messages

### **Database is Empty:**
1. **Run the test activity** first
2. **Click "Add Sample Data"** button
3. **Use your chat app** to create real messages
4. **Check Logcat** for database operations

---

## ⚡ **Quick Test Commands**

### **Launch Test Activity:**
```bash
adb shell am start -n com.sameetasadullah.i180479_180531/.DatabaseTestActivity
```

### **Check Database Exists:**
```bash
adb shell "ls -la /data/data/com.sameetasadullah.i180479_180531/databases/"
```

### **View Database in Command Line:**
```bash
adb shell "sqlite3 /data/data/com.sameetasadullah.i180479_180531/databases/chats.db 'SELECT * FROM chatsTable;'"
```

---

## 🎯 **Recommended Workflow**

1. **Build and run your app**
2. **Launch DatabaseTestActivity**
3. **Add sample data**
4. **Open Android Studio Database Inspector**
5. **View and query your data in real-time**
6. **For complex analysis**: Extract database and use DB Browser

---

## 📱 **Sample Data Preview**

After clicking "Add Sample Data", you'll see conversations like:
```
John Doe → Jane Smith: "Hey, how are you doing?"
Jane Smith → John Doe: "I'm doing great! Thanks for asking."
Alice Johnson → Bob Wilson: "Are we still meeting today?"
Bob Wilson → Alice Johnson: "Yes, see you at 3 PM"
...and more!
```

**Total**: 10 sample messages with realistic chat conversations.

---

## 🎉 **You're Ready!**

Your SQLite database is now accessible through:
- ✅ Android Studio Database Inspector (real-time)
- ✅ Database Test Activity (app interface)  
- ✅ Command line tools (advanced)
- ✅ External SQLite browsers (full-featured)

**Happy database browsing!** 🗄️📊