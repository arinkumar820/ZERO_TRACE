# Bisto Chat Approval System - Testing & Validation Guide

This comprehensive guide walks you through testing the complete admin approval system for Bisto Chat.

## Prerequisites

Before testing, ensure:
- Firebase project is set up (`bisto-chat-3b33a`)
- Android app is built and installed
- Firebase Console access is available
- PHP backend server is running (optional, for message testing)

## Test Overview

The approval system has these key components:
1. **Registration** → User registers, gets `status: "pending"`, is signed out
2. **Login Blocking** → Pending/rejected users cannot login
3. **Admin Approval** → Status changed manually in Firebase Console
4. **Access Granted** → Approved users can login and use app
5. **Backend Verification** → PHP script verifies status before message operations

---

## Test Case 1: New User Registration Flow

### Objective
Verify that new user registration creates pending account and blocks login.

### Steps

#### 1.1 Register New User
1. Open Bisto Chat app
2. Tap "Register here" link from login screen
3. Fill registration form:
   - **Full Name**: `Test User One`
   - **Email**: `testuser1@example.com`
   - **Password**: `password123`
   - **Confirm Password**: `password123`
4. Tap "Register" button

#### 1.2 Expected Results
✅ **Registration Success Messages**:
- Progress bar shows during registration
- Success toast: "Registration successful! Your account is pending admin approval. You will be able to login once approved."
- App redirects to LoginActivity after 2 seconds
- Registration form is cleared

✅ **Firebase Console Verification**:
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select project: `bisto-chat-3b33a`
3. Navigate to Realtime Database
4. Check `/users` node
5. Find user with email `testuser1@example.com`
6. Verify user data structure:
```json
{
  "uid": "firebase_generated_uid",
  "email": "testuser1@example.com",
  "displayName": "Test User One",
  "status": "pending",
  "registrationTimestamp": 1697123456789,
  "lastLoginTimestamp": 0,
  "profileImageUrl": "",
  "phoneNumber": ""
}
```

#### 1.3 Test Login Blocking
1. On login screen, enter:
   - **Email**: `testuser1@example.com`
   - **Password**: `password123`
2. Tap "Login" button

✅ **Expected Blocking Behavior**:
- Progress bar shows briefly
- User is signed out immediately
- Toast message: "Your account is pending admin approval. Please wait for approval before logging in."
- User remains on login screen
- No access to MainActivity

---

## Test Case 2: Admin Approval Process

### Objective
Verify manual approval process via Firebase Console.

### Steps

#### 2.1 Locate Pending User
1. In Firebase Console, navigate to Realtime Database
2. Expand `/users` node
3. Find user: `testuser1@example.com` (from Test Case 1)
4. Verify `status: "pending"`

#### 2.2 Approve User
1. Click on the user's UID
2. Locate `status` field
3. Click on `"pending"` value
4. Change to `"approved"`
5. Click checkmark (✓) to save

✅ **Expected Console Behavior**:
- Status field updates to `"approved"`
- Change is saved immediately
- No errors occur

#### 2.3 Test Approved Login
1. Return to mobile app
2. On login screen, enter:
   - **Email**: `testuser1@example.com`  
   - **Password**: `password123`
3. Tap "Login" button

✅ **Expected Success Behavior**:
- Progress bar shows
- Login succeeds
- Welcome toast: "Welcome back, Test User One!"
- App navigates to MainActivity/fragmentsContainer
- User can access all app features

#### 2.4 Verify Database Updates
1. Check Firebase Console `/users/[uid]` node
2. Verify `lastLoginTimestamp` field was updated
3. Timestamp should be recent (within last few minutes)

---

## Test Case 3: User Rejection Flow

### Objective
Verify user rejection blocks access and shows appropriate messages.

### Steps

#### 3.1 Register Second Test User
1. Follow Test Case 1 steps with different credentials:
   - **Full Name**: `Test User Two`
   - **Email**: `testuser2@example.com`
   - **Password**: `password456`

#### 3.2 Reject User in Console
1. In Firebase Console, find `testuser2@example.com`
2. Change `status` from `"pending"` to `"rejected"`
3. Save changes

#### 3.3 Test Rejected Login
1. On app login screen, enter:
   - **Email**: `testuser2@example.com`
   - **Password**: `password456`
2. Tap "Login" button

✅ **Expected Rejection Behavior**:
- Progress bar shows briefly
- User is signed out immediately
- Toast message: "Your account has been rejected by the administrator. Please contact support if you believe this is an error."
- User remains on login screen
- No access to MainActivity

---

## Test Case 4: Status Change Validation

