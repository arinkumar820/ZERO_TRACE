const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const { initializeDatabase, closeDatabase } = require('./database');
const usersRouter = require('./routes/users');

/**
 * Secure Messaging App Backend Server
 * 
 * This server provides:
 * 1. User registration with rank assignment (Troop=1, Captain=2, Commander=3)
 * 2. Hierarchical chat access control based on user ranks
 * 
 * Key Features:
 * - Secure password hashing with bcrypt
 * - SQLite database with users table
 * - Rank-based messaging permissions
 * - CORS and security middleware
 */

const app = express();
const PORT = process.env.PORT || 3000;

// Security middleware
app.use(helmet()); // Sets various HTTP headers for security
app.use(cors({
    origin: process.env.FRONTEND_URL || '*', // Configure for production
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

// API Routes
app.use('/api/users', usersRouter);

// Health check endpoint
app.get('/health', (req, res) => {
    res.json({
        status: 'OK',
        timestamp: new Date().toISOString(),
        service: 'Secure Messaging Backend',
        version: '1.0.0'
    });
});

// Root endpoint with API documentation
app.get('/', (req, res) => {
    res.json({
        message: 'Secure Messaging App Backend API',
        version: '1.0.0',
        endpoints: {
            health: {
                method: 'GET',
                path: '/health',
                description: 'Health check endpoint'
            },
            register: {
                method: 'POST',
                path: '/api/users/register',
                description: 'Register a new user with rank assignment',
                body: {
                    username: 'string (required)',
                    password: 'string (required)', 
                    rank_level: 'number (required: 1=Troop, 2=Captain, 3=Commander)'
                }
            },
            permissibleChats: {
                method: 'GET',
                path: '/api/users/:userId/permissible-chats',
                description: 'Get list of users the specified user can chat with based on rank hierarchy'
            },
            getUserById: {
                method: 'GET',
                path: '/api/users/:userId',
                description: 'Get user information by ID'
            }
        },
        hierarchy_rules: {
            troop: 'Can chat with Troops (1) and Captains (2)',
            captain: 'Can chat with Troops (1), Captains (2), and Commanders (3)',
            commander: 'Can chat with everyone'
        }
    });
});

// Error handling middleware
app.use((err, req, res, next) => {
    console.error('Error:', err);
    
    // Don't leak error details in production
    const isDevelopment = process.env.NODE_ENV !== 'production';
    
    res.status(err.status || 500).json({
        error: isDevelopment ? err.message : 'Internal server error',
        ...(isDevelopment && { stack: err.stack })
    });
});

// Handle 404 for unknown routes
app.use('*', (req, res) => {
    res.status(404).json({
        error: 'Route not found',
        path: req.originalUrl,
        method: req.method
    });
});

/**
 * Initialize database and start server
 */
async function startServer() {
    try {
        // Initialize database schema
        console.log('Initializing database...');
        await initializeDatabase();
        console.log('Database initialized successfully');
        
        // Start the server on all network interfaces (0.0.0.0)
        const server = app.listen(PORT, '0.0.0.0', () => {
            console.log(`\n🚀 Secure Messaging Backend Server running on port ${PORT}`);
            console.log(`📝 Local API Documentation: http://localhost:${PORT}`);
            console.log(`🌐 Network API Documentation: http://YOUR_IP_ADDRESS:${PORT}`);
            console.log(`❤️  Health Check: http://localhost:${PORT}/health`);
            console.log(`\n🔗 To access from other devices, replace YOUR_IP_ADDRESS with your computer's IP`);
            console.log('\n📋 Available Endpoints:');
            console.log(`   POST /api/users/register - User registration with rank assignment`);
            console.log(`   GET  /api/users/:userId/permissible-chats - Get permissible chat users`);
            console.log(`   GET  /api/users/:userId - Get user information`);
            console.log('\n🏆 Rank Hierarchy:');
            console.log('   1 = Troop (can chat with Troops + Captains)');
            console.log('   2 = Captain (can chat with Troops + Captains + Commanders)');
            console.log('   3 = Commander (can chat with everyone)');
            console.log('\n');
        });
        
        // Graceful shutdown handling
        process.on('SIGINT', async () => {
            console.log('\n\n🛑 Received SIGINT. Gracefully shutting down...');
            
            server.close(async () => {
                console.log('🔒 HTTP server closed');
                
                try {
                    await closeDatabase();
                    console.log('🔒 Database connection closed');
                    console.log('✅ Server shutdown complete');
                    process.exit(0);
                } catch (error) {
                    console.error('❌ Error during database shutdown:', error);
                    process.exit(1);
                }
            });
        });
        
    } catch (error) {
        console.error('❌ Failed to start server:', error);
        process.exit(1);
    }
}

// Start the server
startServer();

module.exports = app;
