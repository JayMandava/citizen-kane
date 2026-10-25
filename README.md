# Citizen Kane

A Telugu news and rashi reader with native Android and iOS source. The apps combine five public news providers, article previews, horoscope predictions, appearance settings, and optional morning reminders.

Releases from this repository target Android. The iOS directory is source-only; no iOS binaries or App Store releases are produced here.

## Project layout

- `android/` — Kotlin, Jetpack Compose, coroutines, Jsoup and Android Custom Tabs.
- `ios/` — SwiftUI app, shared Swift models/providers, Swift Testing tests and XcodeGen specification.
- `LICENSES/` — complete third-party license texts and notices.
- `THIRD_PARTY_NOTICES.md` — font, library and artwork provenance.

Both apps have News and Rashi tabs, five independently configurable providers, twelve bilingual rashi signs, System/Light/Dark appearance, and an optional daily reminder. The Android version uses the NTR font, a monochrome gradient, rounded cards and floating tabs. Native platform chrome and reminder timing differ; pixel-identical parity is not claimed.

## Build Android

Requirements: JDK 17, Android SDK platform 36 and network access to resolve public build dependencies.

```sh
cd android
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Set `JAVA_HOME` and `ANDROID_HOME`, or use an untracked `android/local.properties` containing your SDK location. The Gradle wrapper, Android Gradle Plugin and Kotlin versions are pinned.

The debug package is `dev.jeyanth.citizenkane.debug`; the release package is `dev.jeyanth.citizenkane`. Minimum Android is API 26 and target Android is API 36. Running `:app:assembleRelease` without an explicitly configured production key produces an **unsigned APK**, not an installable production release. Signing keys and passwords must remain outside Git.

## Build iOS source

Requirements: Xcode supporting iOS 26 and XcodeGen. SwiftSoup is pinned to 2.13.9 through SwiftPM.

```sh
cd ios
DEVELOPMENT_TEAM= xcodegen generate
xcodebuild -project CitizenKane.xcodeproj -scheme CitizenKane \
  -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' \
  CODE_SIGNING_ALLOWED=NO build
```

For your own device build, supply your Apple development team locally when generating the project and configure the bundle identifier/app group for your account. Generated Xcode projects, team-specific signing configuration, provisioning profiles and build outputs are not committed.

## Test safely

All committed HTML/XML fixtures are authored synthetic examples. They preserve provider markup shapes, CDATA, image fields and date formats without redistributing recorded publisher articles or personal data. Android and iOS share the same synthetic fixture content.

```sh
cd android
./gradlew :app:testDebugUnitTest :app:assembleDebugAndroidTest
# Select a dedicated emulator or an authorized, unlocked device.
ANDROID_SERIAL=<serial> ./gradlew :app:connectedDebugAndroidTest
```

Debug fixture modes (`news`, `offline`, `error`, `loading`) use isolated app preferences/cache and stub external article/reminder actions. Release builds ignore fixture intents. Live public-provider checks are opt-in with instrumentation argument `live=true` and require network access; they use no accounts or private data.

Physical-device captures use only the app's own Activity/Dialog windows. Never unlock another person's device, clear app data, change default apps or grant permissions just to run tests.

## Data and reminders

Settings and the last successful news feed stay in private on-device storage. News comes from public provider endpoints; full articles open in the platform browser. No account, analytics service or paid API is configured. Android reminders require notification permission and use inexact local alarms, so battery restrictions may delay delivery.

## Licenses

NTR is bundled unmodified under SIL Open Font License 1.1. Its complete copyright and license are included in both platform source trees and `LICENSES/NTR-OFL-1.1.txt`. Android users can read NTR and runtime library notices from **Settings → Open-source licenses**. See [third-party notices](THIRD_PARTY_NOTICES.md) for the complete inventory and upstream links.
