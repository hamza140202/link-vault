# Share Capture Specification — MomoStack

## 1. Primary Mandate
Capture must be instant, non-blocking, and 100% resilient to network absence or failure.

## 2. Intent Handling Strategy
- **Target Activity**: `com.momostack.app.ui.ShareCaptureActivity` (translucent theme for sub-50ms dismiss) and `MainActivity`
- **Supported MIME Types**:
  - `text/plain`
  - `image/*` (PNG, JPEG, WebP)
- **Supported Extras**:
  - `Intent.EXTRA_TEXT`
  - `Intent.EXTRA_SUBJECT`
  - `Intent.EXTRA_STREAM` (Parcelable Uri)
  - `Intent.clipData` (Uri items)

## 3. Ingestion Pipelines

### A. Web Links & Plain Text
1. Extract text and regex scan for web URLs (`https?://[^\s]+`).
2. Clean tracking parameters (`utm_*`, `fbclid`, `si`, `ref`).
3. If URL found:
   - Create `LinkItemEntity` with status `PENDING`.
   - Commit immediately to Room DB (`links.db`).
   - Enqueue `OneTimeWorkRequest` for `EnrichmentWorker`.
   - Show Toast: `"Saved to LinkVault"`.
4. If plain text without URL:
   - Route to Note creation mode automatically.
5. Finish task in < 50ms.

### B. Direct Image & Screenshot Sharing
1. Inspect `intent.type?.startsWith("image/")` and check for `Intent.EXTRA_STREAM` / `clipData`.
2. Intercept before note routing to prevent image screenshots from becoming empty text notes.
3. Show immediate Toast: `"📸 Saving to Screenshot Vault..."`.
4. Delegate asynchronously to `ScreenshotVaultRepository.importScreenshotFromUri(imageUri)`:
   - Stream content to `context.filesDir/screenshots/`.
   - Execute `ScreenshotOcrProcessor` text extraction locally.
   - Insert `ScreenshotEntity` into `ScreenshotVaultDatabase.screenshotDao()`.
   - Trigger `✨ Transcription Completed` notification with 1-tap `[Copy Text]`.
5. Finish task cleanly.
