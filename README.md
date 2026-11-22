# LIXXZY RAT — APK-to-APK consent-based starter

This version produces **two separate APKs** from one Android project:
- `LIXXZY-Agent.apk`: the device owner starts visible sharing and receives pairing requests.
- `LIXXZY-Controller.apk`: enters the pairing code and views basic device status.

This is a transparent device-status demo, not a stealth RAT. It does not provide hidden access, arbitrary commands, silent screen capture, microphone/camera access, or covert file extraction.

## Firebase setup
1. Create a Firebase project and enable Realtime Database.
2. Enable Authentication → Sign-in method → Anonymous.
3. Register Android package `com.lixxzy.rat.agent` in Firebase and download `google-services.json`.
4. Place it at `android/app/google-services.json`.
5. In Firebase Console → Realtime Database → Rules, use the included `database.rules.json` for a temporary test only.
6. Build with GitHub Actions → Build APKs. Download artifact `LIXXZY-Agent-and-Controller-APKs`.

## Pairing
1. Install `LIXXZY-Agent.apk` on the phone whose owner wants to share status.
2. Open it and tap **Start sharing**. Keep the app open; it displays a six-digit code.
3. Install `LIXXZY-Controller.apk` on the controller phone.
4. Enter the code. The controller can view device name, Android version, battery and last-seen time.
5. The Agent owner can stop sharing at any time. Controller can send a disconnect request.

## Important security note
The included database rules are permissive for a quick prototype and anonymous authentication is not strong identity verification. Do not use this configuration for sensitive/private devices or public deployment. Production requires server-validated, expiring pairing tokens, per-device authorization, App Check, and strict database rules. Pairing codes are not a substitute for production authorization.

## Build
GitHub Actions builds two APKs:
- `android/app/build/outputs/apk/agent/debug/app-agent-debug.apk`
- `android/app/build/outputs/apk/controller/debug/app-controller-debug.apk`
