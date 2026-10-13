#!/usr/bin/env python3
"""Execute unmodified complete OTA Dart placement/constructor bodies under QEMU.
Only the external services, allocation, logging, and policy edges are adapters.
"""
import argparse,hashlib,json,os,subprocess,sys,time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

sys.dont_write_bytecode=True
ROOT=test_paths.TEST_DATA_ROOT;HERE=Path(__file__).resolve().parent
ARM=test_paths.BUILD_CACHE / 'arm64-emulation/root'
CC=ARM/'usr/bin/aarch64-linux-gnu-g++-13';QEMU=ARM/'usr/bin/qemu-aarch64-static'
ROM=test_paths.ROM_ROOT / 'launcher-native/lib/arm64-v8a/libapp.so'
FUNCTIONS=[0x8dd2dc,0x8991e8,0x898fa0,0x898c2c,0xaa8cf4,0x8af730,0x8b062c,0xaa9118,0xaa9194,0xaa9444,0x8f8990]
MUTANTS={'ignore-x-bound':'actual occupancy rejects overwide or overtall fallback','force-no-fallback':'actual force flag returns fallback even empty region list','skip-maml-clamp':'original MAML ctor uses phone span policy','lose-original-span':'original MAML original spans retained independent of display span'}
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
 ap=argparse.ArgumentParser();ap.add_argument('--output',type=Path,required=True);a=ap.parse_args();out=a.output.resolve();out.mkdir(parents=True,exist_ok=False)
 inputs=[*sorted(HERE.glob('*.py')),*sorted(HERE.glob('*.cpp')),*sorted(HERE.glob('*.S')),*sorted(HERE.glob('*.h')),test_paths.SOURCE_ROOT / 'tests/hyos-entry/extract_runtime_guard.py',test_paths.SOURCE_ROOT / 'tests/home-layout-native/fixture_symbols.py',ROM,CC,QEMU]
 before={str(p):sha(p)for p in inputs};report={'passed':False,'device_status':'NOT_TESTED','inputs_before':before,'commands':[],'checks':[],'mutations':[]}
 env=dict(os.environ,LD_LIBRARY_PATH=str(ARM/'usr/lib/x86_64-linux-gnu'),PYTHONDONTWRITEBYTECODE='1')
 def run(cmd,name,timeout=90):
  row={'argv':[str(x)for x in cmd],'environment':{'LD_LIBRARY_PATH':env['LD_LIBRARY_PATH'],'PYTHONDONTWRITEBYTECODE':'1'}};report['commands'].append(row);t=time.monotonic();r=subprocess.run(row['argv'],cwd=ROOT,env=env,capture_output=True,text=True,timeout=timeout);row.update(exit_code=r.returncode,seconds=time.monotonic()-t);(out/(name+'.log')).write_text(r.stdout+r.stderr);return r
 def check(name,v):report['checks'].append({'name':name,'passed':v is True});assert v,name
 try:
  r=run([sys.executable,'-B',HERE/'extract.py','--kind','dart','--output',out/'extraction',*[hex(v)for v in FUNCTIONS]],'extract');check('fresh exact complete original-body extraction',r.returncode==0)
  ext=json.loads((out/'extraction/report.json').read_text());report['executed_original_full_functions']=ext['functions'];report['extraction_sha256']=sha(out/'extraction/report.json')
  binary=out/'placement_test';r=run([CC,'--sysroot='+str(ARM),'-std=c++20','-O2','-Wall','-Wextra','-Werror',HERE/'placement_test.cpp',HERE/'placement_probe.S','-o',binary],'build');check('ARM64 fixture builds with warnings as errors',r.returncode==0);report['fixture_binary_sha256']=sha(binary)
  command=[QEMU,'-L',ARM/'usr/aarch64-linux-gnu',binary,ROM];r=run(command,'positive');check('original ARM bodies execute successfully',r.returncode==0)
  lines=r.stdout.splitlines();summary=json.loads(lines[-1]);report['execution']=summary;check('execution is explicitly not a physical device test',summary['passed'] is True and summary['device_status']=='NOT_TESTED')
  traces={k:[json.loads(s[len(k)+1:])for s in lines if s.startswith(k+' ')]for k in ['TRACE','MAML','FAVORITE']};report['traces']=traces
  check('all bounded geometry scenarios ran',len(traces['TRACE'])==384 and len(traces['MAML'])==96 and len(traces['FAVORITE'])==2 and summary['cases']==482)
  patches=[json.loads(s[8:])for s in lines if s.startswith('PATCHES ')];check('one explicit adapter patch journal',len(patches)==1);report['private_fixture_patches']=patches[0]
  for f in ext['functions']:
   va=int(f['va'],16);size=f['size'];check('full original body unmodified '+f['va'],all(at+n<=va or at>=va+size for at,n in patches[0]))
  for name,witness in MUTANTS.items():
   r=run(command+[name],name);valid=r.returncode==3 and 'FAILED' in r.stderr and witness in r.stderr;report['mutations'].append({'name':name,'rejected':valid,'exit_code':r.returncode,'witness':r.stderr.strip(),'log_sha256':sha(out/(name+'.log'))});check('executed instruction mutation rejected '+name,valid)
  after={str(p):sha(p)for p in inputs};report['inputs_after']=after;check('actual ROM fixture and tool bytes stable',before==after)
  report['limits']=[
   'Private original ARM64 mappings under QEMU Linux. No Dart VM/GC, Android process, LSPosed injection, SQLite, XML parser, or physical device execution.',
   'Dart compressed objects are synthetic layouts derived from the executed instructions. Allocation/context cloning, service lookup, string formatting/logging and resource full-screen allowlist are explicit adapters. The external drop-rule predicate is neutral accept-all; original isCellOccupied still executes its exact grid bounds and record collision loops.',
   'Constructor→placement→position-update calls are explicitly composed by the fixture; actual async flush/new-screen/DB batch scheduling was only statically read, not executed.',
   'MAML threshold values 3/4/5 and full-screen allowlist both outcomes are explicit inputs. This report does not alone establish which current device/static config was published in a live process.',
   'Original input ItemInfo/id/span preservation is an in-memory result, not durable database persistence or artwork visual-scale proof.',
   'The deliberate raw oversized geometry cases precede MAML constructor normalization and must not be reported as failures of the normal default MAML path.']
  report['passed']=all(x['passed']for x in report['checks'])
 except Exception as e:report['error']=repr(e);raise
 finally:(out/'report.json').write_text(json.dumps(report,indent=2)+'\n')
 print(json.dumps({'passed':report['passed'],'checks':len(report['checks']),'execution':report.get('execution'),'mutations':len(report['mutations']),'report_sha256':sha(out/'report.json')}))
if __name__=='__main__':main()
