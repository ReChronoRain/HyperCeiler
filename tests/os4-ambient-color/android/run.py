#!/usr/bin/env python3
from pathlib import Path
import argparse,hashlib,json,subprocess,time

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[2]))
import hyperceiler_test_paths as test_paths

ROOT=test_paths.TEST_DATA_ROOT;ADB=test_paths.ANDROID_HOME / 'platform-tools/adb';PKG='local.hyperceiler.ambientcolorprobe';SERIAL='emulator-5580';sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
p=argparse.ArgumentParser();p.add_argument('--build',required=True,type=Path);p.add_argument('--output',required=True,type=Path);p.add_argument('--single-loader',action='store_true');p.add_argument('--oem-media-init',action='store_true');a=p.parse_args();out=a.output.resolve();out.mkdir(parents=True,exist_ok=False);build=json.loads((a.build/'report.json').read_text());assert build['passed']and sha(a.build/'probe.apk')==build['apk_sha256'];commands=[]
def adb(*args,check=True):
 cmd=[str(ADB),'-s',SERIAL,*map(str,args)];commands.append(cmd);r=subprocess.run(cmd,capture_output=True,timeout=90)
 if check:assert r.returncode==0,(cmd,r.stdout,r.stderr)
 return r
identity=adb('emu','avd','name').stdout.decode();assert identity.splitlines()[0]=='hyperceiler_os4_api37';assert adb('shell','getprop','sys.boot_completed').stdout.strip()==b'1';(out/'properties.txt').write_bytes(adb('shell','getprop').stdout);r=adb('install','-r','--abi','arm64-v8a',a.build/'probe.apk');(out/'install.txt').write_bytes(r.stdout+r.stderr);adb('shell','am','force-stop',PKG);adb('shell','run-as',PKG,'rm','-f','files/run/report.json');r=adb('shell','am','start','-W','-n',PKG+'/.MainActivity',*(['--ez','single_loader','true']if a.single_loader else[]),*(['--ez','oem_media_init','true']if a.oem_media_init else[]));(out/'start.txt').write_bytes(r.stdout+r.stderr);until=time.monotonic()+60;data=None
while time.monotonic()<until:
 r=adb('exec-out','run-as',PKG,'cat','files/run/report.json',check=False)
 if r.returncode==0 and r.stdout.strip().startswith(b'{'):data=r.stdout;break
 time.sleep(1)
(out/'logcat.txt').write_bytes(adb('logcat','-d','-t','800','-s','AmbientColorProbe:I','AndroidRuntime:E','art:I','linker:E','HyperCeilerAmbientOS4:I','nativeloader:I').stdout)
if data:
 d=json.loads(data);(out/'android-report.json').write_bytes(data)
 for f in d['frames']:
  assert '/'not in f and f.endswith('.png');(out/f).write_bytes(adb('exec-out','run-as',PKG,'cat','files/run/'+f).stdout)
 report={'passed':d['passed'],'android_check_count':d['check_count'],'failed_checks':[c for c in d['checks']if not c['passed']],'build':{'path':str((a.build/'report.json').resolve()),'sha256':sha(a.build/'report.json')},'commands':commands,'scope':d['scope'],'device_status':'NOT_TESTED','oem_media_init':d['oem_media_init'],'single_color_loader':d['single_color_loader'],'wrapper_sha256':d['wrapper_sha256'],'bridge_sha256':d['bridge_sha256'],'source_sha256':d['source_sha256'],'ota_sha256':d['ota_sha256'],'native_sha256':d['native_sha256']}
else:report={'passed':False,'failure':'No app report; inspect preserved logcat/start logs','commands':commands,'device_status':'NOT_TESTED'}
report['runner']={'path':str(test_paths.report_path(Path(__file__).resolve(), ROOT)),'sha256':sha(Path(__file__))};(out/'report.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report,indent=2));raise SystemExit(0 if report['passed']else 1)
