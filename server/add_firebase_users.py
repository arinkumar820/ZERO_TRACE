#!/usr/bin/env python3
"""
Add Sample Users to Firebase Realtime Database
Run this script to populate Firebase with test users for contact search
"""

import firebase_admin
from firebase_admin import credentials, db
import uuid
import json

# Initialize Firebase Admin SDK
# You'll need to download the service account key from Firebase Console
# and place it in this directory as 'service-account-key.json'

def init_firebase():
    """Initialize Firebase Admin SDK"""
    try:
        # Path to your service account key file
        cred = credentials.Certificate('service-account-key.json')
        
        # Initialize the app with a service account, granting admin privileges
        firebase_admin.initialize_app(cred, {
            'databaseURL': 'https://your-project-id-default-rtdb.firebaseio.com/'  # Replace with your database URL
        })
        print("✅ Firebase initialized successfully")
        return True
    except Exception as e:
        print(f"❌ Firebase initialization failed: {e}")
        print("\n💡 Make sure to:")
        print("1. Download service account key from Firebase Console")
        print("2. Save it as 'service-account-key.json' in this directory")
        print("3. Update the database URL in this script")
        return False

def generate_uid():
    """Generate a Firebase-style UID"""
    return str(uuid.uuid4()).replace('-', '')[:28]

def add_sample_users():
    """Add sample users to Firebase"""
    
    sample_users = [
        {
            'displayName': 'Arin Kumar',
            'email': 'arin@example.com',
            'phoneNumber': '+1-555-0101',
            'bio': 'Hey there! I love coding and technology. Always up for a chat about new frameworks!',
            'status': 'online',
            'profileImageUrl': ''
        },
        {
            'displayName': 'Prachi Sharma',
            'email': 'prachi@example.com',
            'phoneNumber': '+1-555-0102',
            'bio': 'Designer by day, gamer by night. Let\'s create something amazing together!',
            'status': 'online',
            'profileImageUrl': ''
        },
        {
            'displayName': 'John Smith',
            'email': 'john.smith@example.com',
            'phoneNumber': '+1-555-0103',
            'bio': 'Coffee enthusiast and software developer. Building the future, one line of code at a time.',
            'status': 'away',
            'profileImageUrl': ''
        },
        {
            'displayName': 'Sarah Johnson',
            'email': 'sarah.j@example.com',
            'phoneNumber': '+1-555-0104',
            'bio': 'Digital marketing specialist. Love connecting people through great conversations!',
            'status': 'online',
            'profileImageUrl': ''
        },
        {
            'displayName': 'Mike Chen',
            'email': 'mike.chen@example.com',
            'phoneNumber': '+1-555-0105',
            'bio': 'Full-stack developer and tech blogger. Always exploring new technologies.',
            'status': 'offline',
            'profileImageUrl': ''
        },
        {
            'displayName': 'Emily Davis',
            'email': 'emily.davis@example.com',
            'phoneNumber': '+1-555-0106',
            'bio': 'UI/UX designer with a passion for creating beautiful user experiences.',
            'status': 'online',
            'profileImageUrl': ''
        },
        {
            'displayName': 'Alex Rodriguez',
            'email': 'alex.r@example.com',
            'phoneNumber': '+1-555-0107',
            'bio': 'Mobile app developer. iOS and Android expert. Let\'s build the next big app!',
            'status': 'busy',
            'profileImageUrl': ''
        },
        {
            'displayName': 'Lisa Wang',
            'email': 'lisa.wang@example.com',
            'phoneNumber': '+1-555-0108',
            'bio': 'Data scientist and AI researcher. Fascinated by machine learning and neural networks.',
            'status': 'online',
            'profileImageUrl': ''
        },
        {
            'displayName': 'David Thompson',
            'email': 'david.t@example.com',
            'phoneNumber': '+1-555-0109',
            'bio': 'DevOps engineer. Cloud infrastructure and automation specialist.',
            'status': 'away',
            'profileImageUrl': ''
        },
        {
            'displayName': 'Anna Kowalski',
            'email': 'anna.k@example.com',
            'phoneNumber': '+1-555-0110',
            'bio': 'Frontend developer specializing in React and Vue.js. Love creating interactive UIs!',
            'status': 'online',
            'profileImageUrl': ''
        }
    ]
    
    users_ref = db.reference('users')
    added_count = 0
    
    for user_data in sample_users:
        try:
            user_uid = generate_uid()
            user_data['createdAt'] = {'.sv': 'timestamp'}
            user_data['lastSeen'] = {'.sv': 'timestamp'}
            
            users_ref.child(user_uid).set(user_data)
            print(f"✅ Added user: {user_data['displayName']} ({user_data['email']})")
            added_count += 1
            
        except Exception as e:
            print(f"❌ Failed to add user {user_data['displayName']}: {e}")
    
    print(f"\n🎯 Successfully added {added_count} users to Firebase!")
    return added_count

def main():
    """Main function"""
    print("🚀 Firebase Sample Users Inserter")
    print("=" * 50)
    
    if not init_firebase():
        return
    
    try:
        count = add_sample_users()
        
        print("\n" + "=" * 50)
        print("📊 SUMMARY")
        print("=" * 50)
        print(f"👥 Total users added: {count}")
        print("🔍 You can now search for users by:")
        print("   - Display Name: 'Arin', 'Prachi', 'John', etc.")
        print("   - Email: 'arin@example.com', '@example.com', etc.")
        print("\n✅ Firebase setup completed!")
        print("🎯 Your Android app can now search contacts without any servers!")
        
    except Exception as e:
        print(f"❌ Error: {e}")

if __name__ == "__main__":
    main()