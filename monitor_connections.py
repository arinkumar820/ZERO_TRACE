#!/usr/bin/env python3
"""
Real-time connection monitor for WebSocket server
Shows all client connections and messages in real-time
"""

import asyncio
import websockets
import json
from datetime import datetime

async def monitor_client():
    uri = "ws://localhost:8080/"
    
    print("🔍 Connection Monitor Started")
    print("============================")
    print(f"⏰ Started at: {datetime.now().strftime('%H:%M:%S')}")
    print("👀 Listening for all messages...\n")
    
    try:
        async with websockets.connect(uri) as websocket:
            print("✅ Monitor connected to WebSocket server\n")
            
            # Send join as monitor
            join_message = {
                "type": "join",
                "sender_uid": "monitor_python",
                "sender_email": "monitor@python.com",
                "display_name": "Connection Monitor"
            }
            await websocket.send(json.dumps(join_message))
            
            # Listen for all messages
            async for message in websocket:
                try:
                    data = json.loads(message)
                    timestamp = datetime.now().strftime('%H:%M:%S')
                    
                    msg_type = data.get('type', 'unknown')
                    sender = data.get('sender_uid', 'server')
                    content = data.get('message', str(data))
                    
                    print(f"📨 [{timestamp}] {msg_type.upper()}")
                    print(f"   👤 From: {sender}")
                    print(f"   💬 Message: {content[:100]}{'...' if len(content) > 100 else ''}")
                    print(f"   📄 Raw: {message[:200]}{'...' if len(message) > 200 else ''}")
                    print("-" * 50)
                    
                except json.JSONDecodeError:
                    timestamp = datetime.now().strftime('%H:%M:%S')
                    print(f"📨 [{timestamp}] RAW MESSAGE")
                    print(f"   📄 Content: {message}")
                    print("-" * 50)
                    
    except Exception as e:
        print(f"❌ Monitor error: {e}")

if __name__ == "__main__":
    try:
        asyncio.run(monitor_client())
    except KeyboardInterrupt:
        print("\n⏹️  Monitor stopped by user")