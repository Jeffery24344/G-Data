# G Data – Smart Data Saver & Network Optimizer

**Intelligent mobile-data optimization for Android**  
Reduce unnecessary data consumption while keeping performance, video quality, and user experience as good as possible.

> **Core philosophy:** *Use less data without making the internet feel worse.*

**Developer / Publisher:** Big Big Dream  
**Package suggestion:** `com.bigbigdream.gdata` or `com.gdata.app`

---

## What G Data does (and does not do)

### Does
- Real-time & historical mobile data monitoring (official Android APIs)
- Per-app data usage
- Three optimization modes: **Performance**, **Balanced** (default), **Extreme**
- Gaming Mode (prioritizes responsiveness)
- Smart rules engine (battery, remaining data, daily usage, Wi-Fi/mobile)
- Bundle tracking + daily recommended allowance
- Estimated savings (always clearly labelled as estimates)
- Local-first statistics (Room) + Privacy screen
- Optional local VPN (VpnService) for traffic management — **never decrypts HTTPS**
- Useful, non-spammy notifications

### Does **not**
- Claim free data, unlimited data, or magical speed increases
- Decrypt HTTPS / private traffic
- Require root
- Bypass carrier billing or Android security
- Collect passwords, messages, banking data, or browsing content

---

## Tech Stack

| Area              | Choice                          |
|-------------------|---------------------------------|
| Language          | Kotlin                          |
| UI                | Jetpack Compose + Material 3    |
| Architecture      | Clean-ish + MVVM + Hilt         |
| Persistence       | Room + DataStore                |
| Background work   | WorkManager                     |
| Network stats     | NetworkStatsManager             |
| Optional traffic  | VpnService (local, non-decrypting) |
| Min / Target SDK  | 26 / 35                         |

---

## Project Structure

```
app/
├── src/main/java/com/gdata/app/
│   ├── GDataApp.kt
│   ├── MainActivity.kt
│   ├── di/
│   ├── domain/
│   │   ├── model/
│   │   ├── manager/     # ModeManager, BundleManager
│   │   └── engine/      # RulesEngine
│   ├── data/
│   │   ├── local/       # Room entities, DAOs, Database
│   │   └── repository/
│   ├── vpn/            # GDataVpnService
│   ├── notification/
│   ├── worker/
│   ├── ui/
│   │   ├── home/
│   │   ├── apps/
│   │   ├── datasaver/
│   │   ├── statistics/
│   │   ├── network/
│   │   ├── settings/
│   │   ├── privacy/
│   │   ├── navigation/
│   │   ├── theme/
│   │   └── components/
│   └── util/
└── src/main/AndroidManifest.xml
```

---

## Implementation Checklist

### Phase 1 – Foundation (done in design)
- [x] Project skeleton & Gradle setup
- [x] Theme (Material 3, light/dark)
- [x] Navigation (Bottom bar + Settings)
- [x] Domain models (Mode, Bundle, Usage, Savings, Rules)
- [x] ModeManager + BundleManager (DataStore)

### Phase 2 – Core Features
- [x] Home dashboard (usage, savings, remaining, mode toggle)
- [x] NetworkStatsManager repository (today / month / per-app)
- [x] Apps screen (ranked list + Optimize action)
- [x] Data Saver screen (3 modes + Gaming Mode)
- [x] Statistics screen (Today/Week/Month + simple chart)
- [x] Network screen (connection info + on-demand test)
- [x] Settings + Bundle tracking UI

### Phase 3 – Intelligence & Privacy
- [x] Rules engine (battery, remaining data, daily usage, Wi-Fi)
- [x] Notification channels + helper
- [x] Room caching (daily usage, app snapshots, savings)
- [x] Privacy screen + delete local statistics

### Phase 4 – Advanced / Optional
- [x] Optional local VPN (VpnService) + Foreground Service
- [ ] Real measured savings (compare periods with opt. on/off)
- [ ] User-configurable custom rules UI
- [ ] Refined foreground/background split (UsageStatsManager)
- [ ] Baseline profile for faster startup

### Phase 5 – Polish & Release
- [ ] App icon (adaptive)
- [ ] Splash screen
- [ ] Empty / error states polish
- [ ] ProGuard rules
- [ ] Privacy Policy (see `PRIVACY_POLICY.md`)
- [ ] Play Store listing assets
- [ ] Internal / closed testing track

---

## How to build (you don’t need Android Studio installed locally)

### Option A – GitHub Codespaces / cloud IDE
1. Open this repository on GitHub.
2. Click **Code → Codespaces → Create codespace on main**.
3. In the Codespace terminal install the Android SDK (or use a pre-built Android Codespace image).
4. Run `./gradlew assembleDebug`.

### Option B – Android Studio on another machine
1. Clone the repo.
2. Open the project in Android Studio Hedgehog or newer.
3. Let Gradle sync.
4. Run on emulator or device.

### Option C – Command line only (Linux/macOS with SDK)
```bash
git clone https://github.com/Jeffery24344/G-Data.git
cd G-Data
# set ANDROID_HOME and accept licenses
./gradlew assembleDebug
```

> **Note:** A full Gradle Android project structure will continue to be pushed. The current files focus on architecture, key source, README and legal docs so the project is usable and documentable immediately.

---

## Required permissions (user-facing)

| Permission              | Why                                      | User action required      |
|-------------------------|------------------------------------------|---------------------------|
| `PACKAGE_USAGE_STATS`   | Per-app & total data usage               | Grant in Usage Access     |
| `POST_NOTIFICATIONS`    | Alerts (Android 13+)                     | Runtime prompt            |
| VPN permission          | Optional local optimization tunnel       | System VPN consent dialog |
| `FOREGROUND_SERVICE`    | Persistent notification while VPN runs   | Declared only             |

---

## Privacy

See **[PRIVACY_POLICY.md](PRIVACY_POLICY.md)** (Publisher: **Big Big Dream**).

Key points:
- Almost all processing is local.
- No passwords, messages, banking data, or HTTPS decryption.
- User can delete all local statistics in-app.

---

## License

Copyright © Big Big Dream. All rights reserved.  
(You may later choose an open-source license if desired.)

---

## Roadmap ideas

- More accurate savings measurement
- Widget for remaining data
- Quick Settings tile for Gaming Mode / Extreme
- Export usage report (CSV)
- Multi-language support

---

**Built with care so users save real data while still enjoying their phone.**
