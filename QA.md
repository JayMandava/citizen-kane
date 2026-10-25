# Validation and release scope

## Current source validation

| Check | Result |
|---|---|
| Android debug and unsigned release build | Passed with JDK 17 and SDK 36 |
| Android lint | Passed |
| Android JVM tests | 24 passed using authored synthetic fixtures |
| Android emulator UI tests | 13 passed on API 35, including rapid tab navigation, independent headers, offline cache, appearance/recreation and complete offline license notices |
| iOS simulator app/test build | Passed without signing |
| iOS synthetic tests | 20 passed in 8 suites on iOS 26.5 |

The pre-polish Android build also passed a synthetic core-flow scenario on an API 37 physical phone. The subsequent navigation/notice changes were validated on the emulator, and have not received another physical run.

The Android navigation polish adds a short standard Compose tab transition, keeps each tab's header/scroll state independent, and uses separate expanded/inline title sizes. Core provider, storage and reminder behavior is unchanged.

## Coverage still open

- API 26 runtime testing is not yet complete. The official SDK package required is `system-images;android-26;google_apis;arm64-v8a` (revision 3 at validation time). SDK/compile compatibility is not a substitute for that runtime test.
- Full publisher-page rendering after fresh-browser setup is not yet established. The app-to-browser handoff and Back navigation were verified; the fresh emulator browser presents its own first-run agreement.
- Actual scheduled notification delivery, reboot and timezone delivery behavior are not yet verified. Permission-denial handling was checked. Reminders use inexact alarms and can be delayed by platform battery management.
- Native platform chrome differs; this project does not claim pixel-identical Android/iOS rendering.

## Android release assets

Releases are Android-only. A production release must use an owner-approved dedicated signing key, a verified package/version policy, and a final source revision. Keys/passwords stay outside Git. An unsigned Gradle output and a debug-signed test APK are not production release artifacts.

Intended release assets are the production-signed Android APK, SHA-256 checksum, and third-party notices/licenses. Do not attach signing material, test APKs, private QA logs, iOS binaries or App Store artifacts.
