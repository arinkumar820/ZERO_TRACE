package com.sameetasadullah.i180479_180531;

/**
 * Interface for WebSocket client callbacks
 */
public interface WebSocketClientListener {
    
    /**
     * Called when WebSocket connection is established
     */
    void onConnected();
    
    /**
     * Called when a message is received from the server
     * @param message The received message
     */
    void onMessageReceived(WebSocketMessage message);
    
    /**
     * Called when WebSocket connection is closed
     * @param code Close code
     * @param reason Close reason
     * @param remote Whether it was closed by remote
     */
    void onDisconnected(int code, String reason, boolean remote);
    
    /**
     * Called when an error occurs
     * @param error The error that occurred
     */
    void onError(Exception error);
    
    /**
     * Called when attempting to reconnect
     * @param attempt The reconnection attempt number
     */
    void onReconnecting(int attempt);
}