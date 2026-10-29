# ZeltaTV Android — Architecture

> Status: **M1 (first working port)**. This document records the direction of the port and the
> technical decisions. Native Compose + Media3 playback, URL/file source management, search,
> favourites, a TV/phone-friendly layout and session persistence work. Source management is
> currently SharedPreferences + JSON; Room arrives in M2.

## 1. Summary decision

**The Android app will be written as native Kotlin + Jetpack Compose + AndroidX Media3/ExoPlayer.**
A Capacitor/WebView-based wrapper is **not the product path**; it may only be considered as a
one-off spike for design validation if requested.

| Criterion | Native (Media3) | Capacitor + hls.js (WebView) |
| --- | --- | --- |
| HTTP without CORS | ✅ OkHttp DataSource requests directly | ❌ `fetch`/XHR is subject to CORS, `hls.js` cannot fetch segments without CORS |
| 4K HEVC / AV1 | ✅ Hardware decoder, codec fallback, tunnelled playback | ⚠️ Device/WebView dependent, no control, memory/CPU risk at 4K |
| HDR / colour management | ✅ Platform player path | ❌ No guarantee on the WebView video surface |
| AC-3/E-AC-3/DD+ audio | ✅ Codec and passthrough configurable | ❌ WebView codec support limited by device |
| MPEG-TS / RTSP / UDP | ✅ Media3 sources (RTSP, TS) | ❌ HLS/DASH only (MSE) |
| Custom UA / Referer / Cookie | ✅ OkHttp interceptor | ⚠️ Modifying WebView requests is fragile |
| Live HLS control (buffer, retry, live edge) | ✅ Detailed API | ⚠️ hls.js settings limited, blocks the JS thread |
| D-pad / TV focus | ✅ Compose focus + Leanback launcher | ⚠️ Web focus ring works but system TV behaviour is missing |
| PiP / background / audio focus | ✅ Platform APIs | ❌ Absent in WebView |
| Reusing the existing SvelteKit UI | ❌ UI is rewritten | ✅ Nearly free |
| Long-term maintenance | ✅ Aligned with the platform | ⚠️ WebView + JS player + native bridge layer |

**Why native despite the cost of rewriting the UI?** Because the product's value is playback
reliability: IPTV providers do not send CORS headers, most use 4K/HEVC and AC-3, and some serve
plain HTTP or MPEG-TS. A WebView makes the product dependent on the provider at every link in
that chain. The UI, however, can be reproduced in Compose by sharing the design tokens (colour,
typography, focus ring).

## 2. Why Media3/ExoPlayer?

### 2.1 4K / HEVC / codec variety
- IPTV streams often mix **H.264 + HEVC (H.265)**; 4K channels are mostly HEVC.
- Media3 uses **hardware decoding** through `MediaCodec`; falling back to a software decoder on
  unsupported profiles and codec selection are under application control.
- With **tunnelled playback** (`DefaultRenderersFactory.setEnableDecoderFallback`, tunnel mode)
  the video surface is handed directly to the hardware layer on 4K streams; the WebView MSE path
  has no equivalent.
- HDR10/HLG colour transfer and `MediaFormat` colour information are preserved.
- On the audio side, broadcast codecs such as **AC-3 / E-AC-3 / DTS** can play if the device
  supports them; these codecs are not guaranteed in a WebView.

### 2.2 HLS live-stream behaviour
- The Media3 HLS source makes the live window, `liveSyncDuration`, target latency, ABR and retry
  policies after errors configurable at the application level.
- Provider quirks such as malformed/missing `#EXT-X-*` tags, ad markers and
  `#EXT-X-DISCONTINUITY` are handled more predictably by the native parser.
- Low-latency HLS (LL-HLS) and `#EXT-X-PART` support ships with the Media3 version.

### 2.3 CORS and provider realities
A browser engine expects `Access-Control-Allow-Origin` for a manifest/segment from a different
origin. Most IPTV panels **do not send** this header; among those that do, some use a strict
origin list instead of `*`. On the desktop this can be worked around with Electron `webSecurity`
or a proxy; on Android WebView the same solution means either a security hole or a fragile local
proxy.

On the native side CORS **does not conceptually exist**: OkHttp uses the OS HTTP stack. Providers
where `hls.js` gets stuck because of CORS therefore work directly in Media3.

### 2.4 Provider-specific requests
- `User-Agent`, `Referer`, `Origin`, `Cookie` and custom `Authorization` headers can be added per
  source with an OkHttp interceptor.
- Redirect chains, self-signed certificates (with user consent) and plain HTTP are managed in one
  place.
- `MPEG-TS over HTTP`, `RTSP` and in some cases `UDP multicast` sources are possible with Media3;
  they are not possible in a WebView.

