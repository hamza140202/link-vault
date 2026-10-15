# Technical Architecture — MomoStack

## 1. Overview & Core Mission
MomoStack is a dual-engine, local-first Android capture system designed for:
1. **Instant Web Link & Note Capture**: Sub-50ms ingestion via Android Share Sheet, WorkManager background enrichment, heuristic auto-categorization, and Room SQLite storage.
2. **Screenshot Vault & On-Device OCR (Labs)**: Automated system screenshot observation, direct image share interception, local image storage, and 100% on-device text recognition via Google ML Kit.

```
┌────────────────────────────────────────────────────────────────────────┐
│                        External Trigger Sources                        │
│   (System Screenshots, Web Browsers, Social Media, Share Sheet)        │
└───────────────────┬────────────────────────────────┬───────────────────┘
                    │                                │
             [ACTION_SEND text]              [ACTION_SEND image / MediaStore]
                    │                                │
                    ▼                                ▼
       ShareCaptureActivity (< 50ms)     ScreenshotWatcherService / ShareCapture
                    │                                │
                    ├── LinkVault Database           ├── Copy to internal private filesDir
                    │   (links.db)                   │   (filesDir/screenshots/)
                    │                                ├── Post "📸 Screenshot Captured" notification
                    ▼                                │
            WorkManager Worker                       ▼
        (Jsoup / OkHttp Enrichment)       ScreenshotOcrProcessor (ML Kit OCR)
                    │                                │
                    ▼                                ├── ScreenshotVaultDatabase
          Update LinkItemEntity                      │   (screenshot_vault.db)
                                                     ▼
                                          Post "✨ Transcription Completed" notification
                                                     └── CopyTextReceiver (1-Tap Copy)
```

## 2. Layered Architecture

```
┌──────────────────────────────────────────────────────────────────────────┐
│                           Presentation Layer                             │
│       Jetpack Compose + Material 3 + Navigation Compose                  │
│  (HomeScreen, DetailScreen, NotesScreen, SettingsScreen, ScreenshotVault)│
└───────────────────────────▲──────────────────────────────────────────────┘
                            │ StateFlow / UI Events
┌───────────────────────────┴──────────────────────────────────────────────┐
│                            ViewModel Layer                               │
│  VaultViewModel, NoteViewModel, SettingsViewModel, ScreenshotVaultViewModel
└───────────────────────────▲──────────────────────────────────────────────┘
                            │ Kotlin Coroutines / Flows
┌───────────────────────────┴──────────────────────────────────────────────┐
│                           Repository Layer                               │
│        LinkVaultRepository, BackupManager, ScreenshotVaultRepository     │
└─────────────▲─────────────────────────────▲──────────────────────────────┘
              │                             │
┌─────────────┴──────────────┐ ┌────────────┴──────────────────────────────┐
│       Local Storage        │ │            Background Engines             │
│  • LinkVault DB (links.db) │ │  • AndroidX WorkManager (Enrichment)      │
│  • Screenshot Vault DB     │ │  • ScreenshotWatcherService (Foreground)  │
│    (screenshot_vault.db)   │ │  • ScreenshotOcrProcessor (ML Kit)        │
│  • Internal Screenshots Dir│ │  • CopyTextReceiver (Clipboard Broadcast) │
└────────────────────────────┘ └───────────────────────────────────────────┘
```

## 3. Technology Stack
- **Language**: Kotlin 2.0+ (100% Kotlin Coroutines & Flow)
- **UI Toolkit**: Jetpack Compose + Material 3 (Tokens, Shapes, Light/Dark, Window Insets)
- **Primary Storage**: AndroidX Room SQLite (`links.db`) with schema versioning & composite indices
- **Vault Storage**: Isolated AndroidX Room SQLite (`screenshot_vault.db`) for zero link/note schema pollution
- **Machine Learning / OCR**: Google ML Kit Text Recognition (`com.google.mlkit:text-recognition:16.0.1`) executing 100% locally on device
- **System Observers**: `ContentObserver` on `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` and `INTERNAL_CONTENT_URI` with a 450ms SystemUI flush delay
- **Async Execution**: Kotlin Coroutines + AndroidX WorkManager (`EnrichmentWorker`) + Foreground Service (`ScreenshotWatcherService`)
- **Network / Scraping**: OkHttp 4.12 + Jsoup (Strict byte cap, 10s timeouts, no third-party telemetry)
- **Serialization**: Kotlinx Serialization JSON (Type-safe backup/restore)
- **CI/CD**: GitHub Actions building signed `arm64-v8a` optimized release APKs with automated Telegram distribution (`scripts/send_to_telegram.py`)
