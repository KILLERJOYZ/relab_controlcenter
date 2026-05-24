"""
Full strings.xml translator for Android.
Translates ALL strings using Google Translate with:
- Placeholder protection (%1$s, %2$s, etc.)
- Batch processing to stay within API limits
- Retry logic with exponential backoff
- Skips already-completed files
"""
import os
import re
import time
import xml.etree.ElementTree as ET
from deep_translator import GoogleTranslator

# Languages to translate: android_locale_folder -> google_translate_code
LANGUAGES = {
    'ru': 'ru',
    'es': 'es',
    'fr': 'fr',
    'de': 'de',
    'zh-rCN': 'zh-CN',
    'ja': 'ja',
    'ko': 'ko',
    'ar': 'ar',
    'hi': 'hi',
    'pt': 'pt',
    'it': 'it',
    'id': 'id',
    'tr': 'tr',
    'th': 'th',
    'nl': 'nl',
}

# App names / proper nouns that must NOT be translated
NO_TRANSLATE_NAMES = {
    'app_geekbench', 'app_antutu', 'app_3dmark', 'app_genshin_vn',
    'app_war_thunder', 'app_fortnite', 'app_garena_df', 'app_chinese_df',
    'app_chinese_val', 'app_wuthering_waves', 'app_pubg_vn', 'app_wild_rift_vn',
    'app_lien_quan', 'app_cod_vn', 'app_arknight_vn', 'app_stremio',
    'app_aurora_store', 'app_apkpure', 'app_taptap', 'app_epic_games',
    'app_xbox', 'app_petal_maps', 'app_gamehub', 'app_scene', 'app_gfx_tool',
    'app_sai', 'app_localsend', 'app_play_services', 'app_webview',
    'app_wechat', 'app_qq', 'app_name',
}

PLACEHOLDER_RE = re.compile(r'%(\d+\$)?[sd]')
RES_DIR = r"c:\Users\luken\AndroidStudioProjects\relab_tool\app\src\main\res"
BASE_STRINGS = os.path.join(RES_DIR, "values", "strings.xml")

def protect_placeholders(text):
    """Replace %1$s → TOKEN_1_S so Google doesn't mangle them."""
    tokens = {}
    def replace(m):
        tok = f"PHTOK{len(tokens)}END"
        tokens[tok] = m.group(0)
        return tok
    return PLACEHOLDER_RE.sub(replace, text), tokens

def restore_placeholders(text, tokens):
    for tok, original in tokens.items():
        text = text.replace(tok, original)
    # Also fix common mangling patterns just in case
    text = re.sub(r'%\s*(\d+)\s*\$\s*s', r'%\1$s', text)
    text = re.sub(r'%\s*(\d+)\s*\$\s*d', r'%\1$d', text)
    return text

def translate_batch(translator, texts_and_tokens):
    """Translate a list of (text, tokens) tuples. Returns list of restored strings."""
    protected_texts = [t for t, _ in texts_and_tokens]
    for attempt in range(4):
        try:
            if len(protected_texts) == 1:
                results = [translator.translate(protected_texts[0])]
            else:
                results = translator.translate_batch(protected_texts)
            return [
                restore_placeholders(r or orig, tok)
                for (orig, tok), r in zip(texts_and_tokens, results)
            ]
        except Exception as e:
            wait = 2 ** attempt
            print(f"  Retry {attempt+1}/4 after error: {e}. Waiting {wait}s...")
            time.sleep(wait)
    # Fallback: return originals
    return [restore_placeholders(t, tok) for t, tok in texts_and_tokens]

def translate_to_lang(android_lang, gtrans_lang):
    lang_dir = os.path.join(RES_DIR, f"values-{android_lang}")
    os.makedirs(lang_dir, exist_ok=True)
    out_path = os.path.join(lang_dir, "strings.xml")
    
    # Check if already complete (more than 200 bytes)
    if os.path.exists(out_path) and os.path.getsize(out_path) > 5000:
        print(f"[{android_lang}] Already complete, skipping.")
        return

    tree = ET.parse(BASE_STRINGS)
    root = tree.getroot()
    
    # Gather all string elements
    all_elems = [(e.attrib.get('name', ''), e.text or '') for e in root if e.tag == 'string']
    
    translator = GoogleTranslator(source='en', target=gtrans_lang)
    
    # Separate translatable vs non-translatable
    to_translate = []   # list of (index, name, protected_text, tokens)
    results_map = {}    # index -> final text
    
    for i, (name, text) in enumerate(all_elems):
        if name in NO_TRANSLATE_NAMES or not text.strip():
            results_map[i] = text
        else:
            protected, tokens = protect_placeholders(text)
            to_translate.append((i, name, protected, tokens))
    
    print(f"[{android_lang}] Translating {len(to_translate)} strings in batches...")
    
    BATCH_SIZE = 30
    for batch_start in range(0, len(to_translate), BATCH_SIZE):
        batch = to_translate[batch_start:batch_start + BATCH_SIZE]
        texts_and_tokens = [(item[2], item[3]) for item in batch]
        translated = translate_batch(translator, texts_and_tokens)
        for (idx, name, _, _), result in zip(batch, translated):
            results_map[idx] = result
        progress = min(batch_start + BATCH_SIZE, len(to_translate))
        print(f"  [{android_lang}] {progress}/{len(to_translate)} done")
        time.sleep(1.5)  # respectful rate limit
    
    # Build output XML
    new_root = ET.Element('resources')
    for i, (name, original_text) in enumerate(all_elems):
        elem = ET.SubElement(new_root, 'string', {'name': name})
        final = results_map.get(i, original_text)
        # Escape apostrophes for Android XML
        if final:
            final = final.replace("'", r"\'")
        elem.text = final
    
    out_tree = ET.ElementTree(new_root)
    ET.indent(out_tree, space="    ", level=0)
    out_tree.write(out_path, encoding="utf-8", xml_declaration=True)
    print(f"[{android_lang}] DONE -> {out_path}")

if __name__ == '__main__':
    for android_lang, gtrans_lang in LANGUAGES.items():
        translate_to_lang(android_lang, gtrans_lang)
    print("\nAll done!")
