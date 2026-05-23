# Setup overview

Two identical apps live in this repo: **Android** (Kotlin, buildable on Windows) and **iOS** (Swift, requires a Mac). Both look and behave the same on purpose — you and your partner share the exact same UX.

| Concern | Android | iOS |
|---|---|---|
| Build OS | Windows / macOS / Linux | macOS only (Xcode) |
| IDE | Android Studio (free) | Xcode (free) |
| Cost to ship to your own device | Free | Free (7-day sideload) or $99/yr (Apple Developer) |
| Source folder | [`android/`](../android/) | [`ios/`](../ios/) |
| Build doc | [`android/README.md`](../android/README.md) | [`ios/README.md`](../ios/README.md) |
| Outgoing translation | Companion app + Copy + open WhatsApp + paste | Same |
| Incoming translation | Share intent (long-press message → Share → Translate) | Share extension (long-press → Share → Translate) |

## Shared step: get a DeepL API key
1. Go to [deepl.com/pro-api](https://www.deepl.com/pro-api)
2. Sign up for **DeepL API Free** (500,000 chars/month, free unless exceeded)
3. Confirm email, add a credit card (required even for free — they don't bill unless you cross the cap)
4. In the DeepL account page → **API Keys** → copy your key (free keys end with `:fx`)

Paste this key into the **Settings** screen of each app. Free keys go to `api-free.deepl.com` automatically; paid keys go to `api.deepl.com`.

## Build order (recommendation)
1. **Android first** — fastest feedback loop on Windows. Validates the DeepL flow + UI.
2. **iOS once you have Mac access** — same UI, same colors, just rebuilt in SwiftUI.

## In scope (v1)
- EN ↔ ID translation with toggle
- Live auto-translate (450ms debounce)
- Copy translation to clipboard
- Open WhatsApp from the app
- Share-to-Translate on incoming WhatsApp messages
- DeepL API key in settings, persisted per device

## Not in scope (skipped on purpose)
- Translating text that's being **typed** into WhatsApp's compose box. Neither platform exposes the draft, and we ruled out custom keyboards. Workaround: type in our app, then paste.
- Android floating overlay (would be nice but breaks iOS/Android parity — picked parity over magic for now).
- Multiple languages beyond EN/ID.
- History, favorites, offline mode.
