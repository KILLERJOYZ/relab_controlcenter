with open(r"c:\Users\luken\AndroidStudioProjects\relab_tool\app\src\main\java\com\example\relab_tool\ui\DeviceInfoViewModel.kt", "r", encoding="utf-8") as f:
    lines = f.readlines()

for i, line in enumerate(lines, 1):
    if "fun " in line or "class " in line or "val " in line and "by" in line:
        if "private" in line or "public" in line or "internal" in line or "protected" in line or "override" in line:
            print(f"{i}: {line.strip()}")
