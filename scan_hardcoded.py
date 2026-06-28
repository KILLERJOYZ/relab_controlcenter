"""
COMPREHENSIVE hardcoded strings audit across all .kt files.
Looks for patterns that indicate user-visible hardcoded strings.
"""
import re, os, glob

SRC_DIR = r"c:\Users\luken\StudioProjects\relab_controlcenter\app\src\main\java"

# Patterns that indicate hardcoded user-visible strings
# We look for common patterns in Kotlin/Compose code
PATTERNS = [
    # Direct text assignments to UI-visible fields
    (r'"(Unknown)"', "Unknown fallback"),
    (r'"(\d+ clusters?)"', "clusters hardcoded"),
    (r'"(\d+ Cores?)"', "Cores hardcoded"),
    (r'"\$\{.*\} (cluster|Cores|clusters)"', "cluster/cores format"),
    (r'"(Enabled|Disabled|Connected|Disconnected|Not supported)"', "status strings"),
    (r'"(Yes|No|None|N/A)"', "boolean/null strings"),
    (r'"(Read|Write|Total|Free|Used)"', "storage labels"),
    (r'"(Front|Back|External|Main)"', "camera facing"),
    (r'"(Good|Dead|Cold|Overheat|Over Voltage)"', "battery health"),
    (r'"(Charging|Discharging|Full|Not Charging)"', "battery status"),
    (r'"Max Freq:', "max freq format"),
]

results = {}
for filepath in glob.glob(os.path.join(SRC_DIR, "**", "*.kt"), recursive=True):
    basename = os.path.basename(filepath)
    with open(filepath, "r", encoding="utf-8", errors="replace") as f:
        lines = f.readlines()
    
    for i, line in enumerate(lines, 1):
        stripped = line.strip()
        # Skip comments and imports
        if stripped.startswith("//") or stripped.startswith("import ") or stripped.startswith("*"):
            continue
        
        # Check for "Unknown" that's used as a UI-visible fallback
        if '"Unknown"' in stripped:
            # Skip comparison checks like != "Unknown"  
            if '!= "Unknown"' in stripped or '== "Unknown"' in stripped or 'contains("Unknown"' in stripped:
                continue
            key = f"{basename}:{i}"
            results[key] = stripped[:120]
        
        # Check for hardcoded English phrases in string interpolation
        if 'cluster' in stripped.lower() and '"' in stripped:
            if 'clusters"' in stripped or 'cluster"' in stripped:
                key = f"{basename}:{i}"
                results[key] = stripped[:120]
        
        if 'Cores"' in stripped:
            key = f"{basename}:{i}"
            results[key] = stripped[:120]
        
        if '"Max Freq' in stripped:
            key = f"{basename}:{i}"
            results[key] = stripped[:120]

        if '"Not supported"' in stripped or '"Supported"' in stripped:
            key = f"{basename}:{i}"
            results[key] = stripped[:120]

print(f"Found {len(results)} potential hardcoded strings:\n")
for loc, line in sorted(results.items()):
    print(f"  {loc}")
    print(f"    {line}\n")