## 3. HLS.js + WebView limits (summary)

1. **CORS**: The biggest obstacle. If the manifest lacks CORS, playback never starts; if a segment
   lacks CORS, the manifest loads and then the stream freezes.
2. **MSE requirement**: hls.js runs on MediaSource Extensions. Android WebView has MSE, but the
   codec set varies by device vendor; HEVC MSE support is not consistent.
3. **4K cost**: Segments are read into JS memory and copied into the MSE buffer; at 4K this causes
   memory pressure and GC-related stutter. On the native path the buffer flows straight to the
   codec.
4. **No tunnelling/HDR**: The WebView video surface does not guarantee tunnelled playback or HDR
   metadata.
5. **Audio codecs**: AC-3/E-AC-3/DTS are generally unsupported in WebView; the channel stays
   silent.
6. **Lifecycle**: PiP, background audio, audio focus, keeping the screen on and headset buttons
   require a native bridge in a WebView.
7. **TV integration**: System surfaces such as the Leanback launcher, TV banner, channel
   recommendations and playback resumption live on the native side.

## 4. Why is Capacitor not primary?

Capacitor is tempting for packaging the existing SvelteKit output quickly and is valuable as a
**one-off UI spike**. For production, however:

- Playback would still be tied to WebView + hls.js; CORS/codec problems would not be solved.
- Putting native ExoPlayer behind a WebView (transparent WebView + SurfaceView) is possible, but
  it creates a fragile bridge layer for touch/focus, secure surface, screen rotation and PiP.
- Maintaining two separate UI/player contracts (web stores + native bridge) is more expensive than
  a native UI in the long run.
- Play Store/Android TV quality checks (TV focus, back key, immersive) are native expectations.

**Decision:** The production path is native. Capacitor may only be used as a "see the design on a
real device" spike; it is not present in this repository.

## 5. Module structure

```
zeltatv-android/
├─ app/                     # Compose UI, Activity, ViewModel, Media3 surface
│  ├─ ui/                   # ZeltaApp, theme, screens
│  ├─ player/               # PlayerViewModel + Media3 configuration
│  └─ data/                 # (skeleton) demo source; Room/SAF goes here
├─ core/playlist/           # Pure Kotlin M3U parser + unit tests
├─ scripts/android/         # Windows setup/AVD/smoke scripts
├─ docs/                    # architecture, test plan, roadmap
└─ .github/workflows/       # debug APK CI
```

Planned split (M1+):

| Module | Responsibility |
| --- | --- |
| `:core:model` | Channel, PlaylistSource, Category (shared contract) |
| `:core:playlist` | M3U/M3U8 parse, normalise, validate |
| `:core:data` | Room (sources, favourites, history), DataStore (preferences), SAF import |
| `:core:player` | Media3 factory, OkHttp DataSource, error/retry policy, codec preferences |
| `:core:designsystem` | Colour/typography/focus tokens, TV components |
| `:feature:tv` | TV home screen, channel list, OSD |
| `:feature:player` | Playback screen, channel menu |
| `:feature:settings` | Theme, playback preferences, about |

## 6. Network layer and CORS

```
Compose UI -> PlayerViewModel -> ExoPlayer
                                   ├─ HlsMediaSource
                                   ├─ DefaultMediaSourceFactory
                                   └─ DefaultDataSource
                                        ├─ OkHttpDataSource (http/https, custom headers)
                                        ├─ FileDataSource (SAF/local)
                                        └─ (M2) RtmpDataSource / RtspMediaSource
```

- **Header policy**: `User-Agent`, `Referer` and `Cookie` are stored per source and applied with
  an OkHttp interceptor. The "provider headers" field is exposed to the user in M1.
- **TLS**: System CAs by default. A self-signed certificate is accepted per source and with a
  warning only if the user explicitly consents (disabled by default in release).
- **Cleartext HTTP**: `usesCleartextTraffic=true` in the skeleton (desktop parity). Target before
  release: off by default, per-source user consent.
- **Proxy**: Not required. If necessary, a local proxy may be considered for debugging only; a
  production CORS proxy **will not be used** (user traffic and security risk).
- **timeout/retry**: OkHttp connect/read timeout + Media3 `LoadErrorHandlingPolicy` with
  exponential backoff. The desktop's five-attempt reconnect behaviour will move here.

### CORS test
`tools/cors-test-server.mjs` serves the same content over two paths:
- `/cors/...` → `Access-Control-Allow-Origin: *`
- `/no-cors/...` → no CORS header

The native app must play both (the actual acceptance criterion). A future WebView spike should get
stuck on the `/no-cors` path; that is the proof of the architectural decision.

