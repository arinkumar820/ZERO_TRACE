const express = require('express');
const bcrypt = require('bcrypt');
const { 
    userExists, 
    createUser, 
    getUserById, 
    getPermissibleChatUsers 
} = require('../database');

const router = express.Router();

/**
 * PART 1: User Registration Endpoint
 * 
 * POST /api/users/register
 * 
 * This endpoint handles user registration with rank assignment and bio.
 * The rank_level value comes from the frontend UI selection:
 * - "Commander" selection -> rank_level: 1 (highest authority)
 * - "Captain" selection -> rank_level: 2 (middle authority)
 * - "Troops" selection -> rank_level: 3 (basic level)
 */
router.post('/register', async (req, res) => {
    try {
        const { username, password, rank_level, bio } = req.body;

        // Input validation
        if (!username || !password || rank_level === undefined) {
            return res.status(400).json({
                error: 'Username, password, and rank_level are required'
            });
        }

        // Validate username is not empty/whitespace
        if (typeof username !== 'string' || username.trim().length === 0) {
            return res.status(400).json({
                error: 'Username cannot be empty'
            });
        }

        // Validate password is not empty/whitespace
        if (typeof password !== 'string' || password.trim().length === 0) {
            return res.status(400).json({
                error: 'Password cannot be empty'
            });
        }

        // Validate rank_level is valid (1, 2, or 3)
        if (![1, 2, 3].includes(rank_level)) {
            return res.status(400).json({
                error: 'rank_level must be 1 (Commander), 2 (Captain), or 3 (Troops)'
            });
        }

        // Check if username already exists
        const exists = await userExists(username.trim());
        if (exists) {
            return res.status(409).json({
                error: 'Username already exists'
            });
        }

        // Hash the password using bcrypt
        const saltRounds = 12; // Strong salt rounds for security
        const passwordHash = await bcrypt.hash(password, saltRounds);

        // Create the user in the database with bio
        // The rank_level stored here will be used by the chat access endpoint
        const userBio = bio || `${getRankName(rank_level)} in the messaging system`;
        const newUser = await createUser(username.trim(), passwordHash, rank_level, userBio);

        // Return success response with user info (excluding password hash)
        res.status(201).json({
            message: 'User registered successfully',
            user: {
                id: newUser.id,
                username: newUser.username,
                rank_level: newUser.rank_level,
                rank_name: getRankName(newUser.rank_level),
                bio: newUser.bio
            }
        });

    } catch (error) {
        console.error('Registration error:', error);
        res.status(500).json({
            error: 'Internal server error during registration'
        });
    }
});

/**
 * PART 2: Hierarchical Chat Access Endpoint
 * 
 * GET /api/users/:userId/permissible-chats
 * 
 * This endpoint uses the rank_level stored during registration to determine
 * who the user is allowed to message based on the NEW hierarchical rules:
 * 
 * - Commander (rank_level: 1): Can communicate with anyone (1, 2, 3)
 * - Captain (rank_level: 2): Can communicate with anyone (1, 2, 3)
 * - Troops (rank_level: 3): Can only communicate with Troops (3) and Captains (2)
 */
router.get('/:userId/permissible-chats', async (req, res) => {
    try {
        const userId = parseInt(req.params.userId);

        // Validate userId parameter
        if (isNaN(userId) || userId <= 0) {
            return res.status(400).json({
                error: 'Invalid user ID'
            });
        }

        // Get the user's information including their rank_level
        // This rank_level was stored when the user registered
        const user = await getUserById(userId);
        if (!user) {
            return res.status(404).json({
                error: 'User not found'
            });
        }

        // Use the stored rank_level to determine permissible chat users
        // The hierarchical logic is implemented in the database module
        const permissibleUsers = await getPermissibleChatUsers(userId, user.rank_level);

        // Add rank names and bio for better frontend display
        const usersWithRankNames = permissibleUsers.map(chatUser => ({
            id: chatUser.id,
            username: chatUser.username,
            rank_level: chatUser.rank_level,
            rank_name: getRankName(chatUser.rank_level),
            bio: chatUser.bio || 'No bio provided'
        }));

        // Return the list of users this user can chat with
        res.json({
            user: {
                id: user.id,
                username: user.username,
                rank_level: user.rank_level,
                rank_name: getRankName(user.rank_level),
                bio: user.bio || 'No bio provided'
            },
            permissible_chats: usersWithRankNames,
            total_permissible_users: usersWithRankNames.length
        });

    } catch (error) {
        console.error('Permissible chats error:', error);
        res.status(500).json({
            error: 'Internal server error retrieving permissible chats'
        });
    }
});

/**
 * Helper function to convert rank_level to human-readable rank name
 * @param {number} rankLevel - Numeric rank level
 * @returns {string} - Human-readable rank name
 */
function getRankName(rankLevel) {
    switch (rankLevel) {
        case 1:
            return 'Commander';
        case 2:
            return 'Captain';
        case 3:
            return 'Troops';
        default:
            return 'Unknown';
    }
}

/**
 * Additional endpoint: Get user information by ID
 * GET /api/users/:userId
 * 
 * Utility endpoint to get user information
 */
router.get('/:userId', async (req, res) => {
    try {
        const userId = parseInt(req.params.userId);

        if (isNaN(userId) || userId <= 0) {
            return res.status(400).json({
                error: 'Invalid user ID'
            });
        }

        const user = await getUserById(userId);
        if (!user) {
            return res.status(404).json({
                error: 'User not found'
            });
        }

        res.json({
            id: user.id,
            username: user.username,
            rank_level: user.rank_level,
            rank_name: getRankName(user.rank_level),
            bio: user.bio || 'No bio provided'
        });

    } catch (error) {
        console.error('Get user error:', error);
        res.status(500).json({
            error: 'Internal server error retrieving user'
        });
    }
});

module.exports = router;