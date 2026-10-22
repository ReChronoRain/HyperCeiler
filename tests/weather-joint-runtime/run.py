#!/usr/bin/env python3
"""Execute both production Weather runtimes together with real host memory/CAS."""
from pathlib import Path
import argparse,hashlib,json,re,subprocess,tempfile
HERE=Path(__file__).resolve().parent
SOURCE=HERE.parents[1]
CPP=SOURCE/'app/src/main/cpp'
WEATHER=CPP/'targets/weather'
STUBS=HERE.parent/'hyos-control/stubs'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
p=argparse.ArgumentParser(description=__doc__);p.add_argument('--output',type=Path,required=True)
p.add_argument('--negative-controls',action='store_true');a=p.parse_args();out=a.output.resolve();out.mkdir(parents=True,exist_ok=False)
# lifetime_adapter.h includes the real API only, not the private lifetime guard.
files=sorted(p for p in WEATHER.glob('*.h') if p.name!='weather_card_lifetime_guard.generated.h')+[WEATHER/'weather_runtime.cpp',WEATHER/'weather_material_runtime.cpp']
files+=sorted(STUBS.rglob('*.h'))+sorted((CPP/'nativehook').glob('*.h'))+[HERE/'runtime_test.cpp',HERE/'calls.S',HERE/'run.py',HERE.parent/'weather-runtime/card_adapter.h',HERE.parent/'weather-runtime/lifetime_adapter.h']
before={str(f):sha(f)for f in files}
mutations=[
 ('ignore_early_lifetime_residue','weather_runtime.cpp','if (weather_card_lifetime::has_residue()) {','if (false && weather_card_lifetime::has_residue()) {','early lifetime residue refuses both real runtimes and card'),
 ('material_attempts_level_install','weather_runtime.cpp','const bool level_installed = level_selected && install();','const bool level_installed = (level_selected || material_selected) && install();','blur only real component and same caller base'),
 ('ignore_material_residue','weather_runtime.cpp','if (weather_material_runtime::has_residue())\n        g_phase.store(Phase::Residue, std::memory_order_release);','if (weather_material_runtime::has_residue()) { /* negative: ignore */ }','material residue closes both real components'),
 ('bypass_live_level_binding','weather_runtime.cpp','|| !weather_binding::verify_dart(dart, read_memory) || !same_binding())','|| !weather_binding::verify_dart(dart, read_memory))','level live binding changed only level refused'),
 ('substitute_level_dart_for_material','weather_material_runtime.cpp','weather_material_binding::verify_dart(dart, read_memory)','weather_binding::verify_dart(dart, read_memory)','material Dart identity works without level Dart'),
 ('late_commit_after_first_query','weather_material_runtime.cpp','g_phase.compare_exchange_strong(preparing, Phase::Missed, std::memory_order_acq_rel);','(void)preparing; /* negative: first query not terminal */','blocked first original cannot be authorized by late installer'),
 ('omit_material_binding_publication','weather_material_runtime.cpp','g_binding_ready.store(true, std::memory_order_release);','/* negative: binding never published */','both real components consume one shared publication'),
 ('ignore_card_raw3', 'weather_runtime.cpp', '(snapshot.card_type == 2 || snapshot.card_type == 3)', '(snapshot.card_type == 2)', 'card only adapter raw 3'),
 ('reject_card_only_admission', 'weather_runtime.cpp', '(!level_installed && !material_installed && !card_prepared)', '(!level_installed && !material_installed && (card_prepared || !card_prepared))', 'card only adapter raw 2'),
 ('card_depends_on_level', 'weather_runtime.cpp', 'const bool card_prepared = card_selected', 'const bool card_prepared = card_selected && level_installed', 'card only adapter raw 2'),
 ('omit_card_cancel_on_dart_rejection', 'weather_runtime.cpp', 'g_dart_rejected.store(true, std::memory_order_release);\n    weather_card_runtime::cancel();', 'g_dart_rejected.store(true, std::memory_order_release);\n    /* negative: card remains prepared */', 'wrong Dart first export cancels card and actual policies'),
 ('ignore_card_residue', 'weather_runtime.cpp', 'if (weather_card_runtime::status().phase == weather_card_runtime::Phase::Residue)', 'if (false && weather_card_runtime::status().phase == weather_card_runtime::Phase::Residue)', 'card residue closes both actual native policy callbacks'),
 ('omit_card_service', 'weather_runtime.cpp', 'weather_card_runtime::service();', '/* negative: no card maintenance after HWS poll */', 'one poll refresh supplies card and both actual policies'),
 ('omit_card_window_cancellation', 'weather_runtime.cpp', 'void on_card_snapshot_window_missed(uint32_t) noexcept {\n    weather_card_runtime::cancel();', 'void on_card_snapshot_window_missed(uint32_t) noexcept {\n    /* negative: missed first-use window ignored */', 'card missed window leaves actual consumers and cannot install late'),
]
def execute(directory,temporary,source=None,filename=None):
 directory.mkdir(parents=True,exist_ok=True);exe=temporary/(directory.name+'-joint')
 cmd=['g++','-std=c++20','-O1','-g','-Wall','-Wextra','-Werror','-pthread','-fno-omit-frame-pointer','-fsanitize=undefined','-fno-sanitize-recover=all','-I'+str(STUBS),'-I'+str(CPP),'-I'+str(WEATHER)]
 if source:
  macro='HC_RUNTIME_SOURCE'if filename=='weather_runtime.cpp'else'HC_MATERIAL_SOURCE'
  cmd+=['-D'+macro+'="'+str(source)+'"']
 cmd += [str(HERE/'runtime_test.cpp'),str(HERE/'calls.S'),'-o',str(exe)]
 build=subprocess.run(cmd,capture_output=True,text=True,timeout=90)
 (directory/'compile.stdout').write_text(build.stdout);(directory/'compile.stderr').write_text(build.stderr)
 result=dict(compile_command=cmd,compile_exit=build.returncode)
 if build.returncode:return result
 ran=subprocess.run([str(exe)],capture_output=True,text=True,timeout=45)
 (directory/'run.stdout').write_text(ran.stdout);(directory/'run.stderr').write_text(ran.stderr)
 m=re.search(r'RESULT cases=(\d+) failed=(\d+)',ran.stdout)
 result.update(exit_code=ran.returncode,cases=int(m[1])if m else None,failed=int(m[2])if m else None,
               failures=re.findall(r'^FAIL (.*)$',ran.stderr,re.M),signals=re.findall(r'^SIGNAL (.*)$',ran.stderr,re.M))
 return result
