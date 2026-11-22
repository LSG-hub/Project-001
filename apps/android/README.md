# Project 001 · Android app

Kotlin + Jetpack Compose (Material 3). UI preview with sample data; calls, Gemini and sign-in are not connected yet.

- **Screens:** sign-in (country picker + mobile number, verified by a call to this phone, 6-digit voice code as fallback), Calls, call summary, live screening, Family with scam alerts, Assistant, You (settings, Simple mode), Language (search, per-app language), Setup.
- **Adaptive:** bottom bar under 600dp wide, navigation rail at 600dp+, list and summary side by side at 840dp+. Tested at 360×640dp, a phone (411×923dp), a tablet in landscape and 200% text.
- **Text:** every UI string is in `res/values/strings.xml`, ready for translation. Right-to-left languages mirror automatically.
- **Fonts:** Bricolage Grotesque (titles) and Geist (body), both OFL, bundled in `res/font`. Indian scripts use the system's Noto fonts.

## Build and run

Needs JDK 17+ and the Android SDK (`ANDROID_HOME`), with platform 37.2 installed.

```bash
cd apps/android
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Simulate an incoming call on the emulator: `adb emu gsm call +919876543210`.

## Testing phone verification

Sign-in proves the number by a call to this phone: the app holds the call-screening role, waits up to 60 seconds after "Ring my phone", and silently declines the call that comes from the verification number. Every other call is left alone.

Until the server places these calls, debug builds give you two ways in:

- **Voice code:** tap "Get a voice code instead" and enter any 6 digits. Release builds accept no code until the real backend checks it.
- **Call, on the emulator:** tap "Ring my phone", then run `adb emu gsm call 9999900001` (the placeholder verification number).

Android only asks a call-screening app about callers who aren't in the phone's contacts, so the verification number must not be saved on the phone; if it is, the voice code is the way in. Android also logs the declined call as blocked in the call log; a screening app can't turn that off.

The package name `app.project001` is a working name. Choose the final one before the first Play Store upload; it can't change afterwards.

## Dev builds for testers

`./gradlew :app:assembleDev` builds **001 Dev** (`app.project001.dev`): debuggable, with the same test conveniences as debug, and installed next to the store app. It's signed with a fixed dev key so each new build installs over the last one.

- **Automatic:** every merge into `dev` that touches `apps/android` runs `.github/workflows/android-dev.yml`. It builds 001 Dev and sends it with Firebase App Distribution to the `dev-testers` group, who get an email with an install link. The release notes are the commit messages.
- **By hand:** `npx firebase-tools appdistribution:distribute app/build/outputs/apk/dev/app-dev.apk --app <dev app id> --groups dev-testers --project project-001-474105`
- **Signing:** the keystore and its password never go in the repo. Locally they're in `local.properties` (`devSigning.storeFile`, `devSigning.password`). In CI they're the secrets `ANDROID_DEV_KEYSTORE_BASE64` and `ANDROID_DEV_KEYSTORE_PASSWORD`, plus `FIREBASE_ANDROID_DEV_APP_ID`.
- **Testers:** manage them in the Firebase console under App Distribution → Testers & Groups, or with `firebase appdistribution:testers:add <email> --group-alias dev-testers`.
