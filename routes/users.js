const express = require('express');
const bcrypt = require('bcrypt');
const { getDatabase, runQuery, getRow, getAllRows } = require('../database');

const router = express.Router();

/**
 * PART 1: USER REGISTRATION ENDPOINT
 * 
 * POST /api/users/register
 * 
 * This endpoint accepts username, password, and rank_level from the frontend.
 * The rank_level corresponds to:
 * - 1: Troop
 * - 2: Captain  
 * - 3: Commander
 * 
 * The rank_level stored here will be used by the permissible-chats endpoint
 * to determine chat access permissions based on hierarchy.
 */
router.post('/register', async (req, res) => {
    const db = getDatabase();
    
    try {
        const { username, password, rank_level } = req.body;

        // Input validation
        if (!username || !password || rank_level === undefined) {
            return res.status(400).json({
                error: 'Missing required fields: username, password, and rank_level are required'
            });
        }

        // Validate rank_level is valid (1, 2, or 3)
        if (![1, 2, 3].includes(rank_level)) {
            return res.status(400).json({
                error: 'Invalid rank_level. Must be 1 (Troop), 2 (Captain), or 3 (Commander)'
            });
        }

        // Validate username length and format
        if (username.trim().length < 3) {
            return res.status(400).json({
                error: 'Username must be at least 3 characters long'
            });
        }

        // Validate password strength
        if (password.length < 6) {
            return res.status(400).json({
                error: 'Password must be at least 6 characters long'
            });
        }

        // Check if username already exists
        const existingUser = await getRow(
            db, 
            'SELECT id FROM users WHERE username = ?', 
            [username.trim()]
        );

        if (existingUser) {
            return res.status(409).json({
                error: 'Username already exists'
            });
        }

        // Hash the password using bcrypt
        const saltRounds = 12; // Higher salt rounds for better security
        const password_hash = await bcrypt.hash(password, saltRounds);

        // Insert new user into database
        // The rank_level stored here determines chat permissions in the permissible-chats endpoint
        const result = await runQuery(
            db,
            'INSERT INTO users (username, password_hash, rank_level) VALUES (?, ?, ?)',
            [username.trim(), password_hash, rank_level]
        );

        // Return success response with user info (excluding password hash)
        const newUser = {
            id: result.id,
            username: username.trim(),
            rank_level: rank_level,
            rank_name: getRankName(rank_level)
        };

        res.status(201).json({
            message: 'User registered successfully',
            user: newUser
        });

    } catch (error) {
        console.error('Registration error:', error);
        res.status(500).json({
            error: 'Internal server error during registration'
        });
    } finally {
        db.close();
    }
});

/**
 * PART 2: HIERARCHICAL CHAT ACCESS ENDPOINT
 * 
 * GET /api/users/:userId/permissible-chats
 * 
 * This endpoint uses the rank_level that was stored during registration
 * to determine who the user is allowed to message based on hierarchy:
 * 
 * - Troop (rank_level: 1): Can chat with Troops (1) and Captains (2)
 * - Captain (rank_level: 2): Can chat with Troops (1), Captains (2), and Commanders (3)  
 * - Commander (rank_level: 3): Can chat with everyone (1, 2, 3)
 */
router.get('/:userId/permissible-chats', async (req, res) => {
    const db = getDatabase();
    
    try {
        const { userId } = req.params;

        // Validate userId parameter
        if (!userId || isNaN(parseInt(userId))) {
            return res.status(400).json({
                error: 'Invalid userId parameter'
            });
        }

        const userIdInt = parseInt(userId);

        // Get the current user's rank_level from the database
        // This rank_level was stored during registration and now determines chat access
        const currentUser = await getRow(
            db,
            'SELECT id, username, rank_level FROM users WHERE id = ?',
            [userIdInt]
        );

        if (!currentUser) {
            return res.status(404).json({
                error: 'User not found'
            });
        }

        let permissibleUsers = [];

        // Apply hierarchical chat rules based on the user's stored rank_level
        if (currentUser.rank_level === 1) {
            // Troop: Can chat with other Troops (1) and Captains (2)
            permissibleUsers = await getAllRows(
                db,
                'SELECT id, username, rank_level FROM users WHERE rank_level IN (1, 2) AND id != ?',
                [userIdInt]
            );
        } else if (currentUser.rank_level === 2) {
            // Captain: Can chat with Troops (1), other Captains (2), and Commanders (3)
            permissibleUsers = await getAllRows(
                db,
                'SELECT id, username, rank_level FROM users WHERE rank_level IN (1, 2, 3) AND id != ?',
                [userIdInt]
            );
        } else if (currentUser.rank_level === 3) {
            // Commander: Can chat with everyone (all rank levels)
            permissibleUsers = await getAllRows(
                db,
                'SELECT id, username, rank_level FROM users WHERE id != ?',
                [userIdInt]
            );
        }

        // Add rank names for better frontend display
        const formattedUsers = permissibleUsers.map(user => ({
            id: user.id,
            username: user.username,
            rank_level: user.rank_level,
            rank_name: getRankName(user.rank_level)
        }));

        res.json({
            current_user: {
                id: currentUser.id,
                username: currentUser.username,
                rank_level: currentUser.rank_level,
                rank_name: getRankName(currentUser.rank_level)
            },
            permissible_chats: formattedUsers,
            total_count: formattedUsers.length
        });

    } catch (error) {
        console.error('Permissible chats error:', error);
        res.status(500).json({
            error: 'Internal server error while fetching permissible chats'
        });
    } finally {
        db.close();
    }
});

/**
 * Helper function to convert rank_level to readable rank name
 * @param {number} rank_level - The numeric rank level (1, 2, or 3)
 * @returns {string} The corresponding rank name
 */
function getRankName(rank_level) {
    switch (rank_level) {
        case 1:
            return 'Troop';
        case 2:
            return 'Captain';
        case 3:
            return 'Commander';
        default:
            return 'Unknown';
    }
}

/**
 * Additional endpoint to get all users (for testing purposes)
 * GET /api/users/all
 */
router.get('/all', async (req, res) => {
    const db = getDatabase();
    
    try {
        const users = await getAllRows(
            db,
            'SELECT id, username, rank_level, created_at FROM users ORDER BY created_at DESC'
        );

        const formattedUsers = users.map(user => ({
            id: user.id,
            username: user.username,
            rank_level: user.rank_level,
            rank_name: getRankName(user.rank_level),
            created_at: user.created_at
        }));

        res.json({
            users: formattedUsers,
            total_count: formattedUsers.length
        });

    } catch (error) {
        console.error('Get all users error:', error);
        res.status(500).json({
            error: 'Internal server error while fetching users'
        });
    } finally {
        db.close();
    }
});

module.exports = router;