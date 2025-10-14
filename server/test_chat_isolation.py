#!/usr/bin/env python3
"""
Test script to verify chat room message isolation
This script simulates multiple users in different chat rooms to ensure messages don't leak between rooms.
"""

import asyncio
import websockets
import json
import logging
import time

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

async def test_chat_isolation():
    """Test that messages in one chat room don't appear in another"""
    
    uri = "ws://10.248.154.124:8080"
    
    try:
        # Connect two clients
        async with websockets.connect(uri) as user1_ws, websockets.connect(uri) as user2_ws:
            logger.info("✅ Both users connected to server")
            
            # User 1 authentication/join 
            user1_auth = {
                "type": "join_room",  # This handles both auth and room join
                "user_uid": "user1_uid",
                "user_email": "user1@test.com",
                "display_name": "User1",
                "room_id": "deepak_chat"
            }
            await user1_ws.send(json.dumps(user1_auth))
            logger.info("👤 User1 authenticated and joined 'deepak_chat'")
            
            # User 2 authentication/join
            user2_auth = {
                "type": "join_room", 
                "user_uid": "user2_uid",
                "user_email": "user2@test.com",
                "display_name": "User2",
                "room_id": "general_chat"
            }
            await user2_ws.send(json.dumps(user2_auth))
            logger.info("👤 User2 authenticated and joined 'general_chat'")
            
            # Wait for join confirmations
            await asyncio.sleep(1)
            
            # Clear any welcome/join messages
            try:
                while True:
                    await asyncio.wait_for(user1_ws.recv(), timeout=0.1)
                    await asyncio.wait_for(user2_ws.recv(), timeout=0.1)
            except asyncio.TimeoutError:
                pass  # No more messages to clear
            
            logger.info("🧹 Cleared initial messages")
            
            # User 1 sends message in deepak_chat
            user1_message = {
                "type": "chat_message",
                "sender_uid": "user1_uid",
                "sender_email": "user1@test.com",
                "message": "Hello from Deepak chat room!",
                "chat_room_id": "deepak_chat"
            }
            await user1_ws.send(json.dumps(user1_message))
            logger.info("📤 User1 sent message to deepak_chat")
            
            # User 2 sends message in general_chat
            user2_message = {
                "type": "chat_message", 
                "sender_uid": "user2_uid",
                "sender_email": "user2@test.com",
                "message": "Hello from General chat room!",
                "chat_room_id": "general_chat"
            }
            await user2_ws.send(json.dumps(user2_message))
            logger.info("📤 User2 sent message to general_chat")
            
            # Check what each user receives
            await asyncio.sleep(0.5)
            
            user1_messages = []
            user2_messages = []
            
            # Collect User1's messages
            try:
                while True:
                    msg = await asyncio.wait_for(user1_ws.recv(), timeout=0.5)
                    parsed = json.loads(msg)
                    logger.info(f"📥 User1 received message type '{parsed.get('type')}': {parsed}")
                    if parsed.get('type') == 'new_message':
                        user1_messages.append(parsed)
                        logger.info(f"💬 User1 got chat message: {parsed.get('message')} from room {parsed.get('room_id')}")
            except asyncio.TimeoutError:
                pass
            
            # Collect User2's messages  
            try:
                while True:
                    msg = await asyncio.wait_for(user2_ws.recv(), timeout=0.5)
                    parsed = json.loads(msg)
                    logger.info(f"📥 User2 received message type '{parsed.get('type')}': {parsed}")
                    if parsed.get('type') == 'new_message':
                        user2_messages.append(parsed)
                        logger.info(f"💬 User2 got chat message: {parsed.get('message')} from room {parsed.get('room_id')}")
            except asyncio.TimeoutError:
                pass
            
            # Verify isolation
            logger.info("\n" + "="*50)
            logger.info("🔍 TESTING RESULTS:")
            
            # User1 should only receive messages from deepak_chat
            deepak_messages_for_user1 = [m for m in user1_messages if m.get('room_id') == 'deepak_chat']
            general_messages_for_user1 = [m for m in user1_messages if m.get('room_id') == 'general_chat']
            
            # User2 should only receive messages from general_chat
            deepak_messages_for_user2 = [m for m in user2_messages if m.get('room_id') == 'deepak_chat']
            general_messages_for_user2 = [m for m in user2_messages if m.get('room_id') == 'general_chat']
            
            success = True
            
            if len(deepak_messages_for_user1) > 0:
                logger.info("✅ User1 correctly received deepak_chat messages")
            else:
                logger.warning("❌ User1 did not receive deepak_chat messages")
                success = False
                
            if len(general_messages_for_user1) == 0:
                logger.info("✅ User1 correctly did NOT receive general_chat messages")
            else:
                logger.error("❌ User1 incorrectly received general_chat messages!")
                success = False
                
            if len(general_messages_for_user2) > 0:
                logger.info("✅ User2 correctly received general_chat messages")
            else:
                logger.warning("❌ User2 did not receive general_chat messages")
                success = False
                
            if len(deepak_messages_for_user2) == 0:
                logger.info("✅ User2 correctly did NOT receive deepak_chat messages")
            else:
                logger.error("❌ User2 incorrectly received deepak_chat messages!")
                success = False
            
            if success:
                logger.info("\n🎉 SUCCESS: Chat room isolation is working correctly!")
                logger.info("Messages are properly isolated between different chat rooms.")
            else:
                logger.error("\n❌ FAILURE: Chat room isolation is not working!")
                logger.error("Messages are leaking between different chat rooms.")
            
            return success
            
    except Exception as e:
        logger.error(f"❌ Test failed with error: {e}")
        return False

if __name__ == "__main__":
    print("🧪 Testing Chat Room Message Isolation")
    print("=" * 50)
    
    success = asyncio.run(test_chat_isolation())
    
    if success:
        print("\n✅ All tests passed! Your chat isolation fix is working.")
    else:
        print("\n❌ Tests failed. Check the server and client configuration.")