import os

def process_file(filepath):
    if not os.path.exists(filepath):
        return
        
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    lines = content.split('\n')
    new_lines = []
    
    in_loop = False
    loop_brace_count = 0
    
    for i, line in enumerate(lines):
        if 'val start = System.nanoTime()' in line:
            indent = line[:len(line) - len(line.lstrip())]
            new_lines.append(f"{indent}var sleepTime = 0L")
            new_lines.append(line)
            continue
            
        if 'repeat(FRAMES)' in line or 'repeat(frames)' in line:
            new_lines.append(line)
            in_loop = True
            loop_brace_count = 1
            continue
            
        if in_loop:
            if '{' in line:
                loop_brace_count += line.count('{')
            if '}' in line:
                loop_brace_count -= line.count('}')
                if loop_brace_count == 0:
                    in_loop = False
                    indent = line[:len(line) - len(line.lstrip())]
                    new_lines.append(f"{indent}    val sleepStart = System.nanoTime()")
                    new_lines.append(f"{indent}    Thread.sleep(1)")
                    new_lines.append(f"{indent}    sleepTime += (System.nanoTime() - sleepStart)")
                    new_lines.append(line)
                    continue
                    
        # Replace the duration calculation robustly
        if '(System.nanoTime() - start)' in line:
            line = line.replace('(System.nanoTime() - start)', '(System.nanoTime() - start - sleepTime)')
            
        new_lines.append(line)
        
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write('\n'.join(new_lines))
        
    print(f"Successfully processed {filepath}")

base_dir = r"C:\Users\luken\StudioProjects\relab_controlcenter\app\src\main\java\com\example\relab_tool\benchmark\domain\engine"
process_file(os.path.join(base_dir, "GpuBenchmark.kt"))
process_file(os.path.join(base_dir, "GpuVulkanBenchmark.kt"))
