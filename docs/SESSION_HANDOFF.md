# Desktop handoff prompt

Copy everything below the line into a fresh Claude Code session on your desktop after you `git clone` the repo. The prompt starts with `bmad-help` and orients Claude on where the project sits.

---

bmad-help

I'm continuing work on the **Translate** app from a different machine. Here's the state:

**Repo:** https://github.com/slepingdragon/GPT-Overlay-For-whatsapp-translations-
**Branch:** `main` (current head should be `54aa28f` or later — pull before doing anything)
**Folders that matter:**
- `android/` — Kotlin / Jetpack Compose. Buildable on Windows + Android Studio. Tested on my Samsung Galaxy S24 Ultra via USB debugging.
- `ios/` — Swift / SwiftUI. Code is written but **not yet built** — needs a Mac + Xcode. See `ios/DISTRIBUTING.md` for how to ship it to my girlfriend's iPhone (TestFlight is the planned path).
- `branding/` — purple/pink gradient app icon (X with spiral) as SVG, plus install instructions.
- `_dev/web/` — interactive HTML/JS mockup I use to iterate on UI design before changing the native code. Serve with `python -m http.server 8055` from that folder.
- `docs/SETUP.md` — DeepL API key signup, build order, what's in scope.

**What the app does:**
- Companion translate app for EN ↔ ID/SU + 13 other DeepL languages (Spanish, French, German, Italian, Portuguese, Dutch, Polish, Russian, Japanese, Korean, Chinese, Arabic, Turkish). Sundanese routes through MyMemory since DeepL doesn't support it.
- Type in the app → live translation via DeepL with debounced auto-translate (or manual button if auto is off in settings) → tap **Send via WhatsApp** to open her chat directly with the message pre-filled (uses `wa.me/<phone>?text=...` with the default contact number saved in settings).
- Chat-style **history** of every Copy / Send shows above the input as plain text bubbles (favoritable, copyable, deletable).
- iOS **Share Extension** lets her translate received messages: long-press in WhatsApp → Share → Translate.

**Settings UI (iOS-style grouped sections with X close in top-right):**
- Behavior — auto-translate toggle
- WhatsApp — default contact phone number
- Appearance → **Theme** (sub-page) — background image picker, color pickers for Accent / Send / Copy / Message box (each with a "Pick color" button that opens a honeycomb hex grid with custom saved colors, hex input, and a draggable HSV wheel), plus a UI transparency slider
- Favorites — preview of recent favorited translations
- API settings (bottom) — DeepL key + Test key + a usage card showing chars used / left / % / plan limit, auto-fetches from DeepL's `/v2/usage` on open

**Visual constraints I've locked in:**
- **No emoji flags on language chips** — text labels only (EN / ID / SU / ES / …)
- **Container is a true rectangle** edge-to-edge, no side/bottom border, only a hairline top divider
- **Message box opacity capped at 65%** — her photo always shows through at least 35%
- **Background image survives reboots** — copied to internal storage on pick
- **Settings sheets are solid dark `#101216`** — no transparency on the settings panel itself

**The language picker** is a bottom sheet (tap the `+` chip after the favorites row in the main view): favorites section at top with filled hearts, all-languages section below with outline hearts. Tap a language name to select it; tap the heart to toggle favorite without selecting.

**Currently NOT done:**
- iOS app has never been compiled — no Mac access yet. The Swift code is ready but the Xcode project shell needs to be created and the Shared/ files added to both target memberships (see `ios/README.md`). Once compiled, I want to ship via TestFlight (Option A in `ios/DISTRIBUTING.md`).
- The app icon SVG (`branding/app_icon.svg`) isn't exported to PNG and installed as the launcher icon on either platform yet.
- No automated tests.

**What I might want to work on next** (in rough priority):
1. Get the Xcode project actually built (when I have Mac access) and ship a TestFlight build to my girlfriend.
2. Install the new app icon on Android via Image Asset Studio.
3. Translation polish: maybe Indonesian–Sundanese direct routing (currently goes through English heuristic with MyMemory).
4. Anything else that comes up while testing.

**Workflow notes:**
- I test on the connected Samsung Galaxy S24 Ultra via USB; you can verify it's connected with `"C:/Users/bania/AppData/Local/Android/Sdk/platform-tools/adb.exe" devices`. If it's not connected when you try to build, prompt me to plug it in.
- The Gradle wrapper isn't checked in — Android Studio bootstraps it on first sync.
- DeepL API key is **not** in the repo. I paste it into the app's Settings UI on first launch.

Pick up where I left off. Pull, sync Gradle, and ask what I want to work on if it isn't obvious from context.
