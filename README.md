# MomoStack 🥟📚

> **Capture anything → Save it instantly → Automatically organize it → Return to it later.**

MomoStack is a high-performance, local-first Android capture system and library built with Jetpack Compose, Material 3, AndroidX Room SQLite, WorkManager, and Google ML Kit On-Device Text Recognition.

---

## ⚡ Core Highlights

- **Sub-50ms Share Sheet Capture**: Share links, notes, or images from any browser or app. Instant confirmation Toast, zero lag, no blocking for scraping or OCR.
- **📸 Screenshot Vault & On-Device OCR (Labs)**: An isolated local screenshot archive that detects, stores, and extracts readable text with 100% on-device ML Kit OCR.
- **🔄 Universal Screenshot Auto-Capture**: Background service (`ScreenshotWatcherService`) detects screenshots taken anywhere on the device across all Android OEMs (Samsung OneUI, Xiaomi MIUI, OnePlus OxygenOS, Google Pixel) with a 450ms SystemUI write delay for zero-byte prevention.
- **✨ Dual System Notifications with 1-Tap Copy**:
  - `📸 Screenshot Captured` (*"Transcribing text in background..."*)
  - `✨ Transcription Completed` with preview and a headless **`[Copy Text]`** action button to copy straight to your clipboard without opening the app.
- **🖼️ Direct Image Sharing Interception**: Sending screenshots or images from the system Share Sheet directly routes them into the Screenshot Vault and initiates OCR, rather than dumping them as plain text notes.
- **✏️ Editable & Selectable OCR Text**: Tap into the transcription inside the Screenshot Detail sheet to edit wording or select and copy specific sections.
- **Background URL Enrichment**: WorkManager extracts OpenGraph titles, descriptions, favicons, and canonical links safely without blocking the UI.
- **Heuristic Auto-Categorization**: Intelligent rule and keyword classifier tags items into clean categories (Development, Reading, Entertainment, Shopping, Finance, etc.) with safe "Uncategorized" fallback.
- **First-Class Notes & Extreme Clipboard**: Real notes with 1-tap clipboard paste supporting large payloads, Unicode, and rich formatting.
- **Local-First & Zero Cloud Dependency**: Your data never leaves your device. Split SQLite storage (`links.db` + `screenshot_vault.db`) with versioned JSON backup and restore.
- **Anti-AI-Slop Material 3 UI**: Clean Indigo `#4F46E5` design tokens, tabular digits (`.tnum`), crisp vector icons, and zero distracting visual filler.

---

## 🏗️ Architecture

```
[ External Apps (Chrome, Gallery, SystemUI) ]
         │
         ├─── Web Link / Plain Text (ACTION_SEND)
         │         │
         │         ▼
         │   ShareCaptureActivity (< 50ms) ──► Instant Toast & Finish
         │         │
         │         ├──► LinkVault DB (Status: PENDING)
         │         │
         │         ▼
         │   WorkManager (EnrichmentWorker)
         │         ├──► Safe HTML Metadata Fetch (Cap 2MB)
         │         ├──► Auto-Classification Engine
         │         └──► Update LinkVault DB (Status: COMPLETED)
         │
         └─── Image Share / Media URI / System Screenshot
                   │
                   ▼
             [ ScreenshotWatcherService / ShareCaptureActivity ]
                   │
                   ├──► Save to private internal storage (filesDir/screenshots/)
                   ├──► Post "📸 Screenshot Captured" notification
                   │
                   ▼  (Background Thread)
             ScreenshotOcrProcessor (Google ML Kit Text Recognition)
                   │
                   ├──► Save to ScreenshotVaultDatabase (screenshot_vault.db)
                   │
                   ▼
             Post "✨ Transcription Completed" notification
                   └──► Headless [Copy Text] Broadcast Receiver
```

---

## 🚀 Building & Releasing via GitHub Actions

MomoStack builds and distributes automatically via GitHub Actions:
- **CI Build**: Compiles APKs, runs unit tests, computes SHA-256 checksums, and uploads APK artifacts.
- **Release Pipeline (`v*`)**: Automatically compiles release APKs with `arm64-v8a` ABI optimization, publishes GitHub Releases, and delivers the binary directly to Telegram in seconds via `scripts/send_to_telegram.py`.

---

## 📄 License

Private & Proprietary. All rights reserved.
