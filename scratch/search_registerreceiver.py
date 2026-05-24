import os

src_dir = r"c:\Users\luken\AndroidStudioProjects\relab_tool\app\src\main\java"

for root, dirs, files in os.walk(src_dir):
    for file in files:
        if file.endswith(".kt"):
            path = os.path.join(root, file)
            with open(path, "r", encoding="utf-8") as f:
                content = f.read()
                if "registerReceiver" in content:
                    print(f"File: {file} calls registerReceiver")
                    lines = content.splitlines()
                    for i, line in enumerate(lines, 1):
                        if "registerReceiver" in line:
                            print(f"  {i}: {line.strip()}")
