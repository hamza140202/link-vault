import os
import subprocess

SCREENSHOTS = [
    "store-assets/screenshots/01_home_capture.png",
    "store-assets/screenshots/02_offline_pdf.png",
    "store-assets/screenshots/03_notes_editor.png",
    "store-assets/screenshots/04_categories_vault.png",
    "store-assets/screenshots/05_settings_privacy.png",
]
MUSIC_TRACK = ".agents/skills/brag/assets/music/happy-beats-business-moves-vol-10-by-ende-dot-app.mp3"
OUT_VIDEO = "store-assets/video/momostack_launch_ad.mp4"
os.makedirs("store-assets/video", exist_ok=True)

# 25-second video (5 seconds per screen)
concat_file = "store-assets/video/concat.txt"
with open(concat_file, "w") as f:
    for s in SCREENSHOTS:
        abs_s = os.path.abspath(s).replace("\\", "/")
        f.write(f"file '{abs_s}'\n")
        f.write("duration 5\n")
    # Concat demuxer requirement: repeat last image once without duration
    f.write(f"file '{os.path.abspath(SCREENSHOTS[-1]).replace(chr(92), '/')}'\n")

print(f"Concat list written with {len(SCREENSHOTS)} real screenshots.")

# FFmpeg Command:
# 1080x1920 (9:16 vertical video standard for mobile / Play Store trailer / shorts)
cmd = [
    "ffmpeg", "-y",
    "-f", "concat", "-safe", "0", "-i", concat_file,
    "-i", MUSIC_TRACK,
    "-vf", "scale=1080:1920:force_original_aspect_ratio=decrease,pad=1080:1920:(ow-iw)/2:(oh-ih)/2,format=yuv420p",
    "-t", "25",
    "-af", "afade=t=out:st=23:d=2",
    "-c:v", "libx264", "-preset", "medium", "-crf", "18",
    "-c:a", "aac", "-b:a", "192k",
    OUT_VIDEO
]

print("Rendering launch video with FFmpeg...")
res = subprocess.run(cmd, capture_output=True, text=True)
if res.returncode == 0:
    print(f"SUCCESS: Rendered {OUT_VIDEO}")
    file_size_mb = os.path.getsize(OUT_VIDEO) / (1024 * 1024)
    print(f"Video File Size: {file_size_mb:.2f} MB")
else:
    print("FFmpeg Error:", res.stderr)
