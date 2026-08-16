# Ex Emu — Multi-System Emulator for Android

[![Build](https://github.com/Evvyinnit/ex-emu/actions/workflows/build.yml/badge.svg)](https://github.com/Evvyinnit/ex-emu/actions/workflows/build.yml)

**Ex Emu** is a fully working, all-in-one game emulator for Android. It plays
classic **Java (J2ME)** mobile games plus **30+ console and handheld systems** —
Nintendo (NES, SNES, GB/GBC/GBA, NDS, 3DS, N64), Sega (Genesis, Master System,
Game Gear, Sega CD), Sony (PS1, PSP, PS2), arcade (FBNeo, MAME), Atari
(2600/7800/Lynx), PC Engine, Neo Geo, Virtual Boy, DOS and more.

Emulation cores are downloaded on demand from the internet and cached on your
device — the APK stays small.

- **Java (J2ME) games**: load `.jar`/`.jad` files, per-game keyboard mapping,
  scaling, virtual keyboard, saves, Mascot Capsule 3D support
- **Consoles**: libretro cores (mGBA, melonDS, mupen64plus, Snes9x, FCEUmm,
  PPSSPP, PCSX ReARMed, Play!, Citra, DosBox Pure, …)
- Android 6.0+

## Developer

Ex Emu is developed by **Evvyinnit**.

## Getting the APK

Every push to `main` automatically builds a signed release APK in
[GitHub Actions](https://github.com/Evvyinnit/ex-emu/actions) — download the
`ExEmu-2.0.0-release` artifact from the latest successful run (an emulator
smoke test proves the app launches before the run is green).

Tagged releases are published on the [Releases](https://github.com/Evvyinnit/ex-emu/releases)
page. Pushing a `v*` tag (e.g. `git tag v2.0.0 && git push origin v2.0.0`)
builds the APK and attaches it (with SHA-256 checksums) to the release
automatically.

## Building locally

Requirements: JDK 17+, Android SDK (platform 35, build-tools 34.0.0, NDK 23.0.7599858).

```bash
./gradlew assembleDebug          # debug APK, no signing needed
./gradlew assembleRelease        # release APK (requires keystore.properties)
```

Release signing reads `keystore.properties` (`keyAlias`, `keyPassword`, `storeFile`,
`storePassword`); without it the release build is signed with the debug keystore.

## License

- App and libraries: GPL-3.0, see [COPYING](COPYING)
- J2ME emulation engine: Apache License 2.0, see [LICENSE](LICENSE)