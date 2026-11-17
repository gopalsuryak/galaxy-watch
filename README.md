# Galaxy Watch Companion

An open-source watch app and phone app that get your Galaxy Watch's health data onto
**any** Android phone, without root, without patching anything, and without Samsung's
phone apps.

> **Not affiliated with Samsung.** "Galaxy Watch" is a trademark of Samsung Electronics,
> used here only to say which watch this works with. This project contains no Samsung
> code and does not modify, spoof or bypass any Samsung app.

> **Not a medical device.** Readings are for personal tracking only. Don't use them to
> diagnose or treat anything.

## Why this exists

A Galaxy Watch paired with a non-Samsung phone loses features, because Samsung's phone
apps check who made the phone. Getting around that check needs root or modified Samsung
apps, and both have real downsides.

We took a different route, the same idea LibrePods used for AirPods: don't trick the
vendor's app, replace it. The watch reads its own sensors through Google's standard Wear
OS health API and sends the data to our phone app over the normal Wear OS connection.

## How it works

```
 Galaxy Watch (Wear OS)                         Any Android phone
┌──────────────────────────┐                 ┌───────────────────────────┐
│ Health Services          │                 │ WatchDataListenerService  │
│   (Google's sensor API)  │                 │   decodes, saves, deletes │
│          │               │                 │            │              │
│ PassiveDataService       │  Wear OS Data   │ SampleStore (local SQLite)│
│   batches in background ─┼──── Layer ─────▶│            │              │
│          │               │  (syncs when    │ Dashboard + CSV export    │
│ SampleSender             │   back in range)│                           │
└──────────────────────────┘                 └───────────────────────────┘
```

- **`shared/`**: the wire format (plain text, versioned) plus its unit tests.
- **`wear/`**: watch app. Live heart rate on screen; a **Sync** button registers background
  collection with Health Services.
- **`mobile/`**: phone app. Receives batches, stores them locally, shows the latest
  values and recent readings, exports everything to CSV.

Data never leaves your devices. No account, no server, no analytics.

## What it tracks today

| Metric | Source | Works on every Galaxy Watch4+ |
|---|---|---|
| Heart rate | Health Services passive monitoring | Yes |
| Steps today | Health Services | Yes |
| Calories today | Health Services | Yes |
| Distance today | Health Services | Yes |
| ECG, blood pressure, body composition | Samsung Health Sensor SDK | **No**, see roadmap |

Each watch reports which metrics it supports; the app only asks for those.

## Build and install

Requirements: JDK 17+, Android SDK 36.

```bash
./gradlew :shared:test :wear:assembleDebug :mobile:assembleDebug
```

Install the phone app:

```bash
adb -s <phone-serial> install mobile/build/outputs/apk/debug/mobile-debug.apk
```

Install the watch app (turn on ADB debugging over Wi-Fi on the watch first):

```bash
adb -s <watch-ip>:<port> install wear/build/outputs/apk/debug/wear-debug.apk
```

Then open the app on the watch, grant the sensor permissions and tap **Sync**. The
first batch usually reaches the phone within a few minutes; Health Services batches
background data to save battery, so it isn't instant.

**Important:** both apps must be signed with the same key. The Wear OS Data Layer only
delivers data between apps with the same application ID and signature. Debug builds
from the same machine share a key automatically; for release builds, sign both APKs
with the same keystore.

## Known limits

- Background data arrives in batches, not live. That's Health Services saving battery.
- `targetSdk` is held at 35 for now. Android 16 replaces `BODY_SENSORS` with new health
  permissions, and we'd rather move to those deliberately than by accident.
- Only tested end to end on real hardware by contributors; CI only proves it builds and
  the protocol tests pass. Please report what works on your watch and phone.

## Roadmap

- [ ] Heart rate history chart on the phone
- [ ] Export to Health Connect, so other apps can use the data
- [ ] Optional `samsung` build flavour using the Samsung Health Sensor SDK (ECG, body
      composition, raw PPG). Samsung only lets that SDK run in developer mode unless an
      app is approved as a partner, and its library can't be redistributed, so it will be
      off by default and you'll need to download the SDK yourself.
- [ ] Sleep and exercise sessions

## Contributing

Issues and pull requests are welcome. Please include your watch model, Wear OS version
and phone model when reporting a bug. CI runs the tests, lint and both builds on every
pull request.

## License

Apache License 2.0. See [LICENSE](LICENSE).
