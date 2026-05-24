import os

src_dir = r"c:\Users\luken\AndroidStudioProjects\relab_tool\app\src\main\java"

for root, dirs, files in os.walk(src_dir):
    for file in files:
        if file.endswith(".kt"):
            path = os.path.join(root, file)
            with open(path, "r", encoding="utf-8") as f:
                content = f.read()
                if "SoCUtils" in content and "ViewModel" in file:
                    print(f"File: {file} uses SoCUtils")
