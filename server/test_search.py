#!/usr/bin/env python3
"""
Test WebSocket search functionality
"""

import asyncio
import websockets
import json

async def test_search():
    uri = "ws://localhost:8080"
    
    try:
        async with websockets.connect(uri) as websocket:
            print("✅ Connected to WebSocket server")
            
            # First, join as a user (required for authentication)
            join_message = {
                "type": "join_room",
                "sender_uid": "test_search_user",
                "sender_email": "test@search.com",
                "display_name": "Test Search User"
            }
            
            await websocket.send(json.dumps(join_message))
            print("📤 Sent join message")
            
            # Wait for multiple join responses
            responses = []
            for i in range(3):  # Expect up to 3 responses (connected, join_success, history)
                try:
                    response = await asyncio.wait_for(websocket.recv(), timeout=2.0)
                    response_data = json.loads(response)
                    responses.append(response_data)
                    print(f"📥 Response {i+1}: {response_data.get('type', 'unknown')}")
                except asyncio.TimeoutError:
                    break
            
            # Now test search functionality
            search_message = {
                "type": "search_users",
                "query": "gmail",
                "limit": 10
            }
            
            await websocket.send(json.dumps(search_message))
            print("📤 Sent search message for 'gmail'")
            
            # Wait for search response
            search_response = await asyncio.wait_for(websocket.recv(), timeout=5.0)
            result = json.loads(search_response)
            print(f"📥 Search response: {json.dumps(result, indent=2)}")
            
            if result.get('success') and result.get('users'):
                print(f"✅ Found {len(result['users'])} users!")
                for user in result['users']:
                    print(f"   - {user['display_name']} ({user['email']})")
            else:
                print("❌ No users found or search failed")
                print(f"   Error: {result.get('message', 'Unknown error')}")
            
    except Exception as e:
        print(f"❌ Error: {e}")

if __name__ == "__main__":
    print("🔍 Testing WebSocket search functionality...")
    asyncio.run(test_search())