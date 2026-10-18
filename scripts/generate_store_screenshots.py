import os
import sys
from PIL import Image, ImageDraw, ImageFont

# Canvas dimensions (Standard Play Store 9:16 phone screenshot: 1080 x 2400)
W, H = 1080, 2400

# Color Palette (MomoStack Brand)
INDIGO = (79, 70, 229)       # #4F46E5
INDIGO_DARK = (49, 46, 129)  # #312E81
SLATE_900 = (15, 23, 42)     # #0F172A
SLATE_800 = (30, 41, 59)     # #1E293B
SLATE_700 = (51, 65, 85)     # #334155
SLATE_400 = (148, 163, 184)  # #94A3B8
SLATE_100 = (241, 245, 249)  # #F1F5F9
WHITE = (255, 255, 255)
AMBER = (234, 179, 8)        # #EAB308
EMERALD = (16, 185, 129)     # #10B981

def get_font(size, bold=False):
    # Try Windows system fonts
    font_paths = [
        "C:\\Windows\\Fonts\\segoeuib.ttf" if bold else "C:\\Windows\\Fonts\\segoeui.ttf",
        "C:\\Windows\\Fonts\\arialbd.ttf" if bold else "C:\\Windows\\Fonts\\arial.ttf",
    ]
    for p in font_paths:
        if os.path.exists(p):
            return ImageFont.truetype(p, size)
    return ImageFont.load_default()

def draw_device_mockup(draw, x, y, w, h, corner=60):
    # Device Outer Frame (Dark bezel)
    draw.rounded_rectangle([x, y, x + w, y + h], radius=corner, fill=SLATE_800, outline=SLATE_700, width=4)
    # Device Screen Canvas
    margin = 16
    screen_rect = [x + margin, y + margin, x + w - margin, y + h - margin]
    draw.rounded_rectangle(screen_rect, radius=corner - 8, fill=SLATE_900)
    # Camera Punch Hole
    cam_x, cam_y = x + w // 2, y + margin + 24
    draw.ellipse([cam_x - 12, cam_y - 12, cam_x + 12, cam_y + 12], fill=(5, 5, 5))
    return screen_rect

