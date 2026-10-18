import os
import requests

BOT_TOKEN = "8698545201:AAFU8Xe_ScRjdW5a0RjEJrbgOH5FibKmylY"
CHAT_ID = "1263089875"

def send_video(video_path):
    print(f"Uploading video {video_path} to Telegram...")
    caption = (
        "🎬 *MomoStack Official Play Store Launch Ad*\n\n"
        "✨ *Highlights:*\n"
        "• 100% captured live from physical device (moto g85 5G)\n"
        "• Framed in Goldie-style high-res device bezels\n"
        "• Brag audio track integration\n"
        "• Real app feed with saved Instagram, YouTube, and offline PDF cards"
    )
    url = f"https://api.telegram.org/bot{BOT_TOKEN}/sendVideo"
    with open(video_path, "rb") as f:
        files = {"video": ("momostack_launch_ad.mp4", f)}
        data = {"chat_id": CHAT_ID, "caption": caption, "parse_mode": "Markdown"}
        resp = requests.post(url, data=data, files=files, timeout=120)
        print("Video Status:", resp.status_code, resp.json().get("ok"))

def send_screenshots():
    shots = [
        "store-assets/screenshots/01_home_capture.png",
        "store-assets/screenshots/02_offline_pdf.png",
        "store-assets/screenshots/03_notes_editor.png",
        "store-assets/screenshots/04_categories_vault.png",
        "store-assets/screenshots/05_settings_privacy.png"
    ]
    url = f"https://api.telegram.org/bot{BOT_TOKEN}/sendPhoto"
    captions = [
        "📱 *Screenshot 1: Main Feed & Instant Capture*\nReal link cards, tags, and offline status indicators.",
        "📱 *Screenshot 2: Offline Archival*\nDistraction-free offline reader view and web archives.",
        "📱 *Screenshot 3: Private Notes & Momo Mascot*\nRich markdown editor with Momo mascot companionship.",
        "📱 *Screenshot 4: Intelligent Categories*\nFilter by domains, tags, and media types in milliseconds.",
        "📱 *Screenshot 5: Local-First Privacy & Settings*\nZero telemetry, on-device SQLite database, custom themes."
    ]
    for shot, cap in zip(shots, captions):
        if os.path.exists(shot):
            print(f"Uploading screenshot: {shot}...")
            with open(shot, "rb") as f:
                files = {"photo": f}
                data = {"chat_id": CHAT_ID, "caption": cap, "parse_mode": "Markdown"}
                resp = requests.post(url, data=data, files=files, timeout=60)
                print("Photo Status:", resp.status_code, resp.json().get("ok"))

def main():
    video_path = "store-assets/video/momostack_launch_ad.mp4"
    if os.path.exists(video_path):
        send_video(video_path)
    send_screenshots()
    print("Telegram asset delivery complete!")

if __name__ == "__main__":
    main()
