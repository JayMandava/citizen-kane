# Third-party notices

## NTR font

The unmodified NTR-Regular font is from [Google Fonts](https://github.com/google/fonts/tree/main/ofl/ntr). The supplied binary was compared byte-for-byte with that official source. Its [complete SIL Open Font License 1.1](LICENSES/NTR-OFL-1.1.txt) is retained with this exact copyright notice:

> Copyright (c) 2012 Silicon Andhra (fonts.siliconandhra.org). Copyright (c) 2011, Joe Prince, Admix Designs (http://www.admixdesigns.com/) with Reserved Font Name Varela Round.

The complete text is also bundled in the Android APK and iOS font directory. The Android Licenses screen exposes the copyright and license without a network connection.

## Android runtime

AndroidX (including Compose, Material icons, Activity, Lifecycle, Core and Browser), Kotlin, Kotlin coroutines, Kotlin serialization, JetBrains annotations, Guava ListenableFuture and JSpecify declare Apache-2.0. The [complete Apache License 2.0](LICENSES/Apache-2.0.txt) is included. The exact resolved runtime coordinates are listed in `android/app/src/main/assets/THIRD_PARTY_NOTICES.txt`, which is distributed in the APK and displayed by the Licenses screen. No upstream NOTICE files were present in the audited runtime archives.

jsoup 1.18.3 uses the [MIT License](LICENSES/jsoup-MIT.txt), including Copyright (c) 2009-2024 Jonathan Hedley <https://jsoup.org/>. The complete MIT text is explicitly packaged as an asset to preserve it through APK resource merging.

## iOS dependency

SwiftSoup 2.13.9 is fetched through SwiftPM rather than vendored here. Its [MIT license](LICENSES/SwiftSoup-MIT.txt) retains Copyright (c) 2009-2025 Jonathan Hedley and Copyright (c) 2016-2025 Nabil Chatbi (Swift port). SwiftSoup also includes Nick Lockwood's LRUCache; its [complete MIT notice](LICENSES/LRUCache-MIT.txt), including Copyright © 2021 Nick Lockwood, is provided for attribution. This repository publishes no iOS binaries.

## Build and test tools

The Gradle wrapper is Apache-2.0; its original script copyright headers and wrapper license are retained. Test-only JUnit 4.13.2 declares EPL-1.0 and is not part of Android release runtime artifacts. Build/test dependencies are resolved from upstream repositories, not vendored into this source snapshot. Test APKs are not release assets.

## Artwork and fixtures

The simple sled launcher vectors come from the project-supplied Citizen Kane artwork, published as part of the application source with the owner's authorization. No third-party artwork attribution was present in that supplied source; it is not presented as an independently licensed external asset. Material icons retain their Apache-2.0 attribution above. Runtime publisher images are not bundled in source or release assets.

Committed HTML/XML test fixtures are authored synthetic examples, not recorded publisher pages. Historical app screenshots containing publisher articles, private QA logs, local paths and device metadata are excluded from this public snapshot.

No project-wide application-source license has been selected. Third-party components retain their individual licenses as documented here.
