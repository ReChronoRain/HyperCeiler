#!/usr/bin/env python3
"""Three complete Weather runtimes plus shared HWS cache in one host process."""
from pathlib import Path
import argparse, hashlib, importlib.util, json, re, subprocess, tempfile

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

HERE=Path(__file__).resolve().parent
SOURCE=HERE.parents[1];ROOT=test_paths.TEST_DATA_ROOT;CPP=SOURCE/'app/src/main/cpp';WEATHER=CPP/'targets/weather'
STUBS=SOURCE/'tests/hyos-control/stubs'
HELPER=SOURCE/'tests/weather-card-runtime/run.py'
spec=importlib.util.spec_from_file_location('card_transaction_runner',HELPER);helper=importlib.util.module_from_spec(spec);spec.loader.exec_module(helper)
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()

def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--output',type=Path,required=True);p.add_argument('--negative-controls',action='store_true');a=p.parse_args()
 out=a.output.resolve();out.mkdir(parents=True,exist_ok=False)
 files=[*(f for f in sorted(WEATHER.glob('*.h')) if f.name!='weather_card_lifetime_guard.generated.h'),
  *(WEATHER/name for name in ('weather_runtime.cpp','weather_material_runtime.cpp','weather_card_runtime.cpp')),
  WEATHER/'weather_card_arm64.S',HELPER,*sorted(HERE.glob('*.*')),*sorted(STUBS.rglob('*.h')),*sorted((CPP/'nativehook').glob('*.h'))]
 before={str(f):sha(f)for f in files};commands=[]
 def command(cmd,prefix,timeout=90):
  cmd=list(map(str,cmd));commands.append(cmd);r=subprocess.run(cmd,capture_output=True,text=True,timeout=timeout)
  prefix.with_suffix('.stdout').write_text(r.stdout);prefix.with_suffix('.stderr').write_text(r.stderr);return r
 with tempfile.TemporaryDirectory(prefix='hc-card-joint-')as tmp:
  tmp=Path(tmp)
  linked=command([helper.NDK/'aarch64-linux-android35-clang','-fPIC','-shared','-nostdlib',WEATHER/'weather_card_arm64.S','-o',tmp/'templates.so'],out/'template-link')
  assert linked.returncode==0,linked.stderr
  blobs=helper.linked_templates(tmp/'templates.so');approved_path=test_paths.TEST_DATA_ROOT / 'acceptance/weather-card-next/production-gateway-v1/compiled-v2/report.json';approved=json.loads(approved_path.read_text());assert approved['passed']
  assembly=['.section .rodata','.p2align 3'];template_hashes={}
  for kind,raw in blobs.items():
   sym='hc_weather_card_'+kind+'_template';entry=next(t for t in approved['templates']if t['name']==sym);assert raw[:284]==bytes.fromhex(entry['code_hex'])and raw[288:]==bytes(8)
   blob=tmp/(kind+'.bin');blob.write_bytes(raw);template_hashes[kind]=sha(blob)
   assembly += [f'.global {sym}',f'{sym}:',f'.incbin "{blob}",0,284',f'.global {sym}_code_end',f'{sym}_code_end:',f'.incbin "{blob}",284,4',f'.global {sym}_lease_literal',f'{sym}_lease_literal:',f'.incbin "{blob}",288,8',f'.global {sym}_end',f'{sym}_end:','.p2align 3']
  assembly+=['.section .note.GNU-stack,"",@progbits'];asm=tmp/'templates.S';asm.write_text('\n'.join(assembly)+'\n')
  def execute(directory,candidate=None):
   directory.mkdir(parents=True,exist_ok=True);exe=tmp/(directory.name+'-test')
   cmd=['g++','-std=c++20','-O1','-g','-Wall','-Wextra','-Werror','-pthread','-fno-omit-frame-pointer','-fsanitize=undefined','-fno-sanitize-recover=all','-I'+str(STUBS),'-I'+str(CPP),'-I'+str(WEATHER)]
   if candidate:cmd+=['-DHC_RUNTIME_SOURCE="'+str(candidate)+'"']
   cmd += [HERE/'runtime_test.cpp',HERE/'calls.S',asm,'-o',exe]
   built=command(cmd,directory/'compile');r=dict(compile_exit=built.returncode)
   if built.returncode:return r
   ran=command([exe,helper.DART,helper.FLUTTER],directory/'run');match=re.search(r'RESULT cases=(\d+) failed=(\d+)',ran.stdout)
   r.update(exit_code=ran.returncode,cases=int(match[1])if match else None,failed=int(match[2])if match else None,failures=re.findall(r'^FAIL (.*)$',ran.stderr,re.M),signals=re.findall(r'^SIGNAL (.*)$',ran.stderr,re.M))
   return r
  fixed=execute(out/'positive');negative=[]
  mutations=[
   ('omit_poll_lease_refresh','weather_card_runtime::service();','/* mutation: no shared poll card service */','card mode update flows through existing poll'),
   ('omit_card_only_startup','!level_selected && !material_selected && !card_selected','!level_selected && !material_selected','card-only real installer shares HWS worker'),
   ('ignore_card_residue','if (weather_card_runtime::status().phase == weather_card_runtime::Phase::Residue) {','if (false) {','card residue closes all three real components'),
   ('missing_dart_cancel','g_dart_rejected.store(true, std::memory_order_release);\n    weather_card_runtime::cancel();','g_dart_rejected.store(true, std::memory_order_release);','wrong later export cancels real card immediately'),
   ('skip_window_cancel','void on_card_snapshot_window_missed(uint32_t) noexcept {\n    weather_card_runtime::cancel();\n}','void on_card_snapshot_window_missed(uint32_t) noexcept { }','missed card window leaves other real components active'),
   ('omit_actual_installer','weather_card_runtime::install_before_snapshot(dart_base, flutter_base);','(void)dart_base; (void)flutter_base;','card-only real installer shares HWS worker'),
   ('ignore_early_lifetime_residue','if (weather_card_lifetime::has_residue()) {','if (false) {','early lifetime residue prevents all workers and overrides'),
  ]
  if fixed.get('exit_code')==0 and a.negative_controls:
   source=(WEATHER/'weather_runtime.cpp').read_text()
   for name,old,new,witness in mutations:
    assert source.count(old)==1,name
    directory=out/'negative-controls'/name;directory.mkdir(parents=True);candidate=directory/'weather_runtime.cpp';candidate.write_text(source.replace(old,new))
    r=execute(directory,candidate);r.update(name=name,expected_witness=witness,mutated_source_sha256=sha(candidate))
    r['detected']=r['compile_exit']==0 and r.get('exit_code')==1 and any(f.startswith(witness+':')for f in r.get('failures',[]));negative.append(r)
 after={str(f):sha(f)for f in files}
 passed=fixed.get('exit_code')==0 and fixed.get('failed')==0 and before==after and all(r['detected']for r in negative)
 report=dict(passed=passed,fixed=fixed,negative_controls=negative,negative_controls_requested=a.negative_controls,inputs_before=before,inputs_after=after,inputs_unchanged=before==after,
  templates=template_hashes,approved_template_reference=dict(path=str(approved_path),sha256=sha(approved_path)),rom_inputs={str(helper.DART):sha(helper.DART),str(helper.FLUTTER):sha(helper.FLUTTER)},commands=commands,
  scope=['Complete shared weather_runtime.cpp, weather_material_runtime.cpp and weather_card_runtime.cpp execute in one process; the card installer and its full production image guards are not adapters.',
   'Original level/material image identities use the established explicit adapters; their production GOT installers, wrappers and policies execute. Binder transport, clocks and selected permission faults are adapted. One real pthread worker, actual HWS decode/cache, real mmap/protection/maps/process_vm_readv/CAS execute.',
   'Actual Dart/Flutter PT_LOAD ROM bytes are mapped. Final linked A64 gateway bytes, independently reviewed and separately executed, are embedded as data; this host test does not run their A64 instructions.',
   'The early weather_card_lifetime module is an explicit state adapter here; these cases establish controller/card reactions, not real madvise installation or page lifetime. That requires separate lifetime and joint execution evidence.',
   'The first snapshot callback is invoked as a fixture prerequisite; real loader/window association, Android seccomp/SELinux, Dart VM/GC, phone injection and long-term operation are NOT_TESTED.'],device_status='NOT_TESTED')
 (out/'report.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(dict(passed=passed,fixed=fixed,negatives=len(negative),detected=sum(r['detected']for r in negative))))
 return 0 if passed else 1
if __name__=='__main__':raise SystemExit(main())
