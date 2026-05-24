import sys
sys.stdout.reconfigure(encoding='utf-8')

with open(r"c:\Users\luken\AndroidStudioProjects\relab_tool\app\src\main\res\values-vi\strings.xml", "r", encoding="utf-8") as f:
    for line in f:
        if "battery_wear" in line or "actual_capacity" in line:
            print(line.strip())
