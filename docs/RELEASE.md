# Testing Strategy & Acceptance Criteria — MomoStack

## 1. Automated Unit Tests
- **UrlExtractorTest**: Validates extraction of clean URLs from messy strings containing extraneous text, UTM tracking parameters, hashtags, etc.
- **AutoClassifierTest**: Validates categorization heuristics (e.g., GitHub -> Development, YouTube -> Entertainment, Unknown -> Uncategorized).
- **BackupManagerTest**: Validates versioned JSON serialization, schema migration, malformed JSON recovery, and merge vs replace integrity.

## 2. Integration & Manual Acceptance Scenarios
1. **Link Share**: Share URL from Chrome -> instant toast -> saved locally in <50ms.
2. **Background Enrichment**: Runs when network is connected; offline captures remain as `PENDING` until network resumes.
3. **Large Clipboard**: Large clipboard paste (50,000+ words) -> preserves all characters, line breaks, Unicode.
4. **Screenshot Auto-Capture**: Enable service in Settings -> take screenshot anywhere -> `📸 Screenshot Captured` notification appears -> `✨ Transcription Completed` notification appears with text preview and `[Copy Text]` action.
5. **Headless Copying**: Tap `[Copy Text]` on notification -> text is copied to clipboard without opening the app -> toast confirms copy.
6. **Direct Image Sharing**: Share screenshot/image from Google Photos or gallery to MomoStack -> routes to Screenshot Vault -> transcribed cleanly -> does NOT create dummy note.
7. **Interactive Editing**: Tap screenshot card in Vault -> edit text inside `OutlinedTextField` -> highlight/select words -> copy partial selection or full text.
8. **Backup / Restore**: Export backup -> import backup on fresh database -> 100% data fidelity.

---

# Release & CI/CD Specification — MomoStack

## 1. GitHub Actions Workflows

### `.github/workflows/android-build.yml`
- Triggers on `push` and `pull_request` on `main`.
- Compiles Debug & Release APKs.
- Executes unit test suite.
- Computes SHA-256 checksum of generated APKs.
- Uploads build artifacts to workflow run.

### `.github/workflows/release.yml`
- Triggers on Git tags `v*` or manual dispatch.
- Compiles production release APK with `arm64-v8a` ABI optimization.
- Packages assets and generates `SHA256SUMS.txt`.
- Publishes official GitHub Release.
- Executes `scripts/send_to_telegram.py` on the runner to upload the release APK directly to Telegram chat `1263089875` over gigabit uplink in ~3 seconds.
