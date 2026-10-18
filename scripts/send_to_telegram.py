import os
import sys
import requests

BOT_TOKEN = "8698545201:AAFU8Xe_ScRjdW5a0RjEJrbgOH5FibKmylY"
CHAT_ID = "1263089875"

def main():
    version = sys.argv[1] if len(sys.argv) > 1 else "v1.0.14"
    apk_path = "release-assets/momostack-release.apk"
    if not os.path.exists(apk_path):
        # Fallback to local build path if testing locally
        apk_path = "app/build/outputs/apk/release/app-release-unsigned.apk"
    
    if not os.path.exists(apk_path):
        print(f"Error: APK not found at {apk_path}")
        sys.exit(1)
        
    file_size_mb = os.path.getsize(apk_path) / (1024 * 1024)
    print(f"Uploading {apk_path} ({file_size_mb:.2f} MB) to Telegram chat {CHAT_ID}...")

    caption = (
        f"🚀 *MomoStack {version} Release*\n\n"
        "✨ *Updates:*\n"
        "• *Link Share Intercept Fix*: Links shared from Chrome or web browsers save directly to the main stack (no accidental screenshot vault routing).\n"
        "• *1-Click Copy Note*: One-tap copy button directly on note cards and note editor to copy whole note with title.\n"
        "• *Dynamic Note Header*: Editor header displays the actual note title with clean ellipsis instead of static 'Edit Note'.\n"
        "• *1-Click Offline Webpage PDF Saver*: Download icon on link cards archives entire webpages in background with notifications & in-app reader.\n"
        "• *Pixel-Perfect PDF Styling*: Injected print CSS preserves colors, backgrounds, cards, and dark theme matching the live webpage."
    )

    url = f"https://api.telegram.org/bot{BOT_TOKEN}/sendDocument"
    with open(apk_path, "rb") as f:
        files = {"document": (f"momostack-{version}.apk", f)}
        data = {"chat_id": CHAT_ID, "caption": caption, "parse_mode": "Markdown"}
        resp = requests.post(url, data=data, files=files, timeout=180)
        print("Status Code:", resp.status_code)
        print("Response:", resp.text)
        if resp.status_code != 200:
            sys.exit(1)

if __name__ == "__main__":
    main()
