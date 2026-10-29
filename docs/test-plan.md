# Android Test Plan

## 1. Environment matrix

| Profile | AVD | System image | Resolution | API | Status |
| --- | --- | --- | --- | --- | --- |
| Android TV | `ZeltaTV_TV_1080p` | `android-34;android-tv;x86` | 1920x1080 | 34 | Boot + APK + D-pad smoke |
| Phone | `ZeltaTV_Phone` | `android-35;google_apis;x86_64` | 1080x2400 | 35 | Boot + APK + compact UI smoke |
| Tablet | `ZeltaTV_Tablet` | `android-35;google_apis;x86_64` | 2560x1600 | 35 | Boot + APK smoke |

Environment: Windows, JDK 21, Android SDK, emulator, Gradle 8.11.1, AGP 8.7.3, Kotlin 2.0.21,
Media3 1.8.0.

## 2. Automated tests

### JVM

- `:core:playlist:test`
- M3U attribute/CRLF/BOM/comma/blank attribute/repeated URL scenarios.

### Instrumented

- `:app:connectedDebugAndroidTest`
- App launch, D-pad down/centre/right, staying in the foreground and a screenshot.

### Script smoke

`scripts/android/run-smoke.ps1`

- APK installation
- Cold start
- `mCurrentFocus` check
- Screenshot and crash log (`artifacts/android/`)

## 3. Manual verification (1.0.0, 2026-10-21)

### Android TV 1080p

- [x] Welcome wizard: language, source step, library step, ready step
- [x] Setup completion persists `setup_done`; restart opens the home shell
- [x] Home: top bar, 256 dp channel rail, empty player and channel list
- [x] D-pad focus: channel list, main menu, channel menu, settings, sources, about
- [x] OK opens the channel overlay; channel switching and HLS playback
- [x] MENU opens the six-tile desktop-style main menu
- [x] OSD seek (`+10s`), numeric zap, controls auto-hide
- [x] Settings tabs: Shortcuts, Playback, Language, Theme, Data and About
- [x] Sources: URL add, HTTP 404 error banner, delete confirmation and error clear
- [x] Categories: create, assign to a source, delete
- [x] About: branding, version, GitHub/licence links, feature list
- [x] Back closes the channel overlay / menus before leaving the screen

### Phone 1080x2400

- [x] First-run welcome/source/ready flow and demo completion
- [x] Compact home: channel list, search, filters, touch selection
- [x] Compact player: banner, OSD, seek bar, playback controls, channel overlay
- [x] Main menu in two columns, settings sections and theme cards
- [x] Status-bar insets: top bar is not covered by the system bar
- [x] No duplicate channel button overlapping the source label

### Tablet 2560x1600

- [x] Two-pane layout and list/player selection (M1 regression)

## 4. Manual acceptance scenarios

### TV / remote

- [x] Navigating the channel list with the D-pad, focus ring visible
- [x] Opening a channel with OK, back behaviour
- [x] Zapping by channel number
- [x] Channel menu / OSD
- [x] Full-screen immersive; system bars hidden
- [ ] Player state when the app is backgrounded and brought back
- [ ] Physical remote/controller matrix (Xbox, NVIDIA Shield, generic IR)

### Phone / tablet

- [x] Touch channel selection
- [x] Readability on narrow screens
- [ ] Portrait/landscape rotation
- [ ] PiP and background audio (M3)
- [x] Back key behaviour

### Network / playback

- [ ] HLS server without a CORS header (must play with native OkHttp)
- [ ] 4K HEVC sample stream (on a supporting device/emulator)
- [ ] AC-3/E-AC-3 audio (on a suitable device)
- [x] Retry and a user-facing error on a failed URL (HTTP 404)
- [ ] HTTP (cleartext) and self-signed TLS scenario
- [ ] Custom User-Agent / Referer

### Persistence

- [x] Last source and last channel after closing and reopening the app
- [x] Favourites and history
- [x] Theme/language preference
- [x] File import permission (SAF persistable URI)

## 5. Performance

- `adb shell dumpsys meminfo tv.zelta.app`
- `adb shell top -o %CPU -n 1`
- `adb shell dumpsys gfxinfo tv.zelta.app`
- Measure at 1080p on the emulator instead of 4K; evaluate 4K separately on a real device.

## 6. Known limitations

- The release APK is debug-signed; a Play Store AAB needs a private keystore and store account.
- Wide colour/HDR and real hardware codec behaviour cannot be verified on the emulator.
- Public test streams depend on the network; playback tests are not mandatory in CI.
- Physical remote/controller and NVIDIA overlay behaviour need real hardware.
- On Windows, a project path with non-ASCII characters can break the Gradle test worker; use an
  ASCII drive with `subst` (README).
