# iOS Translate — Xcode setup

This walks you through creating the iOS project from the Swift sources in this folder. You need a Mac with Xcode (or rent one via [MacInCloud](https://www.macincloud.com/)).

## Step 1 — Create the Xcode project
1. **Xcode → File → New → Project → iOS → App**
2. Product Name: **Translate**
3. Team: pick your Apple ID
4. Organization Identifier: `com.banshee` (or your own)
5. Interface: **SwiftUI** · Language: **Swift** · Storage: **None**

## Step 2 — Drop in the source files

Open the project in Finder. You'll see a folder called `Translate/` (the host app target).

Replace the contents with the files from this repo's `ios/TranslateApp/`. Then in Xcode's project navigator, drag in:
- `TranslateApp.swift` (replaces the auto-generated `TranslateApp.swift`)
- `ContentView.swift`
- `TranslateApp.entitlements`

Also create a top-level **group** called `Shared` in Xcode and drag in (all five must be added to BOTH targets — see Step 3 step 8):
- `ios/Shared/SharedDefaults.swift`
- `ios/Shared/DeepLClient.swift`
- `ios/Shared/BrandColor.swift`
- `ios/Shared/MyMemoryClient.swift`
- `ios/Shared/Translator.swift`

## Step 3 — Add the Share Extension target
1. **File → New → Target → iOS → Share Extension**
2. Product Name: **TranslateShareExtension**
3. Language: Swift, embed in Translate
4. Xcode auto-generates `ShareViewController.swift`, `MainInterface.storyboard`, `Info.plist`
5. **DELETE the auto-generated `MainInterface.storyboard`** — we use a code-only approach
6. **DELETE the auto-generated `ShareViewController.swift`**
7. From this repo's `ios/TranslateShareExtension/`, drag in:
   - `ShareViewController.swift`
   - `Info.plist` (replace the auto-generated one)
   - `TranslateShareExtension.entitlements`
8. Add all five Shared files to this target too — in Xcode File Inspector (right panel), check both `Translate` AND `TranslateShareExtension` under **Target Membership** for each of: `SharedDefaults.swift`, `DeepLClient.swift`, `BrandColor.swift`, `MyMemoryClient.swift`, `Translator.swift`

## Step 4 — App Groups (so the app & extension share the DeepL key)
For **each** target (Translate AND TranslateShareExtension):
1. Project navigator → select target → **Signing & Capabilities** tab
2. **+ Capability** → **App Groups**
3. Click `+` → add `group.com.banshee.translatekeyboard`
4. Check the checkbox

If you change the group ID, also update it inside `SharedDefaults.swift` and both `.entitlements` files.

## Step 4.5 — Allow WhatsApp URL scheme (so "Send via WhatsApp" works)

In the **Translate** target's Info tab (or directly in `Info.plist`), add:

```
LSApplicationQueriesSchemes  (Array)
   item 0  →  whatsapp        (String)
```

Without this, `UIApplication.shared.canOpenURL` returns false for `whatsapp://send?text=…` and the app falls back to `wa.me`.

## Step 5 — Signing
1. For each target → Signing & Capabilities → Team → pick your Apple ID
2. Bundle Identifier should be unique — let Xcode set it (e.g., `com.banshee.Translate` and `com.banshee.Translate.ShareExtension`)

## Step 6 — Build & install on iPhone
1. Connect iPhone via USB → enable Developer Mode on the phone (Settings → Privacy & Security → Developer Mode)
2. Pick your iPhone in the Xcode device dropdown
3. Hit ▶ Run with the **Translate** scheme selected
4. The app installs and launches on the phone

## Step 7 — Configure DeepL
1. In the app, tap ⚙ → paste your DeepL key → Save → tap **Test key**
2. You should see "OK → halo" or similar

## Using it — outgoing
- Open Translate → type → tap **Copy** → switch to WhatsApp → paste → send

## Using it — incoming (Share Extension)
1. In WhatsApp, long-press your gf's message → **Share**
2. Scroll the share sheet → tap **Translate**
3. The extension opens with her message pre-filled and the translation showing
4. Tap **Copy translation** if you want it on the clipboard, then **Done**

If you don't see **Translate** in the share sheet on first use:
- Tap **More** at the bottom of the share sheet → toggle on **Translate**
- The translate extension only activates on text shares (not URLs or images)

## Troubleshooting
- **Share extension says "Missing DeepL API key"** → you saved the key in the app, but App Groups isn't set up correctly. Re-check Step 4.
- **No translation in main app** → settings → Test key
- **iOS rejects signing** → free Apple ID accounts can sideload but the app expires every 7 days. For permanent install, you need a paid Apple Developer account ($99/yr).
- **App keeps crashing on launch** → most likely cause is an iOS-version-specific SwiftUI API. The code targets iOS 17+. If you're on iOS 16 or older, some `onChange(of:)` calls need to be updated.
