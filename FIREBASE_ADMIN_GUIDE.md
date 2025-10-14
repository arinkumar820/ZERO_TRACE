# Firebase Admin Approval System Guide

## Overview

This document explains the Firebase Realtime Database structure and how to manually approve users through the Firebase Console.

## Database Structure

### Users Node Structure

```json
{
  "users": {
    "{user_uid}": {
      "uid": "string - Firebase user UID",
      "email": "string - User's email address", 
      "displayName": "string - User's full name",
      "status": "string - pending|approved|rejected",
      "registrationTimestamp": "number - Registration timestamp in milliseconds",
      "lastLoginTimestamp": "number - Last successful login timestamp",
      "profileImageUrl": "string - Optional profile image URL",
      "phoneNumber": "string - Optional phone number"
    }
  }
}
```

### Example User Record

```json
{
  "users": {
    "abc123def456": {
      "uid": "abc123def456",
      "email": "john.doe@example.com",
      "displayName": "John Doe",
      "status": "pending",
      "registrationTimestamp": 1703073600000,
      "lastLoginTimestamp": 0,
      "profileImageUrl": "",
      "phoneNumber": ""
    }
  }
}
```

## Status Values

| Status    | Description                                    | Login Allowed |
|-----------|------------------------------------------------|---------------|
| `pending` | New user waiting for admin approval            | ❌ No         |
| `approved`| User approved by admin                         | ✅ Yes        |
| `rejected`| User rejected by admin                         | ❌ No         |

## Admin Approval Process

### Step 1: Access Firebase Console

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your Bisto Chat project
3. Navigate to **Realtime Database** from the left sidebar

### Step 2: Find Pending Users

1. In the database view, expand the `users` node
2. Look for users with `"status": "pending"`
3. You can identify users by their:
   - Email address
   - Display name
   - Registration timestamp

### Step 3: Approve a User

1. Click on the user's UID node to expand it
2. Find the `status` field
3. Click on the value `"pending"`
4. Change it to `"approved"`
5. Press Enter or click the ✓ button to save

### Step 4: Reject a User (if needed)

1. Follow the same steps as approval
2. Change the status to `"rejected"` instead of `"approved"`

## Security Rules

Ensure your Firebase Realtime Database has proper security rules:

```json
{
  "rules": {
    "users": {
      "$uid": {
        ".read": "auth != null && auth.uid == $uid",
        ".write": "auth != null && auth.uid == $uid && !data.exists()",
        "status": {
          ".write": false
        },
        "registrationTimestamp": {
          ".write": false
        }
      }
    }
  }
}
```

These rules ensure:
- Users can only read their own data
- Users can only create their profile once
- Users cannot modify their status or registration timestamp
- Only admins can change status through Firebase Console

## Monitoring New Registrations

### Email Notifications (Optional)

You can set up Cloud Functions to get notified of new registrations:

```javascript
const functions = require('firebase-functions');
const admin = require('firebase-admin');

admin.initializeApp();

exports.notifyAdminOfNewUser = functions.database.ref('/users/{userId}')
    .onCreate((snapshot, context) => {
        const userData = snapshot.val();
        
        if (userData.status === 'pending') {
            // Send email notification to admin
            console.log(`New user registration: ${userData.email}`);
            
            // Implement email sending logic here
            // You can use SendGrid, Nodemailer, etc.
        }
    });
```

### Regular Checks

Administrators should regularly check for pending users:

1. **Daily Review**: Check for new pending users daily
2. **User Verification**: Verify user legitimacy before approval
3. **Batch Processing**: Approve/reject multiple users at once

## User Communication

### After Registration

Users see this message after registration:
> "Registration successful! Your account is pending admin approval. You will be able to login once approved."

### During Login Attempts

Pending users see:
> "Your account is pending admin approval. Please wait for approval before logging in."

Rejected users see:
> "Your account has been rejected by the administrator. Please contact support if you believe this is an error."

## Best Practices

### User Approval Guidelines

1. **Verify Email Domain**: Check if email comes from legitimate domain
2. **Check Display Name**: Ensure name looks realistic
3. **Registration Time**: Be cautious of bulk registrations in short time
4. **Duplicate Check**: Look for duplicate emails or names

### Response Time

- **Target**: Approve/reject users within 24 hours
- **Business Hours**: Prioritize approval during business hours
- **Weekend Coverage**: Have weekend admin coverage if possible

### Documentation

- **Keep Records**: Document approval/rejection reasons
- **Track Metrics**: Monitor approval rates and user activity
- **Regular Review**: Review approval process monthly

## Troubleshooting

### Common Issues

1. **User Cannot Login After Approval**
   - Check if status is exactly "approved" (case-sensitive)
   - Verify Firebase Rules allow the operation
   - Check app logs for authentication errors

2. **Status Not Updating**
   - Ensure you have write permissions
   - Check for typos in status value
   - Verify database connection

3. **Multiple User Records**
   - Check for duplicate UIDs
   - Merge duplicate records if necessary
   - Update security rules to prevent duplicates

### Contact Information

For technical issues with the approval system:
- **Developer**: [Your Contact Information]
- **Firebase Support**: [Firebase Console Support]
- **Documentation**: [Link to this guide]

## Database Backup

### Regular Backups

1. **Automated**: Set up automated daily backups
2. **Before Changes**: Backup before making bulk status changes
3. **Version Control**: Keep backup versions for rollback

### Export User Data

```bash
# Export users data
firebase database:get /users --output users_backup.json
```

## Analytics and Reporting

### User Registration Metrics

Track these metrics in Firebase Analytics:
- New registrations per day
- Approval rate percentage
- Time to approval
- User retention after approval

### Database Queries

Use Firebase Console queries to find:
- All pending users: `users` where `status == "pending"`
- Recently registered: `users` ordered by `registrationTimestamp`
- Active users: `users` where `lastLoginTimestamp > [recent_date]`

---

**Last Updated**: [Current Date]  
**Version**: 1.0  
**Admin Contact**: [Your Email]