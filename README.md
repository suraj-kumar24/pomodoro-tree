# Pomodoro Tree

A personal, ad-free Pomodoro timer for Android with tree-growth gamification. Complete a focus session and grow a tree. Cancel early and it withers.

## Features

- **One-tap start** — tap the seed to begin a focus session, no menus or friction
- **Tree growth** — watch a tree grow through 5 stages as your timer progresses (seed → sprout → sapling → growing tree → full canopy)
- **Over-sitting countermeasures** — escalating vibration alerts, overtime tracking, and progressive tree wilting if you don't acknowledge completion
- **Daily forest** — see all your trees from today in a grid view
- **Weekly trendline** — bar chart with focus minutes per day and week-over-week comparison
- **Yearly heatmap** — GitHub-style contribution grid showing your focus consistency
- **Focus rewards** — set real-world rewards (coffee, new book) with focus-hour costs, redeem when you've earned enough
- **Tags** — categorize sessions (Work, Study, Personal) with color coding
- **Daily goals** — set a target number of pomodoros and track progress
- **Do Not Disturb** — automatically enable DND during focus sessions
- **Home screen widget** — start sessions and see progress without opening the app
- **JSON export** — export all session data for backup or analysis
- **Dark theme** — minimalist muted color palette

## Tech Stack

- Kotlin + Jetpack Compose + Material 3
- Hilt (dependency injection)
- Room (local database)
- Foreground Service (background timer)
- AlarmManager (escalating alerts)
- Jetpack Glance (home screen widget)

## Requirements

- Android 8.0+ (API 26)
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17+

## Build & Install

### Option 1: Android Studio (recommended)

1. Clone the repo:
   ```bash
   git clone https://github.com/suraj-kumar24/pomodoro-tree.git
   ```
2. Open the project in Android Studio
3. Wait for Gradle sync to complete
4. Connect your phone via USB (or set up wireless debugging)
5. Enable **Developer Options** on your phone:
   - Go to Settings → About Phone → tap "Build Number" 7 times
   - Go back to Settings → Developer Options → enable **USB Debugging**
6. Select your device in the toolbar and click **Run** (green play button)

### Option 2: Command line

1. Clone and build:
   ```bash
   git clone https://github.com/suraj-kumar24/pomodoro-tree.git
   cd pomodoro-tree
   ./gradlew assembleDebug
   ```
2. The APK will be at `app/build/outputs/apk/debug/app-debug.apk`
3. Transfer the APK to your phone and install it (you'll need to allow "Install from unknown sources")

### Option 3: ADB install

1. Build the APK (see Option 2, step 1)
2. With your phone connected via USB:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

## Navigation

The app uses gesture-based navigation from the home screen:

| Gesture | Destination |
|---------|-------------|
| Tap center | Start focus session |
| Swipe left | Daily forest |
| Swipe right | Weekly/yearly analytics |
| Swipe down | Settings |
| Swipe up | Rewards |

## Project Structure

```
com.pomodoro.tree/
├── di/                  # Hilt modules
├── data/
│   ├── db/              # Room database, DAOs, entities
│   ├── repository/      # Session & reward repositories
│   └── export/          # JSON export
├── domain/
│   ├── model/           # Domain models (Session, Tag)
│   └── timer/           # Timer state machine
├── service/             # Foreground service, DND manager
├── receiver/            # Boot & alert broadcast receivers
├── ui/
│   ├── theme/           # Colors, typography, Material 3 theme
│   ├── home/            # Home screen (one-tap start)
│   ├── active/          # Active session (tree + timer)
│   ├── completion/      # Post-session + overtime tracker
│   ├── analytics/       # Daily forest, weekly, yearly, rewards
│   ├── settings/        # App settings
│   └── components/      # TreeCanvas, ProgressRing
└── widget/              # Glance home screen widget
```

## License

Personal project. All rights reserved.
