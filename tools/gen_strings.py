import glob, os, sys
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
entries = {}
for f in sorted(glob.glob(os.path.join(ROOT,'i18n_src','*.txt'))):
    for n, line in enumerate(open(f, encoding='utf-8'), 1):
        line = line.rstrip('\n')
        if not line.strip(): continue
        parts = line.split('|')
        if len(parts) != 4:
            sys.exit(f"{f}:{n}: expected 4 fields, got {len(parts)}: {line[:60]}")
        k = parts[0].strip()
        if k in entries: sys.exit(f"duplicate key {k}")
        entries[k] = parts[1:]
def kt(s):
    s = s.replace('\\n', '\u0000')
    s = s.replace('\\', '\\\\').replace('"', '\\"').replace('$', '\\$')
    return s.replace('\u0000', '\\n')
keys = list(entries)
chunks = [keys[i:i+60] for i in range(0, len(keys), 60)]
out = ['package com.ahara.app.i18n', '', '// GENERATED from i18n_src/*.txt by tools/gen_strings.py — edit the sources, not this file.', '',
       '/** Trilingual string table: index 0 = English, 1 = Hindi, 2 = Sanskrit. */',
       'object Strings {',
       '    private val table: HashMap<String, Array<String>> = HashMap(1024)', '',
       '    init {']
for i in range(len(chunks)): out.append(f'        part{i}()')
out += ['    }', '',
 '    private fun put(k: String, en: String, hi: String, sa: String) { table[k] = arrayOf(en, hi, sa) }', '',
 '    /** Translate [key] for language code en / hi / sa. Sanskrit falls back to Hindi, Hindi to English. */',
 '    fun t(lang: String, key: String): String {',
 '        val row = table[key] ?: return key',
 '        val v = when (lang) { "hi" -> row[1]; "sa" -> row[2].ifEmpty { row[1] }; else -> row[0] }',
 '        return v.ifEmpty { row[0] }',
 '    }', '',
 '    /** Translate and substitute {0}, {1} ... placeholders. */',
 '    fun f(lang: String, key: String, vararg args: String): String {',
 '        var s = t(lang, key)',
 '        args.forEachIndexed { i, a -> s = s.replace("{$i}", a) }',
 '        return s',
 '    }', '',
 '    fun has(key: String): Boolean = table.containsKey(key)', '']
for i, ch in enumerate(chunks):
    out.append(f'    private fun part{i}() {{')
    for k in ch:
        en, hi, sa = entries[k]
        out.append(f'        put("{kt(k)}", "{kt(en)}", "{kt(hi)}", "{kt(sa)}")')
    out.append('    }')
out.append('}')
open(os.path.join(ROOT,'app/src/main/java/com/ahara/app/i18n/Strings.kt'),'w',encoding='utf-8').write('\n'.join(out)+'\n')
print(len(keys), "keys,", len(chunks), "chunks")
