#!/usr/bin/env python3
"""
Simple WebSocket client to test the server
"""

import asyncio
import websockets
import json
import sys

async def test_client():
    uri = "ws://localhost:8080/"
    
    try:
        print("Connecting to WebSocket server...")
        async with websockets.connect(uri) as websocket:
            print(" Connected to WebSocket server")
            
            # Wait for connection message
            response = await websocket.recv()
            print(f"Server response: {response}")
            
            # Send join message
            join_message = {
                "type": "join",
                "sender_uid": "test_user_python",
                "sender_email": "test@python.com",
                "display_name": "Python Test Client"
            }
            
            print(" Sending join message...")
            await websocket.send(json.dumps(join_message))
            
            # Wait for join confirmation
            response = await websocket.recv()
            print(f" Join response: {response}")
            
            # Send a test message
            test_message = {
                "type": "message", 
                "sender_uid": "test_user_python",
                "sender_email": "test@python.com",
                "message": "Hello from Python test client!",
                "message_type": "text"
            }
            
            print(" Sending test message...")
            await websocket.send(json.dumps(test_message))
            
            # Wait for any responses
            try:
                response = await asyncio.wait_for(websocket.recv(), timeout=2.0)
                print(f"Message response: {response}")
            except asyncio.TimeoutError:
                print("No immediate response (normal for broadcasts)")
            
            print(" Test completed successfully!")
            
    except Exception as e:
        print(f" Error: {e}")
        return False
        
    return True

if __name__ == "__main__":
    print("🧪 WebSocket Server Test Client")
    print("=" * 40)
    
    success = asyncio.run(test_client())
    sys.exit(0 if success else 1)