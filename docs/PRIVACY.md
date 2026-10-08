# Visual Research & Quality Benchmarks — LinkVault

## 1. Quality Foundations
To avoid AI-slop (excessive decorative gradients, floating blur pills, generic dashboard cards), LinkVault incorporates design lessons from premier Android and Read-Later software (Pocket, Obsidian, Raindrop, Material 3 Expressive):

- **Information Density**: Scannable titles, clear domains, subtle relative timestamps ("2h ago", "Yesterday").
- **Contrast & Legibility**: Meets WCAG AAA guidelines for text on solid surfaces.
- **Micro-Interactions**: Predictable, 150-200ms ease-out transitions for card presses, bookmark toggling, and dialog appearances.
- **Empty & Error States**: Every empty state explicitly teaches the user how to fill it (e.g. "Share any link from Chrome or tap + to capture your first item").

# Privacy Policy & Safeguards — LinkVault

## 1. Zero Cloud Dependency
- 100% of links, notes, and metadata reside exclusively on the device's private SQLite database.
- No analytics trackers, no telemetry SDKs, no ad networks.
- URL enrichment connects directly to the target URL's server; no third-party proxy or metadata broker is queried.
- Clipboard content is processed entirely in memory on the device and is never transmitted over the network.
