#!/usr/bin/env python3
"""Run unchanged existing layout endpoint/idle-cache/atomic-reload tests with current production code."""
from pathlib import Path
import argparse,subprocess,tempfile,os,json,hashlib

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

p=argparse.ArgumentParser();p.add_argument('--output',required=True,type=Path);a=p.parse_args();a.output.mkdir(parents=True,exist_ok=True)
b=Path(__file__).resolve().parent;r=b.parents[1];j=test_paths.JAVA_HOME/'bin'
prod=[r/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook'/f for f in ['rules/home/dock/HomeLayoutNativeEndpointOS4.java','provider/HomeLayoutPrefsSnapshot.java','base/FrameworkHookGate.java']]
tests=['HomeLayoutNativeEndpointOS4Test','HomeLayoutIdleSnapshotTest','HomeLayoutAtomicReloadTest'];old=r/'tests/home-layout-native'
stubs=list((old/'stubs').rglob('*.java'))+[r/'tests/home-dock-window/stubs/android/os'/f for f in ['Binder.java','IBinder.java','Parcel.java']]
with tempfile.TemporaryDirectory(prefix='hc-hyos-legacy-layout-') as t:
 cmd=[str(j/'javac'),'--release','21','-encoding','UTF-8','-d',t,*map(str,stubs+prod+[old/(f+'.java') for f in tests])]
 z=subprocess.run(cmd,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT);(a.output/'legacy-build.txt').write_text(z.stdout)
 if z.returncode:print(z.stdout);raise SystemExit(z.returncode)
 results=[]
 for name in tests:
  z=subprocess.run([str(j/'java'),'-ea','-cp',t,'com.sevtinge.hyperceiler.libhook.rules.home.dock.'+name],text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=60);(a.output/(name+'.txt')).write_text(z.stdout);print(name,z.returncode,z.stdout[-250:]);results.append({'test':name,'exit_code':z.returncode})
 (a.output/'legacy-report.json').write_text(json.dumps({'tests':results,'production_inputs':{str(f):hashlib.sha256(f.read_bytes()).hexdigest() for f in prod},'boundary':'unchanged pre-Hyos regression tests against actual current production endpoint/snapshot/gate, host transport fixtures'},indent=2)+'\n')
 raise SystemExit(0 if all(x['exit_code']==0 for x in results) else 1)
