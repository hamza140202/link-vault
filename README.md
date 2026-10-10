# MomoStack 🥟📚

> **Capture anything → Save it instantly → Automatically organize it → Return to it later.**

MomoStack is a high-performance, local-first Android link and note capture library built with Jetpack Compose, Material 3, AndroidX Room SQLite, and WorkManager.

---

## ⚡ Core Highlights
- **Sub-50ms Share Sheet Capture**: Share from any browser or app, instant confirmation Toast, zero lag, no waiting for web scraping.
- **Background URL Enrichment**: WorkManager extracts OpenGraph titles, descriptions, favicons, and canonical links safely without blocking the UI.
- **Heuristic Auto-Categorization**: Intelligent rule and keyword classifier tags items into clean categories (Development, Reading, Entertainment, Shopping, Finance, etc.) with safe "Uncategorized" fallback.
- **First-Class Notes & Extreme Clipboard**: Real notes with 1-tap clipboard paste supporting large payloads, Unicode, and preserve formatting.
- **Local-First & Zero Cloud Dependency**: Your data never leaves your device. SQLite storage with versioned JSON backup and restore.
- **Anti-AI-Slop Material 3 UI**: Clean Indigo `#4F46E5` design tokens, tabular digits (`.tnum`), crisp vector icons, and zero distracting visual filler.

---

## 🏗️ Architecture

```
External Share Intent
        │
        ▼
ShareCaptureActivity (< 50ms) ──► Instant Toast & Finish
        │
        ├──► Room DB (Status: PENDING)
        │
        ▼
WorkManager (EnrichmentWorker)
        │
        ├──► Safe HTML Metadata Fetch (Cap 2MB)
        ├──► Auto-Classification Engine
        └──► Update Room DB (Status: COMPLETED)
```

---

## 🚀 Building & Releasing via GitHub Actions
LinkVault builds automatically via GitHub Actions:
- On every push/PR: Compiles APKs, runs unit tests, generates SHA-256 checksums, and uploads APK artifacts.
- On release tags (`v*`): Creates a published GitHub Release with downloadable APKs.

---

## 📄 License
Private & Proprietary. All rights reserved.
