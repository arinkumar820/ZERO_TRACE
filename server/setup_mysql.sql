-- MySQL Database Setup for Bisto Chat
-- Run this script in MySQL to create the database

-- Create database
CREATE DATABASE IF NOT EXISTS message_database CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Use the database
USE message_database;

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    uid VARCHAR(255) PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    display_name VARCHAR(255),
    status VARCHAR(50) DEFAULT 'offline',
    last_seen DATETIME DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_status (status)
);

-- Create messages table
CREATE TABLE IF NOT EXISTS messages (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sender_uid VARCHAR(255) NOT NULL,
    sender_email VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    message_type VARCHAR(50) DEFAULT 'text',
    INDEX idx_timestamp (timestamp),
    INDEX idx_sender (sender_uid),
    FOREIGN KEY (sender_uid) REFERENCES users(uid) ON DELETE CASCADE
);

-- Create contacts table
CREATE TABLE IF NOT EXISTS contacts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_uid VARCHAR(255) NOT NULL,
    contact_uid VARCHAR(255) NOT NULL,
    contact_name VARCHAR(255),
    added_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_contact (user_uid, contact_uid),
    FOREIGN KEY (user_uid) REFERENCES users(uid) ON DELETE CASCADE,
    FOREIGN KEY (contact_uid) REFERENCES users(uid) ON DELETE CASCADE
);

-- Create sample data for testing (optional)
-- INSERT INTO users (uid, email, display_name) VALUES 
-- ('user1', 'test1@example.com', 'Test User 1'),
-- ('user2', 'test2@example.com', 'Test User 2');

-- Show created tables
SHOW TABLES;

-- Display table structures
DESCRIBE users;
DESCRIBE messages;
DESCRIBE contacts;