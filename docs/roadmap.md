# Android Roadmap

## M0 - Skeleton and test environment (done)

- Native Compose + Media3 skeleton
- M3U parser and JVM tests
- Three AVDs (TV/phone/tablet), launch and smoke scripts
- Debug APK, UiAutomator D-pad test
- Architecture decision and test plan

## M1 - Real application skeleton (done)

- [x] SharedPreferences + JSON source/favourite/history store
- [x] URL and SAF file import
- [x] Real channel list, search, favourite filter
- [x] Native Media3 HLS playback, error/retry
- [x] Two-pane TV and single-pane phone D-pad/touch layout
- [x] Last source/channel persistence
- [x] Room + categories, DataStore theme/language
- [x] Channel menu/OSD and full settings screen
- [x] Publish the debug APK as a GitHub Actions artifact

## M2 - Product parity (done)

- [x] Desktop theme palettes with seven colour options
- [x] Channel number OSD, channel banner, quality selection
- [x] Immersive playback surface and TV focus handling
- [x] Two-pane phone/tablet layout
- [x] Desktop-parity setup, sources, settings, about and main menu screens
- [x] Turkish/English interface and seven theme palettes
- [x] 1.0.0 release APK (`zeltatv.apk`)

## M3 - Store readiness (remaining)

- [ ] Signed release AAB/APK with a private keystore
- [ ] Android TV Play Store requirements (banner, leanback, quality)
- [ ] Privacy policy and data safety form
- [ ] Closed beta / Firebase App Distribution
- [ ] Performance and battery profile on low-end TV hardware
- [ ] Physical remote/controller matrix (Xbox, NVIDIA Shield, generic IR)
- [ ] PiP and background audio
- [ ] 4K/HEVC and codec fallback on real devices
