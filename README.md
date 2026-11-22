# GamerZone PK — Free Fire Esports Tournament Android App

**GamerZone PK** is a native Kotlin + Jetpack Compose Android application built for Pakistani Free Fire esports tournaments, featuring real-time Firebase Firestore synchronization, Google Sign-In via Android Credential Manager, Role-Based Access Control (Admin & Player), an adaptive Material 3 dark neon-green theme, custom launcher icon, and native Android 12+ & animated in-app splash screens.

---

## Core Features

1. **Splash Screen & Custom Launcher Icon**:
   - Adaptive launcher icon across all densities (`mdpi` to `xxxhdpi` + `anydpi-v26`).
   - Android 12+ native splash theme (`values-v31/themes.xml`) and animated in-app Compose splash screen (`GamerZoneSplashScreen`).
2. **Authentication & Player Registration (`AuthScreen`)**:
   - Real Google Sign-In powered by Jetpack `CredentialManager` (`GetSignInWithGoogleOption` / `GetGoogleIdOption`) + Firebase Authentication.
   - 2-step onboarding for In-Game Name (IGN), numeric Free Fire UID, Squad/Clan Name, and optional WhatsApp number.
3. **Home Arena (`HomeScreen`)**:
   - Custom neon esports banner, live KPI counters (`Total Players`, `Tournaments`, `Total Winners`), quick game-mode filters, featured Upcoming/Live matches, Announcements, and one-tap WhatsApp support.
4. **Tournament System & Details (`TournamentsScreen`, `TournamentDetailScreen`)**:
   - Modes: **Solo**, **Duo**, **Squad**, **Clash Squad**, and **Battle Royale**.
   - Statuses: **Upcoming**, **Live**, and **Completed**.
   - Atomic Firestore transactions for slot booking (`registrations` + `registeredCount` increment), prize pool breakdown (1st/2nd/3rd PKR), Room ID & Password visibility for registered players, roster list, and rules.
5. **Wallet System & Payments (`WalletScreen`)**:
   - **My Wallet**: Live Available Balance (PKR), Add Money, Withdraw Money, full Transaction History, Deposit History, Withdrawal History, and Tournament Fee History filter.
   - **Payment Methods**: **EasyPaisa (`03489674834`)** and **JazzCash (`03406690336`)** with one-tap account number copy.
   - **Add Money Flow**: Players enter deposit amount, choose EasyPaisa or JazzCash, submit Transaction ID (TID), and attach a payment screenshot via Android's zero-permission Photo Picker; Admins inspect the screenshot and approve/credit atomically.
   - **Tournament Entry**: Confirmation dialog displays current and post-entry wallet balance and atomically deducts the entry fee upon joining.
   - **Withdraw System**: Players request withdrawals (min 50 PKR) with atomic balance hold; Admins mark paid or reject with automatic balance refund.
6. **Leaderboard (`LeaderboardScreen`)**:
   - Real-time Player Rankings and Team/Squad Rankings calculated from Match Points + Kill Points = Total Points.
7. **Player Dashboard (`PlayerDashboardScreen`)**:
   - Wallet balance quick card, Active Joined Matches, Completed Match History, Editable Profile (IGN, Free Fire UID, Squad, WhatsApp), Notifications/Alerts, WhatsApp Support, Contact Email, About Us, and Rules & Regulations.
8. **Admin Dashboard (`AdminDashboardScreen`)**:
   - Role-gated via `/admins/{uid}` and verified admin email in `firestore.rules`.
   - Create, Edit, and Delete tournaments; update Room ID/Password & status; view registered players; publish match placements/kill points/winners (with optional PKR prize credit); verify pending deposit screenshots & withdrawal payouts in the **Wallet** tab; publish or delete global announcements; seed starter tournaments.

---

## Architecture & Key Files

- `app/src/main/java/com/example/MainActivity.kt` — Edge-to-edge activity host.
- `app/src/main/java/com/example/ui/GamerZoneApp.kt` — Splash screen, top header with clickable Wallet Balance pill, auth gate, `NavigationBar` / `NavigationRail` adaptive shell.
- `app/src/main/java/com/example/auth/AuthHelper.kt` — Credential Manager Google Sign-In & Firebase Auth integration.
- `app/src/main/java/com/example/data/GamerZoneRepository.kt` — Named Firestore database (`R.string.firestore_database_id`) repository with snapshot listeners and atomic wallet/tournament transactions.
- `app/src/main/java/com/example/viewmodel/GamerZoneViewModel.kt` — `StateFlow` state management for tournaments, wallet, registrations, leaderboard, announcements, and admin actions.
- `app/src/main/java/com/example/util/ScreenshotUtils.kt` — Zero-permission Photo Picker JPEG scaling & Base64 encoding/decoding for deposit proof screenshots.
- `app/src/main/java/com/example/model/EsportsModels.kt` — `@Keep` Firestore data classes & enums (`PlayerProfile`, `Tournament`, `DepositRequest`, `WithdrawalRequest`, `WalletTransaction`, `PaymentMethod`, etc.).
- `app/src/main/java/com/example/ui/screens/` — `AuthScreen.kt`, `HomeScreen.kt`, `TournamentsScreen.kt`, `WalletScreen.kt`, `LeaderboardScreen.kt`, `PlayerDashboardScreen.kt`, `AdminDashboardScreen.kt`.
- `firestore.rules` & `firebase-blueprint.json` — Zero-trust Firestore security rules & schema definition.

