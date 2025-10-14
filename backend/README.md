# Secure Messaging Backend API

A complete Node.js/Express backend system for a secure messaging app with hierarchical chat access control based on user ranks.

## 🏗️ Architecture Overview

The system implements a simplified military-style hierarchy where users are assigned ranks that determine their messaging permissions:

- **Troop (rank_level: 1)**: Can chat with Troops and Captains
- **Captain (rank_level: 2)**: Can chat with Troops, Captains, and Commanders  
- **Commander (rank_level: 3)**: Can chat with everyone

## 📋 Features

- ✅ **User Registration** with rank assignment
- ✅ **Hierarchical Chat Access Control** 
- ✅ **Secure Password Hashing** (bcrypt)
- ✅ **SQLite Database** with proper schema
- ✅ **Input Validation** and error handling
- ✅ **CORS & Security** middleware
- ✅ **API Documentation** endpoint

## 🗄️ Database Schema

### Users Table
```sql
CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    rank_level INTEGER NOT NULL CHECK (rank_level IN (1, 2, 3)),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

## 🚀 Quick Start

### Prerequisites
- Node.js (version 14 or higher)
- npm or yarn

### Installation

1. **Install dependencies:**
   ```bash
   npm install
   ```

2. **Initialize database:**
   ```bash
   # Create database schema only
   npm run init-db
   
   # Create schema + add sample users for testing
   npm run init-db -- --with-samples
   ```

3. **Start the server:**
   ```bash
   npm start
   ```

4. **Access the API:**
   - API Documentation: http://localhost:3000
   - Health Check: http://localhost:3000/health

## 📚 API Endpoints

### 1. User Registration
**POST** `/api/users/register`

Register a new user with rank assignment.

**Request Body:**
```json
{
  "username": "trooper_007",
  "password": "password123",
  "rank_level": 1
}
```

**rank_level values:**
- `1` = Troop
- `2` = Captain
- `3` = Commander

**Response (201 Created):**
```json
{
  "message": "User registered successfully",
  "user": {
    "id": 1,
    "username": "trooper_007",
    "rank_level": 1,
    "rank_name": "Troop"
  }
}
```

### 2. Get Permissible Chat Users
**GET** `/api/users/:userId/permissible-chats`

Get list of users the specified user can chat with based on rank hierarchy.

**Response (200 OK):**
```json
{
  "user": {
    "id": 1,
    "username": "trooper_007", 
    "rank_level": 1,
    "rank_name": "Troop"
  },
  "permissible_chats": [
    {
      "id": 2,
      "username": "soldier_alpha",
      "rank_level": 1,
      "rank_name": "Troop"
    },
    {
      "id": 4,
      "username": "captain_smith",
      "rank_level": 2,
      "rank_name": "Captain"
    }
  ],
  "total_permissible_users": 2
}
```

### 3. Get User by ID
**GET** `/api/users/:userId`

Get user information by ID.

**Response (200 OK):**
```json
{
  "id": 1,
  "username": "trooper_007",
  "rank_level": 1,
  "rank_name": "Troop"
}
```

## 🏆 Hierarchical Access Rules

The system enforces strict hierarchical messaging rules:

### Troop (rank_level: 1)
- ✅ Can message: **Troops** + **Captains**
- ❌ Cannot message: Commanders

### Captain (rank_level: 2)  
- ✅ Can message: **Troops** + **Captains** + **Commanders**
- ✅ Full access to all ranks

### Commander (rank_level: 3)
- ✅ Can message: **Everyone**
- ✅ Highest privilege level

## 🔐 Security Features

- **Password Hashing**: bcrypt with 12 salt rounds
- **Input Validation**: Comprehensive validation for all endpoints
- **SQL Injection Protection**: Parameterized queries
- **CORS**: Configurable cross-origin resource sharing
- **Helmet**: Security headers middleware
- **Error Handling**: Secure error responses (no information leakage)

## 🧪 Testing

The system includes sample users for testing:

### Sample Users (when using --with-samples)
```
Troops:
- trooper_007 (ID: 1)
- soldier_alpha (ID: 2) 
- private_beta (ID: 3)

Captains:
- captain_smith (ID: 4)
- captain_jones (ID: 5)

Commanders:
- commander_rex (ID: 6)
- general_nova (ID: 7)
```

### Test the API

1. **Register a new user:**
   ```bash
   curl -X POST http://localhost:3000/api/users/register \
     -H "Content-Type: application/json" \
     -d '{"username": "test_user", "password": "password123", "rank_level": 2}'
   ```

2. **Get permissible chats:**
   ```bash
   curl http://localhost:3000/api/users/1/permissible-chats
   ```

## 📁 Project Structure

```
backend/
├── server.js              # Main Express server
├── database.js            # Database connection & operations
├── routes/
│   └── users.js           # User-related endpoints
├── scripts/
│   └── initDatabase.js    # Database initialization
├── package.json           # Dependencies & scripts
├── README.md              # Documentation
└── message_database.db    # SQLite database (created at runtime)
```

## 🛠️ Development Scripts

```bash
# Start server in production mode
npm start

# Start server in development mode (if nodemon installed)
npm run dev

# Initialize database
npm run init-db

# Initialize database with sample data
npm run init-db -- --with-samples
```

## 🔧 Configuration

### Environment Variables
- `PORT`: Server port (default: 3000)
- `NODE_ENV`: Environment (development/production)
- `FRONTEND_URL`: CORS origin URL (default: '*')

### Database
The system uses SQLite for simplicity. The database file (`message_database.db`) is created automatically in the project root.

## ⚠️ Important Notes

1. **Rank Level Storage**: The `rank_level` value stored during registration is directly used by the chat access endpoint to enforce hierarchical rules.

2. **Frontend Integration**: The frontend should send the numeric `rank_level` value (1, 2, or 3) based on the user's UI selection.

3. **Security**: In production, ensure proper CORS configuration and use HTTPS.

4. **Database**: For production, consider migrating to PostgreSQL or MySQL.

## 🤝 Frontend Integration

The frontend application should:

1. **Registration Form**: Provide UI elements (Radio buttons/Dropdown) for rank selection:
   - "Troop" → send `rank_level: 1`
   - "Captain" → send `rank_level: 2` 
   - "Commander" → send `rank_level: 3`

2. **Chat Interface**: Use the `/permissible-chats` endpoint to determine which users can be messaged.

3. **Error Handling**: Handle API errors gracefully (409 for duplicate usernames, 400 for validation errors, etc.)

## 📄 License

MIT License - Feel free to use this code for your projects.