package com.sameetasadullah.i180479_180531;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.net.URI;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket client manager for real-time chat
 */
public class WebSocketClientManager {
    private static final String TAG = "WebSocketClientManager";
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    private static final long RECONNECT_DELAY_MS = 3000; // 3 seconds

    private WebSocketClient webSocketClient;
    private WebSocketClientListener listener;
    private Handler mainHandler;
    private Handler backgroundHandler;
    private Gson gson;
    
    private String serverUrl;
    private boolean shouldReconnect = true;
    private int reconnectAttempts = 0;
    private boolean isConnected = false;

    // Traffic shaping configuration and scheduler (keep in sync with EnhancedWebSocketClient)
    private static final long DEFAULT_BASE_INTERVAL_MS = 250; // constant rate base interval
    private static final long DEFAULT_JITTER_MS = 50;        // ± jitter per tick
    private static final int DEFAULT_PAD_MIN_CHARS = 256;    // minimal body padding to dampen compression cues
    private static final int TARGET_FRAME_SIZE = 512;        // target total JSON size with pad field
    private boolean enablePadding = true;
    private TransportScheduler transportScheduler;

    public WebSocketClientManager(String serverUrl, WebSocketClientListener listener) {
        this.serverUrl = serverUrl;
        this.listener = listener;
        // Create Gson with custom type adapters to handle timestamps and WebSocketMessage
        this.gson = new GsonBuilder()
                .registerTypeAdapter(Long.class, new FlexibleTimestampDeserializer())
                .registerTypeAdapter(long.class, new FlexibleTimestampDeserializer())
                .registerTypeAdapter(WebSocketMessage.class, new WebSocketMessageTypeAdapter())
                .create();
        this.mainHandler = new Handler(Looper.getMainLooper());
        
        // Create background handler for WebSocket operations
        android.os.HandlerThread handlerThread = new android.os.HandlerThread("WebSocketThread");
        handlerThread.start();
        this.backgroundHandler = new Handler(handlerThread.getLooper());
    }

    /**
     * Connect to the WebSocket server
     */
    public void connect() {
        backgroundHandler.post(() -> {
            try {
                Log.d(TAG, "Attempting to connect to: " + serverUrl);
                
                URI serverUri = URI.create(serverUrl);
                webSocketClient = new WebSocketClient(serverUri) {
                    @Override
                    public void onOpen(ServerHandshake handshake) {
                        Log.d(TAG, "WebSocket connected successfully");
                        isConnected = true;
                        reconnectAttempts = 0;

                        // Start transport scheduler for constant-rate sending with cover traffic
                        if (transportScheduler == null) {
                            transportScheduler = new TransportScheduler(DEFAULT_BASE_INTERVAL_MS, DEFAULT_JITTER_MS, new TransportScheduler.Sender() {
                                @Override
                                public void send(JSONObject msg) {
                                    sendNow(msg);
                                }
                                @Override
                                public void sendCover() {
                                    try {
                                        JSONObject cover = new JSONObject();
                                        cover.put("type", "ping");
                                        JSONObject padded = PayloadPadder.padWithPadField(cover, TARGET_FRAME_SIZE);
                                        sendNow(padded);
                                    } catch (Exception ignored) { }
                                }
                            });
                        }
                        transportScheduler.start();
                        
                        // Notify listener on main thread
                        mainHandler.post(() -> {
                            if (listener != null) {
                                listener.onConnected();
                            }
                        });
                    }

                    @Override
                    public void onMessage(String message) {
                        Log.d(TAG, "Received message: " + message);
                        
                        try {
                            WebSocketMessage wsMessage = gson.fromJson(message, WebSocketMessage.class);
                            
                            // Notify listener on main thread
                            mainHandler.post(() -> {
                                if (listener != null) {
                                    listener.onMessageReceived(wsMessage);
                                }
                            });
                        } catch (JsonSyntaxException e) {
                            Log.e(TAG, "Failed to parse message: " + message, e);
                        }
                    }

                    @Override
                    public void onClose(int code, String reason, boolean remote) {
                        Log.d(TAG, "WebSocket closed. Code: " + code + ", Reason: " + reason + ", Remote: " + remote);
                        isConnected = false;

                        // Stop scheduler when disconnected
                        stopScheduler();
                        
                        // Notify listener on main thread
                        mainHandler.post(() -> {
                            if (listener != null) {
                                listener.onDisconnected(code, reason, remote);
                            }
                        });
                        
                        // Attempt reconnection if needed
                        if (shouldReconnect && reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                            scheduleReconnect();
                        }
                    }

                    @Override
                    public void onError(Exception ex) {
                        Log.e(TAG, "WebSocket error", ex);

                        // Stop scheduler on error to avoid continued cover sends
                        stopScheduler();
                        
                        // Notify listener on main thread
                        mainHandler.post(() -> {
                            if (listener != null) {
                                listener.onError(ex);
                            }
                        });
                    }
                };

                // Set connection timeout
                webSocketClient.setConnectionLostTimeout(30);
                webSocketClient.connect();
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to create WebSocket connection", e);
                mainHandler.post(() -> {
                    if (listener != null) {
                        listener.onError(e);
                    }
                });
            }
        });
    }

