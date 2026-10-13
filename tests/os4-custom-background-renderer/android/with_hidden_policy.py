#!/usr/bin/env python3
"""Temporary policy only on this project's emulator; always restore exact previous setting."""
from pathlib import Path
import argparse,hashlib,json,subprocess,sys

sys.path.insert(0, str(Path(__file__).resolve().parents[2]))
import hyperceiler_test_paths as test_paths

HERE=Path(__file__).resolve().parent;ADB=test_paths.ANDROID_HOME / 'platform-tools/adb';SERIAL='emulator-5580';sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
p=argparse.ArgumentParser();p.add_argument('--build',type=Path,required=True);p.add_argument('--output',type=Path,required=True);p.add_argument('--single-loader',action='store_true');p.add_argument('--oem-media-init',action='store_true');a=p.parse_args();out=a.output.resolve();assert not out.exists();commands=[]
def adb(*x):
 cmd=[str(ADB),'-s',SERIAL,*x];commands.append(cmd);r=subprocess.run(cmd,capture_output=True,text=True,timeout=30);assert r.returncode==0,(cmd,r.stdout,r.stderr);return r.stdout.strip()
assert adb('emu','avd','name').splitlines()[0]=='hyperceiler_os4_api37';old=adb('shell','settings','get','global','hidden_api_policy');status=1
try:
 adb('shell','settings','put','global','hidden_api_policy','1');assert adb('shell','settings','get','global','hidden_api_policy')=='1'
 r=subprocess.run([sys.executable,'-B',str(HERE/'run.py'),'--build',str(a.build),'--output',str(out),*(['--single-loader']if a.single_loader else[]),*(['--oem-media-init']if a.oem_media_init else[])],capture_output=True,text=True,timeout=120);status=r.returncode;out.mkdir(parents=True,exist_ok=True);(out/'command.txt').write_text(r.stdout+r.stderr)
finally:
 if old=='null':adb('shell','settings','delete','global','hidden_api_policy')
 else:adb('shell','settings','put','global','hidden_api_policy',old)
 restored=adb('shell','settings','get','global','hidden_api_policy');out.mkdir(parents=True,exist_ok=True);(out/'hidden-api-policy.json').write_text(json.dumps({'passed':restored==old,'original':old,'temporary':'1','restored':restored,'commands':commands,'runner_sha256':sha(Path(__file__)),'scope':'Dedicated API37 AVD only. Permits original platform OTA DEX to access framework hidden APIs in a normal probe app; not a SystemUI/LSPosed permission or phone proof.'},indent=2)+'\n');assert restored==old
print('probe exit',status,'policy restored',restored);raise SystemExit(status)
