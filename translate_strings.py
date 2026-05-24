import os
import xml.etree.ElementTree as ET
from deep_translator import GoogleTranslator
import time

# Top languages around the world
languages = {
    'es': 'es', 'fr': 'fr', 'de': 'de', 'zh-rCN': 'zh-CN', 'ja': 'ja', 'ko': 'ko', 
    'ru': 'ru', 'ar': 'ar', 'hi': 'hi', 'pt': 'pt', 'it': 'it', 'id': 'id',
    'tr': 'tr', 'th': 'th', 'nl': 'nl'
}

res_dir = r"c:\Users\luken\AndroidStudioProjects\relab_tool\app\src\main\res"
base_strings = os.path.join(res_dir, "values", "strings.xml")

tree = ET.parse(base_strings)
root = tree.getroot()

for android_lang, gtrans_lang in languages.items():
    lang_dir = os.path.join(res_dir, f"values-{android_lang}")
    os.makedirs(lang_dir, exist_ok=True)
    lang_strings_path = os.path.join(lang_dir, "strings.xml")
    
    if os.path.exists(lang_strings_path):
        print(f"Skipping {android_lang}, already exists.")
        continue
        
    print(f"Translating to {android_lang}...")
    translator = GoogleTranslator(source='en', target=gtrans_lang)
    
    new_root = ET.Element('resources')
    
    texts_to_translate = []
    elements = []
    for elem in root:
        if elem.tag == 'string':
            text = elem.text or ""
            # Don't translate app names or strings that don't need it
            if elem.attrib['name'].startswith('app_') and text not in ["Genshin Impact VN", "PUBG Mobile VN", "Liên Minh Huyền Thoại Tốc Chiến", "Liên Quân Mobile", "Call of Duty Mobile VN", "Arknight Endfield VN"]:
                texts_to_translate.append(None) # skip
            else:
                texts_to_translate.append(text)
            elements.append(elem)
            
    translated_texts = []
    try:
        # Translate one by one or in small batches to avoid breaking %s formatting and rate limits
        for i, text in enumerate(texts_to_translate):
            if text is None:
                translated_texts.append(None)
                continue
            if not text.strip():
                translated_texts.append(text)
                continue
            
            # Simple heuristic: don't translate if it's just a format specifier or very simple
            if text.startswith('%') and len(text) < 5:
                translated_texts.append(text)
                continue
                
            try:
                res = translator.translate(text)
                # Quick fix for broken format specifiers
                res = res.replace('% 1 $ s', '%1$s').replace('% 1 $ d', '%1$d').replace('% 2 $ s', '%2$s').replace('% 3 $ s', '%3$s')
                translated_texts.append(res)
            except Exception as e:
                print(f"Error on '{text}': {e}")
                translated_texts.append(text)
            
            if i % 50 == 0:
                time.sleep(1) # brief pause
    except Exception as e:
        print(f"Error translating {android_lang}: {e}")
        continue
        
    for i, elem in enumerate(elements):
        new_elem = ET.SubElement(new_root, 'string', {'name': elem.attrib['name']})
        if translated_texts[i] is not None:
            # Re-escape apostrophes and quotes for Android XML
            cleaned = translated_texts[i].replace("'", r"\'").replace('"', r'\"')
            new_elem.text = cleaned
        else:
            new_elem.text = elem.text

    tree_out = ET.ElementTree(new_root)
    ET.indent(tree_out, space="    ", level=0)
    tree_out.write(lang_strings_path, encoding="utf-8", xml_declaration=True)
    print(f"Finished {android_lang}")
