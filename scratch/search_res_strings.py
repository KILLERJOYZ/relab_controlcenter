import os

res_dir = r"c:\Users\luken\AndroidStudioProjects\relab_tool\app\src\main\main" # Wait, main is under src/main
# Let's search inside app/src/main/res/values
values_dir = r"c:\Users\luken\AndroidStudioProjects\relab_tool\app\src\main\res\values"
for file in os.listdir(values_dir):
    if file.endswith(".xml"):
        path = os.path.join(values_dir, file)
        with open(path, "r", encoding="utf-8") as f:
            for line in f:
                if "battery_wear" in line or "actual_capacity" in line:
                    print(f"{file}: {line.strip()}")
