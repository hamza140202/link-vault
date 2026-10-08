# URL Enrichment Specification — LinkVault

## 1. Network Constraints
- Safe OkHttp client with:
  - 10-second connect timeout
  - 10-second read timeout
  - Follow redirects (up to 5 hops)
  - Stream capped at 2MB to prevent memory exhaustion on giant media downloads.
- Clean User-Agent header indicating non-malicious client.

## 2. Extraction Precedence
1. **Title**:
   - `meta[property=og:title]`
   - `meta[name=twitter:title]`
   - `<title>` HTML tag
   - Fallback: Host domain or URL path
2. **Description**:
   - `meta[property=og:description]`
   - `meta[name=description]`
   - `meta[name=twitter:description]`
   - Fallback: Null
3. **Favicon**:
   - `link[rel=apple-touch-icon]`
   - `link[rel="shortcut icon"]`
   - `link[rel=icon]`
   - Default fallback: `https://<domain>/favicon.ico`
4. **Preview Image**:
   - `meta[property=og:image]`
   - `meta[name=twitter:image]`

## 3. Failure Resilience
- If domain is offline or returns 4xx/5xx HTTP error, the record remains safely stored with the URL, host domain as title, and status `FAILED_METADATA`.
- Capture never fails because enrichment was unreachable.
