/**
 * SECURE MESSAGING BACKEND
 * 
 * A complete backend system for a secure messaging app with hierarchical chat access control.
 * 
 * Features:
 * 1. User registration with rank assignment (Troop, Captain, Commander)
 * 2. Hierarchical chat access based on user ranks
 * 
 * Database Schema:
 * - users table with id, username, password_hash, and rank_level columns
 * - rank_level: 1 (Troop), 2 (Captain), 3 (Commander)
 * 
 * Chat Rules:
 * - Troop (1): Can chat with Troops (1) and Captains (2)
 * - Captain (2): Can chat with Troops (1), Captains (2), and Commanders (3)
 * - Commander (3): Can chat with everyone
 */

const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const { initializeDatabase } = require('./database');
const usersRouter = require('./routes/users');

// Create Express application
const app = express();
const PORT = process.env.PORT || 3000;

// Security middleware
app.use(helmet());

// CORS configuration - adjust as needed for your frontend
app.use(cors({
    origin: process.env.FRONTEND_URL || 'http://localhost:3001',
    credentials: true
}));

// Body parsing middleware
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true }));

// Request logging middleware
app.use((req, res, next) => {
    console.log(`${new Date().toISOString()} - ${req.method} ${req.path}`);
    next();
});

// Health check endpoint
app.get('/health', (req, res) => {
    res.json({
        status: 'ok',
        timestamp: new Date().toISOString(),
        service: 'secure-messaging-backend'
    });
});

// API documentation endpoint
app.get('/', (req, res) => {
    res.json({
        service: 'Secure Messaging Backend',
        version: '1.0.0',
        description: 'Backend system for hierarchical chat access control',
        endpoints: {
            'POST /api/users/register': {
                description: 'Register a new user with rank',
                body: {
                    username: 'string (min 3 chars)',
                    password: 'string (min 6 chars)',
                    rank_level: 'integer (1=Troop, 2=Captain, 3=Commander)'
                },
                example: {
                    username: 'trooper_007',
                    password: 'password123',
                    rank_level: 1
                }
            },
            'GET /api/users/:userId/permissible-chats': {
                description: 'Get list of users this user can chat with based on rank hierarchy',
                parameters: {
                    userId: 'integer (user ID from registration)'
                },
                example: '/api/users/1/permissible-chats'
            },
            'GET /api/users/all': {
                description: 'Get all registered users (for testing)'
            },
            'GET /health': {
                description: 'Health check endpoint'
            }
        },
        rank_hierarchy: {
            'Troop (1)': 'Can chat with Troops and Captains',
            'Captain (2)': 'Can chat with Troops, Captains, and Commanders',
            'Commander (3)': 'Can chat with everyone'
        }
    });
});

// Mount user routes
// These routes implement the core functionality:
// - /api/users/register: Stores user with rank_level
// - /api/users/:userId/permissible-chats: Uses stored rank_level for chat access
app.use('/api/users', usersRouter);

// 404 handler
app.use('*', (req, res) => {
    res.status(404).json({
        error: 'Endpoint not found',
        message: `${req.method} ${req.originalUrl} is not a valid endpoint`,
        available_endpoints: [
            'POST /api/users/register',
            'GET /api/users/:userId/permissible-chats',
            'GET /api/users/all',
            'GET /health',
            'GET /'
        ]
    });
});

// Global error handler
app.use((err, req, res, next) => {
    console.error('Unhandled error:', err);
    res.status(500).json({
        error: 'Internal server error',
        message: 'An unexpected error occurred'
    });
});

// Initialize database and start server
async function startServer() {
    try {
        console.log('Initializing database...');
        await initializeDatabase();
        console.log('Database initialized successfully');
        
        app.listen(PORT, () => {
            console.log('\n=================================');
            console.log('🚀 SECURE MESSAGING BACKEND');
            console.log('=================================');
            console.log(`📡 Server running on port ${PORT}`);
            console.log(`🔗 Local URL: http://localhost:${PORT}`);
            console.log(`📖 API Docs: http://localhost:${PORT}`);
            console.log(`❤️  Health Check: http://localhost:${PORT}/health`);
            console.log('=================================');
            console.log('\n📋 Available Endpoints:');
            console.log('  POST /api/users/register');
            console.log('  GET  /api/users/:userId/permissible-chats');
            console.log('  GET  /api/users/all');
            console.log('\n🎖️  Rank Hierarchy:');
            console.log('  1 = Troop     → Can chat with Troops & Captains');
            console.log('  2 = Captain   → Can chat with Troops, Captains & Commanders');
            console.log('  3 = Commander → Can chat with everyone');
            console.log('\n⚡ Ready to accept connections!\n');
        });
    } catch (error) {
        console.error('Failed to start server:', error);
        process.exit(1);
    }
}

// Handle graceful shutdown
process.on('SIGTERM', () => {
    console.log('\n🛑 SIGTERM received. Shutting down gracefully...');
    process.exit(0);
});

process.on('SIGINT', () => {
    console.log('\n🛑 SIGINT received. Shutting down gracefully...');
    process.exit(0);
});

// Start the server
startServer();

module.exports = app;
