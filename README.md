# Exj2 — Java Games (J2ME) Emulator for Android

[![Build](https://github.com/Evvyinnit/exj2/actions/workflows/build.yml/badge.svg)](https://github.com/Evvyinnit/exj2/actions/workflows/build.yml)

**Exj2** is a fully working J2ME (Java ME) games emulator for Android, powered by the
excellent open-source [J2ME Loader](https://github.com/nikita36078/J2ME-Loader) engine
(which powers the original "J2ME Loader" app on Google Play).

- Plays most 2D and 3D Java games, including Mascot Capsule 3D ones
- Virtual keyboard, individual per-game settings, scaling support
- Android 4.0+

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
`storePassword`). CI supplies it from repository secrets.

## Credits & License

The emulator engine is [J2ME Loader](https://github.com/nikita36078/J2ME-Loader) by
Nikita Shakarun, licensed under the Apache License 2.0. See [LICENSE](LICENSE).
Mascot Capsule 3D implementation by [woesss](https://github.com/woesss).