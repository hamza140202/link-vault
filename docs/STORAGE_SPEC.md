# Storage Specification — MomoStack

## 1. Primary Database: `links.db` (AndroidX Room)

### Table: `link_items`
- `id` (TEXT, Primary Key, UUID)
- `url` (TEXT, Non-null)
- `normalizedUrl` (TEXT, Nullable, indexed)
- `title` (TEXT, Non-null)
- `domain` (TEXT, Nullable, indexed)
- `description` (TEXT, Nullable)
- `notes` (TEXT, Nullable)
- `category` (TEXT, Non-null, default "Uncategorized", indexed)
- `tagsJson` (TEXT, Non-null, default "[]")
- `isFavorite` (INTEGER, Non-null, default 0, indexed)
- `isArchived` (INTEGER, Non-null, default 0, indexed)
- `status` (TEXT, Non-null, default "PENDING")
- `faviconUrl` (TEXT, Nullable)
- `previewImageUrl` (TEXT, Nullable)
- `createdAt` (INTEGER, Non-null, indexed)
- `updatedAt` (INTEGER, Non-null)

### Table: `categories`
- `id` (TEXT, Primary Key)
- `name` (TEXT, Non-null, Unique)
- `colorHex` (TEXT, Non-null)
- `iconName` (TEXT, Non-null)
- `isSystem` (INTEGER, Non-null, default 0)

---

## 2. Screenshot Vault Database: `screenshot_vault.db` (AndroidX Room)

To maintain absolute modularity and prevent link and note schemas from bloating, the Screenshot Vault operates on its own dedicated SQLite database instance.

### Table: `screenshots`
- `id` (INTEGER, Primary Key, Auto-Generate)
- `localImagePath` (TEXT, Non-null) — Absolute path to image stored in `context.filesDir/screenshots/`
- `originalUri` (TEXT, Nullable) — Content URI of original screenshot if available
- `extractedText` (TEXT, Non-null, default "") — Full transcribed text produced by ML Kit OCR
- `timestamp` (INTEGER, Non-null) — Capture timestamp in milliseconds (indexed for chronological sorting)
- `isFavorite` (INTEGER, Non-null, default 0) — Boolean flag for quick filtering
- `tagsJson` (TEXT, Non-null, default "[]") — Tagging support for future categorization

### Storage Indices & Performance
- Index on `timestamp DESC` for instant reverse-chronological gallery loading.
- On-device file assets stored in internal private sandbox (`filesDir/screenshots/`), automatically wiped if user clears app data, protected from external app inspection.