### Objective
Test transitions between all status states.

### Steps

#### 4.1 Pending → Approved → Rejected → Approved
1. Start with user in `"pending"` status
2. Test login (should fail)
3. Change to `"approved"` in Console
4. Test login (should succeed)
5. Sign out from app
6. Change to `"rejected"` in Console  
7. Test login (should fail with rejection message)
8. Change to `"approved"` in Console
9. Test login (should succeed again)

✅ **Expected Behavior**:
- Each status change immediately affects login ability
- Appropriate messages shown for each status
- No app crashes or authentication errors

---

## Test Case 5: PHP Backend Verification

### Objective
Verify PHP backend correctly validates user status before allowing operations.

### Prerequisites
- PHP backend server running
- MySQL database configured
- `firebase_verification.php` script accessible

### Steps

#### 5.1 Test with Approved User
1. Get Firebase UID of approved user from Test Case 2
2. Send POST request to PHP script:
```bash
curl -X POST http://your-server.com/firebase_verification.php \
  -H "Content-Type: application/json" \
  -d '{
    "action": "verify_user",
    "firebase_uid": "user_uid_from_firebase"
  }'
```

✅ **Expected Response** (Approved User):
```json
{
  "success": true,
  "user": {
    "uid": "user_uid",
    "email": "testuser1@example.com",
    "displayName": "Test User One",
    "status": "approved"
  },
  "message": "User is approved for operations"
}
```

#### 5.2 Test with Pending User
1. Get Firebase UID of pending user
2. Send same verification request

✅ **Expected Response** (Pending User):
```json
{
  "success": false,
  "error": "User account is pending admin approval",
  "code": "USER_PENDING"
}
```

#### 5.3 Test with Rejected User
1. Get Firebase UID of rejected user
2. Send same verification request

✅ **Expected Response** (Rejected User):
```json
{
  "success": false,
  "error": "User account has been rejected",
  "code": "USER_REJECTED"
}
```

#### 5.4 Test Message Sending
1. Using approved user UID, send message:
```bash
curl -X POST http://your-server.com/firebase_verification.php \
  -H "Content-Type: application/json" \
  -d '{
    "action": "send_message",
    "firebase_uid": "approved_user_uid",
    "room_id": "test_room",
    "message_content": "Test message from approved user"
  }'
```

✅ **Expected Response** (Success):
```json
{
  "success": true,
  "message_id": 123,
  "message": "Message sent successfully"
}
```

#### 5.5 Test Message Blocking
1. Using pending/rejected user UID, attempt to send message
2. Should receive error response preventing message insertion

---

## Test Case 6: Edge Cases & Error Handling

### Objective
Test system robustness with edge cases and error conditions.

### 6.1 Invalid Email Registration
1. Try registering with invalid email: `invalid-email`
2. Should show validation error, prevent registration

### 6.2 Duplicate Email Registration
1. Try registering with existing email
2. Should show Firebase error about existing account

### 6.3 Weak Password Registration
1. Try registering with password: `123`
2. Should show validation error about minimum length

### 6.4 Network Interruption
1. Start registration process
2. Disable network during registration
3. Should show appropriate error message

### 6.5 Malformed Database Entry
1. In Firebase Console, manually create user with missing `status` field
2. Try to login with this user
3. Should handle gracefully, default to pending behavior

### 6.6 Non-existent User Login
1. Try logging in with non-registered email
2. Should show Firebase authentication error

---

## Test Case 7: UI/UX Validation

### Objective
Verify user interface behaves correctly throughout approval process.

### 7.1 Visual Feedback Testing
✅ **Registration Screen**:
- Progress bar shows/hides correctly
- Input fields disabled during registration
- Success message is clearly visible
- Warning message about approval is prominent

✅ **Login Screen**:
- Progress bar shows/hides correctly
- Input fields disabled during login attempt
- Error messages are clear and actionable
- Info card about approval system is visible

### 7.2 Navigation Testing
✅ **Registration Flow**:
- Registration → Login redirect works
- Back button behavior is appropriate
- App doesn't crash on screen rotation

✅ **Login Flow**:
- Failed login keeps user on login screen
- Successful login navigates to main app
- "Register here" link works correctly

---

## Test Case 8: Performance & Scale Testing

### Objective
Test system performance with multiple users and operations.

### 8.1 Multiple User Registration
1. Register 5-10 users quickly in succession
2. Check Firebase Console for all users
3. Verify each has correct pending status

### 8.2 Concurrent Login Attempts
1. Have multiple users attempt login simultaneously
2. Verify each gets appropriate response based on status
3. No database corruption or race conditions

