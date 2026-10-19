#!/usr/bin/env python3
from pathlib import Path
import argparse,hashlib,json,subprocess,time

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

ROOT=test_paths.TEST_DATA_ROOT;ADB=test_paths.ANDROID_HOME / 'platform-tools/adb';PKG='local.hyperceiler.ambientprobe';SERIAL='emulator-5580';sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
p=argparse.ArgumentParser();p.add_argument('--build',type=Path,required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args();a.output.mkdir(parents=True,exist_ok=False);build=json.loads((a.build/'build-report.json').read_text());variant=build['variant'];assert variant in ('baseline','fixed');assert build['passed']is True and sha(a.build/'ambient-probe.apk')==build['apk_sha256'];commands=[]
def adb(*args,check=True):
 cmd=[str(ADB),'-s',SERIAL,*map(str,args)];commands.append(cmd);r=subprocess.run(cmd,capture_output=True,timeout=45)
 if check:assert r.returncode==0,(cmd,r.stdout,r.stderr)
 return r
identity=adb('emu','avd','name').stdout.decode();assert identity.splitlines()[0]=='hyperceiler_os4_api37'
assert adb('shell','getprop','sys.boot_completed').stdout.strip()==b'1'
props=adb('shell','getprop').stdout;(a.output/'android-properties.txt').write_bytes(props)
install=adb('install','-r',a.build/'ambient-probe.apk');(a.output/'install.txt').write_bytes(install.stdout+install.stderr)
adb('shell','am','force-stop',PKG)
adb('shell','run-as',PKG,'rm','-f',f'files/{variant}/report.json')
start=adb('shell','am','start','-W','-n',PKG+'/.MainActivity');(a.output/'start.txt').write_bytes(start.stdout+start.stderr)
end=time.monotonic()+40;data=b''
while time.monotonic()<end:
 result=adb('exec-out','run-as',PKG,'cat',f'files/{variant}/report.json',check=False)
 if result.returncode==0 and result.stdout.strip().startswith(b'{'):
  data=result.stdout;break
 time.sleep(1)
log=adb('logcat','-d','-t','400','-s','AmbientProbe:I','AndroidRuntime:E','RuntimeShader:E');(a.output/'logcat.txt').write_bytes(log.stdout)
assert data,'Probe did not publish a report; inspect logcat'
d=json.loads(data);assert d['variant']==variant and d['source_sha256']==build['inputs'][str((test_paths.SNAPSHOT_ROOT / 'source-os4-hyos-test12-20261009-r1'if variant=='baseline'else test_paths.SOURCE_ROOT)/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/utils/hookapi/systemui/controlcenter/drawable/AmbientLightDrawable.kt')]
(a.output/'android-report.json').write_bytes(data)
for f in d['frames']:
 assert '/'not in f['png'] and f['png'].endswith('.png');(a.output/f['png']).write_bytes(adb('exec-out','run-as',PKG,'cat',f'files/{variant}/'+f['png']).stdout)
report=dict(run_complete=True,passed=d['passed'],variant=variant,android_check_count=d['check_count'],failed_checks=d['failed_checks'],apk_sha256=build['apk_sha256'],build_report=dict(path=str(a.build/'build-report.json'),sha256=sha(a.build/'build-report.json')),inputs={str(Path(__file__).resolve()):sha(Path(__file__).resolve())},commands=commands,outputs={q.name:sha(q)for q in sorted(a.output.iterdir())if q.is_file()},avd=identity.strip(),device_status='NOT_TESTED',scope=d['scope']);(a.output/'report.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({k:report[k]for k in ['run_complete','passed','variant','android_check_count','failed_checks']},indent=2))