## 7. UI and TV behaviour

- **Leanback**: The manifest has the `LEANBACK_LAUNCHER` category, `android:banner` (320×180) and
  `android.software.leanback` (required=false). Phone/tablet share the same APK.
- **Focus**: Compose `focusable` + `focusRequester` + amber (`#F6C445`) focus ring. The first
  channel is focused automatically. OK/Enter maps to `Key.DirectionCenter`.
- **Back**: `BackHandler` stops the player; a second BACK exits the app. In M1 a real back stack
  will be added for the channel menu and settings.
- **Immersive**: System bars are hidden with `WindowInsetsControllerCompat`; the screen is kept
  on. For TV overscan, a 5% safe area will be applied in M2.
- **Phone/tablet**: Same components; `BoxWithConstraints` narrows the sidebar on small screens.
  Portrait mode, PiP and touch gestures will be added in M3.

## 8. Theme

The desktop theme works with CSS variables + `localStorage` (`src/lib/theme/store.svelte.ts`). The
Android equivalent:

- Colours are moved into Compose `ColorScheme` (the default palette uses a `#141414` background
  and a `#2EE6F6` accent, both `primary` and `focus`).
- `ZELTA_PALETTES` holds the seven presets (default, mono, light, navy, forest, plum, warm). The
  selected id is saved in `SharedPreferences` under the `theme` key; changing it re-applies
  `MaterialTheme` immediately.
- A preset's accent becomes both `ColorScheme.primary` and the focus colour; presets without an
  accent (light) fall back to `ZeltaTurquoise`. Cards, borders and muted surfaces are derived from
  the background with the desktop HSL rules.
- Classic/Enhanced skins no longer exist; there is one layout and the palette is the only visual
  setting.
- Electron-specific calls such as `setTitleBarOverlay` are ignored on Android.

## 9. Data and persistence

The desktop uses IndexedDB (`zeltatv-library`). On Android, for M1:

- **SharedPreferences + JSON**: sources, last source, last channel, favourites and history. M3U
  content is stored directly for file sources; URL sources are fetched again.
- **SAF**: `ACTION_OPEN_DOCUMENT` with `.m3u/.m3u8/.txt`; content is copied in M1.
- **M2**: Room (`sources`, `categories`, `channels`, `favorites`, `history`) and DataStore (theme,
  language, playback preferences). Source deletion/category management will move to this layer.
- **Backup**: `allowBackup=true`; a user preference for provider URLs will be added before release.

## 10. Testability

- Pure Kotlin parser → JVM unit tests (fast, no emulator).
- Media3/Compose → instrumented tests (UiAutomator smoke + Compose UI tests later).
- Network/CORS → local test server + a manual matrix with real provider samples.
- Performance → `adb shell dumpsys meminfo`, `top`, `gfxinfo`, `frame stats`.

Details: [test-plan.md](test-plan.md).

## 11. Risks and mitigation

| Risk | Impact | Mitigation |
| --- | --- | --- |
| Provider-specific HLS quirks | Playback does not start | Show error codes to the user, per-source header/retry, validation on M3U import |
| Heat/CPU at 4K HEVC | Stutter | Max resolution by device capacity, tunnelled playback, buffer limits |
| Focus loss on TV | Unusable UI | Focus map on every screen + UiAutomator D-pad tests |
| Low-RAM devices | Crash | `onTrimMemory`, bounded bitmap logo cache, LazyColumn |
| Cleartext/self-signed | Security | Per-source consent, off by default in release |
| Loss of UI parity | Brand inconsistency | Design tokens + screenshot comparison (M2) |

## 12. Open decisions (to settle in M1)

1. Single Activity + Compose Navigation, or multiple Activities? (Recommendation: single Activity,
   Navigation.)
2. Should the Room schema store M3U content or normalised channels? (Recommendation: both; content
   with the source, channels in a separate table.)
3. Is compatibility with the desktop `localStorage` keys required for favourites/history?
4. When should the separate navigation layout (two-pane) for tablets arrive? (Recommendation: M3.)
5. Closed beta distribution: Firebase App Distribution or GitHub Actions artifact? (For now, debug
   artifact only.)

## References

- AndroidX Media3: https://developer.android.com/media/media3
- ExoPlayer HLS: https://developer.android.com/media/media3/exoplayer/hls
- Media3 OkHttp data source: https://developer.android.com/media/media3/exoplayer/networking
- Android TV quality guidelines: https://developer.android.com/docs/quality-guidelines/tv-app-quality
- WebView MSE/Media: https://developer.android.com/guide/webapps/webview
- Desktop sources: `zeltatv/src/components/VideoPlayer.svelte`, `zeltatv/src/lib/iptv/*`
