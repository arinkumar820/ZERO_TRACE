-- Supabase Database Schema for Bisto Chat App
-- Run this in your Supabase SQL Editor

-- Create accounts table for user profiles
CREATE TABLE IF NOT EXISTS accounts (
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

-- Create messages table for chat messages
CREATE TABLE IF NOT EXISTS messages (
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

-- Enable Row Level Security (RLS)
ALTER TABLE accounts ENABLE ROW LEVEL SECURITY;
ALTER TABLE messages ENABLE ROW LEVEL SECURITY;

-- Create policies for accounts table
CREATE POLICY "Users can view all accounts" ON accounts FOR SELECT USING (true);
CREATE POLICY "Users can insert their own account" ON accounts FOR INSERT WITH CHECK (auth.uid() = id);
CREATE POLICY "Users can update their own account" ON accounts FOR UPDATE USING (auth.uid() = id);

-- Create policies for messages table
CREATE POLICY "Users can view messages they sent or received" ON messages FOR SELECT USING (auth.uid() = sender_id OR auth.uid() = receiver_id);
CREATE POLICY "Users can insert messages" ON messages FOR INSERT WITH CHECK (auth.uid() = sender_id);
CREATE POLICY "Users can update their own messages" ON messages FOR UPDATE USING (auth.uid() = sender_id);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_messages_sender ON messages(sender_id);
CREATE INDEX IF NOT EXISTS idx_messages_receiver ON messages(receiver_id);
CREATE INDEX IF NOT EXISTS idx_messages_created_at ON messages(created_at);
CREATE INDEX IF NOT EXISTS idx_accounts_email ON accounts(email);