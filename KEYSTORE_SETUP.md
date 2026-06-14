# Release signing & Play Store relaunch

The original app `com.mdinic.game.giana` was published ~10 years ago, then its
**signing key was lost**. Because that listing used classic signing (the developer
held the only key), it can **never be updated again** — Google has no recovery path
for a self-managed key, and the package name is permanently burned.

The relaunch ships as a **new app**: `com.mdinic.game.giana.reborn`, with a **new
keystore** that is enrolled in **Google Play App Signing**. This document is the
checklist for doing that and never losing the key again.

---

## 1. Generate the upload keystore (one time)

Run this once. `keytool` ships with the JDK (already on your PATH).

```powershell
keytool -genkeypair -v `
  -keystore C:\Users\midi\keys\giana-reborn-upload.jks `
  -alias giana-reborn `
  -keyalg RSA -keysize 2048 -validity 10000 `
  -storetype JKS
```

- It will prompt for a **store password**, then a **key password** (you can press
  Enter to reuse the store password), then your name/org for the certificate.
- `-validity 10000` ≈ 27 years. Play requires the key to be valid well past any
  expected update; long validity is correct here.
- Put the file **outside the git repo** (e.g. `C:\Users\midi\keys\`). The repo
  gitignores `*.jks`/`*.keystore` as a backstop, but keeping it out entirely is safer.

## 2. Point the build at it

Edit the gitignored `local.properties` in the repo root and uncomment/fill in:

```properties
giana.keystore.file=C:\\Users\\midi\\keys\\giana-reborn-upload.jks
giana.keystore.storePassword=<the store password you chose>
giana.keystore.keyAlias=giana-reborn
giana.keystore.keyPassword=<the key password you chose>
```

(If `giana.keystore.file` is left unset, release builds are produced **unsigned** —
`android/build.gradle` degrades gracefully so CI/other machines aren't blocked.)

## 3. Build the release App Bundle

Google requires `.aab` (not `.apk`) for new apps:

```powershell
.\gradlew android:bundleRelease
```

Output: `android\build\outputs\bundle\release\android-release.aab`

(`android:assembleRelease` still produces an APK if you want one for sideloading/testing,
but the Play upload is the `.aab`.)

## 4. Play Console — create the new listing

1. **Create app** → name it, pick app/game + free/paid.
2. **Set up → App integrity → App signing:** ensure **Play App Signing is ON**
   (default for new apps). This means Google holds the *app signing key* and you only
   manage the *upload key* you just generated — if you ever lose the upload key again,
   it's a self-service reset, **not** a dead listing. This is the whole point.
3. **Create a release** (Internal testing track first is easiest) → upload the `.aab`.
4. Complete the required forms (these are stricter than 10 years ago):
   - **Data safety form** — the game submits a player **name + score** to a Supabase
     backend, so declare that data collection.
   - **Content rating** questionnaire.
   - **Privacy policy URL** (required when collecting any user data).
   - Target API level: `targetSdk 36` already satisfies current requirements.
5. Roll out to testing, verify, then promote to production.

## 5. Retire the old listing (after the new one is live)

- **Unpublish** the old `com.mdinic.game.giana` listing — do **NOT delete** it.
  Unpublishing hides it from the public but **retains your ratings, reviews, and
  stats in the Console** for your own reference. Deleting loses that data permanently.
- Two identical *live* listings can look like spam to automated checks; one
  *unpublished* old + one live new reads clearly as an owner migration. Same developer
  account on both is the key signal (you have this).
- Note: the old listing's public rating/review **counts do not transfer** to the new
  app — the relaunch starts at zero publicly. This is inherent to the lost-key
  situation. If anything ever gets auto-flagged, appeal with: *"I am the original
  developer; I lost the signing key for com.mdinic.game.giana and am republishing my
  own app, which Google documents as the standard remedy."*

---

## Back up the keystore — do this immediately

Losing this key repeats the original mistake. Store the `.jks` file **and** its
passwords in **at least two** independent places, e.g.:

- [ ] A password manager (store the file as an attachment + the two passwords).
- [ ] An encrypted cloud backup (not the same machine as the repo).
- [ ] (Optional) A printed/offline copy of the passwords kept somewhere safe.

With Play App Signing enabled (step 4.2), even a future upload-key loss is recoverable
— but back up anyway. Never commit the keystore or `local.properties` to git.
