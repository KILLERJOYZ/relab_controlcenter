with open(r"c:\Users\luken\AndroidStudioProjects\relab_tool\app\src\main\java\com\example\relab_tool\ui\DeviceInfoViewModel.kt", "r", encoding="utf-8") as f:
    lines = f.readlines()

for i, line in enumerate(lines, 1):
    if "calc" in line or "health" in line.lower() or "wear" in line.lower() or "cycle" in line.lower():
        print(f"{i}: {line.strip()}")
