with open(r"c:\Users\luken\AndroidStudioProjects\relab_tool\app\src\main\java\com\example\relab_tool\ui\DeviceInfoViewModel.kt", "r", encoding="utf-8") as f:
    lines = f.readlines()

for i in range(1727, 1750):
    if i < len(lines):
        print(f"{i+1}: {lines[i]}", end="")
