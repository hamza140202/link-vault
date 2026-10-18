import os
import sys
from PIL import Image, ImageDraw, ImageFont, ImageFilter

# Canvas dimensions (Standard Play Store 9:16 phone screenshot: 1080 x 2400)
W, H = 1080, 2400

# Color Palette (MomoStack Brand)
INDIGO = (99, 102, 241)       # #6366F1
INDIGO_DARK = (49, 46, 129)  # #312E81
SLATE_950 = (10, 15, 29)     # Deep dark
SLATE_900 = (15, 23, 42)     # #0F172A
SLATE_800 = (30, 41, 59)     # #1E293B
SLATE_700 = (51, 65, 85)     # #334155
SLATE_400 = (148, 163, 184)  # #94A3B8
WHITE = (255, 255, 255)

def get_font(size, bold=False):
    font_paths = [
        "C:\\Windows\\Fonts\\segoeuib.ttf" if bold else "C:\\Windows\\Fonts\\segoeui.ttf",
        "C:\\Windows\\Fonts\\arialbd.ttf" if bold else "C:\\Windows\\Fonts\\arial.ttf",
    ]
    for p in font_paths:
        if os.path.exists(p):
            try:
                return ImageFont.truetype(p, size)
            except Exception:
                pass
    return ImageFont.load_default()

