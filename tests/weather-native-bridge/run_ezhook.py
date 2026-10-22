#!/usr/bin/env python3
from pathlib import Path
import argparse,hashlib,json,os,subprocess,tempfile,re

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

p=argparse.ArgumentParser();p.add_argument('--output',type=Path,required=True);a=p.parse_args();a.output.mkdir(parents=True,exist_ok=True)
b=Path(__file__).resolve().parent;root=b.parents[1];cache=test_paths.BUILD_CACHE;j=test_paths.JAVA_HOME / 'bin';src=root/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook'
files=[src/x for x in ['provider/WeatherPrefsSnapshot.java','provider/SharedPrefsProvider.java','provider/HomeLayoutPrefsSnapshot.java','base/FrameworkHookGate.java','rules/weather/WeatherNativeEndpointOS4.java','rules/weather/WeatherNativeBridgeOS4.java']]
deps=[cache/'changed-hooks-compile'/x for x in ['hook-xposed-102-1.3.0.jar','api-102.0.0.jar']]+list((cache/'gradle-home/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib/2.4.10').rglob('*.jar'));cp=os.pathsep.join(map(str,deps));stubs=[f for f in (b/'stubs').rglob('*.java')if f.name not in ['IMethodHook.java','HookParam.java']]
with tempfile.TemporaryDirectory(prefix='weather-ezhook-')as t:
 cmd=[str(j/'javac'),'--release','21','-cp',cp,'-d',t,*map(str,stubs+files+[b/'EzHookCallbackTest.java'])];z=subprocess.run(cmd,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True);(a.output/'build.txt').write_text(z.stdout)
 if z.returncode:print(z.stdout);raise SystemExit(z.returncode)
 z=subprocess.run([str(j/'java'),'-ea','-cp',t+os.pathsep+cp,'com.sevtinge.hyperceiler.libhook.rules.weather.EzHookCallbackTest'],stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,timeout=30);(a.output/'tests.txt').write_text(z.stdout);print(z.stdout,end='');m=re.search(r'RESULT passes=(\d+) failures=(\d+)',z.stdout)
 report={'passed':z.returncode==0 and m is not None and int(m[2])==0,'tests':{'passed':int(m[1])if m else 0,'failed':int(m[2])if m else -1},'exit_code':z.returncode,'command':cmd,'production_inputs':{str(f):hashlib.sha256(f.read_bytes()).hexdigest()for f in files},'dependencies':{str(f):hashlib.sha256(f.read_bytes()).hexdigest()for f in deps},'boundary':'Production WMS callback executes using real EzHook1.3 HookParam+InvocationContext / API102 Chain, with Android and registration adapters; no LSPosed installation or phone execution'};(a.output/'report.json').write_text(json.dumps(report,indent=2)+'\n');raise SystemExit(0 if report['passed']else 1)
