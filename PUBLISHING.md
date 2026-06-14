# Publishing this app to Google Play

A step-by-step guide to take this libGDX game from source to a live (or testing)
Google Play listing, including generating the signing keystore.

> **Relaunching the existing app after a lost key?** This guide is the general
> publishing reference. The lost-signing-key relaunch (new package name, retiring the
> old listing) has its own runbook: [`KEYSTORE_SETUP.md`](KEYSTORE_SETUP.md). Read that
> one if you're replacing `com.mdinic.game.giana`. Otherwise, follow this file.

---

## 0. Prerequisites

- **A Google Play Developer account** — one-time US$25 registration at
  <https://play.google.com/console>. Newer accounts go through identity verification;
  allow a few days for that the first time.
- **The Android SDK** installed, with `local.properties` containing
  `sdk.dir=<path to SDK>` (this repo already expects that).
- **A JDK 17–21 registered for the Android module's toolchain.** This project pins the
  android module to a JDK 21 toolchain (see `CLAUDE.md` → Toolchain). `keytool` ships
  with any JDK and is on your PATH.
- **A privacy policy URL** if your app collects any user data. This build submits a
  player **name + score** to a Supabase backend, so a privacy policy is required and
  the Data safety form must declare it (see step 6).

---

## 1. Choose the app identity

In `android/build.gradle`, the `defaultConfig` block defines what Play uses:

```groovy
applicationId "com.mdinic.game.giana.reborn"   // PERMANENT Play identity + store URL
versionCode 1                                   // integer; MUST increase every upload
versionName "2.0"                               // human-facing version string
```

- **`applicationId`** is reverse-DNS, lowercase, ≥2 segments, each starting with a
  letter (letters/digits/underscores only — no hyphens). It is **permanent** once
  published and cannot be reused even after deleting the app. It appears in the store
  URL: `play.google.com/store/apps/details?id=<applicationId>`.
- **`versionCode`** is an integer Play uses to order builds. Every upload to a track
  must have a **strictly higher** `versionCode` than the last, or Play rejects it.
- **`versionName`** is just a label shown to users (e.g. `2.0`, `2.0.1`); it has no
  ordering rules.
- **`namespace`** (above `compileSdk`) is the Java/R-class package and is independent
  of `applicationId` — you normally leave it alone.

---

## 2. Generate the upload keystore (one time)

Your app is signed with a key. **Lose the key and you can never update the listing** —
so generate it once, then back it up (step 8). Run:

```powershell
keytool -genkeypair -v \
  -keystore C:\Users\midi\keys\giana-upload.jks \
  -alias giana \
  -keyalg RSA -keysize 2048 -validity 30000 \
  -storetype JKS
```

