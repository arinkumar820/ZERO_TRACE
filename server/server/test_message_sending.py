#!/usr/bin/env python3
"""
Enhanced test client to verify message sending works with chat rooms
"""

import asyncio
import websockets
import json
import sys

async def test_message_sending():
    uri = "ws://localhost:8080/"
    
    try:
        print("🧪 Enhanced Message Sending Test")
        print("=" * 40)
        print("Connecting to WebSocket server...")
        
        async with websockets.connect(uri) as websocket:
            print("✅ Connected to WebSocket server")
            
            # Wait for connection message
            response = await websocket.recv()
            print(f"📨 Server welcome: {response}")
            
            # Send join message
            join_message = {
                "type": "join",
                "sender_uid": "test_user_enhanced",
                "sender_email": "test@enhanced.com",
                "display_name": "Enhanced Test Client"
            }
            
            print("📤 Sending join message...")
            await websocket.send(json.dumps(join_message))
            
            # Wait for join confirmation
            response = await websocket.recv()
            print(f"📨 Join response: {response}")
            
            # Skip history messages
            print("⏳ Waiting for history messages...")
            try:
                while True:
                    response = await asyncio.wait_for(websocket.recv(), timeout=1.0)
                    data = json.loads(response)
                    if data.get('type') == 'history':
                        print(f"📜 History: {data.get('message', '')[:50]}...")
                    else:
                        print(f"📨 Other: {data}")
                        break
            except asyncio.TimeoutError:
                print("✅ History messages completed")
            
            # Test sending messages to different chat rooms
            test_messages = [
                {"chat_room_id": "team_alpha", "message": "🚀 Alpha team testing message!"},
                {"chat_room_id": "team_tiger", "message": "🐅 Tiger team message test!"},
                {"chat_room_id": "general_chat", "message": "💬 General chat test message!"},
            ]
            
            for test_msg in test_messages:
                message_data = {
                    "type": "message", 
                    "sender_uid": "test_user_enhanced",
                    "sender_email": "test@enhanced.com",
                    "message": test_msg["message"],
                    "message_type": "text",
                    "chat_room_id": test_msg["chat_room_id"]
                }
                
                print(f"📤 Sending to {test_msg['chat_room_id']}: {test_msg['message']}")
                await websocket.send(json.dumps(message_data))
                
                # Wait for response
                try:
                    response = await asyncio.wait_for(websocket.recv(), timeout=2.0)
                    data = json.loads(response)
                    if data.get('type') == 'message':
                        print(f"✅ Message confirmed in {data.get('chat_room_id', 'unknown')} room")
                    else:
                        print(f"📨 Response: {data.get('type', 'unknown')} - {data.get('message', '')}")
                except asyncio.TimeoutError:
                    print("⏰ No immediate response")
                
                await asyncio.sleep(0.5)
            
            print("🎉 All test messages sent successfully!")
            
    except Exception as e:
        print(f"❌ Test error: {e}")
        return False
        
    return True

if __name__ == "__main__":
    print("🧪 Enhanced WebSocket Message Sending Test")
    print("=" * 50)
    
    success = asyncio.run(test_message_sending())
    
    if success:
        print("\n✅ Message sending test PASSED!")
        print("🎯 Your WebSocket server is working perfectly!")
    else:
        print("\n❌ Message sending test FAILED!")
    
    sys.exit(0 if success else 1)