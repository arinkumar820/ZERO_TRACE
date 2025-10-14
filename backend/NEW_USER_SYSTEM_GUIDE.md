# 🎯 New User Registration System with Bio & Updated Hierarchy

## ✅ **SYSTEM UPDATED SUCCESSFULLY!**

Your messaging system now includes:
1. **Bio field** for user descriptions
2. **Updated hierarchy** with new communication rules
3. **Enhanced registration** with rank selection

---

## 📋 **New User Registration Process**

When a new user provides credentials, they now select:

### 1. **Basic Credentials**
- **Username:** Unique identifier
- **Password:** Secure password
- **Bio:** Personal description (optional)

### 2. **Rank Selection**
Choose from 3 ranks with specific communication privileges:

#### 🏆 **Commander (Level 1)**
- **Highest Authority**
- **Can communicate with:** EVERYONE (Commanders, Captains, Troops)
- **Example Bio:** "Team lead with full access authority"

#### 👨‍✈️ **Captain (Level 2)** 
- **Middle Management**
- **Can communicate with:** EVERYONE (Commanders, Captains, Troops)
- **Example Bio:** "Squad leader with management experience"

#### 🎖️ **Troops (Level 3)**
- **Basic Level**
- **Can communicate with:** Troops + Captains ONLY (No access to Commanders)
- **Example Bio:** "New team member, eager to learn"

---

## 🔄 **Updated Hierarchy Rules**

### **NEW Communication Matrix:**

| User Rank | Can Communicate With |
|-----------|---------------------|
| **Commander (1)** | ✅ Everyone (1, 2, 3) |
| **Captain (2)** | ✅ Everyone (1, 2, 3) |
| **Troops (3)** | ❌ Commanders (1), ✅ Captains (2), ✅ Troops (3) |

### **Key Changes:**
- ✅ **Commanders & Captains** have full access
- ❌ **Troops** are restricted from messaging Commanders
- ✅ **Bio field** added for user descriptions

---

## 📝 **Registration API Example**

### **Endpoint:** `POST /api/users/register`

### **Request Body:**
```json
{
  "username": "john_doe",
  "password": "securePassword123",
  "rank_level": 2,
  "bio": "Experienced team leader with 5 years in management"
}
```

### **Response:**
```json
{
  "message": "User registered successfully",
  "user": {
    "id": 12,
    "username": "john_doe",
    "rank_level": 2,
    "rank_name": "Captain",
    "bio": "Experienced team leader with 5 years in management"
  }
}
```

---

## 🌐 **Testing Your System**

### **1. Web Interface (Easy)**
Open: `http://10.48.121.125:3000/test-page.html`
- ✅ Register new users with bio
- ✅ Test hierarchy rules
- ✅ Works on any device

### **2. Command Line Test**
```bash
cd backend
node test-new-system.js
```

### **3. Manual API Test**
```bash
# Register new Commander
curl -X POST http://10.48.121.125:3000/api/users/register \
  -H "Content-Type: application/json" \
  -d '{"username": "alex", "password": "pass123", "rank_level": 1, "bio": "Senior leader"}'

# Check communication permissions
curl http://10.48.121.125:3000/api/users/12/permissible-chats
```

---

## 🚀 **Frontend Integration Guide**

### **Registration Form Elements:**

```html
<!-- Username -->
<input type="text" name="username" placeholder="Username" required>

<!-- Password -->
<input type="password" name="password" placeholder="Password" required>

<!-- Bio -->
<textarea name="bio" placeholder="Tell us about yourself (optional)"></textarea>

<!-- Rank Selection -->
<select name="rank_level" required>
  <option value="1">Commander - Full Access</option>
  <option value="2">Captain - Full Access</option>
  <option value="3">Troops - Limited Access</option>
</select>
```

### **JavaScript Registration:**
```javascript
const userData = {
  username: document.getElementById('username').value,
  password: document.getElementById('password').value,
  bio: document.getElementById('bio').value,
  rank_level: parseInt(document.getElementById('rank').value)
};

fetch('http://10.48.121.125:3000/api/users/register', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(userData)
});
```

---

## 📊 **Current System Status**

### **Database:**
- ✅ **Users table** updated with bio column
- ✅ **11 test users** with different ranks
- ✅ **Hierarchy rules** implemented

### **API Endpoints:**
- ✅ **Registration:** `/api/users/register` (includes bio)
- ✅ **Chat permissions:** `/api/users/:id/permissible-chats`
- ✅ **User details:** `/api/users/:id` (includes bio)

### **Test Users Available:**
- **Commanders:** trooper_007, soldier_alpha, private_beta
- **Captains:** captain_smith, captain_jones, arin, sara
- **Troops:** commander_rex, general_nova, deepak, mike

---

## 🎯 **Quick Test Scenarios**

### **Test 1: Commander Access**
- Login as Commander → Should see ALL users
- Result: ✅ Can message everyone

### **Test 2: Captain Access**
- Login as Captain → Should see ALL users
- Result: ✅ Can message everyone

### **Test 3: Troops Access**
- Login as Troops → Should see Troops + Captains only
- Result: ✅ Cannot message Commanders

### **Test 4: Bio Display**
- Check any user profile → Should show bio
- Result: ✅ Bio field visible in all responses

---

## 🔧 **Troubleshooting**

### **If bio field missing:**
```bash
cd backend
node migrate-database.js
```

### **If hierarchy not working:**
```bash
# Restart server
Get-Process -Name "node" | Stop-Process -Force
node server.js
```

### **Test specific user:**
```bash
# Check user permissions
curl http://10.48.121.125:3000/api/users/USER_ID/permissible-chats
```

---

## ✅ **System Ready Checklist**

- ✅ **Bio field:** Added to user registration
- ✅ **Hierarchy updated:** New communication rules implemented
- ✅ **Database migrated:** Bio column added to existing users
- ✅ **API updated:** All endpoints include bio data
- ✅ **Test page updated:** Includes bio input field
- ✅ **Network accessible:** Available on `http://10.48.121.125:3000`
- ✅ **Mobile friendly:** Test page works on all devices

## 🎉 **Your New System is Complete!**

Users can now:
1. **Register with bio** descriptions
2. **Select appropriate rank** based on their role
3. **Communicate according to hierarchy rules**
4. **Access system from any device** on your network

**Ready for production use! 🚀**