def create_screenshot(index, tag, headline, subhead, screen_content_builder, output_path):
    img = Image.new("RGB", (W, H), SLATE_900)
    draw = ImageDraw.Draw(img)

    # 1. Background Ambient Radial Gradient / Aura
    for r in range(600, 0, -20):
        alpha = int(35 * (r / 600))
        color = (INDIGO[0], INDIGO[1], INDIGO[2])
        draw.ellipse([W // 2 - r, 300 - r, W // 2 + r, 300 + r], fill=color)

    # 2. Pill Tag (e.g., "LOCAL-FIRST VAULT")
    font_tag = get_font(28, bold=True)
    tag_w = draw.textlength(tag, font=font_tag)
    tag_x = (W - tag_w) // 2
    draw.rounded_rectangle([tag_x - 24, 120, tag_x + tag_w + 24, 170], radius=25, fill=INDIGO_DARK, outline=INDIGO, width=2)
    draw.text((tag_x, 128), tag, font=font_tag, fill=WHITE)

    # 3. Main Headline
    font_head = get_font(58, bold=True)
    head_w = draw.textlength(headline, font=font_head)
    draw.text(((W - head_w) // 2, 200), headline, font=font_head, fill=WHITE)

    # 4. Subhead
    font_sub = get_font(32, bold=False)
    sub_w = draw.textlength(subhead, font=font_sub)
    draw.text(((W - sub_w) // 2, 280), subhead, font=font_sub, fill=SLATE_400)

    # 5. Device Frame (Occupies bottom area)
    dev_w, dev_h = 860, 1850
    dev_x = (W - dev_w) // 2
    dev_y = 380
    screen_rect = draw_device_mockup(draw, dev_x, dev_y, dev_w, dev_h)

    # 6. Build App Content inside Device Screen
    screen_content_builder(draw, screen_rect[0], screen_rect[1], screen_rect[2], screen_rect[3])

    img.save(output_path, quality=95)
    print(f"Generated: {output_path}")

# ==========================================
# SCREEN 1: Home Stack & Instant Capture
# ==========================================
def build_screen_home(draw, x1, y1, x2, y2):
    # App Bar
    font_logo = get_font(34, bold=True)
    draw.text((x1 + 40, y1 + 60), "🥟 MomoStack", font=font_logo, fill=WHITE)
    draw.text((x2 - 140, y1 + 64), "34 items", font=get_font(24), fill=SLATE_400)

    # Search Bar
    draw.rounded_rectangle([x1 + 30, y1 + 120, x2 - 30, y1 + 190], radius=16, fill=SLATE_800, outline=SLATE_700, width=2)
    draw.text((x1 + 60, y1 + 140), "🔍 Search links, tags, notes...", font=get_font(26), fill=SLATE_400)

    # Category Chips
    chips = [("All", True), ("Reading", False), ("Media", False), ("Work", False), ("Design", False)]
    chip_x = x1 + 30
    for name, active in chips:
        bg = INDIGO if active else SLATE_800
        fg = WHITE if active else SLATE_400
        w = draw.textlength(name, font=get_font(24, bold=active)) + 40
        draw.rounded_rectangle([chip_x, y1 + 220, chip_x + w, y1 + 270], radius=25, fill=bg)
        draw.text((chip_x + 20, y1 + 232), name, font=get_font(24, bold=active), fill=fg)
        chip_x += int(w + 16)

    # Card 1: Article with Thumbnail
    card1_y = y1 + 310
    draw.rounded_rectangle([x1 + 30, card1_y, x2 - 30, card1_y + 260], radius=24, fill=SLATE_800, outline=SLATE_700, width=2)
    # Thumbnail Box
    draw.rounded_rectangle([x1 + 50, card1_y + 30, x1 + 230, card1_y + 230], radius=16, fill=(37, 99, 235))
    draw.text((x1 + 90, card1_y + 110), "🌐 WEB", font=get_font(28, bold=True), fill=WHITE)
    # Content
    draw.text((x1 + 250, card1_y + 35), "github.com · 2h ago", font=get_font(22), fill=SLATE_400)
    draw.text((x1 + 250, card1_y + 70), "DeepSeek-V3 Technical Architecture", font=get_font(28, bold=True), fill=WHITE)
    draw.text((x1 + 250, card1_y + 115), "Comprehensive breakdown of multi-head latent...", font=get_font(22), fill=SLATE_400)
    # Pill & Offline Status
    draw.rounded_rectangle([x1 + 250, card1_y + 165, x1 + 350, card1_y + 205], radius=8, fill=(16, 185, 129, 40))
    draw.text((x1 + 265, card1_y + 172), "Reading", font=get_font(18, bold=True), fill=EMERALD)
    draw.rounded_rectangle([x1 + 365, card1_y + 165, x1 + 490, card1_y + 205], radius=8, fill=INDIGO_DARK)
    draw.text((x1 + 380, card1_y + 172), "📄 PDF Ready", font=get_font(18, bold=True), fill=WHITE)

    # Card 2: Note Card
    card2_y = card1_y + 290
    draw.rounded_rectangle([x1 + 30, card2_y, x2 - 30, card2_y + 220], radius=24, fill=SLATE_800, outline=SLATE_700, width=2)
    draw.text((x1 + 50, card2_y + 30), "📝 App Launch Checklist", font=get_font(30, bold=True), fill=WHITE)
    draw.text((x1 + 50, card2_y + 80), "• Android Keystore + StrongBox enabled\n• On-device OCR benchmark: 82ms\n• Zero analytics verified", font=get_font(22), fill=SLATE_400)

    # Card 3: YouTube Video Card
    card3_y = card2_y + 250
    draw.rounded_rectangle([x1 + 30, card3_y, x2 - 30, card3_y + 260], radius=24, fill=SLATE_800, outline=SLATE_700, width=2)
    draw.rounded_rectangle([x1 + 50, card3_y + 30, x1 + 230, card3_y + 230], radius=16, fill=(220, 38, 38))
    draw.text((x1 + 80, card3_y + 110), "▶ YOUTUBE", font=get_font(24, bold=True), fill=WHITE)
    draw.text((x1 + 250, card3_y + 35), "youtube.com · Yesterday", font=get_font(22), fill=SLATE_400)
    draw.text((x1 + 250, card3_y + 70), "Carmack on Fast Code & Systems", font=get_font(28, bold=True), fill=WHITE)
    draw.text((x1 + 250, card3_y + 115), "Principles of low-latency architecture...", font=get_font(22), fill=SLATE_400)

# ==========================================
# SCREEN 2: 1-Click Offline Webpage PDF Saver
# ==========================================
def build_screen_offline(draw, x1, y1, x2, y2):
    # Reader Top App Bar
    draw.text((x1 + 40, y1 + 50), "← DeepSeek-V3 Technical Architecture", font=get_font(28, bold=True), fill=WHITE)
    draw.rounded_rectangle([x2 - 180, y1 + 45, x2 - 40, y1 + 95], radius=12, fill=EMERALD)
    draw.text((x2 - 160, y1 + 58), "✓ OFFLINE PDF", font=get_font(18, bold=True), fill=WHITE)

    # Rendered Webpage Canvas Simulation (Dark Theme)
    page_y = y1 + 130
    draw.rounded_rectangle([x1 + 30, page_y, x2 - 30, y2 - 40], radius=20, fill=(24, 24, 27), outline=SLATE_700, width=2)

    # Web Header
    draw.text((x1 + 60, page_y + 50), "DEEPSEEK-AI / RESEARCH", font=get_font(22, bold=True), fill=INDIGO)
    draw.text((x1 + 60, page_y + 90), "DeepSeek-V3 Technical Architecture", font=get_font(38, bold=True), fill=WHITE)
    draw.text((x1 + 60, page_y + 150), "Published: Oct 2026 · 14 min read · Full Web Archive", font=get_font(20), fill=SLATE_400)
    draw.line([x1 + 60, page_y + 190, x2 - 60, page_y + 190], fill=SLATE_700, width=2)

    # Code / Graph Block
    draw.rounded_rectangle([x1 + 60, page_y + 220, x2 - 60, page_y + 400], radius=14, fill=(15, 23, 42), outline=INDIGO, width=2)
    draw.text((x1 + 80, page_y + 240), "// Dual-Pipe Parallel Latency Benchmark", font=get_font(20), fill=SLATE_400)
    draw.text((x1 + 80, page_y + 280), "val latency = Benchmark.measureEngine {\n    engine.renderToPdf(pageUrl)\n    stream.flushDiskDirect()\n}", font=get_font(22), fill=EMERALD)

    # Long text simulation
    draw.text((x1 + 60, page_y + 430), "Abstract & Key Innovations", font=get_font(30, bold=True), fill=WHITE)
    body_text = (
        "We introduce DeepSeek-V3, a strong Mixture-of-Experts (MoE) language model\n"
        "with 671B total parameters, of which 37B are activated for each token.\n"
        "To achieve efficient inference and cost-effective training, DeepSeek-V3 adopts\n"
        "Multi-head Latent Attention (MLA) and DeepSeekMoE architecture..."
    )
    draw.text((x1 + 60, page_y + 480), body_text, font=get_font(22), fill=SLATE_100)

# ==========================================
# SCREEN 3: Screenshot Vault with On-Device OCR
# ==========================================
def build_screen_vault(draw, x1, y1, x2, y2):
    draw.text((x1 + 40, y1 + 50), "📸 Screenshot Vault", font=get_font(32, bold=True), fill=WHITE)
    draw.text((x2 - 180, y1 + 58), "On-Device ML Kit", font=get_font(20), fill=INDIGO)

    # Notification Simulation Banner
    notif_y = y1 + 110
    draw.rounded_rectangle([x1 + 30, notif_y, x2 - 30, notif_y + 140], radius=20, fill=SLATE_800, outline=INDIGO, width=2)
    draw.text((x1 + 60, notif_y + 25), "✨ Transcription Completed", font=get_font(26, bold=True), fill=WHITE)
    draw.text((x1 + 60, notif_y + 65), "248 words recognized in 74ms · Tap to view & edit", font=get_font(20), fill=SLATE_400)
    draw.rounded_rectangle([x2 - 190, notif_y + 40, x2 - 50, notif_y + 95], radius=12, fill=INDIGO)
    draw.text((x2 - 170, notif_y + 52), "[Copy Text]", font=get_font(20, bold=True), fill=WHITE)

    # Extracted OCR Detail Card
    detail_y = notif_y + 170
    draw.rounded_rectangle([x1 + 30, detail_y, x2 - 30, y2 - 40], radius=24, fill=SLATE_800, outline=SLATE_700, width=2)
    draw.text((x1 + 60, detail_y + 35), "Captured 18 Oct 2026, 4:35 PM", font=get_font(22), fill=SLATE_400)
    draw.text((x1 + 60, detail_y + 75), "Recognized Text (Editable):", font=get_font(26, bold=True), fill=WHITE)

    ocr_sample = (
        "High-performance local-first caching requires deterministic\n"
        "cache eviction policies combined with streaming disk I/O.\n"
        "By avoiding in-memory array duplication, mobile devices maintain\n"
        "zero UI jank and eliminate OS Low Memory Kills.\n\n"
        "Key takeaways:\n"
        "1. Prefer SQLite WAL mode with 32MB shared memory buffer.\n"
        "2. Offload neural tokenization to background isolates.\n"
        "3. Protect secrets in hardware-backed Android Keystore."
    )
    draw.text((x1 + 60, detail_y + 130), ocr_sample, font=get_font(22), fill=SLATE_100)

# ==========================================
# SCREEN 4: Rich Notes & Smart Paste
# ==========================================
def build_screen_notes(draw, x1, y1, x2, y2):
    draw.text((x1 + 40, y1 + 50), "← Note Editor", font=get_font(30, bold=True), fill=WHITE)
    # Mascot Circle
    draw.ellipse([x2 - 130, y1 + 35, x2 - 60, y1 + 105], fill=INDIGO)
    draw.text((x2 - 110, y1 + 50), "🥟", font=get_font(32), fill=WHITE)

    # Note Content Editor
    editor_y = y1 + 130
    draw.rounded_rectangle([x1 + 30, editor_y, x2 - 30, y2 - 40], radius=24, fill=SLATE_800, outline=SLATE_700, width=2)
    draw.text((x1 + 60, editor_y + 40), "Product Launch & Security Blueprint", font=get_font(34, bold=True), fill=WHITE)
    draw.text((x1 + 60, editor_y + 90), "412 words · 2,840 chars · Last edited 2m ago", font=get_font(20), fill=SLATE_400)
    draw.line([x1 + 60, editor_y + 130, x2 - 60, editor_y + 130], fill=SLATE_700, width=2)

    notes_body = (
        "## Core Principles\n\n"
        "• Local-First: The user's device is the primary source of truth.\n"
        "• Anti-Modding: Active signature verification prevents tampering.\n"
        "• Sub-50ms Share: Instant capture from any external browser.\n\n"
        "## Architectural Verification\n\n"
        "1. ML Kit On-Device OCR executes with zero internet connectivity.\n"
        "2. Webpage PDF printer preserves CSS color adjustment & themes.\n"
        "3. Single-file .momostackbackup restores complete user state."
    )
    draw.text((x1 + 60, editor_y + 160), notes_body, font=get_font(24), fill=SLATE_100)

# Run Generation
screens = [
    (1, "LOCAL-FIRST BOOKMARKING", "Instant 50ms Capture", "Save links and rich notes from any browser silently.", build_screen_home, "store-assets/screenshots/01_home_capture.png"),
    (2, "OFFLINE WEB ARCHIVE", "1-Tap Webpage to PDF", "Read entire formatted websites offline with dark theme fidelity.", build_screen_offline, "store-assets/screenshots/02_offline_pdf.png"),
    (3, "ON-DEVICE OCR ENGINE", "Screenshot Vault", "Recognize and copy text from screenshots with zero cloud telemetry.", build_screen_vault, "store-assets/screenshots/03_screenshot_vault.png"),
    (4, "DISTRACTION-FREE NOTES", "Smart Formatted Paste", "Preserve bold, lists, and links copied from the web seamlessly.", build_screen_notes, "store-assets/screenshots/04_notes_editor.png"),
]

for idx, tag, h, s, builder, out in screens:
    create_screenshot(idx, tag, h, s, builder, out)
