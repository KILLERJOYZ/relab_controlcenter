import os
import xml.etree.ElementTree as ET
import shutil

languages = {
    'ru': {
        'settings_language': 'Язык',
        'settings_general': 'Общие настройки',
        'cit_title': 'Тестирование оборудования (CIT)',
        'cit_desc': 'Запуск диагностики',
        'app_name': 'Инструмент Relab'
    },
    'es': {
        'settings_language': 'Idioma',
        'settings_general': 'General',
        'cit_title': 'Prueba de Hardware (CIT)',
        'cit_desc': 'Ejecutar diagnóstico',
        'app_name': 'Herramienta Relab'
    },
    'fr': {
        'settings_language': 'Langue',
        'settings_general': 'Général',
        'cit_title': 'Test Matériel (CIT)',
        'cit_desc': 'Exécuter les diagnostics',
        'app_name': 'Outil Relab'
    },
    'zh-rCN': {
        'settings_language': '语言',
        'settings_general': '常规',
        'cit_title': '硬件测试 (CIT)',
        'cit_desc': '运行诊断',
        'app_name': 'Relab 工具'
    },
    'ja': {
        'settings_language': '言語',
        'settings_general': '一般',
        'cit_title': 'ハードウェアテスト (CIT)',
        'cit_desc': '診断を実行する',
        'app_name': 'Relab ツール'
    }
}

res_dir = r"c:\Users\luken\AndroidStudioProjects\relab_tool\app\src\main\res"
base_strings = os.path.join(res_dir, "values", "strings.xml")

for android_lang, translations in languages.items():
    lang_dir = os.path.join(res_dir, f"values-{android_lang}")
    os.makedirs(lang_dir, exist_ok=True)
    lang_strings_path = os.path.join(lang_dir, "strings.xml")
    
    # We will create a sparse strings.xml that ONLY contains the translated keys.
    # Android will automatically fallback to values/strings.xml for the rest!
    
    new_root = ET.Element('resources')
    
    for key, val in translations.items():
        elem = ET.SubElement(new_root, 'string', {'name': key})
        elem.text = val

    tree_out = ET.ElementTree(new_root)
    ET.indent(tree_out, space="    ", level=0)
    tree_out.write(lang_strings_path, encoding="utf-8", xml_declaration=True)
    print(f"Created {android_lang}")
