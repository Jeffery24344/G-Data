# Build G Data on GitHub (no Android Studio required)

## Automatic build

Every push to `main` runs **GitHub Actions** and produces a debug APK.

### How to get the APK

1. Open: https://github.com/Jeffery24344/G-Data/actions
2. Click the latest **Build APK** run (green check = success).
3. Scroll to **Artifacts**.
4. Download **G-Data-debug-apk**.
5. Unzip and install `app-debug.apk` on your Android phone.

### Manual rebuild

1. Go to **Actions** → **Build APK**.
2. Click **Run workflow** → **Run workflow**.
3. Wait 2–5 minutes, then download the artifact.

### Install on phone

- Enable **Install unknown apps** for your file manager/browser.
- Open the downloaded APK and install.

---

## If the workflow fails

Open the failed job → open the red step → copy the error text.  
Paste it here and it can be fixed in the next commit.

Common first-time notes:
- First run may take longer while Gradle and the Android SDK download.
- You need Actions enabled on the repo (default for public repos).
