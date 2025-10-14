# 🌐 Network Setup Guide - Run Without NPM

This guide shows you how to run your secure messaging backend **without npm** and make it accessible from other devices on your network.

## 🚀 Quick Start (Multiple Ways)

### Method 1: Double-click Batch File
Simply double-click: **`start-server.bat`**

### Method 2: PowerShell Script (Recommended)
```powershell
.\start-server.ps1
```

### Method 3: Direct Node.js
```bash
node server.js
```

## 📱 Your Network Information

- **Your Computer IP:** `10.48.121.125`
- **Local Access:** `http://localhost:3000`
- **Network Access:** `http://10.48.121.125:3000`

## 🔥 Windows Firewall Setup

To allow other devices to connect, run this command **as Administrator**:

```powershell
New-NetFirewallRule -DisplayName "Node.js Server" -Direction Inbound -Protocol TCP -LocalPort 3000 -Action Allow
```

## 🧪 Testing the Server

### Option 1: HTML Test Page (Easy)
1. Start the server using any method above
2. Open `test-page.html` in any browser on any device
3. Change the IP to your computer's IP: `http://10.48.121.125:8080/test-page.html`

### Option 2: Command Line Test
```bash
node test-client.js
```

### Option 3: Manual cURL Tests
```bash
# Health check
curl http://10.48.121.125:3000/health

# Register arin as Captain
curl -X POST http://10.48.121.125:3000/api/users/register \
  -H "Content-Type: application/json" \
  -d '{"username": "arin", "password": "password123", "rank_level": 2}'

# Register deepak as Commander  
curl -X POST http://10.48.121.125:3000/api/users/register \
  -H "Content-Type: application/json" \
  -d '{"username": "deepak", "password": "password123", "rank_level": 3}'

# Check chat permissions for user ID 8 (arin)
curl http://10.48.121.125:3000/api/users/8/permissible-chats
```

## 📋 API Endpoints Accessible from Any Device

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `http://10.48.121.125:3000/health` | Check server status |
| POST | `http://10.48.121.125:3000/api/users/register` | Register new user |
| GET | `http://10.48.121.125:3000/api/users/{id}/permissible-chats` | Get chat permissions |
| GET | `http://10.48.121.125:3000/api/users/{id}` | Get user info |

## 👥 Adding "arin" and "deepak"

### Using the HTML Test Page:
1. Open `test-page.html` in browser
2. Click "Register Arin (Captain)" 
3. Click "Register Deepak (Commander)"

### Using Command Line:
```bash
node test-client.js
```

### Manual Registration Data:

**Arin (Captain):**
```json
{
  "username": "arin",
  "password": "password123", 
  "rank_level": 2
}
```

**Deepak (Commander):**
```json
{
  "username": "deepak",
  "password": "password123",
  "rank_level": 3
}
```

## 🏆 Rank Hierarchy

- **Rank 1 (Troop):** Can chat with Troops + Captains
- **Rank 2 (Captain):** Can chat with Troops + Captains + Commanders
- **Rank 3 (Commander):** Can chat with everyone

## 📱 Testing from Mobile/Other Devices

1. Connect devices to the same WiFi network
2. Open browser on mobile device
3. Go to: `http://10.48.121.125:3000` 
4. Or open: `http://10.48.121.125:3000/test-page.html`

## 🔧 Troubleshooting

### Server won't start:
```bash
# Check if port 3000 is in use
netstat -an | findstr :3000

# Kill any process using port 3000
Get-Process -Name "node" | Stop-Process -Force
```

### Can't access from other devices:
1. Make sure firewall allows port 3000
2. Check both devices are on same WiFi
3. Try disabling Windows Firewall temporarily
4. Verify IP address hasn't changed

### Get current IP address:
```powershell
(Get-NetIPAddress -AddressFamily IPv4 | Where-Object {$_.IPAddress -ne "127.0.0.1" -and $_.PrefixOrigin -eq "Dhcp"}).IPAddress
```

## 🗃️ Database Location

Your database is located at:
```
C:\Users\arink\Downloads\Bisto-Chat-Java-Firebase-master\Bisto-Chat-Java-Firebase-master\i180479_180531\backend\message_database.db
```

## 🎯 Next Steps

1. **Start server:** Use any method above
2. **Test locally:** Open `http://localhost:3000`
3. **Test network:** Open `http://10.48.121.125:3000` from another device
4. **Register users:** Use HTML test page or command line
5. **Test hierarchy:** Check chat permissions for different ranks

Your backend is now ready for network access without npm! 🎉