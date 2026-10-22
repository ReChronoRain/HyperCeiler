#!/usr/bin/env python3
"""Original Home7719 ARM flush/copy/batch with explicit ready-Future/collection edges.
The final FRB producer boundary is captured; this is not SQLite persistence.
"""
import argparse,hashlib,json,os,resource,subprocess,sys,time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

sys.dont_write_bytecode=True
ROOT=test_paths.TEST_DATA_ROOT;HERE=Path(__file__).resolve().parent
ARM=test_paths.BUILD_CACHE / 'arm64-emulation/root'
CC=ARM/'usr/bin/aarch64-linux-gnu-g++-13';QEMU=ARM/'usr/bin/qemu-aarch64-static'
ROM=test_paths.ROM_ROOT / 'launcher-native/lib/arm64-v8a/libapp.so'
FUNCTIONS=[0x8dd2dc,0x8991e8,0x898fa0,0x898c2c,0xaa8cf4,0x8af730,0x8b062c,0xaa9118,0xaa9194,0xaa9444,0x8f8990,
 0x19553c8,0x19559f4,0xaef6b0,0xaef838,0xaeebd0,0x8e0b24,0x8f6f24,0xa8cd30,0xa8dbf8,0x8fda88,
 0x99a79c,0x18525f8,0x1852c34,0x18523e8,0x185213c,0x8d4658,0x8d470c]
