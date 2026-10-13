#!/usr/bin/env python3
from pathlib import Path
import argparse,subprocess,tempfile,os,json,hashlib

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

p=argparse.ArgumentParser();p.add_argument('--output',required=True,type=Path);p.add_argument('--endpoint-source',type=Path);a=p.parse_args();a.output.mkdir(parents=True,exist_ok=True)
b=Path(__file__).resolve().parent;r=b.parents[1];j=test_paths.JAVA_HOME/'bin'
production=[r/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook'/f for f in ['rules/home/dock/HomeLayoutNativeEndpointOS4.java','provider/HomeLayoutPrefsSnapshot.java','base/FrameworkHookGate.java']]
if a.endpoint_source: production[0]=a.endpoint_source
stubs=[p for p in (r/'tests/home-layout-native/stubs').rglob('*.java') if p.name!='ContentResolver.java']+[r/'tests/home-dock-window/stubs/android/os'/f for f in ['Binder.java','IBinder.java','Parcel.java']]+list((b/'java').rglob('*.java'))
with tempfile.TemporaryDirectory(prefix='hc-hyos-endpoint-') as t:
 cmd=[str(j/'javac'),'--release','21','-encoding','UTF-8','-d',t,*map(str,stubs+production+[b/'HyosEndpointTest.java'])]
 z=subprocess.run(cmd,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT);(a.output/'java-build.txt').write_text(z.stdout)
 if z.returncode:print(z.stdout);raise SystemExit(z.returncode)
 z=subprocess.run([str(j/'java'),'-ea','-cp',t,'com.sevtinge.hyperceiler.libhook.rules.home.dock.HyosEndpointTest'],text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=60);(a.output/'java.txt').write_text(z.stdout);print(z.stdout,end='')
 (a.output/'java-report.json').write_text(json.dumps({'exit_code':z.returncode,'command':cmd,'production_inputs':{str(f):hashlib.sha256(f.read_bytes()).hexdigest() for f in production},'boundary':'actual production endpoint and provider snapshot builder; Android transport/SharedPreferences fixtures; no Android Binder or native module injection'},indent=2)+'\n')
 raise SystemExit(z.returncode)
