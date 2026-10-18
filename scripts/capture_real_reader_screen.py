import subprocess
import time
import os
import sys
import xml.etree.ElementTree as ET

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')

ADB = r"D:\platform-tools\adb.exe"
DEVICE = "10.107.18.77:36251"

def run_adb(args):
    return subprocess.run([ADB, "-s", DEVICE] + args, capture_output=True, text=True, encoding='utf-8', errors='replace')

subprocess.run([ADB, "connect", DEVICE], capture_output=True)

# Clean status bar & notifications
run_adb(["shell", "settings put global heads_up_notifications_enabled 0"])
run_adb(["shell", "cmd statusbar collapse"])
run_adb(["shell", "settings put global sysui_demo_allowed 1"])
run_adb(["shell", "am broadcast -a com.android.systemui.demo -e command enter"])
run_adb(["shell", "am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0941"])
run_adb(["shell", "am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false"])
run_adb(["shell", "am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4"])
run_adb(["shell", "am broadcast -a com.android.systemui.demo -e command notifications -e visible false"])

print("Demo mode set.")

# Step 1: Ensure MomoStack is in foreground
run_adb(["shell", "am start -n com.momostack.app/.ui.MainActivity"])
time.sleep(1)

# Dump UI to check where we are
run_adb(["shell", "uiautomator dump /sdcard/s1.xml"])
run_adb(["pull", "/sdcard/s1.xml", "s1.xml"])

in_detail = False
in_reader = False
if os.path.exists("s1.xml"):
    with open("s1.xml", "r", encoding="utf-8", errors="ignore") as f:
        content = f.read()
        if "Offline Document" in content or "Theme:" in content:
            in_reader = True
        elif "Read Offline" in content or "Saved Item" in content or "AUTOMATED METADATA" in content:
            in_detail = True

print(f"Current state: in_reader={in_reader}, in_detail={in_detail}")

if not in_reader and not in_detail:
    print("Long-pressing on Docker containers item (500, 1540)...")
    run_adb(["shell", "input swipe 500 1540 500 1540 1000"])
    time.sleep(1.5)
    in_detail = True

if in_detail and not in_reader:
    run_adb(["shell", "uiautomator dump /sdcard/s2.xml"])
    run_adb(["pull", "/sdcard/s2.xml", "s2.xml"])
    if os.path.exists("s2.xml"):
        tree = ET.parse("s2.xml")
        read_offline_bounds = None
        for n in tree.iter("node"):
            t = n.attrib.get("text", "")
            if "Read Offline" in t or "Read" in t:
                read_offline_bounds = n.attrib.get("bounds", "")
                print(f"Found Read Offline button at: {read_offline_bounds}")
                break
        
        if read_offline_bounds:
            # Parse bounds [x1,y1][x2,y2]
            parts = read_offline_bounds.replace("][", ",").replace("[", "").replace("]", "").split(",")
            cx = (int(parts[0]) + int(parts[2])) // 2
            cy = (int(parts[1]) + int(parts[3])) // 2
            print(f"Tapping Read Offline at ({cx}, {cy})...")
            run_adb(["shell", f"input tap {cx} {cy}"])
            time.sleep(2)
            in_reader = True
        else:
            print("Could not find Read Offline button in dump, trying swipe up...")
            run_adb(["shell", "input swipe 500 1500 500 800 300"])
            time.sleep(1)
            # dump again
            run_adb(["shell", "uiautomator dump /sdcard/s2b.xml"])
            run_adb(["pull", "/sdcard/s2b.xml", "s2b.xml"])
            if os.path.exists("s2b.xml"):
                tree = ET.parse("s2b.xml")
                for n in tree.iter("node"):
                    t = n.attrib.get("text", "")
                    if "Read Offline" in t or "Read" in t:
                        b = n.attrib.get("bounds", "")
                        parts = b.replace("][", ",").replace("[", "").replace("]", "").split(",")
                        cx = (int(parts[0]) + int(parts[2])) // 2
                        cy = (int(parts[1]) + int(parts[3])) // 2
                        print(f"Tapping Read Offline after scroll at ({cx}, {cy})...")
                        run_adb(["shell", f"input tap {cx} {cy}"])
                        time.sleep(2)
                        in_reader = True
                        break

# Re-collapse any statusbar just in case
run_adb(["shell", "cmd statusbar collapse"])
time.sleep(1)

# Capture pristine screenshot
out_path = r"D:\ansaribilal\link-vault\store-assets\real_captures\06_reader_in_app.png"
run_adb(["shell", "screencap -p /sdcard/06_reader.png"])
run_adb(["pull", "/sdcard/06_reader.png", out_path])
print(f"Captured screen to {out_path}")
