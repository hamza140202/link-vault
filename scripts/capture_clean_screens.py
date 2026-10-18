import subprocess
import time
import os
import sys

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

def main():
    subprocess.run([ADB, "connect", DEVICE], capture_output=True)
    time.sleep(1)
    
    # Reset to home
    adb_shell("settings put system user_rotation 0; cmd statusbar collapse")
    adb_shell("am start -n com.momostack.app/.ui.MainActivity")
    time.sleep(2)
    
    # 1. HOME SCREEN (Tap Tab 1 at 115, 2280)
    print("1. Tapping Home Tab...")
    adb_shell("input tap 115 2280")
    time.sleep(2)
    capture("01_real_home.png")
    
    # 2. CATEGORIES SCREEN (Tap Tab 2 at 328, 2280)
    print("2. Tapping Categories Tab...")
    adb_shell("input tap 328 2280")
    time.sleep(2)
    capture("02_real_categories.png")
    
    # 3. NOTES SCREEN (Tap Tab 4 at 752, 2280)
    print("3. Tapping Notes Tab...")
    adb_shell("input tap 752 2280")
    time.sleep(2)
    capture("03_real_notes.png")
    
    # 4. NOTE EDITOR (Tap FAB or center of notes screen)
    print("4. Opening Note Editor...")
    adb_shell("input tap 940 2050")
    time.sleep(2)
    capture("04_real_editor.png")
    
    # Type sample title and text into editor
    adb_shell("input text 'System%sArchitecture%sDesign'")
    time.sleep(0.5)
    adb_shell("input keyevent 66") # Enter
    adb_shell("input text '- %sLocal-first%sRoom%sDatabase'")
    time.sleep(0.5)
    adb_shell("input keyevent 66") # Enter
    adb_shell("input text '- %sOffline%sWeb%sArchival'")
    time.sleep(1)
    capture("04b_real_editor_typed.png")
    
    # Exit editor back to notes
    adb_shell("input keyevent 4")
    time.sleep(1.5)
    
    # 5. SETTINGS SCREEN (Tap Tab 5 at 964, 2280)
    print("5. Tapping Settings Tab...")
    adb_shell("input tap 964 2280")
    time.sleep(2)
    capture("05_real_settings.png")
    
    # 6. RETURN HOME & TAP A CARD FOR OFFLINE READER VIEW
    print("6. Returning Home for Reader view...")
    adb_shell("input tap 115 2280")
    time.sleep(2)
    # Scroll slightly down and tap card
    adb_shell("input tap 540 1700")
    time.sleep(2.5)
    capture("06_real_reader_mode.png")
    adb_shell("input keyevent 4")
    time.sleep(1)
    
    print("=== All Clean Real Captures Complete! ===")

if __name__ == "__main__":
    main()