### 8.3 Rapid Status Changes
1. Quickly change user status back and forth in Console
2. Test login attempts during changes
3. Verify system handles concurrent access correctly

---

## Automated Testing Script

### Basic Test Automation
Create automated tests for critical paths:

```java
// Example Android Test
@Test
public void testRegistrationCreatessPendingUser() {
    // Register new user
    onView(withId(R.id.et_full_name)).perform(typeText("Test User"));
    onView(withId(R.id.et_email)).perform(typeText("test@example.com"));
    onView(withId(R.id.et_password)).perform(typeText("password123"));
    onView(withId(R.id.et_confirm_password)).perform(typeText("password123"));
    onView(withId(R.id.btn_register)).perform(click());
    
    // Verify redirect to login
    onView(withId(R.id.btn_login)).check(matches(isDisplayed()));
    
    // Verify toast message
    onView(withText(containsString("pending admin approval")))
        .inRoot(withDecorView(not(is(activityRule.getActivity().getWindow().getDecorView()))))
        .check(matches(isDisplayed()));
}

@Test 
public void testPendingUserCannotLogin() {
    // Attempt login with pending user
    onView(withId(R.id.et_email)).perform(typeText("pending@example.com"));
    onView(withId(R.id.et_password)).perform(typeText("password123"));
    onView(withId(R.id.btn_login)).perform(click());
    
    // Should remain on login screen
    onView(withId(R.id.btn_login)).check(matches(isDisplayed()));
    
    // Should show pending message
    onView(withText(containsString("pending admin approval")))
        .inRoot(withDecorView(not(is(activityRule.getActivity().getWindow().getDecorView()))))
        .check(matches(isDisplayed()));
}
```

---

## Validation Checklist

Before deploying, verify all these items:

### ✅ Registration Process
- [ ] New users get `status: "pending"`
- [ ] User is signed out after registration
- [ ] Success message is shown
- [ ] User data structure is correct in Firebase
- [ ] Registration form validation works
- [ ] Firebase Auth profile is updated

### ✅ Login Blocking
- [ ] Pending users cannot login
- [ ] Rejected users cannot login
- [ ] Appropriate messages shown for each status
- [ ] User remains signed out after failed login
- [ ] No access to protected screens

### ✅ Admin Approval
- [ ] Status can be changed in Firebase Console
- [ ] Changes take effect immediately
- [ ] Approved users can login successfully
- [ ] `lastLoginTimestamp` updates correctly

### ✅ UI/UX
- [ ] Progress indicators work correctly
- [ ] Error messages are clear and helpful
- [ ] Navigation flows work as expected
- [ ] App doesn't crash during any process
- [ ] Screen rotation handling works

### ✅ Backend Integration
- [ ] PHP script correctly verifies user status
- [ ] Only approved users can send messages
- [ ] Error responses are properly formatted
- [ ] Database operations are secure

### ✅ Security
- [ ] Status field cannot be modified by app users
- [ ] Pending/rejected users have no app access
- [ ] Firebase security rules are properly configured
- [ ] No sensitive data exposed in logs

## Common Issues & Solutions

### Issue: User not found in database after registration
**Cause**: Registration process failed after Firebase Auth creation
**Solution**: Delete Firebase Auth user and re-register, or manually create profile in Console

### Issue: Login succeeds for pending user
**Cause**: Status check logic error or database sync issue
**Solution**: Check LoginActivity logic, verify Firebase rules, restart app

### Issue: PHP script returns "User not found"
**Cause**: Incorrect Firebase UID or database path
**Solution**: Verify UID from Firebase Console, check PHP script configuration

### Issue: Status changes don't take effect
**Cause**: App caching or authentication state issues  
**Solution**: Restart app, check Firebase connection, verify Console changes saved

---

## Success Criteria

The approval system is working correctly when:

1. ✅ **Registration Flow**: Users register → get pending status → are signed out → cannot login
2. ✅ **Approval Flow**: Admin changes status → users can immediately login → access is granted
3. ✅ **Rejection Flow**: Rejected users cannot login → appropriate messages shown
4. ✅ **Backend Security**: Only approved users can perform operations via PHP backend
5. ✅ **UI/UX**: All interactions are smooth, error-free, and provide clear feedback
6. ✅ **Data Integrity**: Firebase database maintains correct user states and timestamps

## Deployment Readiness

System is ready for production when all test cases pass and:
- [ ] Load testing completed successfully
- [ ] Security review passed
- [ ] Admin training completed
- [ ] Backup and recovery procedures tested
- [ ] Monitoring and alerts configured

---

*Last Updated: December 2024*
*Version: 1.0*