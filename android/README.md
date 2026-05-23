# Android Translate — build & install (Windows OK)

Buildable on your Windows PC. No Mac needed.

## Prerequisites
- Android Studio Jellyfish or newer (already installed on your machine)
- DeepL Free API key from [deepl.com/pro-api](https://www.deepl.com/pro-api)
- Android phone with USB debugging on, OR an emulator

## Step 1 — Open the project
1. **Android Studio → Open** → pick the `android/` folder
2. Trust the project when prompted
3. Gradle sync runs; first time downloads dependencies (~3 min)

## Step 2 — Install on phone
1. Plug phone in, accept the "Allow USB debugging" prompt
2. In Android Studio: top bar device dropdown → pick your phone
3. Hit ▶ Run

## Step 3 — Configure
1. App launches → tap the ⚙ gear (top right) → **Settings**
2. Paste your DeepL key → **Save**
3. (Optional) tap **Test key** to verify it works

## Using it — outgoing messages
1. Open the app
2. Type your English (or Indonesian) message
3. Wait ~half a second → translation appears below
4. Tap **Copy** → app shows "Copied to clipboard"
5. Tap **WhatsApp** to jump to WhatsApp (or just switch yourself)
6. In the WhatsApp chat: long-press the message field → **Paste** → Send

## Using it — incoming messages
1. In WhatsApp, **long-press** a message from your gf
2. Tap **Share** (or the share icon → three dots → Share)
3. From the share sheet, pick **Translate (EN/ID)**
4. The Translate app opens with her message pre-filled in the source field
5. Translation shows immediately

## Project structure
```
android/
├── app/src/main/
│   ├── AndroidManifest.xml              ← share intent filters (SEND, PROCESS_TEXT)
│   ├── res/values/                      ← strings, colors, theme
│   └── java/com/banshee/translatekeyboard/
│       ├── MainActivity.kt              ← Compose translate UI + settings sheet
│       ├── translate/DeepLClient.kt     ← DeepL API client
│       └── settings/Prefs.kt
└── build.gradle.kts
```

## Troubleshooting
- **No translation appears** → settings → check DeepL key, hit Test key
- **"Missing DeepL API key"** → enter and save the key in Settings
- **Build fails: Compose compiler vs Kotlin mismatch** → make sure Android Studio is updated; this project uses Kotlin 1.9.24 + Compose compiler 1.5.14
- **Sharing from WhatsApp shows "Translate (EN/ID)" but app crashes on open** → the share intent filter expects `text/plain`. Some WhatsApp share variants send other MIME types — let me know and I'll add the missing filter.
