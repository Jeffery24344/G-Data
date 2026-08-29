# How to build G Data

## Why it failed before
The repository was missing:
- Application class & MainActivity
- Theme / resource files
- Correct Compose Compiler setup for Kotlin 2.0
- Gradle wrapper properties

Those are now fixed on `main`.

## Build with Android Studio (recommended)
1. Install [Android Studio](https://developer.android.com/studio) (Ladybug or newer).
2. **File → Open** → select the `G-Data` folder (the one that contains `settings.gradle.kts`).
3. Let Gradle sync (Android Studio will download the Gradle wrapper automatically if needed).
4. Click **Run** (green play) on an emulator or device.

## Build from command line
You need the Android SDK and a JDK 17+.

```bash
git clone https://github.com/Jeffery24344/G-Data.git
cd G-Data

# Generate wrapper if gradlew is missing (requires Gradle installed once):
gradle wrapper --gradle-version 8.9

# Then:
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## GitHub Codespaces
1. Open the repo on GitHub → **Code** → **Codespaces** → Create.
2. In the terminal, install Android command-line tools / SDK, then run `./gradlew assembleDebug` (after generating the wrapper).

## Common errors

| Error | Fix |
|-------|-----|
| `SDK location not found` | Create `local.properties` with `sdk.dir=/path/to/Android/Sdk` |
| Compose compiler mismatch | Already fixed: uses `org.jetbrains.kotlin.plugin.compose` |
| Missing icon | Adaptive icon + drawable foreground are in `res/` |
| Hilt / KSP errors | Ensure KSP version matches Kotlin (see root `build.gradle.kts`) |

## Current status
Minimal app **should compile and launch** showing a placeholder “G Data” screen. Full feature screens (Home, Apps, Statistics, etc.) can be added on top of this working base.
