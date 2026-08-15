# Exj2 — Java Games Emulator for Android

[![Build](https://github.com/Evvyinnit/exj2/actions/workflows/build.yml/badge.svg)](https://github.com/Evvyinnit/exj2/actions/workflows/build.yml)

**Exj2** is a fully working J2ME (Java ME) games emulator for Android — plays most
2D and 3D Java games, including Mascot Capsule 3D ones.

- Virtual keyboard, individual per-game settings, scaling support
- Android 4.0+

## Developer

Exj2 is developed by **Evvyinnit**.

## Getting the APK

The latest signed release APK is published on the [Releases](https://github.com/Evvyinnit/exj2/releases)
page — download and install it on your phone. Pushing a `v*` tag (e.g.
`git tag v1.0.0 && git push origin v1.0.0`) publishes a new signed release.

## Building locally

Requirements: JDK 17+, Android SDK (platform 34, build-tools 34.0.0, NDK 23.0.7599858).

```bash
./gradlew assembleOpenDebug          # debug APK, no signing needed
./gradlew assembleOpenRelease        # release APK (requires keystore.properties)
```

Release signing reads `keystore.properties` (`keyAlias`, `keyPassword`, `storeFile`,
`storePassword`).

## License

Licensed under the Apache License 2.0. See [LICENSE](LICENSE).