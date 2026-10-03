# Coil

A calm, one-tap focus timer for Android. Every finished session fires a clay pot onto your shelf; give up early and the unfired clay slumps.

**Stack:** React 19 + TypeScript + Vite for the UI, wrapped as a native Android app with Capacitor 8. All data stays on the phone (WebView `localStorage`).

## Build the APK

Requirements: Node 22+, JDK 21, and the Android SDK (Android Studio installs it; set `ANDROID_HOME`).

```sh
npm install
npm run apk:debug          # builds the web bundle, syncs it into android/, runs Gradle
# → android/app/build/outputs/apk/debug/app-debug.apk
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
```

For a Play Store build, open `android/` in Android Studio (`npx cap open android`) and use *Build → Generate Signed App Bundle*.

## Develop

```sh
npm run dev                # http://localhost:5173 — the full app runs in a browser
npm test                   # unit tests for stats, streaks and the session engine
```

URL switches for checking designs in the browser: `?speed=40` (a 25-minute session runs in ~40 s), `?text=large`, `?rm` (reduced motion). Settings → *Load sample data* fills ten weeks of history so Stats and Shelf look like the design; *Remove sample data* takes it out again without touching real sessions.

## What's where

| Path | |
| --- | --- |
| `src/lib/pot.ts` | Pot forms (bowl/cup/jar/vase) and the coil geometry, ported 1:1 from the prototype |
| `src/lib/engine.ts` | Session timing, call pauses, and the 60-second away rule |
| `src/lib/stats.ts` | Today, streaks, week and month summaries (pure functions, tested) |
| `src/store.tsx` | App state, persistence, timers, notifications, Android back button |
| `src/screens/` | One file per screen: Onboarding, Home, Running, Complete, Abandoned, Break, Stats, Shelf, Tags, Settings |
| `src/styles/organic.css` | The Organic design system, unchanged except for self-hosted fonts |
| `src/styles/coil.css` | Coil tokens (light/dark/large text) and shared controls |
| `android/…/DeviceStatePlugin.java` | Native helper: was there a call, was the phone just locked, system font scale, open notification settings |

## Behaviour notes

- **Leaving the app.** Calls pause the session (detected from the audio mode, no phone permission needed). Locking the phone is not leaving: the timer keeps running and the end notification fires. Switching to another app for more than 60 seconds ends the session, and the Abandoned screen says why.
- **Streak** = consecutive days with at least one finished pot; today without one doesn't break it yet. The daily goal counts sessions.
- **Long break** after every Nth finished session of the day (Settings → Long break after).
- **Month stats** open on the last full month during the first week of a month, as in the design. A month in progress is compared with the same days of the previous month.
- **Notifications** use exact alarms (`USE_EXACT_ALARM`, allowed for timer apps) so the session-end alert is on time. If the user blocks notifications, Home and Settings explain the consequence and link to Android's settings.
- **Larger text:** Android font scale ≥ 1.15 switches to the large type scale from the design. **Reduced motion:** follows "Remove animations".

## Deviations from the design prototype

- No fake status bar, bezel or home indicator; the layout uses the real safe areas.
- Dates, totals, streaks, the heatmap and the shelf are computed from real sessions instead of the prototype's fixed sample numbers. Week and month have working previous/next arrows.
- "iPhone Settings" copy became "Android settings"; export opens the Android share sheet with `coil-sessions.csv`.
- Added *Load / Remove sample data* in Settings → Data.
