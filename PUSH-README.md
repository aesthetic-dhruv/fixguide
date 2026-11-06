# PUSH-README — how to build the Fix APK

Fix is **push-ready, not yet built**. No APK exists yet and nothing has been
pushed. Do these steps in order.

## 1. Create the repo (Raj does this — Muse cannot create repos)

1. GitHub (browser, signed in as `aesthetic-dhruv`) > New repository:
   - **Name:** `fixguide` (repo name can stay `fixguide` — the APP name is
     "Fix" and the package is `com.raj.fix`; only the repo slug keeps the
     old word so the link stays guessable)
   - **Visibility:** Public
   - **Do NOT** tick "Add a README" — the repo must start EMPTY.
2. Install / confirm the Muse GitHub App on this repo:
   GitHub > Settings > Applications > Muse > Repository access > select
   `aesthetic-dhruv/fixguide` > Save. (If the App was already installed on
   "all repositories", this step is already done.)
3. Tell Muse "repo ban gaya, push karo" — Muse will then push this folder.

## 2. What gets pushed

The whole `~/workspace/repair-guide-app/` folder, to branch `main`:

- `app/` — full Kotlin source, package `com.raj.fix`, version 1.0 (code 1)
- `gradle/`, `settings.gradle`, `build.gradle`, `gradle.properties`
- `.github/workflows/android.yml` — **Build Fix APK** workflow.
  This workflow is copied from Nova's working workflow (JDK 17, Android SDK,
  Gradle 8.2, `gradle assembleDebug`, artifact `fix-debug-apk`).
  **Do not replace it with a local workflow file and do not add a
  setup-android step that failed before.** If the build fails, fix only the
  exact error the job log names, then re-run once.
- `logo.png` — original black "Fix" logo (repo-root asset, not in the APK
  except via the `mipmap-*` launcher PNGs)
- `README.md`, this file, `BUILD-REPORT.md`

## 3. Build

- **Automatic:** pushing to `main` triggers the workflow.
- **Manual:** GitHub repo > Actions > **Build Fix APK** > Run workflow.
- Wait for the run to finish, then download the artifact named
  **`fix-debug-apk`** — it contains `app-debug.apk`.

## 4. After the build (do NOT skip)

1. Verify the BUILT APK itself (not just the source) with the
   `verify_cloud_built_apk` skill: package `com.raj.fix`, versionCode 1,
   versionName 1.0, label "Fix", black "Fix" logo, no "iFixit" branding.
2. Install on Raj's phone and test, in order:
   - Search "iPhone 14 Battery Replacement" — 6 results, guide 152966 opens
     with 27 steps, tools, images.
   - Browse Phone > Android Phone > Samsung Android Phone.
   - Save Offline, then switch on aeroplane mode and reopen it from Saved.
   - Fix AI **without** a key: guides show + "Settings me Gemini key daalo".
   - Fix AI **with** the key (entered in Settings, in-app only): Hinglish
     step-wise answer with safety note.
3. Only after that test is the APK "delivered". A successful cloud build
   alone is NOT proof the app works on the phone.