MUTANTS={
 'skip-batch':'original flush reaches one real batch producer with one item',
 'skip-position-x':'batch coordinates equal original placement result',
 'lose-id':'batch preserves original identity',
 'lose-maml-original-span':'submitted MAML original span4x2 retained',
 'lose-maml-display-span':'submitted MAML display span3x2',
 'drop-nonstack':'original flush reaches one real batch producer with one item'}
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
 resource.setrlimit(resource.RLIMIT_CORE,(0,0))
 ap=argparse.ArgumentParser();ap.add_argument('--output',type=Path,required=True);a=ap.parse_args();out=a.output.resolve();out.mkdir(parents=True,exist_ok=False)
 inputs=[*sorted(p for p in HERE.iterdir()if p.suffix in ('.py','.cpp','.h','.S')),
 test_paths.SOURCE_ROOT / 'tests/hyos-entry/extract_runtime_guard.py',test_paths.SOURCE_ROOT / 'tests/home-layout-native/fixture_symbols.py',ROM,CC,QEMU,
 test_paths.TEST_DATA_ROOT / 'acceptance/weather-port-next/dart-consumer/official-dart-3.10.1/runtime_vm_class_id.h',test_paths.TEST_DATA_ROOT / 'acceptance/weather-port-next/dart-consumer/official-dart-3.10.1/runtime_vm_object_store.h']
 before={str(p):sha(p)for p in inputs};report={'passed':False,'kind':'ORIGINAL_ARM_DEFAULT_INVALID_POSITION_FLUSH','device_status':'NOT_TESTED','inputs_before':before,'commands':[],'checks':[],'mutations':[]}
 env=dict(os.environ,LD_LIBRARY_PATH=str(ARM/'usr/lib/x86_64-linux-gnu'),PYTHONDONTWRITEBYTECODE='1');env.pop('TRACE_ADAPTER',None)
 def run(cmd,name,timeout=180):
  row={'argv':[str(x)for x in cmd],'environment':{'LD_LIBRARY_PATH':env['LD_LIBRARY_PATH'],'PYTHONDONTWRITEBYTECODE':'1'}};report['commands'].append(row);t=time.monotonic();r=subprocess.run(row['argv'],cwd=ROOT,env=env,capture_output=True,text=True,timeout=timeout);row.update(exit_code=r.returncode,seconds=time.monotonic()-t);(out/(name+'.log')).write_text(r.stdout+r.stderr);row['log_sha256']=sha(out/(name+'.log'));return r
 def check(name,v):report['checks'].append({'name':name,'passed':v is True});assert v,name
 try:
  r=run([sys.executable,'-B',HERE/'prepare.py','--output',out/'snapshot-bindings'],'snapshot-bindings');check('fresh complete snapshot parser resolves four actual closures and two copy dispatches',r.returncode==0)
  bindings=json.loads((out/'snapshot-bindings/report.json').read_text());check('snapshot parse reaches exact declared end',bindings['passed'] is True and bindings['declared_and_parsed_end']=='0x62d0d8');report['snapshot_bindings_sha256']=sha(out/'snapshot-bindings/report.json')
  r=run([sys.executable,'-B',HERE/'extract.py','--kind','dart','--output',out/'extraction',*[hex(v)for v in FUNCTIONS]],'extract');check('fresh exact complete original-body extraction',r.returncode==0)
  ext=json.loads((out/'extraction/report.json').read_text());report['executed_original_full_functions']=ext['functions'];report['extraction_sha256']=sha(out/'extraction/report.json')
  binary=out/'flush_test';r=run([CC,'--sysroot='+str(ARM),'-std=c++20','-O2','-Wall','-Wextra','-Werror',HERE/'flush_test.cpp',HERE/'placement_probe.S','-o',binary],'build');check('ARM64 fixture builds with warnings as errors',r.returncode==0);report['fixture_binary_sha256']=sha(binary)
  command=[QEMU,'-L',ARM/'usr/aarch64-linux-gnu',binary,ROM];r=run(command,'positive');check('original ARM bodies execute successfully',r.returncode==0)
  lines=r.stdout.splitlines();summary=json.loads(lines[-1]);report['execution']=summary;check('execution explicitly not a physical device test',summary['passed'] is True and summary['device_status']=='NOT_TESTED')
  traces=[json.loads(s[6:])for s in lines if s.startswith('BATCH ')];report['submitted_records']=traces
  expected={(rows,rtl,scenario,ident)for rows in (4,13)for rtl in (0,1)for scenario in (0,1,2)for ident in ((101 if scenario else 201),(0x100000000+(101 if scenario else 201)))}
  check('all 24 distinct intended scenarios reached the original batch producer',len(traces)==24 and {(t['rows'],t['rtl'],t['scenario'],t['id'])for t in traces}==expected and summary['cases']==24)
  check('all captured display spans and original spans remain distinct',all((t['span_x'],t['span_y'],t['original_span'])==((3,2,[4,2])if t['maml'] else (1,1,None)) for t in traces))
  patches=[json.loads(s[8:])for s in lines if s.startswith('PATCHES ')];check('one explicit private adapter patch journal',len(patches)==1);report['private_fixture_patches']=patches[0]
  for f in ext['functions']:
   va=int(f['va'],16);size=f['size'];check('full original body unmodified '+f['va'],all(at+n<=va or at>=va+size for at,n in patches[0]))
  counts=[json.loads(s[15:])for s in lines if s.startswith('ADAPTER_COUNTS ')];check('one actual adapter invocation summary',len(counts)==1);report['adapter_calls']=counts[0]
  check('actual original batch caller reaches capture once per scenario',counts[0].get('31')==24)
  for name,witness in MUTANTS.items():
   r=run(command+[name],name);valid=r.returncode==3 and 'FAILED' in r.stderr and witness in r.stderr;report['mutations'].append({'name':name,'rejected':valid,'exit_code':r.returncode,'witness':r.stderr.strip(),'log_sha256':sha(out/(name+'.log'))});check('executed original instruction mutation rejected '+name,valid)
  after={str(p):sha(p)for p in inputs};report['inputs_after']=after;check('actual ROM fixture and tool bytes stable',before==after)
  report['limits']=[
   'Private original ARM64 mappings under QEMU Linux, synthetic compressed Dart objects. No Dart VM/GC, Android process, LSPosed, SQLite or physical device execution.',
   '28 selected complete function extents are retained. Executing a path through a complete body is not branch coverage: sorting uses one item, occupancy rebuilding takes the original inactive-controller branch, non-stack predicate takes its original non-stack path, and no new-screen creation branch is taken.',
   'Ready-Future AsyncStart/Await/AsyncReturn are explicit adapters without suspension or scheduling interleave. Generic allocations, list/map/set operations and log/service edges are adapters, not translated placement geometry. Four executed closure targets and two model-copy targets are verified from the actual complete snapshot pool/dispatch.',
   'Exactly one pending record per invocation and one existing screen. Multi-record live occupancy rebuilding, sorting, new screens, stack/folder types and active UI subscriptions remain outside this experiment. Post-batch UI notification iterator has no subscribers.',
   'MAML constructor receives resource span4x2 with typed cols3/rows4-or13 and min4; the original constructor computes display3x2. Favorite starts at old x3. Scenario2 explicitly supplies a collision; scenario1 deliberately queues the MAML on an empty screen and does not prove the original classifier naturally chooses it.',
   'Fixture explicitly supplies classifier pending records and typed model wrappers. Original flush, placement, coordinate update, DTO-copy and both batch wrapper bodies run; capture replaces the next FRB producer8d4790. No FRB encoding, Rust queue, SQLite commit, XML parser or asynchronous classifier producer is executed.',
   'Bundle storage uses an external dictionary adapter; original copy writes actual verified key objects for mamlConfigSpanX/Y and transfers the Bundle through DTO+b -> wrapper+1b. DTO+77 is a different source-model field, not the Bundle.',
   'The external drop-rule policy accepts all. Original grid bounds/collision loops still run; tests do not claim every actual MAML/plugin policy, resource metadata or artwork rendering behavior.']
  report['passed']=all(x['passed']for x in report['checks'])
 except Exception as e:report['error']=repr(e);raise
 finally:(out/'report.json').write_text(json.dumps(report,indent=2)+'\n')
 print(json.dumps({'passed':report['passed'],'checks':len(report['checks']),'execution':report.get('execution'),'mutations':len(report['mutations']),'report_sha256':sha(out/'report.json')}))
if __name__=='__main__':main()
