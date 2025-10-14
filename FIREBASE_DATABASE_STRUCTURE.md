# Firebase Realtime Database Structure - Bisto Chat with Approval System

This document outlines the exact Firebase Realtime Database structure for the Bisto Chat application with the admin approval system.

## Project Configuration

- **Project ID**: `bisto-chat-3b33a`
- **Database URL**: `https://bisto-chat-3b33a-default-rtdb.firebaseio.com/`
- **Package Name**: `com.sameetasadullah.i180479_180531`

## Database Structure

### Root Level Structure
```
bisto-chat-3b33a-default-rtdb/
├── users/
│   ├── {userId1}/
│   ├── {userId2}/
│   └── ...
├── chatRooms/ (optional)
├── messages/ (optional)
└── adminSettings/ (optional)
```

## Users Node Structure

### Path: `/users/{userId}`

Each user document contains the following fields:

```json
{
  "users": {
    "firebase_user_uid_here": {
      "uid": "firebase_user_uid_here",
      "email": "user@example.com",
      "displayName": "John Doe",
      "status": "pending",
      "registrationTimestamp": 1697123456789,
      "lastLoginTimestamp": 1697234567890,
      "profileImageUrl": "",
      "phoneNumber": "+1234567890"
    }
  }
}
```

### User Object Fields

| Field | Type | Required | Description | Possible Values |
|-------|------|----------|-------------|-----------------|
| `uid` | String | Yes | Firebase Authentication UID | Firebase generated UID |
| `email` | String | Yes | User's email address | Valid email format |
| `displayName` | String | Yes | User's full name | Any string (2+ characters) |
| `status` | String | Yes | Account approval status | `"pending"`, `"approved"`, `"rejected"` |
| `registrationTimestamp` | Long | Yes | Account creation time | Unix timestamp in milliseconds |
| `lastLoginTimestamp` | Long | No | Last successful login time | Unix timestamp in milliseconds |
| `profileImageUrl` | String | No | URL to profile picture | URL string or empty |
| `phoneNumber` | String | No | User's phone number | Phone number or empty |

## Status Field Details

### Status Values and Behavior

#### 1. `"pending"` (Default)
- **Description**: New user accounts waiting for admin approval
- **User Access**: Cannot login to the app
- **Behavior**: Login attempts result in immediate sign-out with message
- **Toast Message**: "Your account is pending admin approval"

#### 2. `"approved"`
- **Description**: Admin-approved users with full access
- **User Access**: Can login and use all app features
- **Behavior**: Normal app functionality enabled
- **Toast Message**: "Welcome back, [Name]!"

#### 3. `"rejected"`
- **Description**: Admin-rejected users blocked from access
- **User Access**: Cannot login to the app
- **Behavior**: Login attempts result in immediate sign-out with message
- **Toast Message**: "Your account has been rejected by the administrator"

## Sample Database Content

### Example 1: Pending User
```json
{
  "users": {
    "AbCdEfGhIjKlMnOpQrSt": {
      "uid": "AbCdEfGhIjKlMnOpQrSt",
      "email": "john.doe@example.com",
      "displayName": "John Doe",
      "status": "pending",
      "registrationTimestamp": 1697123456789,
      "lastLoginTimestamp": 0,
      "profileImageUrl": "",
      "phoneNumber": "+1234567890"
    }
  }
}
```

### Example 2: Approved User
```json
{
  "users": {
    "XyZaBcDeFgHiJkLmNoP": {
      "uid": "XyZaBcDeFgHiJkLmNoP",
      "email": "jane.smith@example.com", 
      "displayName": "Jane Smith",
      "status": "approved",
      "registrationTimestamp": 1697023456789,
      "lastLoginTimestamp": 1697234567890,
      "profileImageUrl": "https://example.com/profile.jpg",
      "phoneNumber": "+0987654321"
    }
  }
}
```

### Example 3: Rejected User
```json
{
  "users": {
    "QwErTyUiOpAsDfGhJk": {
      "uid": "QwErTyUiOpAsDfGhJk",
      "email": "blocked.user@example.com",
      "displayName": "Blocked User",
      "status": "rejected",
      "registrationTimestamp": 1696923456789,
      "lastLoginTimestamp": 0,
      "profileImageUrl": "",
      "phoneNumber": ""
    }
  }
}
```

## Admin Approval Process

### Manual Approval via Firebase Console

