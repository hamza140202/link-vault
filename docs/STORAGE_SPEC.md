# Storage Specification — LinkVault

## 1. Schema Design (AndroidX Room SQLite)

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

## 2. Indices & Query Performance
- Composite index on `(isArchived, createdAt DESC)` for fast home feed loading.
- Composite index on `(category, isArchived, createdAt DESC)` for filtered queries.
- Index on `isFavorite` for instant bookmark access.
- SQLite `LIKE` and tokenized search across `title`, `domain`, `description`, `notes`.
