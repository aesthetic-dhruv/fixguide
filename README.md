# Fix — Repair Guide App

**Fix** is Raj's own repair-guide app for phone repair work. It is NOT iFixit
and is not affiliated with iFixit. It reads repair guides from iFixit's
public API 2.0 (openly licensed, CC-BY) and shows them with credit — author,
"Source: iFixit", and a link to the original guide on every guide page.

- **App name:** Fix
- **Package:** `com.raj.fix` (single package — do not use `com.raj.fixguide`)
- **Version:** 1.0 (versionCode 1)
- **Logo:** original black rounded-square logo, bold white "Fix" text
  (`logo.png` at the repo root, launcher PNGs under `app/src/main/res/mipmap-*`).
  It is an original design — it does not copy iFixit's logo or wordmark.

## Features

1. **Search** — search box + quick chips (iPhone 14 Battery, Samsung Screen…).
   Live results from `GET https://www.ifixit.com/api/2.0/search/<query>` with
   title, device/category, difficulty, time, thumbnail.
2. **Browse devices** — Phone > iPhone / Android Phone > Samsung / Xiaomi /
   OnePlus… via `GET /api/2.0/categories/<name>`. Tap a model to see its guides.
3. **Guide detail** — full steps with step images, tools list, parts list,
   difficulty, time, conclusion, "Open original on iFixit", Save Offline,
   Favourite. Attribution footer on every guide.
4. **Saved** — offline guides (full guide JSON saved on the phone, opens
   without internet) + favourites.
5. **Fix AI** — chat tab. It FIRST searches the iFixit API for matching
   guides, shows them, THEN asks Gemini using those guides' titles + steps
   as context and answers step-wise in Hinglish, with a battery/screen
   safety note. Without a Gemini key it says so clearly and still shows
   the guides — it never pretends the AI answered.
6. **Settings** — Gemini API key entry (password field, stored only on the
   phone, never in the code), clear offline cache, about / attribution.

## How Fix AI gets its key

The user enters his own Gemini key in the app: Settings > Gemini API Key.
The key is stored only in the phone's private SharedPreferences and is sent
only to Google's Generative Language API (`generativelanguage.googleapis.com`,
model `gemini-2.0-flash`), directly from the phone. There is NO key in this
repo, and no key may ever be committed here. Get a free key at Google AI
Studio (aistudio.google.com).

## Tech

- Native Kotlin, AndroidX, Material Components, RecyclerView.
- No Retrofit / Coil / Room — plain `HttpURLConnection` + `org.json` +
  a tiny image loader + file-based offline cache. Fewer dependencies =
  fewer cloud-build failure points on a phone-only setup.
- `minSdk 24`, `targetSdk 34`, `compileSdk 34`, AGP 8.2.2, Kotlin 1.9.22,
  Gradle 8.2, JDK 17 — the same versions that built Nova successfully
  (Run #4 / #5). Do not bump them without a build reason.
- Gradle is blocked in the Muse sandbox (no Java), so this project is
  **cloud-build-ready only** — it has NOT been compiled or phone-tested yet.

## Build (cloud)

See `PUSH-README.md`. In short: create the repo, push this folder to
`main`, run the **Build Fix APK** workflow (or push to `main`, which also
triggers it), download the `fix-debug-apk` artifact.

## Legal notes

- Guide content: iFixit API 2.0, CC-BY — credit + original link on every guide.
- The iFixit name/logo/trademark is NOT used as this app's branding.
- The legacy repo `iFixit/iFixitAndroid` v2.9.2 was used only as a feature
  reference. None of its code or branding is in this app.
- No premium unlock, no FixBot copy, no hardcoded keys.
