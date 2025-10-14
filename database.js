const sqlite3 = require('sqlite3').verbose();
const path = require('path');

// Database file path
const DB_PATH = path.join(__dirname, 'secure_messaging.db');

/**
 * Initialize database connection and create tables
 * @returns {Promise<sqlite3.Database>} Database instance
 */
function initializeDatabase() {
    return new Promise((resolve, reject) => {
        const db = new sqlite3.Database(DB_PATH, (err) => {
            if (err) {
                console.error('Error opening database:', err.message);
                reject(err);
                return;
            }
            console.log('Connected to SQLite database');
            
            // Create users table if it doesn't exist
            const createTableQuery = `
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username VARCHAR(255) UNIQUE NOT NULL,
                    password_hash VARCHAR(255) NOT NULL,
                    rank_level INTEGER NOT NULL CHECK (rank_level IN (1, 2, 3)),
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            `;
            
            db.run(createTableQuery, (err) => {
                if (err) {
                    console.error('Error creating users table:', err.message);
                    reject(err);
                    return;
                }
                console.log('Users table ready');
                resolve(db);
            });
        });
    });
}

/**
 * Get database connection
 * @returns {sqlite3.Database} Database instance
 */
function getDatabase() {
    return new sqlite3.Database(DB_PATH, (err) => {
        if (err) {
            console.error('Error connecting to database:', err.message);
            throw err;
        }
    });
}

/**
 * Close database connection
 * @param {sqlite3.Database} db - Database instance to close
 */
function closeDatabase(db) {
    return new Promise((resolve, reject) => {
        db.close((err) => {
            if (err) {
                console.error('Error closing database:', err.message);
                reject(err);
                return;
            }
            console.log('Database connection closed');
            resolve();
        });
    });
}

/**
 * Execute a query with parameters
 * @param {sqlite3.Database} db - Database instance
 * @param {string} query - SQL query
 * @param {Array} params - Query parameters
 * @returns {Promise} Query result
 */
function runQuery(db, query, params = []) {
    return new Promise((resolve, reject) => {
        db.run(query, params, function(err) {
            if (err) {
                reject(err);
                return;
            }
            resolve({ id: this.lastID, changes: this.changes });
        });
    });
}

/**
 * Get a single row from query
 * @param {sqlite3.Database} db - Database instance
 * @param {string} query - SQL query
 * @param {Array} params - Query parameters
 * @returns {Promise} Query result
 */
function getRow(db, query, params = []) {
    return new Promise((resolve, reject) => {
        db.get(query, params, (err, row) => {
            if (err) {
                reject(err);
                return;
            }
            resolve(row);
        });
    });
}

/**
 * Get all rows from query
 * @param {sqlite3.Database} db - Database instance
 * @param {string} query - SQL query
 * @param {Array} params - Query parameters
 * @returns {Promise} Query result
 */
function getAllRows(db, query, params = []) {
    return new Promise((resolve, reject) => {
        db.all(query, params, (err, rows) => {
            if (err) {
                reject(err);
                return;
            }
            resolve(rows);
        });
    });
}

module.exports = {
    initializeDatabase,
    getDatabase,
    closeDatabase,
    runQuery,
    getRow,
    getAllRows
};