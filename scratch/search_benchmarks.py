with open(r"c:\Users\luken\AndroidStudioProjects\relab_tool\app\src\main\java\com\example\relab_tool\ui\BenchmarksScreen.kt", "r", encoding="utf-8") as f:
    content = f.read()

import re
# Look for readText, readLines, File, execute, runShellCommand, etc.
matches = re.findall(r".*(readText|readLines|File|runShellCommand|Thread\.sleep|runBlocking).*", content)
if matches:
    print("Found potential issues in BenchmarksScreen.kt:")
    for match in matches:
        print("  ", match)
else:
    print("No blocking indicators found in BenchmarksScreen.kt")
