import subprocess
import time
import os
import sys
import xml.etree.ElementTree as ET

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')

ADB = r"D:\platform-tools\adb.exe"
DEVICE = "10.107.18.77:38193"
OUTPUT_DIR = r"D:\ansaribilal\link-vault\store-assets\real_captures"
os.makedirs(OUTPUT_DIR, exist_ok=True)

def adb_shell(cmd_str):
    cmd = [ADB, "-s", DEVICE, "shell", cmd_str]
    res = subprocess.run(cmd, capture_output=True, text=True)
    return res.stdout.strip()

def capture(filename):
    remote = f"/sdcard/{filename}"
    local = os.path.join(OUTPUT_DIR, filename)
    adb_shell(f"screencap -p {remote}")
    subprocess.run([ADB, "-s", DEVICE, "pull", remote, local], capture_output=True)
    print(f"Captured: {local} ({os.path.getsize(local)} bytes)")
    return local

def dump_ui():
    adb_shell("uiautomator dump /sdcard/dump.xml")
    subprocess.run([ADB, "-s", DEVICE, "pull", "/sdcard/dump.xml", "dump.xml"], capture_output=True)
    nodes = []
    if os.path.exists("dump.xml"):
        try:
            tree = ET.parse("dump.xml")
            for node in tree.iter("node"):
                text = node.attrib.get("text", "")
                desc = node.attrib.get("content-desc", "")
                bounds = node.attrib.get("bounds", "")
                nodes.append({"text": text, "desc": desc, "bounds": bounds})
        except Exception as e:
            print("dump parse error:", e)
    return nodes

def parse_bounds(bounds_str):
    import re
    m = re.findall(r"\[(\d+),(\d+)\]", bounds_str)
    if len(m) == 2:
        x1, y1 = int(m[0][0]), int(m[0][1])
        x2, y2 = int(m[1][0]), int(m[1][1])
        return (x1 + x2) // 2, (y1 + y2) // 2
    return None

def click_element(text_or_desc):
    nodes = dump_ui()
    for n in nodes:
        if text_or_desc.lower() in n["text"].lower() or text_or_desc.lower() in n["desc"].lower():
            pt = parse_bounds(n["bounds"])
            if pt:
                print(f"Clicking '{text_or_desc}' at {pt}")
                adb_shell(f"input tap {pt[0]} {pt[1]}")
                time.sleep(1.5)
                return True
    return False

def main():
    subprocess.run([ADB, "connect", DEVICE], capture_output=True)
    time.sleep(1)
    
    # 0. Orient to portrait and ensure app is foreground
    adb_shell("settings put system user_rotation 0; cmd statusbar collapse")
    adb_shell("am start -n com.momostack.app/.ui.MainActivity")
    time.sleep(2)
    
    # Reset back to root if inside any subscreen
    for _ in range(3):
        adb_shell("input keyevent 4")
        time.sleep(0.5)
    adb_shell("am start -n com.momostack.app/.ui.MainActivity")
    time.sleep(2)

    # ========================================================
    # 1. SCREEN 1: HOME STACK / UNIVERSAL CAPTURE FEED
    # ========================================================
    print("\n--- 1. Capturing Home Stack ---")
    click_element("Home") or adb_shell("input tap 115 2280")
    time.sleep(2)
    capture("01_real_home.png")

    # ========================================================
    # 2. SCREEN 2: LONG CLICK A LINK TO OPEN DETAIL SCREEN
    # ========================================================
    print("\n--- 2. Long-clicking a link card for DetailScreen ---")
    # Card is around (540, 850). Long press via swipe:
    adb_shell("input swipe 540 850 540 850 1200")
    time.sleep(2.5)
    capture("02_real_link_detail.png")
    
    # Print nodes to verify DetailScreen is open
    nodes = dump_ui()
    detail_open = any("detail" in n["text"].lower() or "edit" in n["text"].lower() or "notes" in n["text"].lower() for n in nodes)
    print(f"Detail screen open verification: {detail_open}")
    
    # Return to Home
    adb_shell("input keyevent 4")
    time.sleep(1.5)

    # ========================================================
    # 3. SCREEN 3: SCREENSHOT VAULT
    # ========================================================
    print("\n--- 3. Opening Screenshot Vault ---")
    click_element("Settings") or adb_shell("input tap 964 2280")
    time.sleep(2)
    
    # Click "Screenshot Vault" in settings
    vault_clicked = click_element("Screenshot Vault")
    if not vault_clicked:
        print("Looking for Screenshot Vault in UI nodes...")
        for n in dump_ui():
            if "screenshot" in n["text"].lower() or "vault" in n["text"].lower():
                print(f"Found candidate: {n}")
                pt = parse_bounds(n["bounds"])
                if pt:
                    adb_shell(f"input tap {pt[0]} {pt[1]}")
                    time.sleep(2)
                    vault_clicked = True
                    break
    
    capture("03_real_screenshot_vault.png")
    # Return to settings root
    adb_shell("input keyevent 4")
    time.sleep(1.5)

    # ========================================================
    # 4. SCREEN 4: NOTES PAGE
    # ========================================================
    print("\n--- 4. Opening Notes Page ---")
    # Tap Notes bottom nav (bounds approx 710 to 795, y=2280)
    click_element("Notes") or adb_shell("input tap 752 2280")
    time.sleep(2)
    capture("04_real_notes_page.png")

    # ========================================================
    # 5. SCREEN 5: NOTE EDITOR (INSIDE NOTE)
    # ========================================================
    print("\n--- 5. Opening Note Editor Inside Note ---")
    # On notes screen, either tap an existing note card or tap the FAB to open the editor
    # Let's inspect nodes on notes page
    nodes = dump_ui()
    editor_opened = False
    for n in nodes:
        # Check if there is an existing note or card
        if len(n["text"]) > 10 and "search" not in n["text"].lower():
            pt = parse_bounds(n["bounds"])
            if pt and pt[1] < 2000:
                print(f"Tapping note card '{n['text'][:20]}...' at {pt}")
                adb_shell(f"input tap {pt[0]} {pt[1]}")
                time.sleep(2)
                editor_opened = True
                break
    
    if not editor_opened:
        # Tap FAB bottom right
        print("Tapping Notes FAB...")
        adb_shell("input tap 940 2050")
        time.sleep(2)
        
    capture("05_real_note_editor.png")
    
    # Return back
    adb_shell("input keyevent 4")
    time.sleep(1)

    print("\n=== All 5 Real Perfection Screens Captured! ===")

if __name__ == "__main__":
    main()
