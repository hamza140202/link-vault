import subprocess
import time
import os
import sys
import xml.etree.ElementTree as ET

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
if hasattr(sys.stderr, 'reconfigure'):
    sys.stderr.reconfigure(encoding='utf-8', errors='replace')

ADB = r"D:\platform-tools\adb.exe"
DEVICE = "10.107.18.77:38193"
OUTPUT_DIR = r"D:\ansaribilal\link-vault\store-assets\real_captures"
os.makedirs(OUTPUT_DIR, exist_ok=True)

def adb(args):
    cmd = [ADB, "-s", DEVICE] + args
    res = subprocess.run(cmd, capture_output=True, text=True)
    return res.stdout.strip()

def adb_shell(cmd_str):
    return adb(["shell", cmd_str])

def connect():
    subprocess.run([ADB, "connect", DEVICE], capture_output=True)
    time.sleep(1)

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
    # format: [x1,y1][x2,y2]
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
    print("=== Starting Autonomous Screen Capture ===")
    connect()
    
    # 1. Orientation & notifications
    adb_shell("settings put system accelerometer_rotation 0; settings put system user_rotation 0; cmd statusbar collapse")
    time.sleep(1)
    
    # 2. Launch MomoStack
    adb_shell("am start -n com.momostack.app/.ui.MainActivity")
    time.sleep(3)
    
    # 3. Capture Screen 1: Home / Stacks
    print("\n--- Screen 1: Home Stack ---")
    capture("01_real_home.png")
    
    # 4. Check UI elements
    nodes = dump_ui()
    print(f"Found {len(nodes)} UI nodes on home screen")
    for n in nodes:
        if n["text"] or n["desc"]:
            print(f"Node: text='{n['text']}', desc='{n['desc']}', bounds={n['bounds']}")
            
    # 5. Navigate to Notes tab
    print("\n--- Screen 2: Notes Tab ---")
    clicked = click_element("Notes")
    if not clicked:
        # Fallback to bottom nav tab 3 (approx x=675, y=2280 on 1080x2400)
        print("Tapping Notes bottom nav coordinate (675, 2280)...")
        adb_shell("input tap 675 2280")
        time.sleep(2)
    capture("02_real_notes.png")
    
    # 6. Open Note Editor or tap FAB to compose note
    print("\n--- Screen 3: Note Editor with Momo Mascot ---")
    clicked_fab = click_element("Add note") or click_element("Create note") or click_element("New note")
    if not clicked_fab:
        # Try tapping FAB bottom right or the first note card
        print("Tapping FAB or note card area...")
        adb_shell("input tap 940 2100")
        time.sleep(1.5)
    capture("03_real_editor.png")
    
    # Type some sample note title/body if in editor
    adb_shell("input text 'Deep%sWork%sChecklist'")
    time.sleep(1)
    capture("03b_real_editor_typing.png")
    
    # Press Back to return
    adb_shell("input keyevent 4")
    time.sleep(1.5)
    
    # 7. Navigate to Categories / Vault
    print("\n--- Screen 4: Categories Tab ---")
    clicked_cat = click_element("Categories") or click_element("Tags")
    if not clicked_cat:
        adb_shell("input tap 405 2280")
        time.sleep(2)
    capture("04_real_categories.png")
    
    # 8. Navigate to Settings
    print("\n--- Screen 5: Settings Tab ---")
    clicked_set = click_element("Settings")
    if not clicked_set:
        adb_shell("input tap 945 2280")
        time.sleep(2)
    capture("05_real_settings.png")
    
    # Return to Home tab
    print("\n--- Return to Home & Open Link Reader ---")
    click_element("Links") or click_element("Stacks") or adb_shell("input tap 135 2280")
    time.sleep(2)
    
    # Tap the first card in the stack to open Reader / Web preview
    print("Tapping first card in the stack to open Offline Reader...")
    adb_shell("input tap 540 600")
    time.sleep(3)
    capture("06_real_reader.png")
    
    # Return to home
    adb_shell("input keyevent 4")
    
    print("\n=== Screen Capture Sequence Complete! ===")

if __name__ == "__main__":
    main()
