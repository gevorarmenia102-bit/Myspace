# MySpace Personal Hub — Firebase Setup Guide

## 1. Overview
MySpace Personal Hub is designed with an **offline-first** architecture. If you do not configure Firebase, the app runs smoothly in **Local Mode** with Room SQLite storage. 

To enable realtime multi-device cloud synchronization and cloud photo backup, follow the steps below.

---

## 2. Step-by-Step Firebase Project Configuration

### Step 1: Create Firebase Project
1. Visit the [Firebase Console](https://console.firebase.google.com/).
2. Click **Add project** and enter a name (e.g., `MySpace Personal Hub`).
3. Google Analytics can be disabled since MySpace Hub is privacy-first and does not track telemetry.

### Step 2: Register Android Application
1. In Project Overview, click the **Android** icon to add an Android app.
2. Enter the Package Name:
   ```
   com.example.myspacepersonalhub
   ```
3. Enter an optional App nickname (e.g., `MySpace Android`).
4. (Optional) Provide your SHA-1 fingerprint if using Google Sign-In or App Check.
5. Click **Register app**.

### Step 3: Add `google-services.json`
1. Download the generated `google-services.json` file.
2. Copy `google-services.json` into your local project's `app/` directory:
   ```
   /Users/xagor/Desktop/personal apps/app/google-services.json
   ```
3. In `app/build.gradle.kts`, apply the Google Services plugin if desired:
   ```kotlin
   if (file("google-services.json").exists()) {
       apply(plugin = "com.google.gms.google-services")
   }
   ```

### Step 4: Enable Authentication
1. In Firebase Console, navigate to **Build > Authentication**.
2. Click **Get Started**.
3. Under **Sign-in method**, enable **Email/Password**.

### Step 5: Enable Realtime Database & Security Rules
1. Navigate to **Build > Realtime Database** and click **Create Database**.
2. Select your nearest geographic location (e.g., `us-central1`).
3. Start in **Locked Mode**.
4. Go to the **Rules** tab and paste the following security rules:

```json
{
  "rules": {
    "users": {
      "$uid": {
        ".read": "auth != null && auth.uid === $uid",
        ".write": "auth != null && auth.uid === $uid",
        "contacts": {
          "$contactId": {
            ".validate": "newData.hasChildren(['id', 'displayName'])"
          }
        },
        "notes": {
          "$noteId": {
            ".validate": "newData.hasChildren(['id', 'title'])"
          }
        },
        "tasks": {
          "$taskId": {
            ".validate": "newData.hasChildren(['id', 'title'])"
          }
        }
      }
    }
  }
}
```
> [!IMPORTANT]
> These rules enforce that authenticated users can **ONLY** read and write their own data under `users/{uid}`. Unauthenticated requests are completely blocked.

### Step 6: Enable Firebase Storage (Optional)
1. Navigate to **Build > Storage** and click **Get Started**.
2. Set Storage Rules to enforce owner-only access:
```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /users/{userId}/{allPaths=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

---

## 3. Realtime Database Path Schema

```
users/
  └── {uid}/
        ├── profile/
        │     └── email: "user@example.com"
        ├── contacts/
        │     └── {contactId}/
        │           ├── id: "uuid-string"
        │           ├── displayName: "Alice Smith"
        │           ├── email: "alice@example.com"
        │           ├── company: "Tech Corp"
        │           ├── jobTitle: "Engineer"
        │           ├── birthday: "1992-05-14"
        │           ├── personalNotes: "Encrypted or sensitive text"
        │           ├── isFavorite: true
        │           └── updatedAt: 1774000000000
        ├── notes/
        │     └── {noteId}/
        │           ├── id: "uuid-string"
        │           ├── title: "Meeting Notes"
        │           ├── body: "Discussion details..."
        │           ├── category: "Work"
        │           └── updatedAt: 1774000000000
        ├── tasks/
        │     └── {taskId}/
        │           ├── id: "uuid-string"
        │           ├── title: "Submit taxes"
        │           ├── dueAt: 1774100000000
        │           ├── priority: "HIGH"
        │           └── status: "TODO"
        └── syncMetadata/
              └── lastSyncTimestamp: 1774000000000
```
