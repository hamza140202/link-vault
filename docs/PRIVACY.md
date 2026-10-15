# Visual Research & Quality Benchmarks — MomoStack

## 1. Quality Foundations
To avoid AI-slop (excessive decorative gradients, floating blur pills, generic dashboard cards), MomoStack incorporates design lessons from premier Android and Read-Later software (Pocket, Obsidian, Raindrop, Material 3 Expressive):

- **Information Density**: Scannable titles, clear domains, subtle relative timestamps ("2h ago", "Yesterday").
- **Contrast & Legibility**: Meets WCAG AAA guidelines for text on solid surfaces.
- **Micro-Interactions**: Predictable, 150-200ms ease-out transitions for card presses, bookmark toggling, and dialog appearances.
- **Empty & Error States**: Every empty state explicitly teaches the user how to fill it (e.g. "Share any link from Chrome or tap + to capture your first item").

---

# Privacy Policy & Safeguards — MomoStack

## 1. Zero Cloud Dependency
- 100% of links, notes, and metadata reside exclusively on the device's private SQLite database.
- No analytics trackers, no telemetry SDKs, no ad networks.
- URL enrichment connects directly to the target URL's server; no third-party proxy or metadata broker is queried.
- Clipboard content is processed entirely in memory on the device and is never transmitted over the network.

## 2. On-Device OCR & Media Privacy
- **100% Offline Optical Character Recognition**: Text transcription is powered entirely on-device by Google ML Kit's bundled vision models. No image pixels, cropped regions, or recognized text strings ever leave the device.
- **Private App Sandbox**: Captured screenshots are stored strictly within the app's internal private storage directory (`/data/data/com.momostack.app/files/screenshots/`). Other apps and cloud photo backups cannot access or index these files unless explicitly exported by the user.
- **Opt-in Foreground Service**: The `ScreenshotWatcherService` only runs when toggled on by the user and can be deactivated at any time with a single tap in Settings.
