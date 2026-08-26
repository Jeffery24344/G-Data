# G Data – Detailed Implementation Checklist

Publisher: **Big Big Dream**

Use this list when continuing development (Android Studio, Codespaces, or CI).

## 1. Gradle & Project Setup
- [ ] Root `settings.gradle.kts` / `build.gradle.kts`
- [ ] App module `build.gradle.kts` (Compose, Hilt, Room, WorkManager, DataStore)
- [ ] `gradle.properties` & JVM target 17
- [ ] Application ID finalized (`com.bigbigdream.gdata` recommended)

## 2. Manifest & Permissions
- [ ] `PACKAGE_USAGE_STATS` (tools:ignore ProtectedPermissions)
- [ ] `POST_NOTIFICATIONS`
- [ ] `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SPECIAL_USE`
- [ ] VpnService declaration with `BIND_VPN_SERVICE`
- [ ] `RECEIVE_BOOT_COMPLETED` (optional, for WorkManager reliability)

## 3. Core Domain
- [x] `OptimizationMode` (Performance / Balanced / Extreme)
- [x] `BundleInfo`, `DataUsage`, `SavingsReport`, `AppDataUsage`
- [x] `ModeManager` (DataStore)
- [x] `BundleManager` (DataStore)
- [x] `RulesEngine` + default rules

## 4. Data Layer
- [x] `NetworkStatsRepository` (real NetworkStatsManager)
- [x] Room entities + DAOs (daily, app, savings)
- [x] `CachedUsageRepository`
- [ ] Optional: WorkManager daily snapshot worker

## 5. UI Screens
- [x] Home dashboard
- [x] Apps list
- [x] Data Saver (modes + Gaming Mode)
- [x] Statistics (period selector + chart)
- [x] Network info + on-demand test
- [x] Settings + Bundle editor
- [x] Privacy screen
- [ ] Advanced settings (Disable Optimization / Emergency Reset)

## 6. Services & Background
- [x] Notification channels + `GDataNotificationManager`
- [x] `RulesWorker` (periodic evaluation)
- [x] `GDataVpnService` (optional, non-decrypting)
- [ ] Boot receiver to re-enqueue workers (optional)

## 7. Privacy & Legal
- [x] In-app Privacy screen
- [x] `PRIVACY_POLICY.md` (Big Big Dream)
- [ ] Host Privacy Policy URL for Play Console
- [ ] Data safety form answers prepared

## 8. Polish & Release
- [ ] Adaptive icon
- [ ] Splash screen
- [ ] Dark / light theme tuning
- [ ] ProGuard / R8 rules
- [ ] Baseline profile
- [ ] Internal testing track on Play Console
- [ ] Screenshots & feature graphic

## 9. Testing Matrix
- [ ] Android 8–14 devices / emulators
- [ ] Usage Access denied / granted flows
- [ ] VPN consent accept / deny / revoke
- [ ] No-network behaviour
- [ ] Battery optimization / Doze impact on workers

---

**Remember the product rule:**  
Never claim free data or artificial speed increases. Always label estimates clearly.