def create_framed_screenshot(real_image_path, tag, headline, subhead, output_path):
    if not os.path.exists(real_image_path):
        print(f"Warning: {real_image_path} does not exist!")
        return

    # Base Canvas
    img = Image.new("RGB", (W, H), SLATE_950)
    draw = ImageDraw.Draw(img)

    # 1. Subtle glowing ambient background
    for r in range(500, 0, -25):
        alpha_factor = (r / 500)
        c = (int(30 + 35 * alpha_factor), int(35 + 40 * alpha_factor), int(70 + 70 * alpha_factor))
        draw.ellipse([W // 2 - r, 320 - r, W // 2 + r, 320 + r], fill=c)

    # 2. Category Tag Pill
    font_tag = get_font(26, bold=True)
    tag_w = draw.textlength(tag, font=font_tag)
    tag_x = (W - tag_w) // 2
    draw.rounded_rectangle([tag_x - 28, 110, tag_x + tag_w + 28, 165], radius=28, fill=INDIGO_DARK, outline=INDIGO, width=2)
    draw.text((tag_x, 122), tag, font=font_tag, fill=WHITE)

    # 3. Main Headline
    font_head = get_font(56, bold=True)
    head_w = draw.textlength(headline, font=font_head)
    draw.text(((W - head_w) // 2, 195), headline, font=font_head, fill=WHITE)

    # 4. Subhead
    font_sub = get_font(30, bold=False)
    sub_w = draw.textlength(subhead, font=font_sub)
    draw.text(((W - sub_w) // 2, 275), subhead, font=font_sub, fill=SLATE_400)

    # 5. Device Frame Setup
    # Phone screen width: 880, aspect ratio approx 1080x2400 (h = 880 * 2400 / 1080 = 1955)
    dev_w = 880
    dev_h = 1960
    dev_x = (W - dev_w) // 2
    dev_y = 380
    bezel = 14
    corner = 58

    # Phone drop shadow
    shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    sdraw = ImageDraw.Draw(shadow)
    sdraw.rounded_rectangle([dev_x - 10, dev_y + 15, dev_x + dev_w + 10, dev_y + dev_h + 30], radius=corner + 10, fill=(0, 0, 0, 180))
    shadow = shadow.filter(ImageFilter.GaussianBlur(25))
    img.paste(shadow, (0, 0), shadow)
    draw = ImageDraw.Draw(img)

    # Device Outer Frame (Matte metallic bezel)
    draw.rounded_rectangle([dev_x, dev_y, dev_x + dev_w, dev_y + dev_h], radius=corner, fill=(28, 32, 45), outline=(70, 78, 105), width=3)

    # Inner Screen Rect
    scr_x = dev_x + bezel
    scr_y = dev_y + bezel
    scr_w = dev_w - (bezel * 2)
    scr_h = dev_h - (bezel * 2)
    scr_corner = corner - 10

    # Load and resize real screenshot
    real_img = Image.open(real_image_path).convert("RGBA")
    real_resized = real_img.resize((scr_w, scr_h), Image.Resampling.LANCZOS)

    # Create rounded corner mask for the screen
    mask = Image.new("L", (scr_w, scr_h), 0)
    mask_draw = ImageDraw.Draw(mask)
    mask_draw.rounded_rectangle([0, 0, scr_w, scr_h], radius=scr_corner, fill=255)

    # Paste real screenshot
    img.paste(real_resized, (scr_x, scr_y), mask)

    # Redraw frame border over edges for crisp seam
    draw = ImageDraw.Draw(img)
    draw.rounded_rectangle([dev_x, dev_y, dev_x + dev_w, dev_y + dev_h], radius=corner, fill=None, outline=(60, 68, 90), width=3)

    # Front Camera Punch Hole
    cam_x = dev_x + dev_w // 2
    cam_y = scr_y + 36
    draw.ellipse([cam_x - 13, cam_y - 13, cam_x + 13, cam_y + 13], fill=(8, 8, 12), outline=(30, 30, 40), width=2)
    draw.ellipse([cam_x - 5, cam_y - 5, cam_x + 2, cam_y + 2], fill=(20, 25, 45)) # Lens glint

    # Speaker Grill at top bezel
    draw.rounded_rectangle([cam_x - 45, dev_y + 5, cam_x + 45, dev_y + 9], radius=2, fill=(40, 45, 60))

    # Save
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    img.save(output_path, quality=95)
    print(f"Generated Goldie Play Store Asset: {output_path}")

def main():
    captures_dir = r"D:\ansaribilal\link-vault\store-assets\real_captures"
    out_dir = r"D:\ansaribilal\link-vault\store-assets\screenshots"
    os.makedirs(out_dir, exist_ok=True)

    configs = [
        {
            "src": os.path.join(captures_dir, "01_real_home.png"),
            "tag": "ALL-IN-ONE VAULT",
            "head": "Save Anything. Find Fast.",
            "sub": "Organize links, notes & media in your private vault",
            "out": os.path.join(out_dir, "01_home_capture.png")
        },
        {
            "src": os.path.join(captures_dir, "06_real_reader_mode.png"),
            "tag": "OFFLINE ARCHIVAL",
            "head": "Read Anywhere. Zero Ads.",
            "sub": "Full-text reader view and offline document archives",
            "out": os.path.join(out_dir, "02_offline_pdf.png")
        },
        {
            "src": os.path.join(captures_dir, "04b_real_editor_typed.png"),
            "tag": "PRIVATE NOTES",
            "head": "Distraction-Free Notes",
            "sub": "Rich markdown editor with your companion Momo watching",
            "out": os.path.join(out_dir, "03_notes_editor.png")
        },
        {
            "src": os.path.join(captures_dir, "02_real_categories.png"),
            "tag": "SMART TAGS & STACKS",
            "head": "Intelligent Organization",
            "sub": "Filter by tags, domains, and content types instantly",
            "out": os.path.join(out_dir, "04_categories_vault.png")
        },
        {
            "src": os.path.join(captures_dir, "05_real_settings.png"),
            "tag": "ZERO TELEMETRY",
            "head": "100% Private & Local",
            "sub": "Your data never leaves your device. SQLite on-device only.",
            "out": os.path.join(out_dir, "05_settings_privacy.png")
        }
    ]

    for c in configs:
        create_framed_screenshot(c["src"], c["tag"], c["head"], c["sub"], c["out"])

if __name__ == "__main__":
    main()
