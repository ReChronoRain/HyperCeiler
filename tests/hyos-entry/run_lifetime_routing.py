#!/usr/bin/env python3
"""Reject source mutations of the early Weather lifetime routing; actual component is separately tested."""
import argparse,hashlib,json,re,subprocess,tempfile
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--sdk',type=Path,required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args()
here=Path(__file__).resolve().parent;source=here.parents[1];cpp=source/'app/src/main/cpp';hyos=cpp/'targets/hyos';out=a.output.resolve();out.mkdir(parents=True,exist_ok=False)
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
inputs=[Path(__file__).resolve(),here/'entry_test.cpp',a.sdk.resolve()/'zygisk_next_api.h',hyos/'hyos_entry.cpp',hyos/'hyos_entry.h',hyos/'hyos_control.h',hyos/'hyos_runtime_guard.h',hyos/'hyos_snapshot_guard.h',hyos/'hyos_startup_reason.h',*(cpp/'targets/weather'/n for n in ['weather_binding.h','weather_guard.generated.h','weather_device_level.h','weather_runtime.h','weather_snapshot.h','weather_card_lifetime.h']),cpp/'nativehook/memory_io.h',cpp/'nativehook/nhk_base.h',*sorted((here/'stubs').rglob('*.h'))]
inputs += [cpp/'targets/home/recents_runtime.h', hyos/'hyos_snapshot.h']
before={str(f):sha(f) for f in inputs};original=(hyos/'hyos_entry.cpp').read_text();checks=[];commands=[]
cases=[('omit_plain','if (handle != nullptr) weather_card_lifetime::observe_parent_load(name);','(void)name;',1,'parent successful plain load observes exact returned path and preserves errno'),('omit_extended','if (handle != nullptr) weather_card_lifetime::observe_parent_load(name);','(void)name;',2,'parent successful extended load observes after return and preserves errno'),('capture_failed_load','if (handle != nullptr) weather_card_lifetime::observe_parent_load(name);','weather_card_lifetime::observe_parent_load(name);',0,'failed loader does not capture a lifetime image'),('wrong_child_route','weather_card_lifetime::after_specialization(weather);','weather_card_lifetime::after_specialization(false);',0,'specialization routes only exact Weather main to lifetime early window'),('lose_errno','errno = saved_errno;','(void)saved_errno;',0,'parent successful plain load observes exact returned path and preserves errno')]
with tempfile.TemporaryDirectory(prefix='hc-lifetime-route-') as td:
 root=Path(td);testdir=root/'tests/hyos-entry';prod=root/'app/src/main/cpp/targets/hyos';testdir.mkdir(parents=True);prod.mkdir(parents=True)
 (testdir/'entry_test.cpp').write_bytes((here/'entry_test.cpp').read_bytes());(prod/'hyos_control.h').write_bytes((hyos/'hyos_control.h').read_bytes())
 for name,old,new,nth,witness in [('positive','x','x',-1,'')]+cases:
  changed=original
  if nth!=-1:
   assert old in changed
   if nth==0:changed=changed.replace(old,new)
   else:
    at=-1
    for _ in range(nth):at=changed.index(old,at+1)
    changed=changed[:at]+new+changed[at+len(old):]
  (prod/'hyos_entry.cpp').write_text(changed);binary=root/name
  command=['g++','-std=c++20','-Wall','-Wextra','-Werror','-pthread','-I'+str(a.sdk.resolve()),'-I'+str(here/'stubs'),'-I'+str(cpp),'-I'+str(hyos),str(testdir/'entry_test.cpp'),'-o',str(binary)];commands.append(command)
  b=subprocess.run(command,capture_output=True,text=True);(out/(name+'-build.log')).write_text(b.stdout+b.stderr);assert b.returncode==0,b.stderr
  r=subprocess.run([binary],capture_output=True,text=True,timeout=90);(out/(name+'-run.log')).write_text(r.stdout+r.stderr)
  last=re.findall(r'^RUN_CASE (.*)$',r.stderr,re.M);good=r.returncode==0 and '43/43 PASS;' in r.stdout if nth==-1 else r.returncode!=0 and 'Assertion' in r.stderr and last and last[-1]==witness
  checks.append(dict(name=name,passed=bool(good),exit_code=r.returncode,witness=witness,last_case=last[-1] if last else None,source_sha256=hashlib.sha256(changed.encode()).hexdigest()))
after={str(f):sha(f) for f in inputs};report=dict(passed=all(r['passed'] for r in checks) and before==after,checks=checks,inputs_before=before,inputs_after=after,inputs_unchanged=before==after,commands=commands,scope='Actual entry cpp callbacks and readv guards. Lifetime callback is recording adapter; this proves routing/errno, not GOT component internals or actual Android startup.',device_status='NOT_TESTED');(out/'report.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(dict(passed=report['passed'],checks=len(checks))));raise SystemExit(0 if report['passed'] else 1)
