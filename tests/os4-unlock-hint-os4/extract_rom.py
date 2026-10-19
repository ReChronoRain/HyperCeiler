#!/usr/bin/env python3
"""Fresh complete-class extraction from supplied OTA; never executes its DEX."""
import argparse,hashlib,importlib.util,json,pathlib,subprocess,sys,zipfile

from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

sys.dont_write_bytecode=True
ROOT=test_paths.TEST_DATA_ROOT
CACHE=test_paths.BUILD_CACHE
APK=test_paths.ROM_ROOT / 'components/system_ext/priv-app/MiuiSystemUI/MiuiSystemUI.apk'
s=importlib.util.spec_from_file_location('dex_inventory',test_paths.TEST_DATA_ROOT / 'tools/dex_inventory.py');d=importlib.util.module_from_spec(s);s.loader.exec_module(d)
sha=lambda b:hashlib.sha256(b).hexdigest()
NAMES={'com.android.systemui.keyguard.KeyguardIndicationRotateTextViewController','com.android.systemui.keyguard.KeyguardIndication','com.android.systemui.util.ViewController','com.android.systemui.statusbar.phone.KeyguardIndicationTextView','com.android.systemui.statusbar.KeyguardIndicationController','com.miui.systemui.util.CommonUtil','com.android.systemui.statusbar.KeyguardIndicationController$$ExternalSyntheticLambda8','com.android.systemui.keyguard.ui.binder.KeyguardIndicationAreaBinder','com.android.keyguard.blueprint.KeyguardBottomAreaSection'}
def run(out):
 out.mkdir(parents=True,exist_ok=False)
 cp=':'.join(str(x)for pat in ['**/smali-*.jar','**/guava-*.jar','**/failureaccess-*.jar','**/jcommander-*.jar']for x in (test_paths.ANDROID_HOME / 'cmdline-tools/latest/lib/external').glob(pat));rows=[]
 with zipfile.ZipFile(APK)as z:
  for dex in sorted(n for n in z.namelist()if n.endswith('.dex')):
   raw=z.read(dex);classes=d.dex_classes(raw);selected=sorted(n for n in classes if n in NAMES or n.startswith('com.android.systemui.keyguard.KeyguardIndicationRotateTextViewController$'))
   if not selected:continue
   cmd=[str(test_paths.JAVA_HOME / 'bin/java'),'-cp',cp,'com.android.tools.smali.baksmali.Main','disassemble','--classes',','.join('L'+n.replace('.','/')+';'for n in selected),'-o',str(out/'smali'),str(APK)+'/'+dex]
   p=subprocess.run(cmd,capture_output=True,text=True);assert p.returncode==0,p.stderr;rows.append(dict(dex=dex,sha256=sha(raw),classes=selected,command=cmd))
  cmd=[str(test_paths.ANDROID_HOME / 'build-tools/37.0.0/aapt2'),'dump','xmltree',str(APK),'--file','res/layout/keyguard_bottom_area.xml']
  p=subprocess.run(cmd,capture_output=True,text=True);assert p.returncode==0,p.stderr;(out/'keyguard_bottom_area.xml.txt').write_text(p.stdout)
 (out/'extraction.json').write_text(json.dumps(dict(apk=str(APK),apk_sha256=sha(APK.read_bytes()),script_sha256=sha(pathlib.Path(__file__).read_bytes()),extractions=rows,smali={str(p.relative_to(out)):sha(p.read_bytes())for p in sorted((out/'smali').rglob('*.smali'))}),indent=2)+'\n')
 assert NAMES.issubset({n for r in rows for n in r['classes']}), NAMES-{n for r in rows for n in r['classes']}
 print(sum(len(x['classes'])for x in rows))
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--output',required=True,type=pathlib.Path);run(p.parse_args().output)
