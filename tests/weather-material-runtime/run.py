#!/usr/bin/env python3
"""Real material controller + policy/cache, x86 caller PC, real VM memory/CAS.
Image identity is an explicit adapter, separately covered by material-binding.
"""
from pathlib import Path
import argparse,hashlib,json,re,subprocess,tempfile
B=Path(__file__).resolve().parent;SOURCE=B.parents[1];CPP=SOURCE/'app/src/main/cpp';W=CPP/'targets/weather';STUBS=B.parent/'hyos-control/stubs';C=W/'weather_material_runtime.cpp'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
p=argparse.ArgumentParser();p.add_argument('--output',type=Path,required=True);p.add_argument('--negative-controls',action='store_true');a=p.parse_args();a.output.mkdir(parents=True,exist_ok=False)
inputs=[C,W/'weather_material_runtime.h',W/'weather_material_binding.h',W/'weather_material_guard.generated.h',W/'weather_material_support.h',W/'weather_snapshot.h',B/'runtime_test.cpp',Path(__file__).resolve(),*sorted((CPP/'nativehook').glob('*.h')),*sorted(STUBS.rglob('*.h'))];before={str(p):sha(p)for p in inputs};original=C.read_text()
mutations=[
 ('duplicate-original','const auto result = weather_material_support::invoke(', '(void)g_policy.original(key, length, default_value);\n    const auto result = weather_material_support::invoke(', 'installed exact caller override original once'),
 ('late-publish-binding','g_binding_ready.store(true, std::memory_order_release);','/* negative: late publication */','binding published before observable GOT'),
 ('missing-dart-accepted','dart != 0 && weather_material_binding::verify_dart(dart, read_memory)','(static_cast<void>(dart), true)','missing Dart is terminal miss'),
 ('ignore-initial-consumption','g_phase.compare_exchange_strong(preparing, Phase::Missed, std::memory_order_acq_rel);','static_cast<void>(preparing);','late installer cannot reopen blocked first original'),
 ('late-unconditional-commit','g_phase.compare_exchange_strong(preparing, Phase::Installed, std::memory_order_acq_rel)','(static_cast<void>(preparing), g_phase.store(Phase::Installed, std::memory_order_release), true)','late installer cannot reopen blocked first original'),
 ('overwrite-foreign-owner','const bool changed = __atomic_compare_exchange_n','expected = __atomic_load_n(reinterpret_cast<uintptr_t *>(g_binding.slot), __ATOMIC_ACQUIRE);\n    const bool changed = __atomic_compare_exchange_n','foreign owner before CAS preserved'),
 ('ignore-restore-failure','written.changed && written.restored && nhk::read_protection(page) == protection','written.changed','first restore failure rolls back original'),
 ('omit-rollback','if (written.changed) exchange_slot(replacement, original, protection);','static_cast<void>(written);','first restore failure rolls back original'),
 ('allow-late-after-first-miss','if (g_phase.compare_exchange_strong(installed, Phase::Missed, std::memory_order_acq_rel))','if ((static_cast<void>(installed), false))','missing Dart is terminal miss'),
]
def execute(out,tmp,candidate=None):
 out.mkdir(parents=True,exist_ok=True);exe=tmp/out.name
 cmd=['g++','-std=c++20','-O1','-g','-Wall','-Wextra','-Werror','-pthread','-fno-omit-frame-pointer','-fsanitize=undefined','-fno-sanitize-recover=all','-I'+str(STUBS),'-I'+str(CPP),'-I'+str(W)]
 if candidate:cmd+=['-DHC_MATERIAL_SOURCE="'+str(candidate.resolve())+'"']
 cmd +=[str(B/'runtime_test.cpp'),'-o',str(exe)]
 z=subprocess.run(cmd,capture_output=True,text=True,timeout=60);(out/'compile.stdout').write_text(z.stdout);(out/'compile.stderr').write_text(z.stderr);r={'command':cmd,'compile_exit':z.returncode}
 if z.returncode:return r
 z=subprocess.run([str(exe)],capture_output=True,text=True,timeout=45);(out/'run.stdout').write_text(z.stdout);(out/'run.stderr').write_text(z.stderr);m=re.search(r'RESULT cases=(\d+) failed=(\d+)',z.stdout)
 r.update(exit_code=z.returncode,cases=int(m[1])if m else None,failed=int(m[2])if m else None,failures=re.findall(r'^FAIL (.*)$',z.stderr,re.M));return r
with tempfile.TemporaryDirectory(prefix='weather-material-runtime-')as t:
 tmp=Path(t);fixed=execute(a.output,tmp);neg=[]
 if a.negative_controls:
  for name,old,new,witness in mutations:
   assert original.count(old)==1,(name,original.count(old));changed=original.replace(old,new)
   if name=='late-publish-binding':
    anchor='const auto written = exchange_slot(original, replacement, protection);';assert changed.count(anchor)==1;changed=changed.replace(anchor,anchor+'\n    g_binding_ready.store(true, std::memory_order_release);')
   out=a.output/'negative-controls'/name;out.mkdir(parents=True);candidate=out/'weather_material_runtime.cpp';candidate.write_text(changed);record=execute(out,tmp,candidate)
   record.update(name=name,witness=witness,source_sha256=sha(candidate));record['rejected']=record['compile_exit']==0 and record.get('exit_code')==1 and record.get('failed',0)>0 and any(x.startswith(witness+':')for x in record.get('failures',[]));neg.append(record)
 same=before=={str(p):sha(p)for p in inputs}
 report={'passed':fixed['compile_exit']==0 and fixed.get('exit_code')==0 and fixed.get('cases')==37 and fixed.get('failed')==0 and same and all(x['rejected']for x in neg),'fixed':fixed,'negative_controls':neg,'negative_controls_requested':a.negative_controls,'inputs':before,'inputs_unchanged':same,'device_status':'NOT_TESTED','boundaries':['Complete production material runtime.cpp, real policy and weather_snapshot::Cache included; no extracted code fragments.','Host x86 assembly supplies real __builtin_return_address at exact caller bias. No AArch64 ABI claim; that belongs to separate policy ARM evidence.','Real anonymous mmap, mprotect, atomic GOT compare/exchange, /proc mapping permission reads, process_vm_readv, fork and thread concurrency execute. Only permission failure/events and image binding/cold-state/Dart identity are explicit adapters.','Fresh authorization uses actual Cache with controlled BOOTTIME value; no Binder, Android lifecycle, real pinned image loading or renderer execution.','The late-initialization test deliberately permits a stale last cold observation; target query blocks inside original while installation attempts to commit. This tests the controller race, not contested Rust Once machinery.','Native/Dart capability cache remains application-owned after revoke; this component never resets it.','Negative controls must compile, complete and fail named semantic witnesses; timeout/compile failure is not accepted.']}
 (a.output/'report.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({'passed':report['passed'],'fixed':{k:v for k,v in fixed.items()if k!='command'},'negative_controls':[{k:v for k,v in x.items()if k in ['name','rejected','failed','compile_exit']}for x in neg],'inputs_unchanged':same},indent=2));raise SystemExit(0 if report['passed']else 1)
