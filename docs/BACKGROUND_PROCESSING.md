# Background Processing Specification — LinkVault

## 1. WorkManager Engine
Background enrichment is handled exclusively via `androidx.work.WorkManager` through a dedicated `EnrichmentWorker` extending `CoroutineWorker`.

## 2. Constraints & Triggers
- Requires `NetworkType.CONNECTED`.
- Exponential backoff policy for transient network drops (initial delay: 10s).
- Execution capped at 3 retries max; on 3rd failure status becomes `FAILED_METADATA`, preserving the original URL permanently in the library.

## 3. Worker Tasks
1. Read item from Room DB via DAO by ID.
2. If already enriched or cancelled, exit cleanly.
3. Fetch URL header and HTML body (up to 2MB limit, 10s connect/read timeout).
4. Parse OpenGraph and HTML meta tags.
5. Derive domain, favicon URL, and content classification.
6. Commit updated attributes to Room DB.
7. Mark status `COMPLETED`.
8. Never block the user UI or throw unhandled exceptions.
