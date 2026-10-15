# Background Processing Specification — MomoStack

## 1. WorkManager Engine (URL Enrichment)
Background enrichment for saved URLs is handled via `androidx.work.WorkManager` through a dedicated `EnrichmentWorker` extending `CoroutineWorker`.

### Constraints & Triggers
- Requires `NetworkType.CONNECTED`.
- Exponential backoff policy for transient network drops (initial delay: 10s).
- Execution capped at 3 retries max; on 3rd failure status becomes `FAILED_METADATA`, preserving the original URL permanently in the library.

### Worker Tasks
1. Read item from Room DB via DAO by ID.
2. If already enriched or cancelled, exit cleanly.
3. Fetch URL header and HTML body (up to 2MB limit, 10s connect/read timeout).
4. Parse OpenGraph and HTML meta tags.
5. Derive domain, favicon URL, and content classification.
6. Commit updated attributes to Room DB and mark status `COMPLETED`.

---

## 2. Screenshot Watcher Service (Auto-Capture & OCR Engine)
Automated detection and text transcription of screenshots is executed by `ScreenshotWatcherService`, an Android Foreground Service with type `dataSync`.

### Operational Flow
1. **Foreground Notification**: Posts an ongoing low-priority notification (`"Screenshot Vault Active"`) so the service survives OEM background memory killers.
2. **MediaStore Observers**: Registers `ContentObserver` instances on:
   - `MediaStore.Images.Media.EXTERNAL_CONTENT_URI`
   - `MediaStore.Images.Media.INTERNAL_CONTENT_URI`
3. **Settling Window**: Introduces a deliberate `450ms` delay after `onChange(uri)` triggers. This guarantees that Android SystemUI finishes flushing raw image bytes to disk before MomoStack attempts to read the file.
4. **OEM-Agnostic Pattern Matching**: Queries recent images sorted strictly by `${MediaStore.Images.Media.DATE_ADDED} DESC` (avoiding SQL `LIMIT` syntax forbidden on Android 10+). Validates matching against:
   - `BUCKET_DISPLAY_NAME` (e.g. `"Screenshots"`)
   - `RELATIVE_PATH` / `DATA` (e.g. `"Pictures/Screenshots/"`, `"DCIM/Screenshots/"`)
   - `DISPLAY_NAME` (e.g. `"Screenshot_..."`, `"Screen_..."`, `"ScreenCapture_..."`)
5. **Private Disk Ingestion**: Copies the image stream directly to `context.filesDir/screenshots/`.
6. **Progressive Notification**:
   - Posts `📸 Screenshot Captured` (*"Transcribing text in background..."*).
7. **Local ML Kit Processing**: Passes the internal file to `ScreenshotOcrProcessor.extractTextFromFile(file)` on `Dispatchers.IO`. Downsamples ultra-high-resolution images (4K/QHD) if necessary to ensure low memory footprint.
8. **Completion & Headless Copying**:
   - Commits `ScreenshotEntity` to `ScreenshotVaultDatabase.screenshotDao()`.
   - Replaces reading notification with `✨ Transcription Completed` (`PRIORITY_HIGH`), displaying the text snippet and attaching a `[Copy Text]` action linked to `CopyTextReceiver`.
