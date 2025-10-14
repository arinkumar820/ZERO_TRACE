-- SQL Setup for your Supabase project: hpcfyiilnqchddpotlse.supabase.co
-- Copy and paste this into your Supabase SQL Editor and run it

-- Create accounts table for user profiles
CREATE TABLE IF NOT EXISTS accounts (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    email TEXT UNIQUE NOT NULL,
    first_name TEXT,
    last_name TEXT,
    gender TEXT,
    bio TEXT,
    dp TEXT,
    phone_number TEXT,
    state TEXT,
    last_seen_time TEXT,
    last_seen_date TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create messages table for chat messages
CREATE TABLE IF NOT EXISTS messages (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    message TEXT,
    time TEXT,
    location TEXT,
    key TEXT,
    receiver_id UUID REFERENCES accounts(id),
    sender_id UUID REFERENCES accounts(id),
    image TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Enable Row Level Security
ALTER TABLE accounts ENABLE ROW LEVEL SECURITY;
ALTER TABLE messages ENABLE ROW LEVEL SECURITY;

-- Policies for accounts (users can see all accounts for contact list)
CREATE POLICY "Users can view all accounts" ON accounts FOR SELECT USING (true);
CREATE POLICY "Users can insert their own account" ON accounts FOR INSERT WITH CHECK (auth.uid() = id);
CREATE POLICY "Users can update their own account" ON accounts FOR UPDATE USING (auth.uid() = id);

-- Policies for messages (users can only see their conversations)
CREATE POLICY "Users can view their messages" ON messages FOR SELECT USING (auth.uid() = sender_id OR auth.uid() = receiver_id);
CREATE POLICY "Users can send messages" ON messages FOR INSERT WITH CHECK (auth.uid() = sender_id);
CREATE POLICY "Users can update their own messages" ON messages FOR UPDATE USING (auth.uid() = sender_id);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_messages_sender ON messages(sender_id);
CREATE INDEX IF NOT EXISTS idx_messages_receiver ON messages(receiver_id);
CREATE INDEX IF NOT EXISTS idx_messages_created_at ON messages(created_at);
CREATE INDEX IF NOT EXISTS idx_accounts_email ON accounts(email);