# Translate — EN ↔ ID companion app for WhatsApp

A small **companion translate app** for Android and iOS, designed to be identical on both phones (so you and your partner share the same UX).

Two flows, same on each platform:

1. **Outgoing — type in our app, paste in WhatsApp.** Open the app, type EN or ID, watch the live DeepL translation, tap **Copy**, switch to WhatsApp, paste.
2. **Incoming — share to translate.** Long-press a message in WhatsApp → **Share** → **Translate** → translation appears.

Powered by **DeepL Free** (500k chars/month).

## Layout

```
┌─────────────────────────────────┐
│  Translate                  ⚙   │
├─────────────────────────────────┤
│  [ 🇬🇧 EN ]   ⇄   [ 🇮🇩 ID ]    │   ← swap target language
│                                 │
│  English                        │
│ ┌─────────────────────────────┐ │
│ │ where are you               │ │   ← type here
│ └─────────────────────────────┘ │
│                                 │
│  Indonesian                     │
│ ┌─────────────────────────────┐ │
│ │ kamu di mana                │ │   ← live translation
│ └─────────────────────────────┘ │
│                                 │
│  [ Copy ]      [ WhatsApp ]     │
└─────────────────────────────────┘
```

## Get started
Pick your platform:
- **Android** (buildable on Windows): [`android/README.md`](android/README.md)
- **iOS** (needs a Mac + Xcode): [`ios/README.md`](ios/README.md)

For the high-level setup and DeepL key signup, see [`docs/SETUP.md`](docs/SETUP.md).

## Project structure
```
.
├── android/                                  ← Kotlin Android Studio project
│   └── app/src/main/
│       ├── AndroidManifest.xml               ← share intent filters (SEND, PROCESS_TEXT)
│       ├── res/values/                       ← strings, colors, theme
│       └── java/com/banshee/translatekeyboard/
│           ├── MainActivity.kt               ← Jetpack Compose UI
│           ├── translate/DeepLClient.kt
│           └── settings/Prefs.kt
├── ios/                                      ← Swift Xcode project
│   ├── Shared/                               ← shared between app & extension
│   │   ├── DeepLClient.swift
│   │   └── SharedDefaults.swift              ← App Group UserDefaults
│   ├── TranslateApp/                         ← host app (SwiftUI)
│   │   ├── TranslateApp.swift
│   │   ├── ContentView.swift
│   │   └── TranslateApp.entitlements
│   └── TranslateShareExtension/              ← iOS Share Extension
│       ├── ShareViewController.swift
│       ├── Info.plist
│       └── TranslateShareExtension.entitlements
└── docs/SETUP.md
```

## Stack
- **Android**: Kotlin, Jetpack Compose, Material 3, OkHttp, AndroidX
- **iOS**: Swift, SwiftUI, App Group UserDefaults for sharing config across the app + share extension
- **Translation**: DeepL Free API

## v1 scope
- EN ↔ ID translation with toggle
- Live auto-translate (debounced 450ms)
- Copy translation to clipboard
- Open WhatsApp from the app
- Share-to-Translate on incoming WhatsApp messages
- DeepL API key in settings, persisted per device

## Not in scope (yet)
- More than 2 languages
- History / favorites
- Offline mode
- Android floating overlay (skipped to keep iOS/Android parity)
