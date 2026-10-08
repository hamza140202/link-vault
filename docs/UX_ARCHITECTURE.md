# UX Architecture & Screen Inventory — LinkVault

## 1. Information Architecture
The application is organized into 4 primary views accessible via the bottom Navigation Bar, plus detailed inspection and search surfaces:

```
┌────────────────────────────────────────────────────────┐
│                   Top Search & Filter Bar              │
├────────────────────────────────────────────────────────┤
│                                                        │
│                    Main Content Area                   │
│                                                        │
│  [1. Library]     [2. Notes]     [3. Vault/Tags] [4. Settings] │
│   All Links        Standalone &   Categories &    Backup/JSON  │
│   Favorites        Linked Notes   Taxonomy        Stats/Theme  │
│   Archive                                                      │
├────────────────────────────────────────────────────────┤
│           Bottom Navigation Bar (4 destinations)       │
└────────────────────────────────────────────────────────┘
```

## 2. Screen Inventory & Flow
1. **Share Capture Activity**:
   - Headless / translucent overlay.
   - Saves within 50ms, notifies with Toast, exits.
2. **Library Screen (Home)**:
   - Filter chips: All, Favorites, Archive, Processing.
   - Feed of enriched Link Cards.
   - Swiping / context menu: Favorite, Share, Copy Clean Link, Edit Note, Archive, Delete.
3. **Detail & Note Screen**:
   - Top: Clean header with title, domain badge, source URL, open in browser.
   - Middle: Automated Metadata preview (description, preview image).
   - Bottom: Dedicated "Personal Notes" block with Markdown support, edit toggle, and instant Save.
4. **Notes Screen (First-Class)**:
   - Direct note composition.
   - Dedicated **Clipboard Action Button**: Pastes full clipboard content, supports multi-megabyte payloads, Unicode, preserve line breaks. Shows real-time character and word count in tabular digits.
5. **Categories / Taxonomy Screen**:
   - Manage categories: Development, Reading, Entertainment, Shopping, Finance, Work, Uncategorized.
   - Custom category creation with color picker.
6. **Settings & Backup Screen**:
   - Database health metrics (total items, storage used).
   - Real JSON Backup Export with file share.
   - Real JSON Backup Restore with schema validation and merge options.
   - Dark/Light Theme toggle.
