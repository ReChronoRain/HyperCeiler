#!/usr/bin/env python3
"""Execute actual production templates copied to RX pages, not a device/VM test."""
import argparse,hashlib,json,os,resource,subprocess
from pathlib import Path

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

HERE=Path(__file__).resolve().parent
ROOT=test_paths.TEST_DATA_ROOT
ARM=test_paths.BUILD_CACHE / 'arm64-emulation/root'
NDK=test_paths.ANDROID_NDK_HOME / 'toolchains/llvm/prebuilt/linux-x86_64/bin'
SHA='5b458c7a5a34bc64336b4895229f3a04e8a21edafd8c8b2c6d7926258f90b7fc'
EXPECTED={'passed':14392,'failed':0,'original_cases':720,'original_abi_cases':16,'guarded_original_cases':2016,'guarded_abi_cases':84,'device_tested':False}
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
 resource.setrlimit(resource.RLIMIT_CORE,(0,0))
 p=argparse.ArgumentParser();p.add_argument('--output',type=Path,required=True);p.add_argument('--negative-controls',action='store_true');args=p.parse_args()
 out=args.output.resolve();out.mkdir(parents=True,exist_ok=False)
 asm=test_paths.SOURCE_ROOT / 'app/src/main/cpp/targets/weather/weather_card_arm64.S';header=asm.with_name('weather_card_gateway.h')
 rom=test_paths.TEST_DATA_ROOT / 'acceptance/native-evidence/weather-libapp.so';assert sha(rom)==SHA
 inputs={str(f):sha(f)for f in [asm,header,rom,HERE/'run.py',HERE/'gateway_harness.cpp',HERE/'adapters.S',HERE/'ancestry.json']}
 for name,h in json.loads((HERE/'ancestry.json').read_text())['sources'].items():assert sha(ROOT/name)==h
 env=os.environ.copy();env['LD_LIBRARY_PATH']=str(ARM/'usr/lib/x86_64-linux-gnu')
 commands=[];results=[]
 def command(cmd,log):
  commands.append([str(x)for x in cmd]);r=subprocess.run([str(x)for x in cmd],env=env,capture_output=True,text=True)
  (out/log).write_text(r.stdout+r.stderr);return r
 def run(label,path=asm,flags=(),negative=False,include=None):
  cmd=[ARM/'usr/bin/aarch64-linux-gnu-g++-13','--sysroot='+str(ARM),'-O2','-std=c++20','-Wall','-Wextra','-Werror']
  if include:cmd+=['-I'+str(include)]
  cmd+=['-I'+str(test_paths.SOURCE_ROOT / 'app/src/main/cpp'),*flags,HERE/'gateway_harness.cpp',HERE/'adapters.S',path,'-o',out/label]
  c=command(cmd,label+'-build.txt');assert c.returncode==0,(label,c.stderr)
  r=command([ARM/'usr/bin/qemu-aarch64-static','-L',ARM/'usr/aarch64-linux-gnu',out/label,rom],label+'-run.txt')
  summary=json.loads(r.stdout.splitlines()[-1])if r.stdout.splitlines()else None
  if negative:
   assert (r.returncode==1 and summary and summary['failed']>0)or(r.returncode==-11 and 'signal 11'in r.stderr),(label,r.returncode,r.stdout,r.stderr)
  else:assert r.returncode==0 and summary==EXPECTED,(label,r.returncode,summary,r.stderr)
  results.append({'label':label,'negative':negative,'returncode':r.returncode,'summary':summary,'binary_sha256':sha(out/label),'stderr':r.stderr})
  print(json.dumps(results[-1]),flush=True)
 run('normal')
 clock=out/'clock-denied.S';clock.write_text(asm.read_text().replace('mov x8,#113','mov x8,#4095'))
 run('clock-denied',clock,('-DEXPECT_CLOCK_FAILURE',))
 if args.negative_controls:
  source=asm.read_text()
  muts=[
   ('accept-expired',' cmp x9,x10\n b.hs .Ldone_\\name',' cmp x9,x10\n nop'),
   ('accept-wrong-class',' cmp x9,#254\n b.ne .Ldone_\\name',' cmp x9,#254\n nop'),
   ('write-neighbor',' stur x17,[x16,#0x8b]',' stur x17,[x16,#0x83]'),
   ('corrupt-register',' ldp x11,x12,[x15,#48]',' ldp x11,x12,[x15,#48]\n mov x12,#0'),
   ('lose-flags','.Ldone_\\name:\n ldr x16,[x15,#64]\n msr nzcv,x16','.Ldone_\\name:\n ldr x16,[x15,#64]\n nop'),
   ('constructor-wrong-frame','card_epilogue hc_weather_card_constructor_template, -8','card_epilogue hc_weather_card_constructor_template, -16'),
   ('read-saved-limit',' ldr x17,[x26,#0x48]',' ldr x17,[x26,#0x678]'),
   ('omit-headroom-check',' b.ls .Lno_stack_\\name // equality is conservatively rejected',' nop'),
   ('permit-equal-96',' b.ls .Lno_stack_\\name // equality is conservatively rejected',' b.lo .Lno_stack_\\name'),
   ('lose-rejected-flags','.Lno_stack_\\name:\n msr nzcv,x16','.Lno_stack_\\name:\n nop'),
   ('lose-admitted-flags',' msr nzcv,x16\n sub x15,x15,#96',' nop\n sub x15,x15,#96'),
   ('accept-zero-limit',' cbz x17,.Lno_stack_\\name',' nop'),
   ('allow-subtraction-wrap',' b.ls .Lno_stack_\\name // no subtraction underflow',' nop'),
   ('literal-pc-off-by-eight',' ldr x17,\\name\\()_lease_literal',' ldr x17,\\name\\()_lease_literal+8'),
  ]
  for label,old,new in muts:
   assert source.count(old)==(2 if label=='literal-pc-off-by-eight'else 1),(label,source.count(old))
   changed=source.replace(old,new)
   # Replace a single instruction; retain all production size assertions.
   if label=='corrupt-register':
    changed=source.replace(' ldp x11,x12,[x15,#48]',' mov x12,#0')
   path=out/(label+'.S');path.write_text(changed);run(label,path,negative=True)
  changed=header.read_text().replace('(lease_address & 7) != 0','false')
  override=out/'bad-header/targets/weather';override.mkdir(parents=True)
  (override/'weather_card_gateway.h').write_text(changed)
  run('accept-unaligned-lease',include=out/'bad-header',negative=True)
 # Actual Android shared ELF, with no external data relocation for templates.
 shared=out/'libweather-card-template.so'
 c=command([NDK/'aarch64-linux-android35-clang','-shared','-nostdlib','-fPIC',asm,'-o',shared],'android-shared-build.txt');assert c.returncode==0,c.stderr
 c=command([NDK/'llvm-readelf','-r',shared],'android-shared-relocations.txt');assert c.returncode==0 and 'There are no relocations'in c.stdout,c.stdout
 c=command([NDK/'llvm-nm','--print-size',shared],'android-shared-symbols.txt');assert c.returncode==0
 assert all(sha(Path(f))==h for f,h in inputs.items())
 report={'passed':True,'scope':'Actual copied production templates and exact ROM constructor/tails with explicit dependency adapters; no installer, phone or real Dart VM','device_status':'NOT_TESTED','inputs':inputs,'inputs_unchanged':True,'results':results,'positive_assertions_per_run':EXPECTED['passed'],'negative_controls_rejected':sum(x['negative']for x in results),'android_shared_elf_sha256':sha(shared),'android_shared_no_relocations':True,'commands':commands,'limitations':['SVC ENOSYS fallback does not establish Android vendor seccomp TRAP/KILL safety.','Thread/frame/objects/pool and helper callees are controlled adapters.','Deadline and lease reads are sampled and cannot guarantee instantaneous revoke, same-value ABA exclusion or asynchronous GC/safepoint semantics.','The fixture copies templates to independent RX pages and replaces only first original epilogue words, but does not implement a production installer or prove near-page placement on a phone.']}
 (out/'report.json').write_text(json.dumps(report,indent=2)+'\n')
if __name__=='__main__':main()
