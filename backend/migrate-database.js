const sqlite3 = require('sqlite3').verbose();
const path = require('path');

// Database migration script to add bio column and update existing users

const dbPath = path.join(__dirname, 'message_database.db');

console.log('🔄 MIGRATING DATABASE TO NEW SCHEMA');
console.log('====================================\n');

const db = new sqlite3.Database(dbPath, (err) => {
    if (err) {
        console.error('❌ Error opening database:', err.message);
        process.exit(1);
    }
    console.log('✅ Connected to database');
});

async function migrateDatabase() {
    return new Promise((resolve, reject) => {
        // Check if bio column already exists
        db.all("PRAGMA table_info(users)", (err, rows) => {
            if (err) {
                reject(err);
                return;
            }
            
            const hasBioColumn = rows.some(row => row.name === 'bio');
            
            if (hasBioColumn) {
                console.log('✅ Bio column already exists');
                resolve();
                return;
            }
            
            console.log('🔧 Adding bio column to users table...');
            
            // Add bio column
            db.run("ALTER TABLE users ADD COLUMN bio TEXT DEFAULT ''", (err) => {
                if (err) {
                    reject(err);
                    return;
                }
                
                console.log('✅ Bio column added successfully');
                
                // Update existing users with default bio based on rank
                const updateQueries = [
                    "UPDATE users SET bio = 'Commander in the messaging system' WHERE rank_level = 1",
                    "UPDATE users SET bio = 'Captain in the messaging system' WHERE rank_level = 2", 
                    "UPDATE users SET bio = 'Troops member in the messaging system' WHERE rank_level = 3"
                ];
                
                let completed = 0;
                updateQueries.forEach((query, index) => {
                    db.run(query, (err) => {
                        if (err) {
                            console.error(`❌ Error updating rank ${index + 1}:`, err.message);
                        } else {
                            console.log(`✅ Updated bio for rank level ${index + 1}`);
                        }
                        
                        completed++;
                        if (completed === updateQueries.length) {
                            resolve();
                        }
                    });
                });
            });
        });
    });
}

async function showCurrentUsers() {
    return new Promise((resolve, reject) => {
        console.log('\n📊 CURRENT USERS WITH NEW SCHEMA:');
        console.log('================================');
        
        db.all("SELECT id, username, rank_level, bio FROM users ORDER BY rank_level, id", (err, rows) => {
            if (err) {
                reject(err);
                return;
            }
            
            const getRankName = (level) => {
                switch(level) {
                    case 1: return 'Commander';
                    case 2: return 'Captain';
                    case 3: return 'Troops';
                    default: return 'Unknown';
                }
            };
            
            rows.forEach(user => {
                console.log(`ID: ${user.id} | ${user.username} | ${getRankName(user.rank_level)} (Level ${user.rank_level}) | Bio: ${user.bio}`);
            });
            
            console.log(`\n📈 Total users: ${rows.length}`);
            resolve();
        });
    });
}

async function testNewHierarchy() {
    return new Promise((resolve, reject) => {
        console.log('\n🔍 TESTING NEW HIERARCHY RULES:');
        console.log('===============================');
        
        // Test with a Troops user (rank_level 3) - should only see Troops and Captains
        db.all(`
            SELECT id, username, rank_level, bio 
            FROM users 
            WHERE rank_level IN (2, 3) AND id != 1
        `, (err, rows) => {
            if (err) {
                reject(err);
                return;
            }
            
            console.log('✅ Troops (Level 3) can communicate with:');
            rows.forEach(user => {
                const rankName = user.rank_level === 2 ? 'Captain' : 'Troops';
                console.log(`   • ${user.username} (${rankName})`);
            });
            
            // Test with a Commander user (rank_level 1) - should see everyone
            db.all(`SELECT id, username, rank_level FROM users WHERE id != 1`, (err2, allUsers) => {
                if (err2) {
                    reject(err2);
                    return;
                }
                
                console.log('\n✅ Commander (Level 1) can communicate with:');
                allUsers.forEach(user => {
                    const rankName = user.rank_level === 1 ? 'Commander' : user.rank_level === 2 ? 'Captain' : 'Troops';
                    console.log(`   • ${user.username} (${rankName})`);
                });
                
                resolve();
            });
        });
    });
}

// Run migration
async function runMigration() {
    try {
        await migrateDatabase();
        await showCurrentUsers();
        await testNewHierarchy();
        
        console.log('\n🎉 DATABASE MIGRATION COMPLETE!');
        console.log('\n📋 NEW HIERARCHY RULES:');
        console.log('• Commander (Level 1): Can communicate with ANYONE');
        console.log('• Captain (Level 2): Can communicate with ANYONE');
        console.log('• Troops (Level 3): Can communicate with Troops + Captains ONLY');
        console.log('\n✅ Your system is ready with bio field and new hierarchy!');
        
    } catch (error) {
        console.error('❌ Migration failed:', error);
    } finally {
        db.close((err) => {
            if (err) {
                console.error('❌ Error closing database:', err.message);
            } else {
                console.log('\n🔐 Database connection closed');
            }
        });
    }
}

runMigration();