with tempfile.TemporaryDirectory(prefix='hc-weather-joint-')as temporary:
 temporary=Path(temporary);fixed=execute(out,temporary);negative=[]
 if fixed['compile_exit']==0 and a.negative_controls:
  for name,filename,old,new,witness in mutations:
   original=(WEATHER/filename).read_text();assert original.count(old)==1,name
   directory=out/'negative-controls'/name;directory.mkdir(parents=True)
   candidate=directory/filename;candidate.write_text(original.replace(old,new))
   r=execute(directory,temporary,candidate,filename)
   r.update(name=name,expected_witness=witness,mutated_source_sha256=sha(candidate))
   r['detected']=r['compile_exit']==0 and r.get('exit_code')==1 and r.get('failed',0)>0 and any(f.startswith(witness+':')for f in r.get('failures',[]))
   negative.append(r)
after={str(f):sha(f)for f in files}
passed=fixed['compile_exit']==0 and fixed.get('exit_code')==0 and fixed.get('failed')==0 and fixed.get('cases')==58 and before==after and all(n['detected']for n in negative)
r=dict(passed=passed,expected_cases=58,preserved_prior_cases=54,retained_original_joint_cases=34,retained_card_routing_cases=20,new_lifetime_admission_cases=4,fixed=fixed,negative_controls=negative,negative_controls_requested=a.negative_controls,
       inputs_before=before,inputs_after=after,inputs_unchanged=before==after,
       production_inputs={str(f):before[str(f)]for f in [WEATHER/'weather_runtime.cpp',WEATHER/'weather_material_runtime.cpp',WEATHER/'weather_snapshot.h',WEATHER/'weather_device_level.h',WEATHER/'weather_material_support.h']},
       boundary=[
       'Both complete production weather_runtime.cpp and weather_material_runtime.cpp execute in one process with actual Cache, HWS decoder and both policies; neither runtime is a substitute adapter.',
       'Card runtime is explicitly an adapter for configuration routing, prepare/install result and cancellation/maintenance observations. It does not execute the production card installer, ARM gateways, code lease publication or renderer; this is NOT a three-production-runtime integration result.',
       'Card lifetime has_residue is a boolean adapter. New cases exercise actual early shared-controller refusal before worker/Binder/GOT/card work, including concurrency and no retry, but do not execute lifetime/proc-task/page-protection implementation.',
       'Only readonly image identity/binding, Binder transport, BOOTTIME, poll sleep/thread counting and permission faults are adapted. Real pthread worker, condition-variable startup wait, mmap, mprotect, proc-maps permissions, process_vm_readv and atomic GOT CAS execute.',
       'Host assembly fixes material and level return-PC difference to 0x5fc76c-0x4402e4, so both production caller checks use exactly the same synthetic Weather base. This does not establish AArch64/Dart register ABI.',
       'Readonly binding adapters inspect real mapped GOT words via the production memory reader; full ELF guard verification is proved separately by the binding suites, not by this integration fixture.',
       'The Lazy state read is an adapter backed by a real uint32 memory read. Real Rust Once initialization and Dart consumers are separately tested in the ARM fixture, not executed here.',
       'Every case forks and exits; process exit ends detached workers. A genuinely stuck Binder call remains one thread; startup returns bounded and late response cannot reopen.',
       'The historical joint-runtime-v1 run had 34/34 positive and 5/6 detected negative controls; removing g_level_installed alone was redundant with actual live GOT ownership rejection after rollback. This run replaces that redundant mutation with bypassing the live level binding check; no production fix or fixture weakening was needed.',
       'No Android Binder service, runtime loader, actual Weather process, Dart VM/GC, renderer or phone result. Device NOT_TESTED.'])
(out/'report.json').write_text(json.dumps(r,indent=2)+'\n')
print(json.dumps({'passed':passed,'fixed':{k:v for k,v in fixed.items()if k!='compile_command'},'negative_controls':[{k:v for k,v in x.items()if k in ['name','detected','failed','compile_exit']}for x in negative],'inputs_unchanged':before==after},indent=2))
raise SystemExit(0 if passed else 1)
