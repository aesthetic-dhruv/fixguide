# BUILD-REPORT — Fix v1.0 (2026-11-06)

## Identity (final, after Raj's correction)

- **App name / label: Fix** (NOT FixGuide — renamed everywhere)
- **Package: `com.raj.fix`** — ONE package only. The earlier `com.raj.fixguide`
  is gone from all code, Gradle files and the manifest. (The GitHub repo slug
  suggested in PUSH-README is still `fixguide` — that is only a repo name,
  not the app name or package.)
- **Version:** 1.0, versionCode 1
- **Logo:** **Raj's own logo (final, sent 2026-11-06 13:20 IST)** — crossed
  hammer + wrench, white tools on a black background, no text. Original
  user file saved as `logo-user.png` (607×350, workspace/user/files/
  1000005676_0_443d.png); `logo.png` (512×512, visually checked) is that
  design composited white-on-black in a black rounded square. Launcher
  PNGs in `mipmap-mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi` + adaptive-icon XMLs
  regenerated from it. Earlier generated logos kept as
  `logo-with-fix-text.png` and `logo-old-no-screwdriver.png`. It does NOT
  copy iFixit's logo, wordmark or trademarked design.

## What was built

Complete native Kotlin app in `~/workspace/repair-guide-app/` (55 files,
264 KB), push-ready, no APK yet:

- **Search** (`HomeFragment`): live iFixit API search + 4 quick chips.
- **Browse** (`BrowseFragment`): Phone > iPhone / Android Phone > Samsung /
  Xiaomi / OnePlus… via categories API, with back-stack.
- **Guide detail** (`GuideDetailActivity`): steps with images, tools, parts,
  difficulty, time, conclusion, Save Offline, Favourite, Open Original,
  CC-BY attribution footer (author + "Source: iFixit" + original URL).
- **Saved** (`SavedFragment`): offline guides + favourites.
- **Fix AI** (`AiFragment` + `GeminiClient`): searches iFixit FIRST, shows
  guides, then — only with a key — asks Gemini (`gemini-2.0-flash`) with the
  guides' steps as context and answers in Hinglish with a battery/screen
  safety note. No key = clear "Settings me Gemini key daalo" + guides only.
- **Settings** (`SettingsActivity`): Gemini key (password field, phone-local
  only), clear offline cache, about/attribution.
- **Data layer:** `IfixitApi` (HttpURLConnection + org.json, no key),
  `Models` (parsers matched to live JSON), `ImageLoader` (no extra lib),
  `OfflineStore` (raw guide JSON files, no Room), `Prefs` (key + favourites).
- **Build:** AGP 8.2.2 / Kotlin 1.9.22 / Gradle 8.2 / JDK 17 / SDK 34 —
  the exact versions that built Nova (Run #4/#5). Workflow
  `.github/workflows/android.yml` is Nova's working workflow, renamed to
  **Build Fix APK**, artifact `fix-debug-apk`. No setup step that failed
  before was reintroduced.

## What was VERIFIED (with evidence)

1. **iFixit API live, re-checked 2026-11-06 with Python:**
   - `GET /api/2.0/search/iPhone%2014%20Battery%20Replacement` →
     totalResults 6; first guides 152973 (14 Pro), 152966 (14), 153339
     (14 Plus); difficulty Moderate, time_required_max 7200.
   - `GET /api/2.0/guides/152966` → **27 steps**; step fields `title`,
     `lines[]` (`text_raw`, `bullet`, `level`), `media.type=image` with
     `standard/medium/thumbnail` URLs; `tools[]` (11 items, incl. P2
     Pentalobe, Spudger, Isopropyl Alcohol, optional flags); `parts[]`
     (iPhone 14 Battery + adhesive); `author.username = Tobias Isakeit`;
     `time_required_min 3600`, `max 7200`; `conclusion_raw` present.
   - `GET /api/2.0/categories/Phone` → children iPhone, Android Phone,
     Windows Phone…; `Android Phone` → Samsung, Xiaomi, OnePlus, Google,
     Realme… + 19 guides. Parsers in `Models.kt` use exactly these fields.
2. **Automated source check (script run, 2026-11-06):**
   - All 25 XML files well-formed; all 14 Kotlin files start with
     `package com.raj.fix`; braces/parens/brackets balanced in every file.
   - Cross-check: every `R.id.*`, `R.layout.*`, `@color/*`, `@string/*`,
     `@menu/*`, `@style/*` referenced in Kotlin/XML exists — **0 missing**
     (63 ids defined, 11 layouts).
   - All 16 launcher PNGs valid (48–192 px) + `logo.png` 512 px.
   - No hardcoded key (`AIza` — none), no `FixGuide`/`fixguide` left in
     code, Gradle, manifest or workflow. The only 2 "fixguide" hits in the
     whole project are INTENTIONAL doc mentions: the suggested repo slug
     `aesthetic-dhruv/fixguide` and the README warning never to use the old
     package name.
   - "iFixit" appears only as: API base URL/docs, error messages naming the
     data source, guide attribution, and README legal notes — never as app
     branding. No iFixit logo/wordmark anywhere.
3. **Logo visually checked:** black rounded square, bold white "Fix" —
   clean at 512 px (see `logo.png`).

## What is NOT verified (do not claim otherwise)

- **No local compile:** Gradle is blocked in this sandbox (no Java), so
  the project has NOT been compiled here.
- **No cloud build yet:** no repo exists yet, nothing pushed, no APK.
- **No phone test:** search, guide view, offline, Fix AI with/without key
  are all untested on a real phone. A successful cloud build alone will
  NOT prove the app works — built-APK verification + the phone test list
  in PUSH-README §4 are still required.

## Exact next step

1. Raj creates an EMPTY public repo **`aesthetic-dhruv/fixguide`** in the
   browser and selects it for the Muse GitHub App (see `PUSH-README.md` §1).
2. Muse pushes `~/workspace/repair-guide-app/` to `main`.
3. Run Actions > **Build Fix APK** (or it auto-runs on push); download
   artifact **`fix-debug-apk`** → `app-debug.apk`.
4. Verify the BUILT APK per `verify_cloud_built_apk` (package `com.raj.fix`,
   v1.0/code 1, label "Fix", black logo), then run the phone test list.