(macOS/Linux: same command with `\` line-continuations replaced by `\` → `\`, i.e. use
`\` at end of line in bash, and a Unix path like `~/keys/giana-upload.jks`.)

You'll be prompted for:
- a **keystore password**, then a **key password** (press Enter to reuse the keystore
  password — simplest),
- certificate details (name / org / locality). These are baked into the cert; for a
  personal release they're not critical, but they can't be edited later.

Notes:
- `-validity 10000` ≈ 27 years. Play wants the key valid well beyond any future update.
- **Store the `.jks` OUTSIDE the git repo** (e.g. `C:\Users\midi\keys\`). The repo
  gitignores `*.jks`/`*.keystore` as a backstop, but keeping it out entirely is safest.

---

## 3. Point the build at the keystore

`android/build.gradle` reads signing credentials from the **gitignored**
`local.properties` in the repo root. Add (or uncomment) these four lines and fill in
your real values:

```properties
giana.keystore.file=C:\\Users\\midi\\keys\\giana-upload.jks
giana.keystore.storePassword=<your keystore password>
giana.keystore.keyAlias=giana
giana.keystore.keyPassword=<your key password>
```

(Use `\\` for backslashes in a `.properties` file. Path may be absolute or relative to
the repo root.)

If `giana.keystore.file` is left unset, **release builds are produced unsigned** — the
build script degrades gracefully so CI and other machines aren't blocked. You just
can't upload an unsigned build to Play.

---

## 4. Build the release App Bundle

Google requires the **`.aab`** (Android App Bundle) format for new apps — not `.apk`.

```powershell
.\gradlew android:bundleRelease
```

Output: `android\build\outputs\bundle\release\android-release.aab`

This is the file you upload to Play. (An APK for sideload testing comes from
`.\gradlew android:assembleRelease` → `android\build\outputs\apk\release\`, but Play
takes the `.aab`.)

**Verify your build is signed** before uploading:

```powershell
jarsigner -verify -verbose -certs `
  android\build\outputs\apk\release\android-release.apk
```

(Or inspect the `.aab` after Play processes it. An unsigned build means step 3 wasn't
picked up.)

---

## 5. Create the app in Play Console

1. Go to <https://play.google.com/console> → **Create app**.
2. Fill in app name, default language, app vs. game, free vs. paid, and accept the
   declarations.
3. **Set up → App integrity → App signing:** confirm **Play App Signing is ON**
   (the default for new apps). With it on, **Google holds the app signing key** and you
   only manage the **upload key** you generated in step 2. The practical payoff: if you
   ever lose the upload key, it's a self-service reset — **not** a permanently dead
   listing. Keep this on.

---

## 6. Complete the required store content

Play won't let you release until these are done (Store presence + Policy sections):

- **Store listing** — title, short + full description, app icon (512×512), feature
  graphic (1024×500), and at least 2 screenshots per form factor you target (phone is
  enough to start).
- **Data safety form** — declare what you collect. This game submits a **player name
  and score** to a Supabase backend, so declare that (collected, tied to the score,
  not sold). Be accurate; this is enforced.
- **Content rating** — fill the questionnaire; you get an IARC rating.
- **Privacy policy URL** — required because you collect user data. Host a simple policy
  page and paste the URL.
- **Target audience & ads** — declare the age range and whether the app shows ads.
- **App access** — note that the game requires no login (so reviewers can test it).
- **Target API level** — Play enforces a recent minimum; `targetSdk 36` in this project
  already satisfies it.

---

## 7. Roll out

Start on a **testing track** so you catch problems before the public does:

1. **Testing → Internal testing → Create release.**
2. Upload `android-release.aab`. (First upload to a Play-App-Signing app may ask you to
   confirm letting Google generate/manage the app signing key — accept.)
3. Add release notes, add your own Google account as an internal tester, save & roll
   out. Install via the opt-in link on your device and verify it runs.
4. When satisfied, **promote** the release to **Closed/Open testing** or straight to
   **Production**. Production rollouts can be staged (e.g. 20% → 100%).

After production review (hours to a couple of days for a new app), the listing goes
live at `play.google.com/store/apps/details?id=<applicationId>`.

---

## 8. Back up the keystore — do this immediately

This is the step everyone skips and later regrets. Store the `.jks` file **and** its
passwords in **at least two independent places**:

- [ ] A password manager (attach the `.jks`; store both passwords).
- [ ] An encrypted off-machine backup (cloud or external drive — not the same disk as
      the repo).
- [ ] (Optional) An offline written copy of the passwords somewhere safe.

Even with Play App Signing on (which makes upload-key loss recoverable), back it up
anyway. **Never commit the keystore or `local.properties` to git** — both are
gitignored in this repo; keep it that way.

---

## Quick reference

| Step | Command / location |
|------|--------------------|
| Generate keystore | `keytool -genkeypair -v -keystore <path>.jks -alias giana -keyalg RSA -keysize 2048 -validity 10000` |
| Configure signing | edit root `local.properties` (`giana.keystore.*`) |
| Build bundle | `.\gradlew android:bundleRelease` → `android\build\outputs\bundle\release\android-release.aab` |
| Build test APK | `.\gradlew android:assembleRelease` → `android\build\outputs\apk\release\` |
| Verify signature | `jarsigner -verify -verbose -certs <apk>` |
| Bump for next release | raise `versionCode` (and usually `versionName`) in `android/build.gradle` |
| Console | <https://play.google.com/console> |
