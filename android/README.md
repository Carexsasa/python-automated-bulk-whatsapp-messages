# GroceryCompare Android app

Native Android (Kotlin, Jetpack Compose, min SDK 26) client for the GroceryCompare backend.
Type or speak a grocery item → ranked list of where it's cheapest this week across Lidl,
Aldi Süd/Nord, Netto Marken-Discount and Kaufland, with €/kg or €/l unit prices, offer
validity, offer-vs-regular flag, and distance to the nearest branch.

## Stack
MVVM · Hilt · Retrofit/OkHttp + kotlinx.serialization · Room (offline cache) · WorkManager
(daily price-drop check) · Coroutines/Flow · Compose Material 3 · German + English.

## Build
Requires Android Studio (Koala+) or the Android SDK/command-line tools.

```bash
cd android
# generate the gradle wrapper jar once (not committed):
gradle wrapper --gradle-version 8.9
./gradlew assembleDebug
```
Create `android/local.properties` with `sdk.dir=/path/to/Android/sdk`.

## Pointing at the backend
`app/build.gradle.kts` sets `API_BASE_URL`:
- Emulator → `http://10.0.2.2:8000/` (host machine).
- Physical device → your machine's LAN IP (uncomment the debug `buildConfigField`).

## Structure
```
data/remote   Retrofit GroceryApi + DTOs (mirror the backend's normalized models)
data/local    Room: OfferEntity/WatchedItemEntity, DAOs, AppDatabase (offline cache)
data/repo     GroceryRepository — offline-first: cache + network refresh, price-drop check
di            Hilt module (Retrofit/OkHttp/Room)
ui/search     SearchViewModel + SearchScreen (autocomplete, voice, ranked cards)
ui/theme      Material 3 theme
work          PriceRefreshWorker — WorkManager periodic watched-item check + notifications
```

## Features wired
- **Search** with debounced **autocomplete** and **voice input** (RecognizerIntent, de-DE).
- **Offline-first**: results render from Room instantly; network refresh updates them.
- **Distance filter** via device location passed to the backend store locator.
- **Watch price** → `PriceRefreshWorker` notifies on drops for watched items.
- **Shopping-list mode** backed by the `/basket` endpoint (best single store vs split).
- **Disclaimer** shown on the results surface.

> Note: this module is a working skeleton with the full architecture in place. It has not
> been compiled in the delivery environment (no Android SDK); build it in Android Studio.