---

## Building the APK

### Option A: Export Directly from AI Studio (Fastest)
1. Open the **Settings** menu in AI Studio.
2. Choose **Generate APK** (or **Export as ZIP** / **Push to GitHub** to inspect or customize the full source code).

### Option B: Build Locally in Android Studio or Terminal
**Prerequisites:**
- Android Studio (Ladybug or newer) or JDK 17 + Android SDK (`compileSdk 36`, `minSdk 24`).
- Note: `app/google-services.json` and `app/src/main/res/values/firebase_applet_config.xml` are already configured for the provisioned Firebase backend (`com.aistudio.freefireposter.xkmqz`).

**Build a Debug APK (ready to install and test with Google Sign-In):**
- Inside AI Studio terminal:
  ```bash
  gradle :app:assembleDebug
  ```
- On your local machine (if using Gradle wrapper or system `gradle`):
  ```bash
  ./gradlew assembleDebug
  ```
- Output APK path:
  ```
  app/build/outputs/apk/debug/app-debug.apk
  ```

**Build a Signed Release APK for Public Distribution:**
1. Generate an upload/release keystore (keep it safe and private):
   ```bash
   keytool -genkey -v -keystore my-upload-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
   ```
2. Extract your release SHA-1 and SHA-256 fingerprints:
   ```bash
   keytool -list -v -keystore my-upload-key.jks -alias upload
   ```
3. **Important for Google Sign-In:** Add both your release `SHA-1` and `SHA-256` fingerprints to your Android app (`com.aistudio.freefireposter.xkmqz`) in the **Firebase Console -> Project Settings -> Your apps**, then download the updated `google-services.json` into `app/google-services.json` so Google Sign-In works on release builds.
4. Set environment variables and build the release APK:
   ```bash
   export KEYSTORE_PATH="$(pwd)/my-upload-key.jks"
   export STORE_PASSWORD="your-store-password"
   export KEY_PASSWORD="your-key-password"
   ./gradlew assembleRelease
   ```
   Output: `app/build/outputs/apk/release/app-release.apk`

---

## Free Public Website & Direct APK Distribution ($0 Cost)

GamerZone PK uses **direct APK distribution** through its own mobile-friendly public website (`/website`) hosted for **$0/month**—with **zero app store console accounts or paid hosting**.

### What's Inside `/website`
- `website/index.html` & `website/styles.css` — Mobile-first dark neon esports site with a direct **Download APK** button (`website/downloads/gamerzone-pk.apk`) and 3-step Android install guide.
- `website/app.js` & `website/config.js` — Live **Firebase Web SDK** integration connected to the exact same Firestore database (`ai-studio-android-ffposter-c25512ad-21aa-40be-872b-2126a42fdc0e`) as the Android APK, including **Tournaments**, **My Wallet** (EasyPaisa `03489674834` & JazzCash `03406690336`), **Leaderboard**, **Player Dashboard**, and **Admin Dashboard**.
- `.github/workflows/deploy-pages.yml` — Automated CI/CD pipeline that compiles the APK and publishes the entire website + APK to **GitHub Pages**.

### Option 1: Deploy for Free on GitHub Pages (Recommended — Includes Direct 27 MB APK Hosting)
1. **Export / Push Project**: In AI Studio, open **Settings → Push to GitHub** (or download ZIP and push to a free GitHub repository).
2. **Enable GitHub Pages**:
   - Open your GitHub repository → **Settings → Pages**.
   - Under **Build and deployment → Source**, select **GitHub Actions**.
3. **Run Deployment**:
   - Pushing to `main` (or clicking **Actions → Build APK & Deploy GamerZone PK Website → Run workflow**) automatically builds the APK, places it at `downloads/gamerzone-pk.apk`, and publishes your site at:
     `https://<your-github-username>.github.io/<your-repo-name>/`
4. **Authorize Website Google Sign-In**:
   - Open **Firebase Console → Project (`gen-lang-client-0582973628`) → Authentication → Settings → Authorized domains**.
   - Click **Add domain** and enter `<your-github-username>.github.io`.

### Option 2: Deploy for Free on Cloudflare Pages
1. **Host the 27.1 MB APK** in **GitHub Releases** (free up to 2 GB/file, because Cloudflare Pages limits individual static assets to 25 MiB):
   - Create a release in GitHub (`Releases → Draft a new release`), upload `app/build/outputs/apk/debug/app-debug.apk` (or signed release APK), and copy its direct asset URL.
   - Set `apkDownloadUrl` in `website/config.js` to that GitHub Release URL and remove `website/downloads/gamerzone-pk.apk` before uploading `website/` to Cloudflare Pages.
2. **Deploy on Cloudflare Pages**:
   - Log in to **Cloudflare Dashboard → Workers & Pages → Create → Pages**.
   - Connect your GitHub repo (Build output directory: `website`, Build command: leave blank) or drag-and-drop the `website` folder.
   - Your public URL (`https://gamerzone-pk.pages.dev`) goes live immediately.
3. **Authorize Domain in Firebase**:
   - Add `gamerzone-pk.pages.dev` (your `.pages.dev` domain) under **Firebase Console → Authentication → Settings → Authorized domains**.

### How Players Install the APK on Android
1. Visit your public website URL on any Android phone and tap **Download Android APK**.
2. Open the downloaded `.apk` file from browser notifications or **Files → Downloads**.
3. When prompted by Android, tap **Settings → Allow from this source → Install**.

