"""
Fix compilation errors: replace context.getString(R.string.unknown) with 
getApplication<Application>().getString(R.string.unknown) in functions
that don't have 'context' in scope. 
Better approach: use .translateUnknown() which already handles this.
"""
import re

VM_PATH = r"c:\Users\luken\StudioProjects\relab_controlcenter\app\src\main\java\com\example\relab_tool\ui\DeviceInfoViewModel.kt"

with open(VM_PATH, "r", encoding="utf-8") as f:
    content = f.read()

# Lines with errors - these are in private functions without context param
# The issue is that our bulk replacement changed ?: "Unknown" and return "Unknown"
# to use context.getString() but these functions don't have context.
# 
# Solution: In functions without context, use the translateUnknown() helper
# or getApplication<Application>().getString()

# Strategy: Replace ALL remaining context.getString(R.string.unknown) 
# with getApplication<Application>().getString(R.string.unknown)
# EXCEPT in places where context is already defined (inside coroutines with val context = ...)

# Actually, simpler: just use .translateUnknown() everywhere by reverting to "Unknown"
# and adding .translateUnknown() call

# Let's check each error line and fix specifically

lines = content.split('\n')

# Error lines: 688, 1267, 1271, 1311, 1354, 1480, 3024, 3054, 3240, 3259, 3261, 3907, 4024
error_lines = [688, 1267, 1271, 1311, 1354, 1480, 3024, 3054, 3240, 3259, 3261, 3907, 4024]

count = 0
for i in range(len(lines)):
    line_num = i + 1
    if 'context.getString(R.string.unknown)' in lines[i]:
        # Check if this line is inside a function that has context in scope
        # We need to look up to find the enclosing function
        has_context = False
        for j in range(i, max(0, i-50), -1):
            if 'val context = getApplication' in lines[j] or 'context: Context' in lines[j]:
                has_context = True
                break
            if re.match(r'\s*(private |internal |public )?fun ', lines[j]):
                break
        
        if not has_context:
            old = lines[i]
            lines[i] = lines[i].replace(
                'context.getString(R.string.unknown)',
                'getApplication<Application>().getString(R.string.unknown)'
            )
            if old != lines[i]:
                count += 1
                print(f"  Fixed line {line_num}")
    
    # Also fix context.getString(R.string.supported) in functions without context
    if 'context.getString(R.string.supported)' in lines[i]:
        has_context = False
        for j in range(i, max(0, i-50), -1):
            if 'val context = getApplication' in lines[j] or 'context: Context' in lines[j]:
                has_context = True
                break
            if re.match(r'\s*(private |internal |public )?fun ', lines[j]):
                break
        
        if not has_context:
            old = lines[i]
            lines[i] = lines[i].replace(
                'context.getString(R.string.supported)',
                'getApplication<Application>().getString(R.string.supported)'
            )
            if old != lines[i]:
                count += 1
                print(f"  Fixed line {line_num} (supported)")

# Also fix any remaining hardcoded "Unknown" that we missed
for i in range(len(lines)):
    line_num = i + 1
    stripped = lines[i].strip()
    # Skip comparison lines
    if '!= "Unknown"' in stripped or '== "Unknown"' in stripped or 'contains("Unknown"' in stripped:
        continue
    # Fix remaining ?: "Unknown" 
    if '?: "Unknown"' in lines[i]:
        # Check context
        has_context = False
        for j in range(i, max(0, i-50), -1):
            if 'val context = getApplication' in lines[j] or 'context: Context' in lines[j]:
                has_context = True
                break
            if re.match(r'\s*(private |internal |public )?fun ', lines[j]):
                break
        
        if has_context:
            lines[i] = lines[i].replace('?: "Unknown"', '?: context.getString(R.string.unknown)')
        else:
            lines[i] = lines[i].replace('?: "Unknown"', '?: getApplication<Application>().getString(R.string.unknown)')
        count += 1
        print(f"  Fixed remaining line {line_num}")
    
    # Fix remaining catch { "Unknown" }
    if '{ "Unknown" }' in lines[i] and 'catch' in lines[i]:
        has_context = False
        for j in range(i, max(0, i-50), -1):
            if 'val context = getApplication' in lines[j] or 'context: Context' in lines[j]:
                has_context = True
                break
            if re.match(r'\s*(private |internal |public )?fun ', lines[j]):
                break
        
        if has_context:
            lines[i] = lines[i].replace('{ "Unknown" }', '{ context.getString(R.string.unknown) }')
        else:
            lines[i] = lines[i].replace('{ "Unknown" }', '{ getApplication<Application>().getString(R.string.unknown) }')
        count += 1
        print(f"  Fixed remaining catch line {line_num}")

content = '\n'.join(lines)

with open(VM_PATH, "w", encoding="utf-8") as f:
    f.write(content)

print(f"\nFixed {count} lines total")
