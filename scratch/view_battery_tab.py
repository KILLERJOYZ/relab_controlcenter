import sys
sys.stdout.reconfigure(encoding='utf-8')

with open(r"c:\Users\luken\AndroidStudioProjects\relab_tool\app\src\main\java\com\example\relab_tool\ui\DeviceInfoScreen.kt", "r", encoding="utf-8") as f:
    lines = f.readlines()

for i in range(1300, 1420):
    if i < len(lines):
        print(f"{i+1}: {lines[i]}", end="")
