# Veda Drop — Enable closed-app push (FCM)

Booking-update and chat-message notifications **while the app is open or alive** already
work (the app posts a local system notification — see `VedaDropViewModel.postLocalNotification`).
**Closed-app / killed-app push** is fully coded on both client and server; it only needs
**Firebase credentials**. Do these three steps once.

The code already in place (do NOT re-add):
- Client: `VedaDropMessagingService` (receives pushes, deep-links on tap), token
  registration on login (`VedaDropViewModel.registerFcmToken`), `firebase-messaging`
  dependency, the `nikhatglow_default` notification channel (`MainActivity`).
- Server: `app/vedadrop/service.py` FCM HTTP v1 sender (`notify()` writes the DB row
  **and** best-effort pushes), gated by the `fcm_push` flag + `GLAMGO_FCM_CREDENTIALS`.

## 1. Client — add Firebase to the Android app
1. Firebase console → create/choose a project → add an **Android app** with package
   **`com.aistudio.glamgo.pxrjty`**.
2. Download **`google-services.json`** and drop it in **`app/google-services.json`**.
3. Put the Google Services plugin on the build classpath:
   - `gradle/libs.versions.toml`, under `[plugins]`:
     ```toml
     google-services = { id = "com.google.gms.google-services", version = "4.4.2" }
     ```
   - root `build.gradle.kts`, inside `plugins { }`:
     ```kotlin
     alias(libs.plugins.google.services) apply false
     ```
   `app/build.gradle.kts` already applies it automatically once `google-services.json`
   exists (the guarded block at the bottom of the file). Rebuild the APK.

## 2. Server — give the backend a service account
1. Firebase console → Project settings → **Service accounts** → **Generate new private
   key** → download the JSON.
2. Put it on the prod box and point the backend at it (already read by the code):
   ```
   GLAMGO_FCM_CREDENTIALS=/path/to/firebase-service-account.json
   ```
   (in `docker-compose.prod.yml` env for the `backend` + `celery-worker` services, mounted into the container).

## 3. Turn the flag on
Admin → Veda Drop → Feature flags → enable **`fcm_push`** (or set env `GLAMGO_FCM_ENABLED=1`).

## Verify
Log in on a device (registers the token), background/kill the app, then have the other
side trigger a booking update or send a chat message → a push should arrive and deep-link
into the booking when tapped. The in-app bell + the alive-app local notification work
regardless of this setup.
