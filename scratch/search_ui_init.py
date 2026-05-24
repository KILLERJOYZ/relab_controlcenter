import os

ui_dir = r"c:\Users\luken\AndroidStudioProjects\relab_tool\app\src\main\java\com\example\relab_tool\ui"

for root, dirs, files in os.walk(ui_dir):
    for file in files:
        if file.endswith(".kt"):
            path = os.path.join(root, file)
            with open(path, "r", encoding="utf-8") as f:
                content = f.read()
                if "init {" in content:
                    print(f"File: {file} has init block")
                    # print the init block lines
                    lines = content.splitlines()
                    for idx, line in enumerate(lines):
                        if "init {" in line:
                            for offset in range(0, 15):
                                if idx + offset < len(lines):
                                    print(f"  {idx+offset+1}: {lines[idx+offset]}")
