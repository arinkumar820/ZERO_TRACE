#!/usr/bin/env python3
"""
Examine the existing database schema
"""

import sqlite3
import os

# Database path
DATABASE_PATH = os.path.join("server", 'bisto_chat.db')

def examine_schema():
    """Examine database schema"""
    try:
        conn = sqlite3.connect(DATABASE_PATH)
        cursor = conn.cursor()
        
        # Get all tables
        cursor.execute("SELECT name FROM sqlite_master WHERE type='table';")
        tables = cursor.fetchall()
        
        print("🗂️  DATABASE SCHEMA ANALYSIS")
        print("=" * 50)
        
        for table in tables:
            table_name = table[0]
            print(f"\n📋 TABLE: {table_name}")
            
            # Get table structure
            cursor.execute(f"PRAGMA table_info({table_name});")
            columns = cursor.fetchall()
            
            for col in columns:
                print(f"  - {col[1]}: {col[2]} {'(PRIMARY KEY)' if col[5] else ''}")
            
            # Get row count
            cursor.execute(f"SELECT COUNT(*) FROM {table_name}")
            count = cursor.fetchone()[0]
            print(f"  📊 Rows: {count}")
            
            # Show sample data for messages table
            if table_name == 'messages' and count > 0:
                cursor.execute(f"SELECT * FROM {table_name} LIMIT 3")
                samples = cursor.fetchall()
                print(f"  📝 Sample data:")
                for i, sample in enumerate(samples, 1):
                    print(f"    {i}. {sample}")
        
        conn.close()
        print("\n" + "=" * 50)
        
    except Exception as e:
        print(f"❌ Error: {e}")

if __name__ == "__main__":
    examine_schema()