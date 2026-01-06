# Pitkiot Firebase Quick Start Guide

Your app now supports **online note collection** via Firebase! Players can submit notes through a simple web form (no app needed), and the host receives them in real-time.

## 🚀 Quick Setup (5 minutes)

### Step 1: Create Firebase Project (2 min)

1. Go to https://console.firebase.google.com/
2. Click **"Add project"**
3. Name it: `Pitkiot`
4. Disable Google Analytics (optional)
5. Click **"Create project"**

### Step 2: Add Android App (2 min)

1. In Firebase Console, click the **Android icon**
2. Enter package name: `nevo_mashiach.pitkiot`
3. Download `google-services.json`
4. **Move it to**: `/Users/Nevo.Mashiach/workspace/Pitkiot/app/google-services.json`
   - MUST be in the `app/` folder!

### Step 3: Enable Firestore (1 min)

1. In Firebase Console → **Build** → **Firestore Database**
2. Click **"Create database"**
3. Choose **"Start in test mode"**
4. Select location (choose closest to you)
5. Click **"Enable"**

### Step 4: Deploy Web Form (2 min)

1. Install Firebase CLI (if needed):
   ```bash
   npm install -g firebase-tools
   ```

2. Login and deploy:
   ```bash
   cd /Users/Nevo.Mashiach/workspace/Pitkiot
   firebase login
   firebase init hosting
   ```

   When prompted:
   - Select your project
   - Public directory: `web`
   - Single-page app: **Yes**
   - Automatic builds: **No**

3. Update the web form with your Firebase config:
   - Open `web/submit.html`
   - Find line ~240: `const firebaseConfig = {`
   - Get your config from Firebase Console → Project Settings → General → Your apps → Config
   - Replace `YOUR_API_KEY`, `YOUR_PROJECT_ID`, etc.

4. Deploy:
   ```bash
   firebase deploy --only hosting
   ```

5. You'll get a URL like: `https://pitkiot-xxxxx.web.app`

### Step 5: Update Android App (1 min)

1. Open `app/src/main/java/nevo_mashiach/pitkiot/NoteManagement.java`
2. Find line ~99: `private static final String FIREBASE_HOSTING_URL`
3. Replace with your URL (without `/submit`):
   ```java
   private static final String FIREBASE_HOSTING_URL = "https://pitkiot-xxxxx.web.app";
   ```

### Step 6: Build & Run

1. Sync Gradle in Android Studio
2. Build the project
3. Run on your device

---

## ✅ How to Use

### Host (Android App):

1. Open **Pitkiot** app
2. Go to **"ניהול פתקים"** (Note Management)
3. Tap **"איסוף מקוון"** (Online Collection)
4. A dialog shows:
   - 4-digit code
   - QR code
   - Full URL
5. Share the link/code via WhatsApp
6. Watch notes arrive in real-time!
7. Tap **"סיים ושמור פתקים"** when done

### Players (Any Browser):

1. Click the link from WhatsApp
2. Opens a simple web form
3. Enter their name (optional)
4. Type their note(s)
5. Tap **"שלח פתק"** (Submit)
6. Done! Can submit more if needed

---

## 🎯 What You Get

✅ No app needed for players
✅ Works on any device (phone, tablet, computer)
✅ Real-time updates for host
✅ QR code for easy sharing
✅ Supports multiple notes per submission
✅ Shows who submitted what
✅ Completely free (Firebase Spark plan)

---

## 🔧 Troubleshooting

**Build error: "google-services.json not found"**
- File must be in `app/` directory (NOT root)
- Check filename is exactly `google-services.json`

**Web form shows "Firebase unavailable"**
- Update Firebase config in `web/submit.html`
- Check Firestore is enabled in Firebase Console
- Verify security rules are published

**Notes not appearing in real-time**
- Check internet connection
- Verify `FIREBASE_HOSTING_URL` is correct in `NoteManagement.java`
- Ensure Firestore security rules allow read/write

**QR code doesn't work**
- Make sure URL in dialog matches your Firebase Hosting URL
- Try using the "Copy Link" button instead

---

## 📦 What's Been Added

### New Files:
- `app/src/main/java/nevo_mashiach/pitkiot/NotActivities/NoteCollectionSession.java` - Firebase session manager
- `app/src/main/res/layout/dialog_note_collection.xml` - Collection dialog UI
- `web/submit.html` - Web form for note submission
- `firebase.json` - Firebase Hosting config
- `firestore.rules` - Database security rules
- `FIREBASE_SETUP.md` - Detailed setup instructions

### Modified Files:
- `build.gradle` - Added Firebase plugin
- `app/build.gradle` - Added Firebase & QR code dependencies
- `app/src/main/AndroidManifest.xml` - Added INTERNET permission
- `app/src/main/java/nevo_mashiach/pitkiot/NoteManagement.java` - Added online collection feature
- `app/src/main/res/layout/activity_note_management.xml` - Added "איסוף מקוון" button

### Features:
- ✅ Firebase Firestore integration
- ✅ Real-time note synchronization
- ✅ QR code generation
- ✅ Web form with Hebrew UI
- ✅ Session-based collection
- ✅ Copy-to-clipboard for easy sharing

---

## 🎉 You're All Set!

The old SMS method still works! You now have TWO ways to collect notes:
1. **SMS Scan** - "סרוק סמסים" (original method)
2. **Online Collection** - "איסוף מקוון" (new method!)

Players can now submit notes from anywhere without SMS or installing the app!

---

## 💡 Need Help?

Check `FIREBASE_SETUP.md` for detailed instructions and advanced configuration options.
