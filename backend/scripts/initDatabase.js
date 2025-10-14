const { initializeDatabase, createUser, closeDatabase } = require('../database');
const bcrypt = require('bcrypt');

/**
 * Database Initialization Script
 * 
 * This script:
 * 1. Creates the database schema (users table)
 * 2. Optionally populates with sample users for testing
 * 
 * Run with: npm run init-db
 */

async function initializeDatabaseWithSamples() {
    console.log('🚀 Starting database initialization...\n');
    
    try {
        // Initialize the database schema
        await initializeDatabase();
        console.log('✅ Database schema created successfully\n');
        
        // Ask if user wants to add sample data
        const shouldAddSamples = process.argv.includes('--with-samples');
        
        if (shouldAddSamples) {
            console.log('👥 Adding sample users for testing...\n');
            await addSampleUsers();
            console.log('✅ Sample users added successfully\n');
        } else {
            console.log('ℹ️  To add sample users, run: npm run init-db -- --with-samples\n');
        }
        
        console.log('🎉 Database initialization complete!');
        console.log('\n📋 Next steps:');
        console.log('1. Run: npm install');
        console.log('2. Start server: npm start');
        console.log('3. API will be available at: http://localhost:3000\n');
        
    } catch (error) {
        console.error('❌ Database initialization failed:', error);
        process.exit(1);
    } finally {
        await closeDatabase();
    }
}

/**
 * Add sample users for testing the hierarchical chat system
 */
async function addSampleUsers() {
    const saltRounds = 12;
    
    const sampleUsers = [
        // Troops (rank_level: 1)
        { username: 'trooper_007', password: 'password123', rank_level: 1 },
        { username: 'soldier_alpha', password: 'password123', rank_level: 1 },
        { username: 'private_beta', password: 'password123', rank_level: 1 },
        
        // Captains (rank_level: 2)
        { username: 'captain_smith', password: 'password123', rank_level: 2 },
        { username: 'captain_jones', password: 'password123', rank_level: 2 },
        
        // Commanders (rank_level: 3)
        { username: 'commander_rex', password: 'password123', rank_level: 3 },
        { username: 'general_nova', password: 'password123', rank_level: 3 }
    ];
    
    for (const user of sampleUsers) {
        try {
            const passwordHash = await bcrypt.hash(user.password, saltRounds);
            const createdUser = await createUser(user.username, passwordHash, user.rank_level);
            
            const rankName = getRankName(user.rank_level);
            console.log(`   ✅ Created ${rankName}: ${user.username} (ID: ${createdUser.id})`);
            
        } catch (error) {
            if (error.code === 'SQLITE_CONSTRAINT') {
                console.log(`   ⚠️  User ${user.username} already exists, skipping...`);
            } else {
                console.error(`   ❌ Failed to create user ${user.username}:`, error.message);
            }
        }
    }
    
    console.log('\n🏆 Hierarchical Access Summary:');
    console.log('   • Troops (1) can chat with: Troops + Captains');
    console.log('   • Captains (2) can chat with: Troops + Captains + Commanders');
    console.log('   • Commanders (3) can chat with: Everyone');
}

/**
 * Helper function to get rank name
 */
function getRankName(rankLevel) {
    switch (rankLevel) {
        case 1: return 'Troop';
        case 2: return 'Captain'; 
        case 3: return 'Commander';
        default: return 'Unknown';
    }
}

// Run the initialization
if (require.main === module) {
    initializeDatabaseWithSamples();
}

module.exports = {
    initializeDatabaseWithSamples,
    addSampleUsers
};