# Media Browser (Android, Kotlin + Jetpack Compose)

A generic, tag-based media gallery app — masonry/grid feed, search with
tag chips, a detail modal, favorites (posts, tags, and artists), a
download manager, and full theme customization. Wired to the real
**Pexels API** (free, public, royalty-free photos and videos) out of the
box so every feature is verifiable immediately, with everything
structured so you can swap in a different provider later.

## Getting a free Pexels API key (required to see real content)

1. Go to https://www.pexels.com/api/ and sign up — it's free and instant.
2. Copy your API key.
3. Open the app, go to **Settings → Advanced API Configuration**, and
   paste it into **"API Key / Credential 1"**.

   (Alternatively, hardcode it once for local testing in
   `app/build.gradle.kts`: replace the empty string in
   `buildConfigField("String", "PEXELS_DEFAULT_API_KEY", "\"\"")` with
   your key. Don't commit a real key to source control.)

Without a key, network calls will return 401 errors — the rest of the UI
(navigation, settings, theme customization, layout) still works, but
feeds will be empty.

## Architecture

MVVM + Repository pattern, single-activity Jetpack Compose app:

```
UI (Compose screens + ViewModels)
        │
MediaRepository (single source of truth)
   │                       │
Retrofit/OkHttp        Room (favorites: posts/tags/artists, downloads)
(PexelsApi)             DataStore (settings, theme, layout)
```

## What's implemented

- **Home** — masonry or fixed grid (switchable in Settings), real Pexels
  curated photos mixed with popular videos (every 4th page is a video
  page, so the video player and download flow are always reachable
  without a separate "videos" tab)
- **Search** — Gmail-style tag chips inside the search bar (colored per
  tag), debounced suggestions, explicit search submit via the arrow
  button, removing a tag re-filters results immediately
- **Post detail** — an in-place dialog/modal (not a navigation route), so
  the feed behind it never loses scroll position. Shows full tag list,
  artist, source link, download and favorite actions. Tags/artists open a
  small action menu: **Open in new tab**, **Add to search**, **Favourite**
- **Favorites** — three tabs: **Posts** (Room-backed, offline), **Tags**,
  **Artists** — favoriting a tag/artist from the action menu shows up here
- **Downloads** — progress bar + percentage, status icons (in progress /
  completed / failed), delete/cancel
- **Settings** — dark/light/system mode, grid columns, card corner radius,
  home/favorites layout style (grid vs masonry), full theme color pickers
  (accent, background, surface, and per-tag-category colors — applies
  live, no restart), safe search and data saver toggles, Wi-Fi-only
  downloads, cache management, and the Advanced API Configuration section
  (provider name, base URL, two generic credential fields)

## Performance notes

- `PostsPagingSource`/`PexelsPagingSource` cache the favorite-ID set once
  per paging session instead of re-querying Room on every page load.
- `DownloadWorker` uses a 256KB read buffer (vs. the 8KB default) and only
  writes progress to the database/notification every ~3% instead of every
  single percent, cutting DB writes substantially on large downloads.
- Coil's in-memory cache is sized at 35% of available memory to reduce
  re-decoding while scrolling.

## Swapping to a different provider

Everything Pexels-specific lives in:
- `data/remote/PexelsApi.kt`
- `data/remote/dto/PexelsPhotoDto.kt`, `PexelsVideoDto.kt`
- `data/mapper/PexelsMappers.kt`
- `data/paging/PexelsPagingSource.kt`
- The `pexelsApi` parameter in `MediaRepositoryImpl.kt`

Replace these five with equivalents for your provider of choice; nothing
in the UI layer (screens, ViewModels, components) needs to change, since
they only depend on the domain models (`Post`, `PostDetail`,
`TagSuggestion`) and the `MediaRepository` interface.

A second, fully generic `MediaApi.kt` + matching DTOs are also included
as an unused template/reference for a simpler tags-based REST API shape,
in case that's a closer match for a future provider.

## Requirements

- Android Studio (Ladybug or newer recommended)
- JDK 17
- minSdk 26, targetSdk/compileSdk 35

## Setup

1. Open the project root in Android Studio, let Gradle sync.
2. Get a free Pexels API key (see above) and enter it in Settings, or
   hardcode it in `build.gradle.kts` for local testing.
3. Run on a device/emulator running Android 8.0 (API 26) or later.
