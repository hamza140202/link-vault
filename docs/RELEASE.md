# Testing Strategy & Acceptance Criteria — LinkVault

## 1. Automated Unit Tests
- **UrlExtractorTest**: Validates extraction of clean URLs from messy strings containing extraneous text, UTM tracking parameters, hashtags, etc.
- **AutoClassifierTest**: Validates categorization heuristics (e.g., GitHub -> Development, YouTube -> Entertainment, Unknown -> Uncategorized).
- **BackupManagerTest**: Validates versioned JSON serialization, schema migration, malformed JSON recovery, and merge vs replace integrity.

## 2. Integration & Manual Acceptance Scenarios
1. Share URL from Chrome -> instant toast -> saved locally in <50ms.
2. Background enrichment runs when network is connected.
3. Turn on Airplane Mode -> Share URL -> verified saved as PENDING / local URL safely.
4. Large clipboard paste (50,000+ words) -> preserves all characters, line breaks, Unicode.
5. Export backup -> import backup on fresh database -> 100% data fidelity.

# Release & CI/CD Specification — LinkVault

## 1. GitHub Actions Workflows
- `.github/workflows/android-build.yml`:
  - Triggers on `push` and `pull_request` on `main`.
  - Compiles Debug & Release APKs.
  - Executes unit test suite.
  - Computes SHA-256 checksum of generated APKs.
  - Uploads build artifacts to workflow run.
- `.github/workflows/release.yml`:
  - Triggers on Git tags `v*` or manual dispatch.
  - Produces release APK.
  - Creates GitHub Release with signed/release APK assets and release notes.
