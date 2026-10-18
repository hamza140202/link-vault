import os
import requests

BOT_TOKEN = "8698545201:AAFU8Xe_ScRjdW5a0RjEJrbgOH5FibKmylY"
CHAT_ID = "1263089875"

def send_video(video_path):
    print(f"Uploading launch video {video_path} to Telegram...")
    caption = (
        "🎬 *MomoStack Official Play Store Launch Ad*\n\n"
        "✨ *Features Showcased:*\n"
        "• 1. *Universal Link Feed* (Instagram, YouTube, GitHub, articles)\n"
        "• 2. *Link Intelligence Detail* (Long-press metadata inspection)\n"
        "• 3. *Screenshot Vault & On-Device OCR* (Smart Text Extractor)\n"
        "• 4. *Private Notes Page* (Fast distraction-free notes)\n"
        "• 5. *Full-Page Note Editor* (Checklists & snippets focus mode)\n"
        "• 6. *Smart Categories Vault* (Organize links into topic & app folders)\n\n"
        "⚡ 100% genuine physical device captures in Goldie device frames."
    )
    url = f"https://api.telegram.org/bot{BOT_TOKEN}/sendVideo"
    with open(video_path, "rb") as f:
        files = {"video": ("momostack_launch_ad.mp4", f)}
        data = {"chat_id": CHAT_ID, "caption": caption, "parse_mode": "Markdown"}
        resp = requests.post(url, data=data, files=files, timeout=120)
        print("Video Status:", resp.status_code, resp.json().get("ok"))

def send_screenshots():
    shots = [
        ("store-assets/screenshots/01_home_capture.png", "📱 *1. Universal Link Vault*\nSave anything from Instagram, YouTube, GitHub & web browsers with automated metadata."),
        ("store-assets/screenshots/02_link_detail.png", "📱 *2. Link Intelligence (Long-Press Detail)*\nLong-press any link to inspect automated tags, domain info, and trigger 1-tap offline export."),
        ("store-assets/screenshots/03_screenshot_vault.png", "📱 *3. Smart Screenshot Vault*\nAuto-capture screenshots & transcribe text on-device with zero cloud telemetry."),
        ("store-assets/screenshots/04_notes_page.png", "📱 *4. Private Notes Page*\nInstant personal thoughts, reflections, and ideas with fast search."),
        ("store-assets/screenshots/05_note_editor.png", "📱 *5. Full-Page Note Editor*\nClean focus editor to draft notes, checklists, and snippets with live word counts."),
        ("store-assets/screenshots/06_categories_vault.png", "📱 *6. Smart Categories Vault*\nOrganize links into topic folders for Instagram, GitHub, Twitter & custom collections.")
    ]
    url = f"https://api.telegram.org/bot{BOT_TOKEN}/sendPhoto"
    import time
    for shot, cap in shots:
        if os.path.exists(shot):
            print(f"Uploading: {shot}...")
            for attempt in range(5):
                try:
                    with open(shot, "rb") as f:
                        files = {"photo": f}
                        data = {"chat_id": CHAT_ID, "caption": cap, "parse_mode": "Markdown"}
                        resp = requests.post(url, data=data, files=files, timeout=60)
                        if resp.status_code == 200 and resp.json().get("ok"):
                            print(f"Photo {shot} Status: 200 True")
                            break
                        else:
                            print(f"Photo {shot} attempt {attempt+1} failed: {resp.text}")
                except Exception as e:
                    print(f"Photo {shot} attempt {attempt+1} exception: {e}")
                time.sleep(2)

def main():
    video_path = "store-assets/video/momostack_launch_ad.mp4"
    if os.path.exists(video_path):
        send_video(video_path)
    send_screenshots()
    print("Telegram delivery finished successfully!")

if __name__ == "__main__":
    main()
