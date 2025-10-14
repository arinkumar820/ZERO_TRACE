# PIN Authentication Unlock Guide

## Overview
Your Bisto Chat app has a 6-digit PIN authentication system with 2-attempt limit. After 2 failed attempts, the app gets permanently locked. Here are all the ways to unlock it:

## 🔓 Unlock Methods

### 1. **Emergency Button (Original Method)**
- When app is locked, press the **Emergency button** 
- You'll see: "🚨 Admin Emergency Access. Press and hold for 5 seconds to unlock"
- **Long press and hold** the emergency button for 5 seconds
- App will restart and be unlocked

### 2. **Master PIN System (NEW)**
The app now supports master PINs that always work, even when locked:

**Default Master PINs:**
- `999999` - Default admin PIN
- `112233` - Emergency code

**To use:**
- Enter the master PIN like a normal PIN (even when locked)
- App will automatically unlock and reset

**To set custom master PIN:**
```java
// Add this to your app initialization
AdminUnlockUtility.setupMasterPin(context, "your_custom_pin");
```

### 3. **Secret Gesture Unlock (NEW)**
- When app is locked, **tap 7 times** on the "🔒 APPLICATION LOCKED" text
- You'll get progress hints at taps 3 and 5
- After 7 taps: automatic unlock and restart

### 4. **Command Line Tool (NEW)**
Use the provided `emergency_unlock.bat` script:

**Requirements:**
- Android device connected via USB
- USB Debugging enabled
- ADB installed

**Usage:**
```cmd
# Run the script
emergency_unlock.bat

# Select option:
# 1. Emergency unlock (reset to default PIN 123456)
# 2. Reset attempts counter only  
# 3. Factory reset (clear all PIN data)
# 4. Show current lock status
# 5. Kill app process
```

### 5. **Manual ADB Commands**
For advanced users:
```bash
# Reset PIN preferences
adb shell "run-as com.sameetasadullah.i180479_180531 sh -c 'rm -f /data/data/com.sameetasadullah.i180479_180531/shared_prefs/pin_preferences.xml'"

# Force stop app
adb shell am force-stop com.sameetasadullah.i180479_180531
```

### 6. **Programmatic Unlock**
Use the AdminUnlockUtility class in your code:

```java
// Emergency unlock
AdminUnlockUtility.emergencyUnlock(context);

// Reset attempts only
AdminUnlockUtility.resetAttempts(context);

// Check lock status
boolean isLocked = AdminUnlockUtility.isAppLocked(context);

// Factory reset
AdminUnlockUtility.factoryReset(context);
```

## 🔧 Developer Options

### Changing Master PINs
Edit `AdminUnlockUtility.java`:
```java
private static final String DEFAULT_MASTER_PIN = "your_pin";
private static final String EMERGENCY_CODE = "your_emergency_code";
```

### Changing Secret Gesture
Edit `PinAuthenticationActivity.java`:
```java
private static final int SECRET_TAP_COUNT = 7; // Change tap count
private static final long TAP_TIMEOUT = 2000; // Change timeout
```

### Adding More Unlock Methods
1. Extend `AdminUnlockUtility` class
2. Add new methods to `PinAuthenticationActivity`
3. Update this documentation

## 📊 Current Status Check

To check the current lock status, use:
```java
String info = AdminUnlockUtility.getDebugInfo(context);
Log.d("PIN_STATUS", info);
```

## 🚨 Emergency Situations

**If all methods fail:**
1. Uninstall and reinstall the app
2. Clear app data from Android Settings
3. Use Android's Application Manager to force stop and clear data

## 🛡️ Security Notes

- Master PINs are stored in SharedPreferences
- Secret gesture requires physical access to device
- All unlock methods log their usage for security audit
- Emergency unlock resets PIN to default `123456`

## 🔄 Testing Unlock Methods

1. **Test the lock:** Enter wrong PIN twice to lock the app
2. **Test master PIN:** Try entering `999999` or `112233`
3. **Test secret gesture:** Tap 7 times on locked screen
4. **Test emergency button:** Long press the emergency button
5. **Test ADB tool:** Use the batch script

---

**Default Credentials After Unlock:**
- PIN: `123456`
- Attempts: Reset to 0
- Status: Unlocked

Remember to change the default PIN after unlocking!