import os
import xml.etree.ElementTree as ET
from deep_translator import GoogleTranslator
import time

languages = {
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
    
    if os.path.exists(lang_strings_path) and os.path.getsize(lang_strings_path) > 100:
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
            if elem.attrib['name'].startswith('app_') and text not in ["Genshin Impact VN", "PUBG Mobile VN", "Liên Minh Huyền Thoại Tốc Chiến", "Liên Quân Mobile", "Call of Duty Mobile VN", "Arknight Endfield VN"]:
                texts_to_translate.append(None)
            else:
                texts_to_translate.append(text)
            elements.append(elem)
            
    translated_texts = []
    batch_indices = []
    batch_texts = []
    
    for i, text in enumerate(texts_to_translate):
        if text is None or not text.strip() or (text.startswith('%') and len(text) < 5):
            translated_texts.append(text)
        else:
            translated_texts.append(None)
            batch_indices.append(i)
            batch_texts.append(text)
            
    chunk_size = 50
    for i in range(0, len(batch_texts), chunk_size):
        chunk = batch_texts[i:i+chunk_size]
        try:
            res = translator.translate_batch(chunk)
            for j, translated in enumerate(res):
                if translated:
                    translated = translated.replace('% 1 $ s', '%1$s').replace('% 1 $ d', '%1$d').replace('% 2 $ s', '%2$s').replace('% 3 $ s', '%3$s')
                idx = batch_indices[i+j]
                translated_texts[idx] = translated
        except Exception as e:
            print(f"Batch error: {e}")
            for j, original in enumerate(chunk):
                idx = batch_indices[i+j]
                translated_texts[idx] = original
        time.sleep(1)
        
    for i, elem in enumerate(elements):
        new_elem = ET.SubElement(new_root, 'string', {'name': elem.attrib['name']})
        if translated_texts[i] is not None:
            cleaned = translated_texts[i].replace("'", r"\'").replace('"', r'\"')
            new_elem.text = cleaned
        else:
            new_elem.text = elem.text

    tree_out = ET.ElementTree(new_root)
    ET.indent(tree_out, space="    ", level=0)
    tree_out.write(lang_strings_path, encoding="utf-8", xml_declaration=True)
    print(f"Finished {android_lang}")
