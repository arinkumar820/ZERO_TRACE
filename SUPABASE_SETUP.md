# Supabase Setup Guide for Bisto Chat

This guide explains how to set up Supabase for the chat application.

## Prerequisites

1. Create a Supabase account at [supabase.com](https://supabase.com)
2. Create a new project

## Configuration

1. **Get your Supabase credentials:**
   - Go to your Supabase project dashboard
   - Navigate to Settings > API
   - Copy the `URL` and `anon public` API key

2. **Update SupabaseConfig.java:**
   ```java
   public static final String SUPABASE_URL = "https://your-project-ref.supabase.co";
   public static final String SUPABASE_API_KEY = "your-anon-public-api-key";
   ```

## Database Schema

Create the following tables in your Supabase database:

### 1. Accounts Table
```sql
CREATE TABLE accounts (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    email TEXT UNIQUE NOT NULL,
    first_name TEXT,
    last_name TEXT,
    gender TEXT,
    bio TEXT,
    dp TEXT, -- Profile picture URL
    phone_number TEXT,
    state TEXT,
    last_seen_time TEXT,
    last_seen_date TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

### 2. Messages Table
```sql
CREATE TABLE messages (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    message TEXT,
    time TEXT,
    location TEXT,
    key TEXT,
    receiver_id UUID REFERENCES accounts(id),
    sender_id UUID REFERENCES accounts(id),
    image TEXT, -- Image URL if message contains image
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

## Storage Buckets

Create the following storage buckets:

1. **profile_pictures** - for user profile images
2. **chat_images** - for images shared in chats

## Row Level Security (RLS)

Enable RLS and create policies as needed for your security requirements.

## Testing

To test the registration:
1. Make sure you have an active internet connection
2. Update the SupabaseConfig.java with your credentials
3. Run the app and try registering with a valid email address
4. Check your Supabase dashboard to see if the user was created

## Troubleshooting

- **Network errors**: Check internet connection and Supabase URL
- **403 errors**: Verify your API key is correct
- **422 errors**: Usually validation issues (weak password, invalid email format)

## Next Steps

After registration works, you'll need to:
1. Implement the profile creation in `inputCredentials.java`
2. Set up message sending/receiving functionality
3. Implement image upload to Supabase Storage