#!/usr/bin/env python3
"""Report OS4 UI gate facts; an enabled entry is not a verified implementation."""
from pathlib import Path
import json,re,xml.etree.ElementTree as E
ROOT=Path(__file__).resolve().parents[2]
BASE=ROOT/'library/core/src/main'
source=(BASE/'java/com/sevtinge/hyperceiler/hooker/home/HomeOS4AdaptationGate.java').read_text()
sets={name:set(re.findall(r'"(prefs_key_[^"]+)"',body))for name,body in re.findall(r'Set<String>\s+(\w+)\s*=\s*Set.of\((.*?)\);',source,re.S)}
strings={}
for file in ('strings_app.xml','strings.xml'):
 for node in E.parse(BASE/'res/values-zh-rCN'/file).getroot():
  if node.tag=='string':strings[node.get('name')]=''.join(node.itertext())
android='{http://schemas.android.com/apk/res/android}';app='{http://schemas.android.com/apk/res-auto}'
pages={'home_title_new':'ADAPTED_TITLE_KEYS','home_recent':'ADAPTED_RECENT_KEYS','home_other_new':'ADAPTED_OTHER_KEYS','home_gesture':'ADAPTED_GESTURE_KEYS','home_drawer':None}
replaced={'prefs_key_home_title_icon_size_enable','prefs_key_home_title_icon_size','prefs_key_home_recent_hide_clean_up','prefs_key_always_show_clean_up','prefs_key_home_drawer_blur'}
rows=[]
for page,group in pages.items():
 allowed=sets[group] if group else {'prefs_key_home_drawer_hide_search'}
 for node in E.parse(BASE/f'res/xml/{page}.xml').iter():
  key=node.get(android+'key')
  if not key or node.tag.endswith(('Category','Screen')):continue
  title=node.get(android+'title','');title=strings.get(title.removeprefix('@string/'),title)
  status='replaced_or_legacy_hidden' if key in replaced else 'declared_adapted_requires_evidence' if key in allowed else 'ui_gate_unadapted'
  rows.append({'page':page,'key':key,'title':title,'status':status,'dependency':node.get(android+'dependency')})
report={'scope':'Explicit OS4 home adaptation gates; other module/version branches require a separate audit. No UI gate is proof of runtime behavior.','entries':rows}
Path(__file__).with_name('inventory.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
counts={status:sum(r['status']==status for r in rows)for status in sorted({r['status'] for r in rows})}
print(json.dumps(counts))
for r in rows:
 if r['status']=='ui_gate_unadapted':print(r['page'],r['key'],r['title'])
