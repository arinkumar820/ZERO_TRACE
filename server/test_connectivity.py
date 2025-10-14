#!/usr/bin/env python3
"""
Test WebSocket server connectivity
"""

import asyncio
import websockets
import json

async def test_connectivity():
    uri = "ws://192.168.1.75:8080"  # Test from the same IP your Android app uses
    
    try:
        print(f"🔌 Attempting to connect to {uri}")
        
        # Set a longer timeout to match what might be happening with Android
        async with websockets.connect(uri, timeout=10) as websocket:
            print("✅ Connected successfully!")
            
            # Test basic message exchange
            test_message = {
                "type": "join_room",
                "sender_uid": "connectivity_test",
                "sender_email": "test@connectivity.com",
                "display_name": "Connectivity Test"
            }
            
            await websocket.send(json.dumps(test_message))
            print("📤 Sent test message")
            
            # Wait for response
            response = await asyncio.wait_for(websocket.recv(), timeout=5.0)
            print(f"📥 Received: {response}")
            
            return True
            
    except asyncio.TimeoutError:
        print("❌ Connection timeout - server not responding")
        return False
    except ConnectionRefusedError:
        print("❌ Connection refused - server not running or blocked")
        return False
    except Exception as e:
        print(f"❌ Connection error: {e}")
        return False

async def test_local():
    uri = "ws://localhost:8080"
    
    try:
        print(f"🔌 Testing localhost connection to {uri}")
        
        async with websockets.connect(uri, timeout=5) as websocket:
            print("✅ Localhost connection successful!")
            
            # Quick ping test
            await websocket.send(json.dumps({"type": "ping"}))
            response = await asyncio.wait_for(websocket.recv(), timeout=2.0)
            print(f"📥 Localhost response: {response}")
            return True
            
    except Exception as e:
        print(f"❌ Localhost connection failed: {e}")
        return False

if __name__ == "__main__":
    print("🧪 Testing WebSocket connectivity...\n")
    
    # Test localhost first
    local_result = asyncio.run(test_local())
    print()
    
    # Test network connection
    network_result = asyncio.run(test_connectivity())
    print()
    
    if local_result and network_result:
        print("✅ Server is accessible from both localhost and network")
        print("✅ Your Android app should be able to connect")
    elif local_result and not network_result:
        print("⚠️  Server works locally but not from network")
        print("🔥 Firewall might be blocking external connections")
    else:
        print("❌ Server connection issues detected")