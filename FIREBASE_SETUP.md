# NshutiTrack — Firebase Setup Instructions

## Required: google-services.json

Before building, you MUST add your Firebase config file:

1. Go to https://console.firebase.google.com
2. Create a new project called "NshutiTrack"
3. Add an Android app with package name: `com.example.nshutiplanner`
4. Download `google-services.json`
5. Place it at: `app/google-services.json`

## Firebase Services to Enable

In the Firebase Console, enable:
- [ ] Authentication → Email/Password
- [ ] Firestore Database → Start in test mode
- [ ] Storage → Start in test mode
- [ ] Cloud Messaging (FCM) → auto-enabled

## Firestore Indexes Required

Create composite indexes for these collections (Firebase will prompt you on first query):
- `plans`: coupleId ASC + createdAt DESC
- `tasks`: coupleId ASC + createdAt DESC
- `messages`: coupleId ASC + timestamp ASC
- `moods`: coupleId ASC + timestamp DESC

## Firestore Security Rules (Production)

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{uid} {
      allow read, write: if request.auth.uid == uid;
    }
    match /{collection}/{docId} {
      allow read, write: if request.auth != null &&
        resource.data.coupleId.matches('.*' + request.auth.uid + '.*');
    }
  }
}
```
