const sqlite3 = require('sqlite3').verbose();
const path = require('path');

/**
 * Database connection and schema management for the secure messaging app
 * 
 * This module handles:
 * - SQLite database connection
 * - Users table creation with rank_level for hierarchical access control
 * - Database utility functions
 */

// Database file path
const dbPath = path.join(__dirname, 'message_database.db');

// Create and configure database connection
const db = new sqlite3.Database(dbPath, (err) => {
    if (err) {
        console.error('Error opening database:', err.message);
        process.exit(1);
    }
    console.log('Connected to SQLite message_database at:', dbPath);
});

/**
 * Initialize database schema
 * Creates the users table with the required structure for rank-based messaging
 */
const initializeDatabase = () => {
    return new Promise((resolve, reject) => {
        const createUsersTable = `
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username VARCHAR(255) UNIQUE NOT NULL,
                password_hash VARCHAR(255) NOT NULL,
                rank_level INTEGER NOT NULL CHECK (rank_level IN (1, 2, 3)),
                bio TEXT DEFAULT '',
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        `;

        db.run(createUsersTable, (err) => {
            if (err) {
                console.error('Error creating users table:', err.message);
                reject(err);
            } else {
                console.log('Users table initialized successfully');
                resolve();
            }
        });
    });
};

/**
 * Check if username already exists in the database
 * @param {string} username - Username to check
 * @returns {Promise<boolean>} - True if username exists, false otherwise
 */
const userExists = (username) => {
    return new Promise((resolve, reject) => {
        const query = 'SELECT id FROM users WHERE username = ?';
        db.get(query, [username], (err, row) => {
            if (err) {
                reject(err);
            } else {
                resolve(!!row); // Convert to boolean
            }
        });
    });
};

/**
 * Create a new user in the database
 * @param {string} username - User's username
 * @param {string} passwordHash - Hashed password
 * @param {number} rankLevel - User's rank level (1=Commander, 2=Captain, 3=Troops)
 * @param {string} bio - User's bio/description
 * @returns {Promise<Object>} - Created user object
 */
const createUser = (username, passwordHash, rankLevel, bio = '') => {
    return new Promise((resolve, reject) => {
        const query = 'INSERT INTO users (username, password_hash, rank_level, bio) VALUES (?, ?, ?, ?)';
        db.run(query, [username, passwordHash, rankLevel, bio], function(err) {
            if (err) {
                reject(err);
            } else {
                // Return the created user (without password hash)
                resolve({
                    id: this.lastID,
                    username: username,
                    rank_level: rankLevel,
                    bio: bio
                });
            }
        });
    });
};

/**
 * Get user by ID
 * @param {number} userId - User ID
 * @returns {Promise<Object|null>} - User object or null if not found
 */
const getUserById = (userId) => {
    return new Promise((resolve, reject) => {
        const query = 'SELECT id, username, rank_level, bio FROM users WHERE id = ?';
        db.get(query, [userId], (err, row) => {
            if (err) {
                reject(err);
            } else {
                resolve(row || null);
            }
        });
    });
};

/**
 * Get permissible chat users based on rank hierarchy
 * @param {number} userId - Current user's ID
 * @param {number} userRankLevel - Current user's rank level
 * @returns {Promise<Array>} - Array of users the current user can chat with
 */
const getPermissibleChatUsers = (userId, userRankLevel) => {
    return new Promise((resolve, reject) => {
        let query;
        let params;

        // NEW Hierarchical chat access logic based on rank_level:
        // Commander (1): Can communicate with anyone (1, 2, 3)
        // Captain (2): Can communicate with anyone (1, 2, 3)
        // Troops (3): Can only communicate with Troops (3) and Captains (2)
        
        if (userRankLevel === 1) {
            // Commander: Can chat with everyone
            query = 'SELECT id, username, rank_level, bio FROM users WHERE id != ?';
            params = [userId];
        } else if (userRankLevel === 2) {
            // Captain: Can chat with everyone
            query = 'SELECT id, username, rank_level, bio FROM users WHERE id != ?';
            params = [userId];
        } else if (userRankLevel === 3) {
            // Troops: Can only chat with Troops (3) and Captains (2)
            query = 'SELECT id, username, rank_level, bio FROM users WHERE rank_level IN (2, 3) AND id != ?';
            params = [userId];
        } else {
            // Invalid rank level
            reject(new Error('Invalid rank level'));
            return;
        }

        db.all(query, params, (err, rows) => {
            if (err) {
                reject(err);
            } else {
                resolve(rows || []);
            }
        });
    });
};

/**
 * Close database connection
 */
const closeDatabase = () => {
    return new Promise((resolve, reject) => {
        db.close((err) => {
            if (err) {
                reject(err);
            } else {
                console.log('Database connection closed');
                resolve();
            }
        });
    });
};

module.exports = {
    db,
    initializeDatabase,
    userExists,
    createUser,
    getUserById,
    getPermissibleChatUsers,
    closeDatabase
};