1. **Access Firebase Console**
   - Go to https://console.firebase.google.com/
   - Select project: `bisto-chat-3b33a`

2. **Navigate to Realtime Database**
   - Click "Realtime Database" in left sidebar
   - Select database: `bisto-chat-3b33a-default-rtdb`

3. **Find Pending Users**
   - Navigate to `/users` node
   - Look for users with `status: "pending"`

4. **Approve User**
   - Click on the user's UID
   - Find the `status` field
   - Change value from `"pending"` to `"approved"`
   - Click the checkmark to save

5. **Reject User**
   - Click on the user's UID
   - Find the `status` field
   - Change value from `"pending"` to `"rejected"`
   - Click the checkmark to save

### Finding Users for Approval

#### Method 1: Browse by Registration Date
- Users are sorted by UID (Firebase generated)
- Check `registrationTimestamp` for recent registrations

#### Method 2: Search by Email
- Use browser search (Ctrl+F) to find specific email
- Look for `status: "pending"` entries

#### Method 3: Filter by Status
- Firebase Console doesn't support filtering
- Manually scan for `"pending"` status values

## Database Security Rules

### Recommended Rules
```json
{
  "rules": {
    "users": {
      "$uid": {
        ".read": "$uid === auth.uid || root.child('users/' + auth.uid + '/status').val() === 'approved'",
        ".write": "$uid === auth.uid && root.child('users/' + $uid + '/status').val() === 'approved'",
        "status": {
          ".write": false
        }
      }
    }
  }
}
```

### Rule Explanation
- **Read Access**: Users can read their own profile OR if they are approved users
- **Write Access**: Users can only write to their own profile AND only if approved
- **Status Protection**: Status field cannot be modified by users (admin-only via Console)

## App Integration

### Registration Flow
1. User fills registration form
2. Firebase Auth creates account
3. App creates user profile in `/users/{uid}` with `status: "pending"`
4. User is immediately signed out
5. User cannot login until status changes to `"approved"`

### Login Flow
1. User attempts login with Firebase Auth
2. If auth succeeds, app checks `/users/{uid}/status`
3. If status is `"approved"`: Allow access, update `lastLoginTimestamp`
4. If status is `"pending"` or `"rejected"`: Sign out user, show message

### PHP Backend Integration
- Before allowing message operations, verify user status
- Only users with `status: "approved"` can send messages
- Reference: `firebase_verification.php`

## Testing the Approval System

### Test Scenario 1: New Registration
1. Register new user through app
2. Check Firebase Console: user should have `status: "pending"`
3. Try to login: should fail with approval message
4. Approve via Console: change status to `"approved"`
5. Login should now succeed

### Test Scenario 2: Rejected User
1. Set user status to `"rejected"` in Console
2. Try to login: should fail with rejection message
3. Change status to `"approved"`
4. Login should now succeed

### Test Scenario 3: Database Verification
1. Check that `registrationTimestamp` is set correctly
2. Verify `lastLoginTimestamp` updates after successful login
3. Confirm user data structure matches expected format

## Troubleshooting

### Common Issues

#### User Not Found in Database
- **Cause**: Registration process failed after Firebase Auth creation
- **Solution**: User needs to register again or manually create profile in Console

#### Status Field Missing
- **Cause**: Old user records or incomplete migration
- **Solution**: Manually add `status: "pending"` field in Console

#### Login Still Fails After Approval  
- **Cause**: App cache or authentication state issues
- **Solution**: User should restart app and try logging in again

#### PHP Script Can't Access Firebase
- **Cause**: Missing service account credentials or incorrect database URL
- **Solution**: Verify Firebase Admin SDK setup and credentials path

## Monitoring and Analytics

### Key Metrics to Track
- Number of pending registrations
- Approval/rejection rates
- Time between registration and approval
- Failed login attempts due to pending status

### Firebase Analytics Events
Consider tracking these custom events:
- `user_registration_pending`
- `user_login_blocked_pending`
- `user_login_blocked_rejected`  
- `user_approved_login_success`

## Security Considerations

### Data Protection
- User emails and profile data are stored in Firebase
- Status field prevents unauthorized access
- Consider encrypting sensitive user data

### Admin Access Control
- Only administrators should have Firebase Console access
- Use Firebase Admin SDK for programmatic user management
- Consider building admin panel for easier user management

### Audit Trail
- Track who approved/rejected users
- Log timestamps for status changes
- Consider adding approval reason fields