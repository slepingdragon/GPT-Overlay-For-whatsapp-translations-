# Getting the iOS app onto your girlfriend's iPhone

The honest constraints first: **you can't just send her an `.apk` like Android.** Apple requires every iOS app to be signed by an Apple developer account before it'll install on a device. You have three realistic paths, from easiest to messiest.

---

## Option A — TestFlight (recommended, $99/year)

This is what every iOS startup does to share builds with testers. Smoothest experience for her — she gets a normal-looking app via Apple's TestFlight, and you can push updates with one click.

**One-time setup (you, on a Mac):**

1. Sign up at <https://developer.apple.com/programs/> — **$99/year**, pays for itself the first month if you value not having to mess with sideloading every week.
2. In Xcode, open the project, set the bundle identifier to something unique you own (`com.banshee.translate` etc.) and set your developer team in **Signing & Capabilities** for both targets (the app + the share extension).
3. Xcode → **Product → Archive** (wait ~3–5 min).
4. Window → **Organizer** → select your archive → **Distribute App → App Store Connect → Upload**.
5. After ~10–30 min you'll get an email "Build is ready to test."
6. Go to <https://appstoreconnect.apple.com> → **My Apps → Translate → TestFlight** tab.
7. Add yourself and your girlfriend as **External Testers** by email.
8. Submit the build to Apple's review (takes a few hours to a day for first build; minutes for updates) → click **Start Testing** when approved.

**For her (one-time, ~2 minutes):**

1. She installs the free **TestFlight** app from the App Store.
2. She gets an email/SMS invite from TestFlight with a code or link.
3. She opens TestFlight, accepts the invite, taps **Install**.
4. Translate appears on her home screen.

**Updating later:** archive → upload → done. She gets the new build automatically (or with one tap). Each build is valid for 90 days, so re-upload roughly every quarter.

---

## Option B — Free sideload via your Mac (no $99 fee, weekly hassle)

Works with a free Apple ID. The catch: **the app expires after 7 days** and has to be reinstalled. Realistic only if you live together or see each other weekly.

1. In Xcode → **Signing & Capabilities** → Team → choose your free **Personal Team**.
2. Set the bundle identifier to something unique (any `com.yourname.translate` works).
3. Plug her iPhone into your Mac with USB.
4. On her iPhone: **Settings → Privacy & Security → Developer Mode → On** (requires reboot).
5. In Xcode, select her iPhone as the run target → ▶ Run.
6. First time only: on her phone, go to **Settings → General → VPN & Device Management → [your Apple ID]** → tap **Trust**.
7. After 7 days the app stops launching with a "developer can't be verified" error. Plug back in, run again to refresh.

Three iPhones max per free Apple ID, ten app IDs per week — fine for two phones.

---

## Option C — AltStore (free, self-refreshing if you set it up)

Third-party sideloader that re-signs apps every 7 days automatically as long as a companion app (AltServer) is running on a Mac/PC on the same Wi-Fi.

Setup is fiddlier — see <https://altstore.io>. Not recommended unless you really want to avoid the $99 fee and don't see her in person often. Apple periodically blocks AltStore methods; expect occasional breakage.

---

## What I'd actually do

If this is going to be a thing you both use regularly: **pay the $99 and use TestFlight**. The time you'll save not re-sideloading every week is worth it within the first month, and updates are a one-button push from Xcode.

If you just want to try it for a couple weeks: **Option B**, sideload from your Mac while she's over.

---

## Before any of this works

Whichever option, the iOS project has to actually build first. That means:

1. You need a **Mac** (any model from the last ~5 years, must run macOS 14+).
2. **Xcode 15+** installed (free from the Mac App Store, ~15 GB download).
3. Follow [`ios/README.md`](README.md) to set up the Xcode project from the Swift files in this repo (create app target, add share extension target, drag in Shared/ files with the right Target Memberships, add App Groups capability, etc.).
4. Both targets need an **App Group** with ID `group.com.banshee.translatekeyboard` (or whatever you change it to in `SharedDefaults.swift`).
5. Build & run on your own iPhone first to confirm everything works before inviting her.

If you don't have a Mac, **rent one in the cloud**: <https://www.macincloud.com/> runs ~$30/mo, gives you full Xcode access. Good enough for setup + one TestFlight upload, then you only need it again for updates.
