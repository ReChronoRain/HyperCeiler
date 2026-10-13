#!/usr/bin/env python3
import hashlib,importlib.util,json,subprocess,sys,zipfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

sys.dont_write_bytecode=True
ROOT=test_paths.TEST_DATA_ROOT
import argparse
a=argparse.ArgumentParser();a.add_argument('--output',type=Path,required=True);OUT=a.parse_args().output
OUT.mkdir(parents=True,exist_ok=False)
APK=test_paths.ROM_ROOT / 'components/system_ext/priv-app/MiuiSystemUI/MiuiSystemUI.apk'
CACHE=test_paths.BUILD_CACHE
MODULE=test_paths.TEST_DATA_ROOT / 'tools/dex_inventory.py'
sp=importlib.util.spec_from_file_location('dex_fresh',MODULE);dex=importlib.util.module_from_spec(sp);sp.loader.exec_module(dex)
sha=lambda b:hashlib.sha256(b).hexdigest()
PREFIXES=('com.android.systemui.keyguard.KeyguardIndicationRotateTextViewController','com.android.systemui.util.ViewController')
EXACT={'com.android.systemui.keyguard.KeyguardIndication','com.android.systemui.statusbar.KeyguardIndicationController','com.android.systemui.statusbar.phone.KeyguardIndicationTextView','com.android.systemui.util.ViewController'}
lib=test_paths.ANDROID_HOME / 'cmdline-tools/latest/lib/external'
jars=sorted(p for pat in ('**/smali-*.jar','**/guava-*.jar','**/failureaccess-*.jar','**/jcommander-*.jar') for p in lib.glob(pat))
cp=':'.join(map(str,jars));java=test_paths.JAVA_HOME / 'bin/java'
records=[];selected=set();allclasses={};commands=[]
with zipfile.ZipFile(APK) as z:
 for entry in z.namelist():
  if not entry.endswith('.dex'):continue
  data=z.read(entry);classes=dex.dex_classes(data);allclasses.update({k:entry for k in classes})
  found=sorted(n for n in classes if n in EXACT or n.startswith(PREFIXES));selected.update(found)
  rec=dict(dex=entry,sha256=sha(data),class_count=len(classes),selected=found)
  if found:
   command=[str(java),'-cp',cp,'com.android.tools.smali.baksmali.Main','disassemble','--classes',','.join('L'+n.replace('.','/')+';'for n in found),'-o',str(OUT/'smali'),str(APK)+'/'+entry]
   p=subprocess.run(command,capture_output=True);assert p.returncode==0,p.stdout+p.stderr
   commands.append(dict(argv=command,exit_code=p.returncode,stdout=p.stdout.decode(),stderr=p.stderr.decode()))
  records.append(rec)
assert not EXACT-selected,sorted(EXACT-selected)
AAPT=test_paths.ANDROID_HOME / 'build-tools/37.0.0/aapt2'
for member in ():
 p=subprocess.run([str(AAPT),'dump','xmltree',str(APK),'--file',member],capture_output=True);assert p.returncode==0,p.stdout+p.stderr
 (OUT/(Path(member).name+'.txt')).write_bytes(p.stdout)
 commands.append(dict(argv=[str(AAPT),'dump','xmltree',str(APK),'--file',member],exit_code=p.returncode))
source=test_paths.SOURCE_ROOT / 'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/systemui/lockscreen/HideLockScreenHint.kt'
report=dict(device_status='NOT_TESTED',apk=str(APK),apk_sha256=sha(APK.read_bytes()),source={},
 extractor_sha256=sha(Path(__file__).read_bytes()),parser_sha256=sha(MODULE.read_bytes()),dependencies={str(p):sha(p.read_bytes())for p in jars+[AAPT,java]},
 complete_selected_class_count=len(selected),missing_requested_classes=sorted(EXACT-selected),dex=records,commands=commands,files={str(p.relative_to(OUT)):sha(p.read_bytes())for p in sorted((OUT/'smali').rglob('*.smali'))},
 limits=['Fresh complete selected class extraction, not yet a semantic approval or execution of any Android hook.','All DEX member class definitions scanned; reflective or dynamically loaded targets are not enumerated.'])
(OUT/'extraction.json').write_text(json.dumps(report,indent=2)+'\n')
# Complete class membership count is recorded per DEX; existing old index remains frozen.
print(json.dumps(dict(classes=len(selected),dexes=len(records),apk_sha256=report['apk_sha256'])))
