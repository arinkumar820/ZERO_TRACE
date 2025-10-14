#!/usr/bin/env python3
"""
Test room-based messaging to verify messages only go to the correct rooms
"""

import asyncio
import websockets
import json
import sys

async def test_client(name, user_id, room_id):
    """Create a test client for a specific room"""
    uri = "ws://localhost:8080/"
    
    try:
        print(f"🔌 {name} connecting to server...")
        async with websockets.connect(uri) as websocket:
            print(f"✅ {name} connected!")
            
            # Wait for connection message
            await websocket.recv()
            
            # Send join message
            join_message = {
                "type": "join",
                "sender_uid": user_id,
                "sender_email": f"{user_id}@test.com",
                "display_name": name
            }
            await websocket.send(json.dumps(join_message))
            
            # Wait for join confirmation
            await websocket.recv()
            print(f"✅ {name} joined server")
            
            # Skip history messages
            try:
                while True:
                    response = await asyncio.wait_for(websocket.recv(), timeout=1.0)
                    data = json.loads(response)
                    if data.get('type') != 'history':
                        break
            except asyncio.TimeoutError:
                pass
            
            # Send a message to specific room
            test_message = {
                "type": "message",
                "sender_uid": user_id,
                "sender_email": f"{user_id}@test.com",
                "display_name": name,
                "message": f"Hello from {name} in {room_id}!",
                "chat_room_id": room_id,
                "message_type": "text"
            }
            
            print(f"📤 {name} sending message to {room_id}")
            await websocket.send(json.dumps(test_message))
            
            # Listen for messages for a few seconds
            print(f"👂 {name} listening for messages...")
            messages_received = []
            
            try:
                for _ in range(5):  # Listen for up to 5 messages
                    response = await asyncio.wait_for(websocket.recv(), timeout=2.0)
                    data = json.loads(response)
                    if data.get('type') == 'message':
                        messages_received.append({
                            'from': data.get('sender_name', 'Unknown'),
                            'room': data.get('chat_room_id', 'unknown'),
                            'message': data.get('message', ''),
                            'should_receive': data.get('chat_room_id') == room_id
                        })
                        print(f"📨 {name} received: {data.get('message', '')} from {data.get('chat_room_id', 'unknown')}")
            except asyncio.TimeoutError:
                print(f"⏰ {name} finished listening")
            
            return messages_received
            
    except Exception as e:
        print(f"❌ {name} error: {e}")
        return []

async def test_room_isolation():
    """Test that messages are properly isolated by room"""
    print("🧪 Testing Room-Based Message Isolation")
    print("=" * 50)
    
    # Create clients for different rooms
    clients = [
        ("Alice_Alpha", "alice_001", "team_alpha"),
        ("Bob_Tiger", "bob_002", "team_tiger"), 
        ("Carol_Alpha", "carol_003", "team_alpha"),
        ("David_Kalpaditya", "david_004", "user_kalpaditya")
    ]
    
    print("🚀 Starting test clients...")
    
    # Run all clients concurrently
    tasks = []
    for name, user_id, room_id in clients:
        task = asyncio.create_task(test_client(name, user_id, room_id))
        tasks.append((name, room_id, task))
    
    # Wait for all tasks to complete
    results = {}
    for name, room_id, task in tasks:
        try:
            messages = await task
            results[name] = {
                'room': room_id,
                'messages_received': messages
            }
        except Exception as e:
            print(f"❌ Task failed for {name}: {e}")
            results[name] = {'room': room_id, 'messages_received': []}
    
    print("\n📊 Test Results:")
    print("=" * 50)
    
    # Analyze results
    success = True
    for client_name, client_data in results.items():
        client_room = client_data['room']
        messages = client_data['messages_received']
        
        print(f"\n👤 {client_name} (in {client_room}):")
        
        if not messages:
            print(f"  ℹ️  Received no messages")
            continue
            
        for msg in messages:
            expected = msg['should_receive']
            if expected:
                print(f"  ✅ Correctly received: {msg['message']} from {msg['room']}")
            else:
                print(f"  ❌ SHOULD NOT RECEIVE: {msg['message']} from {msg['room']}")
                success = False
    
    print(f"\n🎯 Test Result: {'✅ PASSED' if success else '❌ FAILED'}")
    
    if success:
        print("\n🎉 Room isolation is working correctly!")
        print("Messages to Kalpaditya stay in Kalpaditya's room")
        print("Team Alpha messages stay in Team Alpha room")
        print("Team Tiger messages stay in Team Tiger room")
    else:
        print("\n💥 Room isolation is NOT working correctly!")
        print("Messages are leaking between rooms!")
    
    return success

if __name__ == "__main__":
    print("🧪 Room-Based Messaging Test")
    print("=" * 50)
    
    success = asyncio.run(test_room_isolation())
    sys.exit(0 if success else 1)