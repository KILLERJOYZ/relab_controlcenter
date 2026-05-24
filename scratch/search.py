import os

keywords = ["Thread.sleep", "runBlocking", "withContext", "Dispatchers", "delay", "System.gc", "GC", "cpu", "thermal", "battery", "refreshRate"]
src_dir = r"c:\Users\luken\AndroidStudioProjects\relab_tool\app\src\main\java"

for root, dirs, files in os.walk(src_dir):
    for file in files:
        if file.endswith(".kt"):
            path = os.path.join(root, file)
            try:
                with open(path, "r", encoding="utf-8") as f:
                    for i, line in enumerate(f, 1):
                        for kw in keywords:
                            if kw.lower() in line.lower():
                                print(f"{file}:{i} ({kw}): {line.strip()[:100]}")
            except Exception as e:
                print(f"Error reading {file}: {e}")
