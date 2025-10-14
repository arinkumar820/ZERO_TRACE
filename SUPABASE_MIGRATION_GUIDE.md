# Firebase to Supabase Migration Guide

## ✅ Completed Steps

1. **Removed Firebase Configuration**
   - Deleted `google-services.json`
   - Removed Firebase plugin from `build.gradle` files
   - Commented out Firebase dependencies

2. **Added Supabase Dependencies**
   - Added Supabase Kotlin client libraries
   - Added required Ktor and Coroutines dependencies

3. **Created Supabase Configuration Files**
   - `SupabaseConfig.java` - Configuration constants
   - `SupabaseHelper.java` - Helper class template

## 🔄 Next Steps: Setting Up Your Supabase Project

### 1. Create Supabase Project
1. Go to [supabase.com](https://supabase.com)
2. Create a new project
3. Note your:
   - Project URL (e.g., `https://yourproject.supabase.co`)
   - Anon/Public API Key

### 2. Update Configuration
Update `SupabaseConfig.java` with your actual credentials:
```java
public static final String SUPABASE_URL = "https://yourproject.supabase.co";
public static final String SUPABASE_API_KEY = "your-actual-anon-key";
```

### 3. Create Database Tables

#### Accounts Table
```sql
CREATE TABLE accounts (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    email TEXT UNIQUE NOT NULL,
    name TEXT,
    phone TEXT,
    profile_image_url TEXT,
    auth_user_id UUID REFERENCES auth.users(id)
);
```

#### Messages Table
```sql
CREATE TABLE messages (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    sender_id UUID REFERENCES accounts(id),
    receiver_id UUID REFERENCES accounts(id),
    content TEXT,
    message_type TEXT DEFAULT 'text', -- 'text', 'image', etc.
    image_url TEXT,
    is_read BOOLEAN DEFAULT false
);
```

### 4. Set up Storage Buckets
1. In Supabase Dashboard, go to Storage
2. Create buckets:
   - `profile_pictures` - for user profile images
   - `chat_images` - for chat images

### 5. Update Row Level Security (RLS)
Enable RLS on your tables and create policies:

```sql
-- Enable RLS
ALTER TABLE accounts ENABLE ROW LEVEL SECURITY;
ALTER TABLE messages ENABLE ROW LEVEL SECURITY;

-- Sample policies (customize as needed)
CREATE POLICY "Users can view their own account" ON accounts
    FOR SELECT USING (auth.uid() = auth_user_id);

CREATE POLICY "Users can update their own account" ON accounts
    FOR UPDATE USING (auth.uid() = auth_user_id);
```

## 🔧 Code Migration Patterns

### Authentication Migration

**Before (Firebase):**
```java
FirebaseAuth mAuth = FirebaseAuth.getInstance();
FirebaseUser user = mAuth.getCurrentUser();

// Sign in
mAuth.signInWithEmailAndPassword(email, password)
    .addOnCompleteListener(task -> {
        if (task.isSuccessful()) {
            // Success
        }
    });
```

**After (Supabase - TODO: Implement):**
```java
// You'll need to implement this in SupabaseHelper.java
SupabaseHelper supabase = SupabaseHelper.getInstance();
User user = supabase.getCurrentUser();

// Sign in
supabase.signIn(email, password, new AuthCallback() {
    @Override
    public void onSuccess(User user) {
        // Success
    }
    
    @Override
    public void onError(String error) {
        // Error handling
    }
});
```

### Database Operations Migration

**Before (Firebase Realtime Database):**
```java
DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Accounts");
ref.child(userId).setValue(account);
```

**After (Supabase - TODO: Implement):**
```java
// Insert account
SupabaseHelper.getInstance().insertAccount(account);

// Query accounts
List<Account> accounts = SupabaseHelper.getInstance().getAccounts();
```

### Storage Migration

**Before (Firebase Storage):**
```java
StorageReference storageRef = FirebaseStorage.getInstance().getReference("Images");
storageRef.child("image.jpg").putBytes(data);
```

**After (Supabase - TODO: Implement):**
```java
SupabaseHelper.getInstance().uploadImage("chat_images", "image.jpg", data);
String imageUrl = SupabaseHelper.getInstance().getImageUrl("chat_images", "image.jpg");
```

## 📋 Files That Need Manual Updates

You'll need to update the following files to replace Firebase calls with Supabase:

1. **Authentication Files:**
   - `screen2.java` - Login screen
   - `screen3.java` - Registration screen
   - `fragmentsContainer.java` - User session management

2. **Database Operation Files:**
   - `inputCredentials.java` - User profile creation
   - `fragment_screen4.java` - Message handling
   - `fragment_screen6.java` - Contact management
   - `screen5.java` - Chat functionality
   - `screen8.java` - Account updates

3. **All Adapter Files:**
   - Replace Firebase database listeners with Supabase queries

## 🚀 Implementation Priority

1. **High Priority:**
   - Authentication (sign up, sign in, sign out)
   - User account management
   - Basic message sending/receiving

2. **Medium Priority:**
   - Image upload functionality
   - Real-time message updates
   - Contact management

3. **Low Priority:**
   - Profile picture updates
   - Advanced features

## 💡 Important Notes

- **Real-time Updates**: Firebase Realtime Database provides automatic real-time updates. With Supabase, you'll need to use PostgreSQL's `LISTEN/NOTIFY` or implement polling
- **Authentication**: Supabase uses PostgreSQL with built-in auth, different from Firebase Auth
- **Storage**: Supabase storage works similarly but has different API methods
- **Data Structure**: You may need to adjust your data models to work better with PostgreSQL (relational) vs Firebase (NoSQL)

## 🔍 Testing

After implementing Supabase integration:
1. Test user registration/login
2. Test message sending/receiving
3. Test image uploads
4. Test data persistence

## 📚 Resources

- [Supabase Documentation](https://supabase.com/docs)
- [Supabase Android Tutorial](https://supabase.com/docs/guides/getting-started/tutorials/with-android-kotlin)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)

---

**Note**: This migration requires significant code changes. Consider implementing features incrementally and testing thoroughly at each step.