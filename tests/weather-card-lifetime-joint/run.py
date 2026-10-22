#!/usr/bin/env python3
"""Four complete Weather runtimes, actual early madvise GOT and shared HWS cache."""
from pathlib import Path
import argparse, hashlib, importlib.util, json, re, subprocess, tempfile

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

HERE=Path(__file__).resolve().parent
SOURCE=HERE.parents[1];ROOT=test_paths.TEST_DATA_ROOT;CPP=SOURCE/'app/src/main/cpp';WEATHER=CPP/'targets/weather'
STUBS=SOURCE/'tests/hyos-control/stubs'
LIBC=test_paths.TEST_DATA_ROOT / 'acceptance/weather-card-next/page-lifetime-next/libc.so'
HELPER=SOURCE/'tests/weather-card-runtime/run.py'
spec=importlib.util.spec_from_file_location('card_transaction_runner',HELPER);helper=importlib.util.module_from_spec(spec);spec.loader.exec_module(helper)
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()

def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--output',type=Path,required=True);p.add_argument('--negative-controls',action='store_true');a=p.parse_args()
 out=a.output.resolve();out.mkdir(parents=True,exist_ok=False)
 files=[*sorted(WEATHER.glob('*.h')),
  *(WEATHER/name for name in ('weather_runtime.cpp','weather_material_runtime.cpp','weather_card_runtime.cpp','weather_card_lifetime.cpp')),
  WEATHER/'weather_card_arm64.S',HELPER,*sorted(HERE.glob('*.*')),*sorted(STUBS.rglob('*.h')),*sorted((CPP/'nativehook').glob('*.h'))]
 files += [helper.DART,helper.FLUTTER,LIBC]
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
  def execute(directory,candidate=None,macro='HC_LIFETIME_SOURCE'):
   directory.mkdir(parents=True,exist_ok=True);exe=tmp/(directory.name+'-test')
   cmd=['g++','-std=c++20','-O1','-g','-Wall','-Wextra','-Werror','-pthread','-fno-omit-frame-pointer','-fsanitize=undefined','-fno-sanitize-recover=all','-I'+str(STUBS),'-I'+str(CPP),'-I'+str(WEATHER)]
   if candidate:cmd+=['-D'+macro+'="'+str(candidate)+'"']
   cmd += [HERE/'runtime_test.cpp',HERE/'calls.S',asm,'-o',exe]
   built=command(cmd,directory/'compile');r=dict(compile_exit=built.returncode)
   if built.returncode:return r
   ran=command([exe,helper.DART,helper.FLUTTER,LIBC],directory/'run');match=re.search(r'RESULT cases=(\d+) failed=(\d+)',ran.stdout)
   r.update(exit_code=ran.returncode,cases=int(match[1])if match else None,failed=int(match[2])if match else None,failures=re.findall(r'^FAIL (.*)$',ran.stderr,re.M),signals=re.findall(r'^SIGNAL (.*)$',ran.stderr,re.M))
   return r
  fixed=execute(out/'positive');negative=[]
  mutations=[
   ('omit_protected_page_filter','weather_card_lifetime.cpp','HC_LIFETIME_SOURCE',
    'if (!(previous & 1) || advice != MADV_DONTNEED',
    'if ((static_cast<void>(previous), true) || advice != MADV_DONTNEED',
    'actual hooked import skips each protected epilogue page'),
   ('skip_single_thread_admission','weather_card_lifetime.cpp','HC_LIFETIME_SOURCE',
    '|| !single_thread() || !images()', '|| false || !images()',
    'specialization with second real thread declines card'),
   ('allow_grandchild_capture','weather_card_lifetime.cpp','HC_LIFETIME_SOURCE',
    '|| call(SYS_getppid) != g_image.parent_pid', '',
    'grandchild cannot reuse a different parent capture'),
   ('omit_reader_exclusion','weather_card_lifetime.cpp','HC_LIFETIME_SOURCE',
    'return g_readers.compare_exchange_strong(expected, 1, std::memory_order_acq_rel, std::memory_order_acquire);',
    '(void)expected; g_readers.fetch_or(1, std::memory_order_acq_rel); return true;',
    'busy original reader prevents both code patches'),
   ('release_while_patched','weather_card_lifetime.cpp','HC_LIFETIME_SOURCE',
    'if (!read(site, word) || word != guard::kOriginalWord) return false;',
    'if (!read(site, word)) return false;',
    'repeated startup cannot add a second worker or renew stale lease'),
   ('omit_slot_ownership_recheck','weather_card_lifetime.cpp','HC_LIFETIME_SOURCE',
    '&& slot_is(wrapper())', '',
    'foreign observer removal terminally closes card on poll'),
   ('ignore_lifetime_residue','weather_runtime.cpp','HC_RUNTIME_SOURCE',
    'if (weather_card_lifetime::has_residue()) {','if (false) {',
    'early permission residue stops all consumers and worker'),
   ('omit_lifetime_before_card_patch','weather_card_runtime.cpp','HC_CARD_RUNTIME_SOURCE',
    'if (!weather_card_lifetime::protect(dart_base, flutter_base, g_binding.epilogue))',
    'if (false)',
    'real parent capture fork admission and three feature installation'),
  ]
  if fixed.get('exit_code')==0 and a.negative_controls:
   for name,filename,macro,old,new,witness in mutations:
    source=(WEATHER/filename).read_text();assert source.count(old)==1,(name,source.count(old))
    # Preserve the reference to keep the negative about runtime logic, not Werror.
    if name=='skip_single_thread_admission':new='|| (static_cast<void>(&single_thread), false) || !images()'
    directory=out/'negative-controls'/name;directory.mkdir(parents=True);candidate=directory/filename;candidate.write_text(source.replace(old,new))
    r=execute(directory,candidate,macro);r.update(name=name,expected_witness=witness,mutated_source_sha256=sha(candidate))
    r['detected']=r['compile_exit']==0 and r.get('exit_code')==1 and any(f.startswith(witness+':')for f in r.get('failures',[]));negative.append(r)
 after={str(f):sha(f)for f in files}
 passed=fixed.get('exit_code')==0 and fixed.get('failed')==0 and before==after and all(r['detected']for r in negative)
 report=dict(passed=passed,fixed=fixed,negative_controls=negative,negative_controls_requested=a.negative_controls,inputs_before=before,inputs_after=after,inputs_unchanged=before==after,
  templates=template_hashes,approved_template_reference=dict(path=str(approved_path),sha256=sha(approved_path)),rom_inputs={str(helper.DART):sha(helper.DART),str(helper.FLUTTER):sha(helper.FLUTTER),str(LIBC):sha(LIBC)},commands=commands,
  scope=['All four complete production CPPs execute: early lifetime observer, card installer, material runtime and shared Weather controller; no lifetime state or GOT installer adapter.',
   'Parent read-only capture occurs before real fork. Child single-thread /proc/self/task, real pid/tid/ppid, process_vm_readv, mmap/mprotect, slot and code-word CAS execute. Parent state is rechecked unchanged after all children.',
   'Actual pinned Dart, Flutter and Android libc PT_LOAD bytes are anonymous mappings. The maps adapter changes only the first Flutter mapping pathname; actual addresses/permissions remain from the host procfs. Selected permission failures and original-return blocking are explicit fixtures.',
   'Calls through the actual changed Flutter GOT invoke the production observer. Its pinned A64 libc madvise forward address is checked but executed by host madvise, so real unprotected pages are discarded while the two code pages survive. This does not execute Flutter original A64 release function; that is separate selectedarm evidence.',
   'Level/material image identities, Binder transport and BOOTTIME use explicit established adapters. Their actual installers, wrappers/policies and shared cache execute. The single worker is a real pthread.',
   'Linked full A64 gateway templates are embedded as data and compared to independently executed compiled-v2 code. Host tests do not run those A64 gateway instructions.',
   'The verified first-snapshot callback is invoked by fixture; actual loader association, Android seccomp/SELinux, VM/GC and phone stability remain NOT_TESTED.'],device_status='NOT_TESTED')
 (out/'report.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(dict(passed=passed,fixed=fixed,negatives=len(negative),detected=sum(r['detected']for r in negative))))
 return 0 if passed else 1
if __name__=='__main__':raise SystemExit(main())
