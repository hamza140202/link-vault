# Share Capture Specification — LinkVault

## 1. Primary Mandate
Capture must be instant, non-blocking, and 100% resilient to network absence or failure.

## 2. Intent Handling Strategy
- **Target Activity**: `com.momostack.app.ui.ShareCaptureActivity`
- **Filter**:
  ```xml
  <intent-filter>
      <action android:name="android.intent.action.SEND" />
      <category android:name="android.intent.category.DEFAULT" />
      <data android:mimeType="text/plain" />
  </intent-filter>
  ```
- **Theme**: `@android:style/Theme.Translucent.NoTitleBar` to eliminate UI flashing and avoid distracting the user from their current task.

## 3. Parsing Pipeline
1. Extract `Intent.EXTRA_TEXT` and `Intent.EXTRA_SUBJECT`.
2. Regex scan for web URLs (`https?://[^\s]+`).
3. If URL found:
   - Extract raw URL.
   - Clean tracking parameters (`utm_*`, `fbclid`, `si`, `ref`).
   - Retain remaining text as initial note content if text accompanied the link.
4. If plain text without URL:
   - Route to Note creation mode automatically.
5. Create local `LinkItemEntity` with status `PENDING`.
6. Commit immediately to Room DB using `Dispatchers.IO`.
7. Enqueue `OneTimeWorkRequest` for `EnrichmentWorker` with item UUID.
8. Show a lightweight Toast: `"Saved to LinkVault"`.
9. Call `finishAffinity()` within < 50ms of launch.
