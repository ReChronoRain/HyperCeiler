#!/usr/bin/env python3
"""Actual production Java classes, deterministic Android/lifecycle/clock adapters."""
from pathlib import Path
import argparse,hashlib,json,os,re,subprocess,tempfile

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

p=argparse.ArgumentParser();p.add_argument('--output',type=Path,required=True);p.add_argument('--override-dir',type=Path);a=p.parse_args();a.output.mkdir(parents=True,exist_ok=True)
b=Path(__file__).resolve().parent;root=b.parents[1];src=root/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook';j=test_paths.JAVA_HOME/'bin'
files=[src/x for x in ['provider/WeatherPrefsSnapshot.java','provider/SharedPrefsProvider.java','provider/HomeLayoutPrefsSnapshot.java','base/FrameworkHookGate.java','rules/weather/WeatherNativeEndpointOS4.java','rules/weather/WeatherNativeBridgeOS4.java']]
if a.override_dir:files=[a.override_dir/f.name if (a.override_dir/f.name).exists() else f for f in files]
with tempfile.TemporaryDirectory(prefix='hc-weather-java-') as t:
 cmd=[str(j/'javac'),'--release','21','-encoding','UTF-8','-d',t,*map(str,list((b/'stubs').rglob('*.java'))+files+[b/'WeatherBridgeTest.java'])]
 z=subprocess.run(cmd,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT);(a.output/'build.txt').write_text(z.stdout)
 if z.returncode: print(z.stdout);raise SystemExit(z.returncode)
 z=subprocess.run([str(j/'java'),'-ea','-Dhws.corpus='+str(a.output/'corpus'),'-cp',t,'com.sevtinge.hyperceiler.libhook.rules.weather.WeatherBridgeTest'],text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=90);(a.output/'tests.txt').write_text(z.stdout);print(z.stdout,end='');m=re.search(r'RESULT passes=(\d+) failures=(\d+)',z.stdout)
 report={'passed':z.returncode==0 and bool(m) and int(m[2])==0,'tests':{'passed':int(m[1]) if m else 0,'failed':int(m[2]) if m else -1},'exit_code':z.returncode,'command':cmd,'production_inputs':{str(f):hashlib.sha256(f.read_bytes()).hexdigest() for f in files},'test_inputs':{str(f):hashlib.sha256(f.read_bytes()).hexdigest() for f in list((b/'stubs').rglob('*.java'))+[b/'WeatherBridgeTest.java',b/'run.py']},'limits':['Host transport, framework handle and hook registration adapters; actual full production provider, snapshot, endpoint, shared gate and WMS callback','No Android Binder execution, LSPosed lifecycle/injection, Weather Rust consumer or actual phone effects; NOT_TESTED']}
 (a.output/'report.json').write_text(json.dumps(report,indent=2)+'\n');raise SystemExit(0 if report['passed'] else 1)
