# ZeltaTV Android

<p align="center">
  <img src="docs/images/hero.svg" alt="ZeltaTV Android - the same satellite-receiver IPTV player on TV, phone and tablet" width="100%">
</p>

Native Android TV, phone and tablet edition of [ZeltaTV](https://github.com/zeltatv/zeltatv), built
with Kotlin, Jetpack Compose and Media3/ExoPlayer. Same shell as the desktop app, designed for a
remote and for touch.

## Download

[**Download ZeltaTV APK**](https://github.com/zeltatv/android/releases/latest) - the APK is
debug-signed and intended for sideloading.

## Photo Gallery

<p align="center">
  <img src="docs/images/screenshots/06-player-landscape.png" alt="ZeltaTV Android playing a channel in landscape with the transport controls" width="100%">
</p>

| | |
|---|---|
| <img src="docs/images/screenshots/02-player.png" alt="Portrait playback: 16:9 player above the channel list" width="300"> | <img src="docs/images/screenshots/08-channels-landscape.png" alt="Landscape channel list" width="100%"> |
| <img src="docs/images/screenshots/01-sources.png" alt="Sources dialog on the Link tab" width="300"> | <img src="docs/images/screenshots/03-channels.png" alt="Channel list with search, favourites and history" width="300"> |
| <img src="docs/images/screenshots/04-menu.png" alt="Main menu tiles" width="300"> | <img src="docs/images/screenshots/05-settings.png" alt="Theme settings with seven colour palettes" width="300"> |

<p align="center">
  <img src="docs/images/screenshots/07-menu-landscape.png" alt="Main menu in landscape" width="100%">
</p>

## Features

- Desktop-identical shell: top bar, channel rail, full-bleed player, channel menu and settings
- 18 languages and seven colour palettes
- Sources by link, file or library, with categories and a tile library on first run
- Search, favourites, history, numeric zap and quality selection
- Android TV D-pad layout, phone portrait and landscape, 44 dp touch targets
- Restores the last source and channel on relaunch

## Build

```powershell
.\gradlew.bat :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/zeltatv.apk`

## Test

```powershell
.\gradlew.bat :core:playlist:test
```

## Licence

GPL-3.0-only.
