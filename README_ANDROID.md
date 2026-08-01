# Android Kotlin app README

This branch adds a minimal Android application written in Kotlin that can be built into an APK.

How to build

1) Open the project in Android Studio (recommended). Android Studio will configure the Gradle wrapper and download required SDKs.
2) Alternatively, install Gradle and Android SDK command-line tools, then run:
   - gradle assembleDebug    (or ./gradlew assembleDebug if you generate a wrapper)

Notes
- Package name: com.thanh.tieuchi
- Minimum SDK: 21
- Compile/Target SDK: 33
- Kotlin: 1.8.10

If you want, I can:
- Add a Gradle wrapper into the repo so you can build from CLI with ./gradlew
- Configure CI (GitHub Actions) to build an APK automatically on push/PR
- Add signing configs and instructions for release builds

