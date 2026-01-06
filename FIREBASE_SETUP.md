# Firebase Setup Instructions for Pitkiot

## Step 1: Create a Firebase Project

1. Go to the [Firebase Console](https://console.firebase.google.com/)
2. Click "Add project" (or select an existing project)
3. Enter project name: `Pitkiot` (or any name you prefer)
4. Disable Google Analytics (optional, not needed for this app)
5. Click "Create project"

## Step 2: Add Android App to Firebase

1. In your Firebase project, click the Android icon to add an Android app
2. **Android package name**: `nevo_mashiach.pitkiot`
3. **App nickname** (optional): `Pitkiot`
4. **Debug signing certificate SHA-1** (optional, can skip for now)
5. Click "Register app"

## Step 3: Download Configuration File

1. Download the `google-services.json` file
2. **IMPORTANT**: Move this file to: `/Users/Nevo.Mashiach/workspace/Pitkiot/app/google-services.json`
   - It must be in the `app/` directory, NOT the root directory
3. This file contains your Firebase project credentials

## Step 4: Enable Firestore Database

1. In Firebase Console, go to **Build** → **Firestore Database**
2. Click "Create database"
3. Choose **Start in test mode** (for development)
   - This allows read/write access without authentication
   - **Important**: Change these rules before publishing your app!
4. Select a Firestore location (choose closest to your users)
5. Click "Enable"

## Step 5: Configure Firestore Security Rules

1. In Firestore Database, go to the **Rules** tab
2. Replace the rules with:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Allow anyone to read and write to collection sessions
    match /sessions/{sessionId} {
      allow read, write: if true;

      match /submissions/{submissionId} {
        allow read, write: if true;
      }
    }
  }
}
```

3. Click "Publish"

**Note**: These rules allow anyone to read/write. This is fine for this use case since:
- Sessions are temporary and short-lived
- No sensitive data is stored
- Session IDs are randomly generated and hard to guess

## Step 6: Set Up Firebase Hosting (for the web form)

1. Install Firebase CLI (if not already installed):
   ```bash
   npm install -g firebase-tools
   ```

2. Login to Firebase:
   ```bash
   firebase login
   ```

3. Initialize Firebase Hosting in your project:
   ```bash
   cd /Users/Nevo.Mashiach/workspace/Pitkiot
   firebase init hosting
   ```

4. Select options:
   - Choose your Firebase project
   - Public directory: `web` (we'll create this)
   - Configure as single-page app: **Yes**
   - Set up automatic builds: **No**

5. The web form HTML will be created in the `web/` directory

## Step 7: Deploy the Web Form

After the HTML file is created (next step), deploy it:

```bash
firebase deploy --only hosting
```

Your web form will be accessible at: `https://your-project-id.web.app/submit.html`

## Step 8: Build and Run the App

1. Sync Gradle in Android Studio
2. Build the project
3. Run on your device/emulator

## Troubleshooting

### Error: "google-services.json not found"
- Make sure the file is in the `app/` directory
- Check the filename is exactly `google-services.json`

### Error: "Firestore unavailable"
- Verify Firestore is enabled in Firebase Console
- Check your internet connection
- Ensure security rules are published

### Web form not accessible
- Run `firebase deploy --only hosting`
- Check if Firebase Hosting is enabled
- Verify the URL from Firebase Console

## Important Notes

- Keep `google-services.json` private - don't commit it to public repositories
- The free Firebase plan (Spark) includes:
  - 1 GB storage
  - 10 GB/month network egress
  - 50K reads/day, 20K writes/day
  - Should be more than sufficient for this app

## Next Steps

Once setup is complete:
1. The Android app will be able to create collection sessions
2. Users can submit notes via the web form
3. The host app will receive notes in real-time
