/**
 * Firebase Realtime Database Migration Script
 * Adds 'status' field to existing users for approval system
 * 
 * Instructions:
 * 1. Open Firebase Console
 * 2. Go to Realtime Database
 * 3. Click on "Rules" tab
 * 4. Temporarily set rules to allow write access
 * 5. Run this script in browser console
 * 6. Restore original security rules
 */

// Firebase Database Migration Script
const migrationScript = {
    
    // Your Firebase project config
    firebaseConfig: {
        databaseURL: "https://bisto-chat-3b33a-default-rtdb.firebaseio.com/"
    },
    
    /**
     * Add status field to all existing users
     * Default status is 'approved' for existing users
     */
    async migrateExistingUsers() {
        try {
            console.log('🔄 Starting user migration...');
            
            // Get reference to users node
            const usersRef = firebase.database().ref('users');
            
            // Get all existing users
            const snapshot = await usersRef.once('value');
            const users = snapshot.val();
            
            if (!users) {
                console.log('❌ No users found in database');
                return;
            }
            
            const userIds = Object.keys(users);
            console.log(`📊 Found ${userIds.length} users to migrate`);
            
            // Update each user
            const updates = {};
            let migratedCount = 0;
            let skippedCount = 0;
            
            for (const uid of userIds) {
                const user = users[uid];
                
                // Check if user already has status field
                if (!user.status) {
                    // Add status field - default to 'approved' for existing users
                    updates[`users/${uid}/status`] = 'approved';
                    
                    // Also ensure other required fields exist
                    if (!user.registrationTimestamp) {
                        updates[`users/${uid}/registrationTimestamp`] = Date.now();
                    }
                    if (!user.lastLoginTimestamp) {
                        updates[`users/${uid}/lastLoginTimestamp`] = 0;
                    }
                    if (!user.profileImageUrl) {
                        updates[`users/${uid}/profileImageUrl`] = '';
                    }
                    if (!user.phoneNumber) {
                        updates[`users/${uid}/phoneNumber`] = '';
                    }
                    
                    migratedCount++;
                    console.log(`✅ Prepared migration for user: ${user.email || uid}`);
                } else {
                    skippedCount++;
                    console.log(`⏭️ User already has status: ${user.email || uid} (${user.status})`);
                }
            }
            
            if (Object.keys(updates).length === 0) {
                console.log('🎉 All users already migrated!');
                return;
            }
            
            // Apply all updates in single operation
            console.log(`🚀 Applying updates for ${migratedCount} users...`);
            await firebase.database().ref().update(updates);
            
            console.log('✅ Migration completed successfully!');
            console.log(`📈 Statistics:`);
            console.log(`   - Users migrated: ${migratedCount}`);
            console.log(`   - Users skipped: ${skippedCount}`);
            console.log(`   - Total users: ${userIds.length}`);
            
        } catch (error) {
            console.error('❌ Migration failed:', error);
        }
    },
    
    /**
     * Verify migration results
     */
    async verifyMigration() {
        try {
            console.log('🔍 Verifying migration...');
            
            const usersRef = firebase.database().ref('users');
            const snapshot = await usersRef.once('value');
            const users = snapshot.val();
            
            if (!users) {
                console.log('❌ No users found');
                return;
            }
            
            const userIds = Object.keys(users);
            let validUsers = 0;
            let invalidUsers = 0;
            
            for (const uid of userIds) {
                const user = users[uid];
                const hasRequiredFields = user.status && user.email && user.displayName;
                
                if (hasRequiredFields) {
                    validUsers++;
                    console.log(`✅ Valid user: ${user.email} (status: ${user.status})`);
                } else {
                    invalidUsers++;
                    console.log(`❌ Invalid user: ${user.email || uid} (missing fields)`);
                }
            }
            
            console.log(`📊 Verification complete:`);
            console.log(`   - Valid users: ${validUsers}`);
            console.log(`   - Invalid users: ${invalidUsers}`);
            
        } catch (error) {
            console.error('❌ Verification failed:', error);
        }
    },
    
    /**
     * Show current database structure
     */
    async showCurrentStructure() {
        try {
            const usersRef = firebase.database().ref('users');
            const snapshot = await usersRef.once('value');
            const users = snapshot.val();
            
            console.log('📋 Current database structure:');
            console.log(JSON.stringify(users, null, 2));
            
        } catch (error) {
            console.error('❌ Failed to show structure:', error);
        }
    }
};

// Auto-run migration if script is loaded
console.log('🔧 Firebase Database Migration Script loaded');
console.log('📋 Available commands:');
console.log('   - migrationScript.migrateExistingUsers()');
console.log('   - migrationScript.verifyMigration()');
console.log('   - migrationScript.showCurrentStructure()');
console.log('');
console.log('🚀 To start migration, run: migrationScript.migrateExistingUsers()');