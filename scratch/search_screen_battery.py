with open(r"c:\Users\luken\AndroidStudioProjects\relab_tool\app\src\main\java\com\example\relab_tool\ui\DeviceInfoScreen.kt", "r", encoding="utf-8") as f:
    lines = f.readlines()

for i, line in enumerate(lines, 1):
    if "battery" in line.lower() or "charging" in line.lower() or "0h 0m" in line.lower() or "dung lượng thực tế" in line.lower():
        print(f"{i}: {line.strip()}")
