# MomoStack: Google Play Store Production Readiness & Security Master Plan

This comprehensive guide details everything required to take **MomoStack** from its current beta status (`v1.0.15-beta4`) to a hardened, fully compliant, production-ready release on the **Google Play Store**.

---

## 1. Security Hardening & Anti-Reverse Engineering

Currently, MomoStack builds with `isMinifyEnabled = false` and has hardcoded keystore passwords in `app/build.gradle.kts`. For Google Play Store, this must be completely hardened against decompilation, string extraction, and modding.

### 1.1 R8 / ProGuard Optimization & Obfuscation
1. **Enable Minification & Shrinking in `app/build.gradle.kts`**:
   ```kotlin
   buildTypes {
       release {
           isMinifyEnabled = true
           isShrinkResources = true
           proguardFiles(
               getDefaultProguardFile("proguard-android-optimize.txt"),
               "proguard-rules.pro"
           )
           signingConfig = signingConfigs.getByName("release")
       }
   }
   ```
2. **`proguard-rules.pro` tailored for MomoStack**:
   - **Kotlinx Serialization**: Keep `@Serializable` companion objects and properties.
   - **Room SQLite**: Preserve entities, DAOs, and TypeConverters.
   - **ML Kit Text Recognition**: Keep core native JNI wrappers and pipeline entry points.
   - **OkHttp / Jsoup**: Retain rules for HTTP/TLS reflection and network client internals.
   - **Jetpack Compose**: Keep `@Composable` function models, `@Stable`, and `@Immutable` state holders.
   - **Mapping File Generation**: Store and upload `app/build/outputs/mapping/release/mapping.txt` to Google Play Console for de-obfuscating crash logs.

### 1.2 Anti-Tamper & Anti-Modding Protections
- **Installer Verification**: Verify at runtime that the app was installed via official package installers (`com.android.vending` for Google Play) rather than modified sideloaded packages.
  ```kotlin
  fun isInstalledFromPlayStore(context: Context): Boolean {
      val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
      } else {
          @Suppress("DEPRECATION")
          context.packageManager.getInstallerPackageName(context.packageName)
      }
      return installer == "com.android.vending"
  }
  ```
- **Runtime APK Signature Verification**: Check the signing certificate fingerprint dynamically at startup to abort execution if an attacker unpacked, modified bytecode, and re-signed the APK with a different key.
- **Google Play Integrity API**: Integrate the Play Integrity API (the successor to SafetyNet) to verify genuine app binary, genuine device, and genuine Google Play license.
- **Anti-Debugging & Emulator Detection**: Check `android.os.Debug.isDebuggerConnected()` and detect debug build flags before processing sensitive user notes.

### 1.3 Data Protection at Rest & Network Security
- **Network Security Configuration (`res/xml/network_security_config.xml`)**:
  - Enforce `cleartextTrafficPermitted="false"` globally.
  - Require modern TLS 1.3/1.2 for all URL metadata fetching.
- **Database & Vault Protection**:
  - Store sensitive user-configured credentials (e.g. backup passphrases, export keys) in `EncryptedSharedPreferences` backed by the **Android Keystore (Tee/StrongBox)**.
  - Set file permissions for exported backups and PDFs inside private app storage directories (`context.filesDir` / `context.cacheDir`) with secure `FileProvider` authorities.

---

## 2. Google Play Store Compliance & Policy

### 2.1 Developer Account & Verification
- **Account Type**: Google now mandates standard identity verification for all individual accounts and a verified **D-U-N-S Number** for organization accounts.
- **Closed Testing Mandate (New Personal Accounts)**: For personal developer accounts created after Nov 2023, Google requires a minimum of **20 opted-in testers for at least 14 continuous days** in a closed testing track before applying for production access.

### 2.2 Permissions Declaration & Scrutiny
MomoStack uses several key Android capabilities that need explicit justifications:
- **`POST_NOTIFICATIONS`**: Must explain in-app (runtime dialog) that notifications are used solely for screenshot transcription completion and background offline PDF generation alerts.
- **Storage / Media Access (`READ_MEDIA_IMAGES` / MediaStore)**:
  - MomoStack observes MediaStore to auto-capture screenshots.
  - Google Play has strict photo/media permission policies. We must declare that screenshot capture is an explicitly user-toggled feature (not continuous unprompted indexing), with all OCR happening purely on-device via ML Kit without network transmission.
