# Design System Specification — LinkVault (Anti-AI-Slop)

## 1. Design Tokens & Visual Principles
- **No Neon Gradients**: Solid, high-contrast Indigo `#4F46E5` primary color.
- **No Decorative Floating Blobs**: Clean surfaces following Material 3 guidelines.
- **No Emojis as UI Icons**: Professional vector icons (Material Symbols / Lucide-style vectors).
- **Tabular Figures**: Every numeric counter (word counts, item numbers, backup timestamps) utilizes tabular figures (`FontFeatureSettings = "tnum"`).
- **Semantic Color Tokens**:
  - `Primary`: `#4F46E5` (Indigo 600)
  - `OnPrimary`: `#FFFFFF`
  - `PrimaryContainer`: `#EEF2FF` (Indigo 50)
  - `OnPrimaryContainer`: `#312E81` (Indigo 900)
  - `Surface`: Light: `#F8FAFC` (Slate 50) / Dark: `#0F172A` (Slate 900)
  - `SurfaceVariant`: Light: `#F1F5F9` (Slate 100) / Dark: `#1E293B` (Slate 800)
  - `Outline`: Light: `#E2E8F0` (Slate 200) / Dark: `#334155` (Slate 700)
  - `TextPrimary`: Light: `#0F172A` (Slate 900) / Dark: `#F8FAFC` (Slate 50)
  - `TextSecondary`: Light: `#64748B` (Slate 500) / Dark: `#94A3B8` (Slate 400)

## 2. Corner Radii & Elevation
- Cards: `12dp` rounded corners, `1dp` outline stroke with zero or subtle tactile shadow (elevation 1dp).
- Chips: `8dp` rounded corners, muted backgrounds.
- Dialogs / Sheets: `20dp` top radii.
- Minimum touch target: `48dp` on all interactive touch elements.

## 3. Typography
- Hierarchy:
  - Headline Small: 24sp, SemiBold
  - Title Medium: 16sp, SemiBold
  - Body Medium: 14sp, Regular
  - Label Medium: 12sp, Medium
