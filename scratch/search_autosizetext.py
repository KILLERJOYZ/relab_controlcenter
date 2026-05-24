import os

src_dir = r"c:\Users\luken\AndroidStudioProjects\relab_tool\app\src\main\java"

for root, dirs, files in os.walk(src_dir):
    for file in files:
        if file.endswith(".kt"):
            path = os.path.join(root, file)
            with open(path, "r", encoding="utf-8") as f:
                for i, line in enumerate(f, 1):
                    if "AutoSizeText" in line:
                        print(f"{file}:{i}: {line.strip()}")