    /**
     * Send a message to the WebSocket server
     */
    public void sendMessage(WebSocketMessage message) {
        backgroundHandler.post(() -> {
            if (webSocketClient != null && isConnected) {
                try {
                    // Serialize using Gson, then pad fields using org.json for consistent frame size
                    String jsonMessage = gson.toJson(message);
                    JSONObject json = new JSONObject(jsonMessage);
                    if (enablePadding) {
                        // Minimal body padding (if 'message' exists) to reduce compression cues
                        json = PayloadPadder.padForJson(json, DEFAULT_PAD_MIN_CHARS);
                        // Normalize total frame size with 'pad' field
                        json = PayloadPadder.padWithPadField(json, TARGET_FRAME_SIZE);
                    }
                    // Enqueue for scheduled sending
                    if (transportScheduler != null) {
                        transportScheduler.enqueue(json);
                    } else {
                        // Fallback to immediate send if scheduler not started yet
                        sendNow(json);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to enqueue/send message", e);
                    mainHandler.post(() -> {
                        if (listener != null) {
                            listener.onError(e);
                        }
                    });
                }
            } else {
                Log.w(TAG, "WebSocket not connected. Cannot send message.");
                mainHandler.post(() -> {
                    if (listener != null) {
                        listener.onError(new Exception("WebSocket not connected"));
                    }
                });
            }
        });
    }

    /**
     * Send a 'message_seen' acknowledgement to the server for a specific message.
     */
    public void sendSeenAck(String roomId, int messageId, String seenByUid) {
        backgroundHandler.post(() -> {
            try {
                org.json.JSONObject json = new org.json.JSONObject();
                json.put("type", "message_seen");
                json.put("room_id", roomId);
                json.put("message_id", messageId);
                json.put("seen_by", seenByUid);
                if (transportScheduler != null) {
                    transportScheduler.enqueue(json);
                } else if (webSocketClient != null && isConnected) {
                    webSocketClient.send(json.toString());
                }
                Log.d(TAG, "Sent seen ack for message_id=" + messageId + " in room=" + roomId);
            } catch (Exception e) {
                Log.e(TAG, "Failed to send seen ack", e);
            }
        });
    }

    /**
     * Disconnect from the WebSocket server
     */
    public void disconnect() {
        shouldReconnect = false;
        backgroundHandler.post(() -> {
            if (webSocketClient != null) {
                try {
                    webSocketClient.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error while closing WebSocket", e);
                }
            }
            stopScheduler();
        });
    }

    /**
     * Check if WebSocket is connected
     */
    public boolean isConnected() {
        return isConnected && webSocketClient != null && !webSocketClient.isClosed();
    }

    /**
     * Schedule reconnection attempt
     */
    private void scheduleReconnect() {
        reconnectAttempts++;
        Log.d(TAG, "Scheduling reconnect attempt " + reconnectAttempts + " in " + RECONNECT_DELAY_MS + "ms");
        
        mainHandler.post(() -> {
            if (listener != null) {
                listener.onReconnecting(reconnectAttempts);
            }
        });
        
        backgroundHandler.postDelayed(() -> {
            if (shouldReconnect && reconnectAttempts <= MAX_RECONNECT_ATTEMPTS) {
                Log.d(TAG, "Attempting reconnection " + reconnectAttempts);
                connect();
            }
        }, RECONNECT_DELAY_MS);
    }

    /**
     * Reset reconnection attempts (call when manually reconnecting)
     */
    public void resetReconnectAttempts() {
        reconnectAttempts = 0;
        shouldReconnect = true;
    }

    /**
     * Clean up resources
     */
    public void cleanup() {
        shouldReconnect = false;
        backgroundHandler.post(() -> {
            if (webSocketClient != null) {
                try {
                    webSocketClient.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error during cleanup", e);
                }
            }
            stopScheduler();
        });
    }
    private void sendNow(JSONObject message) {
        try {
            String jsonMessage = message.toString();
            int len = jsonMessage != null ? jsonMessage.length() : 0;
            Log.d(TAG, "Sending message (" + len + " chars): " + jsonMessage);
            if (webSocketClient != null && isConnected) {
                webSocketClient.send(jsonMessage);
            } else {
                Log.w(TAG, "WebSocket not connected. Dropping immediate send.");
            }
        } catch (Exception e) {
            Log.e(TAG, "sendNow failed", e);
            mainHandler.post(() -> {
                if (listener != null) {
                    listener.onError(e);
                }
            });
        }
    }

    private void stopScheduler() {
        try {
            if (transportScheduler != null) {
                transportScheduler.stop();
            }
        } catch (Exception ignored) { }
    }
}
