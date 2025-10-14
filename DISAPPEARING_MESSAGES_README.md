# Disappearing Messages Feature

## Overview
This chat application now includes a comprehensive disappearing messages feature where ALL messages automatically disappear after a specified time period. Users can choose from 5 different time intervals, but all messages will disappear - there is no option for permanent messages.

## Features

### ⏱️ Customizable Timer Options
- **30 seconds** - For very sensitive information
- **45 seconds** - Quick sensitive messages
- **1 minute** - Brief temporary messages  
- **3 minutes** - Short-term discussions
- **5 minutes** - Medium-term conversations

### 🎛️ User Controls
- **Timer Button**: Tap the timer button (⏱️) next to the message input to select disappearing time for the next message
- **Timer Status**: Visual indicator showing current timer setting above the message input
- **Per-Message Control**: Each message can have its own disappearing timer
- **Visual Feedback**: Timer button changes appearance based on current setting

### 📊 Message Display
- **Countdown Display**: Shows remaining time for disappearing messages (can be toggled in settings)
- **Dynamic Icons**: Different icons based on remaining time (⏱️ → ⚡ → 🔥 → 💨)
- **Color Coding**: Countdown text changes color as expiration approaches
- **Disappeared State**: Shows "💨 This message has disappeared" when expired

### ⚙️ Settings & Preferences
- **Default Timer**: Set your preferred default disappearing time
- **Show Countdown**: Toggle countdown display on/off
- **Auto-Enable**: Automatically enable disappearing messages for all new messages
- **Persistent Settings**: All preferences are saved and restored between app sessions

## Implementation Details

### Core Classes

#### 1. `DisappearingMessageSettings`
- Manages user preferences and timer presets
- Uses SharedPreferences for persistence
- Provides utility methods for time formatting and display

#### 2. `DisappearingMessageManager`
- Handles countdown timers and UI updates
- Manages message cleanup and expiration
- Provides callback interface for adapter updates

#### 3. `WebSocketMessage` (Enhanced)
- Added disappearing timer support
- Custom timing instead of hardcoded values
- Better timestamp tracking with `createdAt` field

#### 4. `ChatMessageAdapter` (Enhanced)
- Real-time countdown display
- Visual feedback for disappearing messages
- Automatic UI updates when messages expire

#### 5. `ChatActivity` (Enhanced)
- Timer selection dialog
- Visual timer status display
- Integration with disappearing message system

#### 6. `DisappearingMessageSettingsActivity`
- Dedicated settings screen
- User-friendly configuration interface
- Real-time setting updates

### Technical Features
- **Memory Efficient**: Uses weak references and cleanup to prevent memory leaks
- **Thread Safe**: All UI updates happen on main thread
- **Performance Optimized**: Efficient countdown updates (1s intervals, 250ms when < 10s remaining)
- **Backward Compatible**: Fallback constructor maintains compatibility with existing code

## Usage

### For End Users

1. **Setting a Timer for a Message**:
   - Tap the timer button (⏱️) next to the message input
   - Select desired disappearing time from the dialog (30s, 45s, 1min, 3min, or 5min)
   - Type your message and send - **the message will disappear after YOUR selected time**
   - The timer status will show your current setting
   - Each message uses the timer that was selected when it was sent

2. **Configuring Default Settings**:
   - Open the disappearing messages settings (if integrated into main app settings)
   - Set your preferred default timer
   - Toggle countdown display and auto-enable options

3. **Viewing Disappearing Messages**:
   - Messages show countdown timer (if enabled)
   - Timer changes color and icon as expiration approaches
   - Messages show "disappeared" placeholder after expiration

### For Developers

1. **Creating Disappearing Messages**:
```java
// Create message with custom timer
WebSocketMessage message = WebSocketMessage.createDisappearingMessage(
    senderUid, senderEmail, messageText, DisappearingMessageSettings.TIMER_3_MINUTES
);

// Or use the disappearing manager
DisappearingMessageManager manager = new DisappearingMessageManager(context, listener);
WebSocketMessage message = manager.createMessageWithTimer(
    senderUid, senderEmail, messageText, customTimer
);
```

2. **Integrating with RecyclerView**:
```java
// Use enhanced adapter constructor
ChatMessageAdapter adapter = new ChatMessageAdapter(messageList, currentUserId, context);

// Notify when message list changes
adapter.notifyMessageListChanged();
```

3. **Managing Settings**:
```java
DisappearingMessageSettings settings = DisappearingMessageSettings.getInstance(context);
settings.setDefaultTimer(DisappearingMessageSettings.TIMER_1_MINUTE);
settings.setShowCountdown(true);
```

## Security Considerations

- Messages are only hidden in the UI - consider server-side expiration for true security
- Local storage cleanup may be needed for complete message removal
- Network messages should include expiration metadata for cross-device sync

## Future Enhancements

- Server-side message expiration
- Screenshot detection with disappearing message alerts  
- Read-once messages that disappear after being viewed
- Bulk timer setting for multiple messages
- Timer synchronization across devices

## Files Added/Modified

### New Files:
- `DisappearingMessageSettings.java` - Settings management
- `DisappearingMessageManager.java` - Timer and cleanup management  
- `DisappearingMessageSettingsActivity.java` - Settings UI
- `activity_disappearing_message_settings.xml` - Settings layout
- `timer_button_background.xml` - Timer button drawable
- `ic_timer.xml` - Timer icon drawable
- `setting_item_background.xml` - Settings item background

### Modified Files:
- `WebSocketMessage.java` - Added disappearing message support
- `ChatMessageAdapter.java` - Enhanced with countdown display
- `ChatActivity.java` - Added timer controls and settings
- `activity_chat.xml` - Added timer button and status display
- `item_message_sent.xml` - Already had countdown_text field
- `item_message_received.xml` - Already had countdown_text field

## Installation

1. Copy all new files to their respective directories
2. Ensure all modified files are updated
3. The feature is ready to use - no additional configuration required
4. Users can access settings through the timer button or dedicated settings activity

---

**Note**: This feature enhances privacy and security by allowing temporary conversations, but remember that true security requires server-side implementation of message expiration.