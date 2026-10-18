import subprocess
import os
import sys
import xml.etree.ElementTree as ET

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')

ADB = r"D:\platform-tools\adb.exe"
DEVICE = "10.107.18.77:38193"

subprocess.run([ADB, "connect", DEVICE], capture_output=True)
subprocess.run([ADB, "-s", DEVICE, "shell", "uiautomator dump /sdcard/cur.xml"], capture_output=True)
subprocess.run([ADB, "-s", DEVICE, "pull", "/sdcard/cur.xml", "cur.xml"], capture_output=True)

if os.path.exists("cur.xml"):
    tree = ET.parse("cur.xml")
    for n in tree.iter("node"):
        t = n.attrib.get("text", "")
        d = n.attrib.get("content-desc", "")
        b = n.attrib.get("bounds", "")
        if t or d:
            print(f"{b} | text: \"{t}\" | desc: \"{d}\"")
