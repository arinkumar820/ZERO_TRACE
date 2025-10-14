# 🧪 Firebase Admin Approval System - Testing Guide

## Prerequisites
✅ App installed on device/emulator: **DONE**
✅ Firebase project configured with google-services.json
✅ Firebase Authentication enabled (Email/Password)
✅ Firebase Realtime Database created

---

## 🔥 **Firebase Setup Verification**

### Step 1: Verify Firebase Configuration
1. Open your [Firebase Console](https://console.firebase.google.com/)
2. Select your Bisto Chat project
3. Check these services are enabled:
   - **Authentication** → Sign-in method → Email/Password ✅
   - **Realtime Database** → Created and rules configured ✅

### Step 2: Check google-services.json
Verify the file exists at:
```
app/google-services.json
```

---

## 🧪 **Test Case 1: New User Registration**

### What Should Happen:
1. User registers → Account created with `status="pending"`
2. User immediately signed out
3. User **CANNOT** login until admin approval

### Steps to Test:
1. **Launch the app** on your emulator
2. **Navigate to Registration** (should be available from login screen)
3. **Fill registration form:**
   - Full Name: `Test User`
   - Email: `testuser@example.com`
   - Password: `password123`
   - Confirm Password: `password123`
4. **Tap "Register"**

### ✅ Expected Results:
- [ ] Loading indicator appears during registration
- [ ] Success message: *"Registration successful! Your account is pending admin approval..."*
- [ ] User automatically redirected to login screen
- [ ] User is signed out (cannot access app)

### 🔍 **Verify in Firebase Console:**
1. Go to **Realtime Database**
2. Look for `users` → `{user_uid}` → Check:
   ```json
   {
     "uid": "firebase_user_uid",
     "email": "testuser@example.com", 
     "displayName": "Test User",
     "status": "pending",  ← Should be "pending"
     "registrationTimestamp": 1234567890
   }
   ```

---

## 🧪 **Test Case 2: Pending User Login Attempt**

### What Should Happen:
- Login attempt **FAILS**
- User signed out with message
- Access denied

### Steps to Test:
1. **Try to login** with the newly registered user:
   - Email: `testuser@example.com`
   - Password: `password123`
2. **Tap "Login"**

### ✅ Expected Results:
- [ ] Loading indicator appears
- [ ] Authentication succeeds (Firebase Auth works)
- [ ] App checks status in database
- [ ] User signed out immediately
- [ ] Message: *"Your account is pending admin approval. Please wait for approval..."*
- [ ] User stays on login screen

---

## 🧪 **Test Case 3: Admin Approval Process**

### Steps to Approve User:
1. **Open Firebase Console**
2. **Navigate to Realtime Database**
3. **Find the user:** `users` → `{user_uid}`
4. **Click on the status field** showing `"pending"`
5. **Change it to:** `"approved"`
6. **Press Enter** or click ✓ to save

### 🔍 **Verification in Firebase:**
The status should now show:
```json
{
  "status": "approved"  ← Changed from "pending"
}
```

---

## 🧪 **Test Case 4: Approved User Login**

### What Should Happen:
- Login succeeds
- User accesses MainActivity
- Last login timestamp updated

### Steps to Test:
1. **Return to the app**
2. **Login again** with the same credentials:
   - Email: `testuser@example.com`
   - Password: `password123`
3. **Tap "Login"**

### ✅ Expected Results:
- [ ] Loading indicator appears
- [ ] Authentication succeeds
- [ ] Status check passes (`status="approved"`)
- [ ] Welcome message: *"Welcome back, Test User!"*
- [ ] User accesses MainActivity/fragmentsContainer
- [ ] Login successful!

### 🔍 **Verify in Firebase Console:**
Check that `lastLoginTimestamp` was updated:
```json
{
  "status": "approved",
  "lastLoginTimestamp": 1234567890  ← Should be updated
}
```

---

## 🧪 **Test Case 5: Rejected User (Optional)**

### Steps to Test:
1. **Create another test user** or change existing user status to `"rejected"`
2. **Try to login** with rejected user

### ✅ Expected Results:
- [ ] Login fails
- [ ] Message: *"Your account has been rejected by the administrator..."*

---

## 🔧 **Troubleshooting Common Issues**

### Issue 1: Firebase Not Connected
**Symptoms:** App crashes or authentication fails
**Solution:**
1. Check `google-services.json` is in `app/` folder
2. Verify Firebase project configuration
3. Check internet connection

### Issue 2: Database Rules Error
**Symptoms:** Permission denied errors
**Fix Database Rules:**
```json
{
  "rules": {
    "users": {
      "$uid": {
        ".read": "auth != null && auth.uid == $uid",
        ".write": "auth != null && auth.uid == $uid && !data.exists()",
        "status": {
          ".write": false
        }
      }
    }
  }
}
```

### Issue 3: User Not Found in Database
**Symptoms:** "User profile not found" error
**Solution:** Check if user registration completed successfully in Firebase Console

---

## 📱 **Real Device Testing**

### For Physical Android Device:
1. **Enable Developer Options:**
   - Settings → About Phone → Tap "Build number" 7 times
   - Settings → Developer Options → USB Debugging ✅

2. **Connect device via USB**

3. **Install on device:**
   ```bash
   .\gradlew installDebug
   ```

4. **Repeat all test cases** on physical device

---

## 📊 **Success Criteria Checklist**

### Registration Flow:
- [ ] User can register successfully
- [ ] Profile created with `status="pending"`
- [ ] User immediately signed out
- [ ] Cannot login before approval

### Login Flow - Pending:
- [ ] Firebase Auth succeeds
- [ ] Status check works
- [ ] User signed out with pending message

### Admin Approval:
- [ ] Can change status in Firebase Console
- [ ] Status updates are saved

### Login Flow - Approved:
- [ ] Authentication succeeds
- [ ] Status check passes
- [ ] User accesses main app
- [ ] Last login timestamp updated

### Security:
- [ ] Pending users cannot access app
- [ ] Rejected users cannot access app
- [ ] Only approved users get full access

---

## 🐛 **Debug Information**

### View App Logs:
```bash
# View Android logs
adb logcat -s "LoginActivity" "RegistrationActivity" "UserProfile"
```

### Firebase Console Locations:
1. **Authentication:** Users tab - see all registered users
2. **Database:** Data tab - see user profiles and status
3. **Usage:** Check authentication and database usage

---

## 📞 **Need Help?**

### Common Log Messages:
- `"Firebase Auth registration successful"` ✅ Registration working
- `"User signed out after registration"` ✅ Auto-signout working  
- `"User status: pending"` ✅ Status check working
- `"Proceeding to MainActivity"` ✅ Approval flow working

### If Tests Fail:
1. Check Firebase Console for error messages
2. Verify internet connection
3. Confirm Firebase configuration
4. Check Android logs for detailed errors

---

**🎉 If all test cases pass, your Firebase Admin Approval System is working perfectly!**