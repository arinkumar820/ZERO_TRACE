-- Enhanced Bisto Chat Database Schema
-- Supports named group chats (Team Alpha, Team Tiger, etc.) and P2P conversations
-- Each chat has its own message context with proper separation

USE message_database;

-- 1. Chat Rooms Table (Groups and P2P conversations)
CREATE TABLE IF NOT EXISTS chat_rooms (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    type ENUM('group', 'personal') DEFAULT 'group',
    avatar_url TEXT,
    created_by VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    INDEX idx_type (type),
    INDEX idx_created_by (created_by),
    INDEX idx_active (is_active)
);

-- 2. Enhanced Messages Table (with room association)
CREATE TABLE IF NOT EXISTS messages (
    id INT AUTO_INCREMENT PRIMARY KEY,
    chat_room_id VARCHAR(255) NOT NULL,
    sender_uid VARCHAR(255) NOT NULL,
    sender_email VARCHAR(255) NOT NULL,
    sender_name VARCHAR(255),
    message TEXT NOT NULL,
    message_type VARCHAR(50) DEFAULT 'text',
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_edited BOOLEAN DEFAULT FALSE,
    edited_at DATETIME NULL,
    reply_to_message_id INT NULL,
    INDEX idx_timestamp (timestamp),
    INDEX idx_sender (sender_uid),
    INDEX idx_room (chat_room_id),
    INDEX idx_room_time (chat_room_id, timestamp),
    FOREIGN KEY (chat_room_id) REFERENCES chat_rooms(id) ON DELETE CASCADE,
    FOREIGN KEY (reply_to_message_id) REFERENCES messages(id) ON DELETE SET NULL
);

-- 3. Room Participants Table
CREATE TABLE IF NOT EXISTS room_participants (
    id INT AUTO_INCREMENT PRIMARY KEY,
    room_id VARCHAR(255) NOT NULL,
    user_uid VARCHAR(255) NOT NULL,
    user_name VARCHAR(255),
    user_email VARCHAR(255),
    joined_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_admin BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    last_read_message_id INT NULL,
    UNIQUE KEY unique_participant (room_id, user_uid),
    INDEX idx_room (room_id),
    INDEX idx_user (user_uid),
    FOREIGN KEY (room_id) REFERENCES chat_rooms(id) ON DELETE CASCADE
);

-- 4. Insert Sample Named Group Chats
INSERT IGNORE INTO chat_rooms (id, name, description, type, avatar_url, created_by) VALUES
('team_alpha', 'Team Alpha 🚀', 'Elite development team - Mission critical projects', 'group', 'https://via.placeholder.com/150/FF5722/FFFFFF?text=TA', 'system'),
('team_tiger', 'Team Tiger 🐅', 'Fierce marketing warriors - Conquering new markets', 'group', 'https://via.placeholder.com/150/FF9800/FFFFFF?text=TT', 'system'),
('team_phoenix', 'Team Phoenix 🔥', 'Rising from challenges - Innovation specialists', 'group', 'https://via.placeholder.com/150/F44336/FFFFFF?text=TP', 'system'),
('team_storm', 'Team Storm ⚡', 'Fast response team - Emergency problem solvers', 'group', 'https://via.placeholder.com/150/3F51B5/FFFFFF?text=TS', 'system'),
('team_shadow', 'Team Shadow 🥷', 'Stealth operations - Security and privacy experts', 'group', 'https://via.placeholder.com/150/424242/FFFFFF?text=SH', 'system'),
('general_chat', 'General Chat 💬', 'Open discussions for everyone', 'group', 'https://via.placeholder.com/150/4CAF50/FFFFFF?text=GC', 'system'),
('tech_talk', 'Tech Talk 💻', 'Technology discussions and programming tips', 'group', 'https://via.placeholder.com/150/2196F3/FFFFFF?text=TT', 'system'),
('random_fun', 'Random Fun 🎲', 'Casual conversations and entertainment', 'group', 'https://via.placeholder.com/150/9C27B0/FFFFFF?text=RF', 'system');

-- 5. Insert Sample Messages for Different Teams
INSERT IGNORE INTO messages (chat_room_id, sender_uid, sender_email, sender_name, message, message_type) VALUES
-- Team Alpha Messages
('team_alpha', 'user1', 'john@alpha.com', 'John Alpha', 'Team Alpha ready for deployment! 🚀', 'text'),
('team_alpha', 'user2', 'sara@alpha.com', 'Sara Smith', 'All systems green. Let''s dominate this project!', 'text'),
('team_alpha', 'user1', 'john@alpha.com', 'John Alpha', 'Alpha team never fails. Mission accomplished! 💪', 'text'),

-- Team Tiger Messages  
('team_tiger', 'user3', 'mike@tiger.com', 'Mike Tiger', 'Tigers are on the hunt! New campaign launching soon 🐅', 'text'),
('team_tiger', 'user4', 'lisa@tiger.com', 'Lisa Wong', 'Market research shows 95% success rate. We''re unstoppable!', 'text'),
('team_tiger', 'user3', 'mike@tiger.com', 'Mike Tiger', 'Tiger team leads the pack in Q4 results! 📈', 'text'),

-- Team Phoenix Messages
('team_phoenix', 'user5', 'alex@phoenix.com', 'Alex Phoenix', 'Rising from the ashes with innovative solutions! 🔥', 'text'),
('team_phoenix', 'user6', 'emma@phoenix.com', 'Emma Chen', 'Phoenix team turns challenges into opportunities!', 'text'),

-- General Chat Messages
('general_chat', 'user7', 'david@company.com', 'David Johnson', 'Welcome to our enhanced chat system! 👋', 'text'),
('general_chat', 'user8', 'anna@company.com', 'Anna Williams', 'Love the new team-based organization!', 'text');

-- 6. Add users to their respective teams
INSERT IGNORE INTO room_participants (room_id, user_uid, user_name, user_email, is_admin) VALUES
-- Team Alpha Members
('team_alpha', 'user1', 'John Alpha', 'john@alpha.com', TRUE),
('team_alpha', 'user2', 'Sara Smith', 'sara@alpha.com', FALSE),

-- Team Tiger Members  
('team_tiger', 'user3', 'Mike Tiger', 'mike@tiger.com', TRUE),
('team_tiger', 'user4', 'Lisa Wong', 'lisa@tiger.com', FALSE),

-- Team Phoenix Members
('team_phoenix', 'user5', 'Alex Phoenix', 'alex@phoenix.com', TRUE),
('team_phoenix', 'user6', 'Emma Chen', 'emma@phoenix.com', FALSE),

-- General participants
('general_chat', 'user7', 'David Johnson', 'david@company.com', FALSE),
('general_chat', 'user8', 'Anna Williams', 'anna@company.com', FALSE);

-- 7. Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_messages_room_timestamp ON messages(chat_room_id, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_room_participants_active ON room_participants(room_id, is_active);

-- 8. Show the new structure
SELECT 'Chat Rooms Created:' as Info;
SELECT id, name, type, description FROM chat_rooms ORDER BY type, name;

SELECT '\nSample Messages by Team:' as Info;
SELECT 
    cr.name as Team,
    m.sender_name as Sender,
    m.message,
    DATE_FORMAT(m.timestamp, '%H:%i') as Time
FROM messages m
JOIN chat_rooms cr ON m.chat_room_id = cr.id
ORDER BY cr.name, m.timestamp DESC
LIMIT 20;