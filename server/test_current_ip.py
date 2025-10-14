#!/usr/bin/env python3
"""
Test WebSocket server connectivity with current IP
"""

import asyncio
import websockets
import json

async def test_current_ip():
    uri = "ws://10.248.154.124:8080"  # Current IP
    
    try:
        print(f"🔌 Testing connection to {uri}")
        
        async with websockets.connect(uri, timeout=5) as websocket:
            print("✅ Connection successful!")
            
            # Test basic message exchange
            test_message = {
                "type": "join_room",
                "sender_uid": "test_user",
                "sender_email": "test@example.com",
                "display_name": "Test User"
            }
            
            await websocket.send(json.dumps(test_message))
            print("📤 Sent test message")
            
            # Wait for response
            response = await asyncio.wait_for(websocket.recv(), timeout=3.0)
            print(f"📥 Received: {response}")
            
            return True
            
    except Exception as e:
        print(f"❌ Connection failed: {e}")
        return False

if __name__ == "__main__":
    result = asyncio.run(test_current_ip())
    if result:
        print("\n✅ Your WebSocket server is accessible at ws://10.248.154.124:8080")
        print("🔧 Update your Android app to use this IP address")
    else:
        print("\n❌ Server not accessible from network IP")
        print("🔥 Check firewall settings")