# Technical Architecture — LinkVault

## 1. Overview & Core Mission
LinkVault is a local-first Android application designed for rapid link and note capture via the Android system Share Sheet, background enrichment, automatic classification, and full local retrieval.

```
External Apps (Chrome, Twitter, YouTube, etc.)
      │
      ▼  [Android Share Intent: ACTION_SEND]
ShareCaptureActivity (< 50ms)
      │
      ├── Insert into Room DB (Status: PENDING)
      ├── Dispatch instant user Toast ("Saved to LinkVault")
      └── Enqueue EnrichmentWorker via WorkManager
      │
      ▼  [Background Thread]
EnrichmentWorker
      ├── URL Normalization & Domain Extraction
      ├── Safe Network Fetch (Jsoup/OkHttp, 10s timeout, max 2MB stream)
      ├── Metadata Extraction (Title, Description, Favicon, OG tags)
      ├── AutoClassifier (Rule & Heuristic Engine)
      └── Update Room DB (Status: COMPLETED)
```

## 2. Layered Architecture

```
┌────────────────────────────────────────────────────────┐
│                   Presentation Layer                   │
│   Jetpack Compose + Material 3 + Navigation Compose   │
│   (HomeScreen, DetailScreen, NotesScreen, Settings)   │
└───────────────────────────▲────────────────────────────┘
                            │ StateFlow / UI Events
┌───────────────────────────┴────────────────────────────┐
│                    ViewModel Layer                     │
│    VaultViewModel, NoteViewModel, SettingsViewModel    │
└───────────────────────────▲────────────────────────────┘
                            │ Kotlin Coroutines / Flows
┌───────────────────────────┴────────────────────────────┐
│                    Repository Layer                    │
│             LinkVaultRepository & BackupManager        │
└─────────────▲─────────────────────────────▲────────────┘
              │                             │
┌─────────────┴──────────────┐ ┌────────────┴────────────┐
│      Local Storage         │ │    Background Engine    │
│  AndroidX Room SQLite DB   │ │   AndroidX WorkManager  │
│  (Entities, DAOs, Indices) │ │    (EnrichmentWorker)   │
└────────────────────────────┘ └─────────────────────────┘
```

## 3. Technology Stack
- **Language**: Kotlin 2.0+ (100% Kotlin Coroutines & Flow)
- **UI Toolkit**: Jetpack Compose + Material 3 (Tokens, Shapes, Light/Dark)
- **Storage**: AndroidX Room SQLite with schema versioning & indices
- **Async Execution**: Kotlin Coroutines + AndroidX WorkManager
- **Network / Scraping**: OkHttp 4.12 + Jsoup (Strict byte cap, no arbitrary execution)
- **Serialization**: Kotlinx Serialization JSON (Type-safe backup/restore)
- **Dependency Injection**: Clean ServiceLocator / Factory pattern for lightweight zero-reflection runtime
- **CI/CD**: GitHub Actions building linted, tested Release & Debug APKs
