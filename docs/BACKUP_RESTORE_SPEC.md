# Backup & Restore Specification — LinkVault

## 1. Versioned JSON Architecture
Backups in LinkVault are versioned JSON archives designed to be transparent, portable, human-readable, and immune to internal SQLite binary changes.

```json
{
  "backupVersion": 1,
  "appVersion": "1.0.0",
  "exportedAt": 1728345600000,
  "deviceInfo": "Android 14 (API 34)",
  "items": [
    {
      "id": "e2b145a0-9c44-486a-b516-778841a10051",
      "url": "https://developer.android.com/jetpack/compose",
      "normalizedUrl": "https://developer.android.com/jetpack/compose",
      "title": "Jetpack Compose UI App Development Toolkit",
      "domain": "developer.android.com",
      "description": "Jetpack Compose is Android's recommended modern toolkit for building native UI.",
      "notes": "Review the rememberSaveable documentation for process death handling.",
      "category": "Development",
      "tags": ["android", "compose", "kotlin"],
      "isFavorite": true,
      "isArchived": false,
      "status": "COMPLETED",
      "createdAt": 1728340000000,
      "updatedAt": 1728342000000
    }
  ],
  "categories": [
    {
      "id": "cat_dev",
      "name": "Development",
      "colorHex": "#4F46E5",
      "iconName": "code",
      "isSystem": true
    }
  ]
}
```

## 2. Integrity & Restore Validation Rules
1. **Schema Check**: Validates `backupVersion` matches supported schemas (version 1 supported in v1.0.0).
2. **Sanitization**: Checks all UUIDs, URLs, and timestamps.
3. **Merge vs Overwrite**:
   - In Overwrite mode, existing database records are cleared after confirmation.
   - In Merge mode, records with matching URLs or IDs are updated only if the backup timestamp is newer; user notes are never silently deleted.
4. **Error Handling**: Corrupt or malformed files trigger descriptive, non-fatal dialogs explaining the exact line/token issue without affecting existing local data.
