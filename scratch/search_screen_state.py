with open(r"c:\Users\luken\AndroidStudioProjects\relab_tool\app\src\main\java\com\example\relab_tool\ui\DeviceInfoScreen.kt", "r", encoding="utf-8") as f:
    lines = f.readlines()

for i, line in enumerate(lines, 1):
    if "collectAsState" in line or "by viewModel." in line or "remember" in line:
        if i <= 300: # only print first 300 lines for now
            print(f"{i}: {line.strip()}")
