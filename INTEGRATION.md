# GlamGo Android — Backend API Integration

This app was originally a **local-only** Jetpack Compose prototype (Room + mock
data, no login). It has been converted to a **100% server-backed** client that
talks to the live GlamGo backend that ships inside this repo
(`backend/app/glamgo/`, mounted at `/api/glamgo/v1/*`).

## What changed

### New network layer — `app/src/main/java/com/example/data/remote/`
| File | Purpose |
|------|---------|
| `Dtos.kt` | Moshi DTOs for the full REST contract (snake_case ↔ camelCase). |
| `GlamGoApi.kt` | Retrofit interface — auth, catalog, partners, quote, bookings, wallet, reviews, complaints, wishlist, AI chat, chat, partner jobs. |
| `TokenStore.kt` | Per-identity access/refresh token storage (customer **and** partner sessions held separately, matching the backend's identity isolation). |
| `ApiClient.kt` | OkHttp + Retrofit + Moshi. Bearer auth interceptor + 401 refresh authenticator. **`NetworkConfig.baseUrl`** is the single place to point at prod vs. a local backend. |
| `Mappers.kt` | DTO → existing in-app models, so the Compose screens are untouched. |

### Rewired data + UI seams
- **`data/Repository.kt`** — fully rewritten as an online repository. In-memory
  `StateFlow`s act as a UI cache, refreshed from the server after every
  mutation. Public surface (flows + methods) is unchanged so the screens
  compile as-is.
- **`ui/GlamGoViewModel.kt`** — server-backed; adds OTP-login session state.
  Removed the old local "fake booking progression" simulator (the real backend
  state machine now drives status).
- **`ui/LoginScreen.kt`** (new) — OTP phone login (customer + partner). Gated in
  `GlamMainShell`: unauthenticated users see the login screen.
- **`data/Mocks.kt`** — `GlamMockDataSource` categories/services/partners are now
  **hydrated from the backend** at startup; the literals remain only as an
  offline fallback so the UI never renders empty.
- **`AndroidManifest.xml`** — added `INTERNET` + `ACCESS_NETWORK_STATE`.

### Theme + typography polish
- `ui/theme/Color.kt`, `Theme.kt`, `Type.kt` — replaced the muddy gold /
  near-black plum palette with a refined **rose → plum** brand (rose is the hero
  action colour, gold demoted to a champagne accent, darks softened to warm
  charcoal-plum). Type scale relaxed from `Black`/tight-tracking to
  `SemiBold`/comfortable line-heights. All colour value **names are unchanged**,
  so screen code didn't move.

## Configure the backend URL
`data/remote/ApiClient.kt → NetworkConfig.baseUrl`:
- **Production (default):** `https://odiobook.com/api/glamgo/v1/`
- **Local backend from the emulator:** `http://10.0.2.2:8000/api/glamgo/v1/`
  (10.0.2.2 = host loopback). For cleartext HTTP on API 28+, add a debug
  `networkSecurityConfig` or `android:usesCleartextTraffic="true"` to a debug
  manifest.

## Dev login (no SMS)
When the backend runs with `GLAMGO_DEV_OTP=1` (default in non-production), the
OTP is returned in the request response and shown on the login screen as
"Dev OTP: ######" — enter it to sign in. The seeded demo **partner** phone is
`9000000001`.

## Build
This module needs Android Studio / the Android SDK + Gradle (it is a Google AI
Studio export with no committed Gradle wrapper). From Android Studio: open
`GlamGo-Android/`, let Gradle sync, then Run. Retrofit/OkHttp/Moshi were already
on the dependency list — no Gradle changes were required for the integration.

> The backend side of this integration (Phase-2 endpoints: reviews, complaints,
> wishlist, AI assist, chat) was added in this repo and verified end-to-end
> (44/44 smoke assertions green). The Android code is written to compile against
> that contract but must be built in Android Studio (no Android toolchain in the
> server CI environment).
