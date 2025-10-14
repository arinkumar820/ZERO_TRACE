# Custom Timer Usage Examples

## Available Timer Constants

You now have easy access to all timer constants directly in ChatActivity:

```java
// Timer Constants (in milliseconds)
private static final long TIMER_30_SECONDS = 30000;    // 30 seconds
private static final long TIMER_45_SECONDS = 45000;    // 45 seconds  
private static final long TIMER_1_MINUTE = 60000;      // 1 minute
private static final long TIMER_3_MINUTES = 180000;    // 3 minutes
private static final long TIMER_5_MINUTES = 300000;    // 5 minutes

// Current timer (can be set to any of the above)
private long currentDisappearingTimer = TIMER_1_MINUTE; // Default to 1 minute
```

## How to Use the Custom Timer Methods

### 1. Setting Timer Values

You can programmatically set the timer to any of the predefined values:

```java
// Set timer to 30 seconds
setTimer30Seconds();

// Set timer to 45 seconds  
setTimer45Seconds();

// Set timer to 1 minute
setTimer1Minute();

// Set timer to 3 minutes
setTimer3Minutes();

// Set timer to 5 minutes
setTimer5Minutes();
```

### 2. Checking Current Timer

You can check which timer is currently selected:

```java
if (isTimer30Seconds()) {
    Log.d(TAG, "Current timer is 30 seconds");
}

if (isTimer1Minute()) {
    Log.d(TAG, "Current timer is 1 minute");
}

if (isTimer5Minutes()) {
    Log.d(TAG, "Current timer is 5 minutes");
}

// Get current timer display name
String timerName = getCurrentTimerDisplayName(); // e.g., "30 seconds", "1 minute"
```

### 3. Sending Messages with Specific Timers

You can send messages with specific timers without changing the current default:

```java
// Send a message that disappears in 30 seconds
sendMessageWith30SecondTimer("This message disappears in 30 seconds!");

// Send a message that disappears in 45 seconds
sendMessageWith45SecondTimer("This message disappears in 45 seconds!");

// Send a message that disappears in 1 minute
sendMessageWith1MinuteTimer("This message disappears in 1 minute!");

// Send a message that disappears in 3 minutes
sendMessageWith3MinuteTimer("This message disappears in 3 minutes!");

// Send a message that disappears in 5 minutes
sendMessageWith5MinuteTimer("This message disappears in 5 minutes!");
```

### 4. Example Usage Scenarios

#### Scenario 1: Quick Setup for Different Users
```java
// For privacy-focused users
setTimer30Seconds();  // All messages disappear quickly

// For normal conversations  
setTimer3Minutes();   // Good balance

// For longer discussions
setTimer5Minutes();   // More time to read
```

#### Scenario 2: Context-Specific Messaging
```java
// Sending sensitive information
sendMessageWith30SecondTimer("Here's the confidential data...");

// Regular chat continues with current timer
sendMessage(); // Uses whatever timer is currently set

// Important information that needs more time
sendMessageWith5MinuteTimer("Please read this important announcement carefully...");
```

#### Scenario 3: Dynamic Timer Selection
```java
// Based on message content or user preference
String messageText = messageEditText.getText().toString();

if (messageText.contains("confidential") || messageText.contains("secret")) {
    // Sensitive messages disappear quickly
    sendMessageWith30SecondTimer(messageText);
} else if (messageText.length() > 200) {
    // Long messages get more time to be read
    sendMessageWith5MinuteTimer(messageText);
} else {
    // Regular messages use current setting
    sendMessage();
}
```

## Benefits of This Approach

### ✅ **Easy to Use**
```java
// Instead of writing:
currentDisappearingTimer = DisappearingMessageSettings.TIMER_30_SECONDS;

// You can simply write:
setTimer30Seconds();
```

### ✅ **Type Safety**
```java
// Instead of using magic numbers:
if (currentDisappearingTimer == 30000) { ... }

// Use readable constants:
if (isTimer30Seconds()) { ... }
```

### ✅ **Flexibility** 
```java
// You can mix and match:
setTimer1Minute();                    // Set default to 1 minute
sendMessageWith30SecondTimer("Hi");   // But send this specific message with 30 seconds
sendMessage();                        // Next message uses 1 minute again
```

### ✅ **Clear Intent**
```java
// Code is self-documenting:
if (isTimer30Seconds()) {
    // Show warning about quick disappearing
    Toast.makeText(this, "⚡ Messages will disappear quickly!", Toast.LENGTH_SHORT).show();
}
```

## Implementation Details

All the convenience methods automatically:
1. ✅ Set the timer value
2. ✅ Update the UI display  
3. ✅ Update button appearance
4. ✅ Log the change for debugging

```java
public void setTimer30Seconds() {
    currentDisappearingTimer = TIMER_30_SECONDS;    // Set the timer
    updateTimerStatusDisplay();                     // Update UI text
    updateTimerButtonAppearance();                  // Update button
    Log.d(TAG, "Timer set to 30 seconds");        // Debug logging
}
```

This makes it super easy to customize your disappearing message timers programmatically!