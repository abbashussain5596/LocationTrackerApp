# GPS Background Location Tracker

A modern Android implementation of the GPS Background Location Tracker case study —
continuous foreground/background tracking, low-battery adaptive updates, accuracy
filtering, persisted history, cached last-known location, runtime permission flow,
and a single-screen real-time map.

## Stack

- **Kotlin 2.1** + **Jetpack Compose** (Material 3)
- **Single-Activity / MVVM** with `StateFlow`
- **Hilt** for dependency injection
- **Coroutines + Flow** for async work; main thread only renders UI
- **`FusedLocationProviderClient`** (Google Play Services) — built-in GPS/network/wifi fusion + automatic provider fallback
- **Foreground Service** with `foregroundServiceType="location"` for reliable background tracking (Android 14+ compliant)
- **Room** for location history, **DataStore (Preferences)** for last-known cache
- **Maps Compose** (Google Maps SDK)
- **Accompanist Permissions** for runtime permission flow (foreground + background)
- Gradle Kotlin DSL with `libs.versions.toml` version catalog

## Architecture

```
ui/         (Compose, ViewModel)        ← main thread only renders, observes StateFlow
 └─ map/     ← MapScreen, MapViewModel, MapUiState
 └─ permission/ ← PermissionGate, LocationPermissionState
service/    LocationTrackingService     ← foreground service, drives the location stream
data/
 └─ location/  FusedLocationDataSource  ← callbackFlow over fused provider
 └─ local/db   Room (LocationEntity/Dao) ← location history
 └─ local/cache LastLocationCache       ← DataStore last-known cache
 └─ repository LocationRepository       ← single source of truth, persistence on IO
domain/     TrackedLocation, TrackingConfig
di/         Hilt modules (Data, Dispatcher)
```

The repository is the single source of truth. Live updates flow:
`fused provider → callbackFlow → repository.persist (IO) → DAO + DataStore → UI Flows`.
The UI thread only reads `StateFlow<MapUiState>` and renders the map.

## How the case study requirements are addressed

| Requirement | Implementation |
| --- | --- |
| **Background tracking** | `LocationTrackingService` (foreground service, `START_STICKY`, `foregroundServiceType="location"`) |
| **Async / no main-thread blocking** | `callbackFlow` location source, `flowOn(Dispatchers.IO)` for persistence, `viewModelScope` for UI work |
| **Adaptive resource management** | `TrackingConfig.Default` vs `TrackingConfig.LowPower`. Service watches `ACTION_POWER_SAVE_MODE_CHANGED`, `ACTION_BATTERY_LOW`/`OKAY`, and battery level; switches config dynamically and `flatMapLatest` re-subscribes the stream |
| **`distanceFilter` / configurable interval** | `LocationRequest.Builder` `setMinUpdateDistanceMeters`, `setMinUpdateIntervalMillis`, `intervalMillis` — driven by `TrackingConfig` |
| **Accuracy handling** | `Priority.PRIORITY_HIGH_ACCURACY` (default) / `PRIORITY_BALANCED_POWER_ACCURACY` (low-power); `accuracyThresholdMeters` filter drops noisy fixes; fused provider handles network-fallback automatically |
| **Caching** | `LastLocationCache` (DataStore) — UI shows cached position immediately on launch; replaced when a fresh fix arrives |
| **History persistence** | Room `location_history` table; `recentHistory` Flow drives the UI |
| **Permission management** | `PermissionGate` re-prompts until granted; two-stage flow (foreground → background); fallback to app settings on permanent denial; supports `POST_NOTIFICATIONS` on API 33+ |
| **Single-screen map UI** | `MapScreen` shows Google Map + lat/lon/accuracy/provider/timestamp; recenters only on meaningful coordinate changes (`LaunchedEffect` keyed on lat/lon) |
| **App-killed scenario** | Foreground service with `START_STICKY` keeps location updates alive when the activity is destroyed; user can stop via notification action |

## Setup

1. **Get a Google Maps API key.** In the [Google Cloud Console](https://console.cloud.google.com/),
   enable the *Maps SDK for Android* and create an API key.
2. **Add it to `local.properties`** at the repo root (this file is gitignored):

   ```properties
   MAPS_API_KEY=AIza...your_key_here
   ```

3. **Open in Android Studio** (Hedgehog or newer recommended). Sync — Gradle will
   download dependencies and generate the `gradlew` wrapper jar on first sync.
4. **Run** on a physical device (preferred — emulator GPS is mocked) running
   Android 7.0 (API 24) or higher with Google Play Services.

## Build from CLI

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

## Validation checklist (mapping to the case study's testing strategy)

- **Background execution** — keep the app open, press Start, then send to background.
  The foreground notification stays; `adb logcat` will show coordinates flowing.
  History rows accumulate in the DB.
- **Persistence** — kill the app and relaunch. The cached last-known location
  appears on the map instantly (DataStore), and the history count remains.
- **Real-device testing** — outdoor (sub-10m accuracy), indoor (often >50m, those
  fixes are filtered out by `accuracyThresholdMeters`).
- **Low-battery testing** — toggle battery saver in Quick Settings; the service
  switches to `TrackingConfig.LowPower` (30s interval, 25m distance filter,
  balanced-power priority).
- **App termination** — swipe the app away. The foreground service survives;
  if killed by the system, `START_STICKY` triggers restart.
- **Permission edge cases** — deny once → re-prompt; deny permanently → "Open
  app settings" CTA; background-only denied → app still works in foreground.

## Notes

- `minSdk = 24`, `targetSdk = 35`. Background-location permission flow follows
  Android 10+ rules (separate request after foreground grant).
- Hilt + KSP is used (not kapt) for faster builds.
- The Compose Compiler is the bundled Kotlin 2.0+ compiler plugin
  (`org.jetbrains.kotlin.plugin.compose`).
