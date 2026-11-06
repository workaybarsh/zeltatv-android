# Upstream Alignment

The canonical ZeltaTV repository is [zeltatv/zeltatv](https://github.com/zeltatv/zeltatv).
This Android repository is a separate port; it keeps the same product name, branding, GPL-3.0
licence and release conventions while using the Android-native structure below.

## Repository mapping

| Upstream (desktop) | Android port |
| --- | --- |
| `src/components/` (Svelte UI) | `app/src/main/java/tv/zelta/app/ui/` (Compose) |
| `src/lib/iptv/parser.ts` | `core/playlist/` (pure Kotlin) |
| `src/lib/iptv/store.svelte.ts` | `app/.../data/PlaylistStore.kt` (M1) → Room in M2 |
| `src/components/VideoPlayer.svelte` | `app/.../player/PlayerViewModel.kt` (Media3) |
| `electron/` shell | `app/.../MainActivity.kt` + AndroidManifest |
| `.github/workflows/` | `ci/android-debug.workflow.yml` (activation pending workflow scope) |
| `static/assets/` | `app/src/main/res/` |

## Rules

- Product name, icon language and GPL-3.0-only licence stay aligned with upstream.
- Desktop-only files (`electron/`, Svelte routes/UI) are not copied into this repo; the Android
  port mirrors their **responsibilities**, not their implementation.
- New upstream user-facing features are evaluated for Android parity in `docs/roadmap.md`.
- Release versioning follows upstream: desktop 1.0.0 is the reference product; Android starts
  its own M1/M2 line and will align marketing version at the first public release.
