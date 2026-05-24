with open(r"c:\Users\luken\AndroidStudioProjects\relab_tool\app\src\main\java\com\example\relab_tool\ui\DeviceInfoScreen.kt", "r", encoding="utf-8") as f:
    lines = f.readlines()

for i, line in enumerate(lines, 1):
    if "collectAsStateWithLifecycle" in line or "collectAsState(" in line:
        print(f"{i}: {line.strip()}")