- **`INTERNET`**: Used strictly for user-requested link metadata enrichment and web PDF extraction.

### 2.3 Data Safety Section (Zero-Cloud Local-First Declaration)
Because MomoStack operates local-first, the Data Safety Form declarations are clean and user-friendly:
- **Data Collected**:
  - **Photos/Media**: Processed on-device only; not transmitted to any external server.
  - **Text / Notes / URLs**: Stored in local SQLite; zero analytics, zero crash telemetry transmitted off-device.
- **Data Shared**: **No** data shared with any third party.
- **Security Practices**:
  - All network traffic is encrypted via HTTPS.
  - User can delete all notes, links, and backups locally with 1 tap.
  - Full data export and portability supported (JSON backups).

### 2.4 Privacy Policy & Legal Links
- A hosted **Privacy Policy URL** is mandatory on the Play Store listing.
- Can be hosted via a public GitHub Pages repository (e.g. `https://hamza140202.github.io/link-vault/privacy.html`).
- Must clearly outline:
  - Local-first architecture (no external database or tracking).
  - On-device ML Kit processing.
  - Local backup storage and user control.

---

## 3. Store Listing Assets & Metadata

| Asset | Specifications | Purpose |
|---|---|---|
| **App Icon** | 512 x 512 px, 32-bit PNG, max 1024 KB | High-res store icon |
| **Feature Graphic** | 1024 x 500 px, JPG or 24-bit PNG | Promoted banner on Play Store listing |
| **Phone Screenshots** | Min 4 screenshots, 16:9 or 9:16, 1080 x 1920 or 1080 x 2400 | Showcase Home stack, Notes editor, Offline reader, and Screenshot vault |
| **App Title** | Up to 30 characters | E.g., `MomoStack: Offline Link & Notes` |
| **Short Description** | Up to 80 characters | Fast overview of local capture, offline reader, and on-device OCR |
| **Full Description** | Up to 4000 characters | Complete feature guide, privacy guarantee, and usage instructions |

---

## 4. Release Engineering & Build Setup

### 4.1 Transition from APK to Android App Bundle (.aab)
- Google Play requires `.aab` for all new app releases.
- In `app/build.gradle.kts`:
  - Run `./gradlew bundleRelease` to generate `app-release.aab`.
  - Google Play generates optimized device-specific APKs (Dynamic Delivery) based on device screen density, CPU architecture (ARM64), and language, cutting user download size by ~30-50%.

### 4.2 Play App Signing Keystore Separation
- Current keystore is stored directly in git (`keystore/release.keystore`).
- **Production Standard**:
  - Migrate keystore passwords and sensitive keys to GitHub Secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`).
  - Enroll in **Google Play App Signing**: Your local key acts as the **Upload Key**, while Google manages and protects the primary **App Signing Key**.

### 4.3 Automated GitHub Actions Play Store Deployment
We can extend `.github/workflows/release.yml` with `r0adkll/upload-google-play@v1` using a Google Cloud Service Account to automatically publish release `.aab` bundles directly to the Google Play Console Internal or Closed track upon tagging.

---

## 5. Live App Inspection & Local Device Verification Strategy

To verify the app directly on an actual device or simulator alongside code development:
1. **Physical Device ADB Connection**:
   - Connect your Android device via USB with USB Debugging enabled.
   - Run `adb devices` to check availability.
   - Install release or debug builds directly via:
     ```powershell
     adb install -r app/build/outputs/apk/release/momostack-release.apk
     ```
2. **Live UI Inspection & Screenshot Verification**:
   - Take screenshots of the running screen via ADB:
     ```powershell
     adb exec-out screencap -p > live_preview.png
     ```
   - Inspect live layout hierarchy and Compose bounds via Android Studio Layout Inspector or ADB UI dump (`adb shell uiautomator dump`).
3. **Logcat Monitoring**:
   - Monitor real-time logs for WorkManager, ML Kit OCR, and PDF printing:
     ```powershell
     adb logcat -s "MomoStack:*" "WebPdfExtractor:*" "EnrichmentWorker:*"
     ```
