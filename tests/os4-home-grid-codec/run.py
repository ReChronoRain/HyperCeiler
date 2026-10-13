#!/usr/bin/env python3
"""Compile and execute private copies of the OTA ARM64 DeviceParam codec.
No translated target implementation, production edits, Android install or flashing.
"""
import argparse,hashlib,json,os,subprocess,sys,time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

sys.dont_write_bytecode=True
ROOT=test_paths.TEST_DATA_ROOT
HERE=Path(__file__).resolve().parent
ARM=test_paths.BUILD_CACHE / 'arm64-emulation/root'
CC=ARM/'usr/bin/aarch64-linux-gnu-g++-13'
QEMU=ARM/'usr/bin/qemu-aarch64-static'
ROM=test_paths.ROM_ROOT / 'launcher-native/lib/arm64-v8a'
EXECUTED={'dart':[0x8bb7bc,0x8bb864,0x86ea54,0x86ead4], 'rust':[0x7ed3b8,0x84152c,0x965f98,0x965ffc,0xaa40f8,0x6c46e0,0x6c567c,0x6c44d8,0x6c4584,0x9aa6f8]}
MUTANTS={'constant-two':'writes X Y literal4 Ymin', 'wrong-y-field':'writes X Y literal4 Ymin','raw-payload-shift':'writes X Y literal4 Ymin','skip-byte-copy':'writes X Y literal4 Ymin','decoder-no-advance':'valid full dispatcher/FRB must not panic','save-skip-copy':'original save and getter expose full decoded DeviceParam','decoder-skip-truncation':'truncated decoder reaches panic adapter','end-ignore-length':'full FRB rejects trailing declared data'}
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
 p=argparse.ArgumentParser();p.add_argument('--output',type=Path,required=True);a=p.parse_args();out=a.output.resolve();out.mkdir(parents=True,exist_ok=False)
 inputs=[*sorted(HERE.glob('*.py')),*sorted(HERE.glob('*.cpp')),*sorted(HERE.glob('*.S')),test_paths.SOURCE_ROOT / 'tests/hyos-entry/extract_runtime_guard.py',test_paths.SOURCE_ROOT / 'tests/home-layout-native/fixture_symbols.py',test_paths.SOURCE_ROOT / 'library/core/src/main/res/xml/home_layout.xml',ROM/'libapp.so',ROM/'libapp_launcher.so',CC,QEMU]
 before={str(p):sha(p)for p in inputs};report={'passed':False,'device_status':'NOT_TESTED','inputs_before':before,'commands':[],'checks':[],'mutations':[]}
 env=dict(os.environ,LD_LIBRARY_PATH=str(ARM/'usr/lib/x86_64-linux-gnu'),PYTHONDONTWRITEBYTECODE='1')
 def run(cmd,name,timeout=45):
  record={'command':[str(v)for v in cmd],'environment_overrides':{'LD_LIBRARY_PATH':env['LD_LIBRARY_PATH'],'PYTHONDONTWRITEBYTECODE':'1'}};report['commands'].append(record);start=time.monotonic();r=subprocess.run(record['command'],cwd=ROOT,env=env,capture_output=True,text=True,timeout=timeout);record.update(exit_code=r.returncode,seconds=time.monotonic()-start);(out/(name+'.log')).write_text(r.stdout+r.stderr);return r
 def check(name,value):report['checks'].append({'name':name,'passed':value is True});assert value,name
 try:
  r=run([sys.executable,'-B',HERE/'extract.py','--output',out/'extraction'],'extract');check('fresh full ROM extraction succeeded',r.returncode==0)
  ext=json.loads((out/'extraction/report.json').read_text());report['extraction_sha256']=sha(out/'extraction/report.json')
  full=[]
  for kind,vas in EXECUTED.items():
   for va in vas:
    f=ext['functions'][f'{kind}-{va:x}'];full.append({'image':kind,**f})
  report['executed_original_full_functions']=full
  binary=out/'codec_test';r=run([CC,'--sysroot='+str(ARM),'-std=c++20','-O2','-Wall','-Wextra','-Werror',HERE/'codec_test.cpp',HERE/'codec_probe.S','-o',binary],'build');check('actual ARM64 fixture compiled with warnings as errors',r.returncode==0);report['fixture_binary_sha256']=sha(binary)
  command=[QEMU,'-L',ARM/'usr/aarch64-linux-gnu',binary,ROM/'libapp.so',ROM/'libapp_launcher.so']
  r=run(command,'positive');check('original private ARM execution exits successfully',r.returncode==0);lines=r.stdout.splitlines();actual=json.loads(lines[-1]);check('actual executable reports success and no physical-device result',actual['passed'] is True and actual['device_status']=='NOT_TESTED');report['execution']=actual
  report['tuple_traces']=[json.loads(s[6:])for s in lines if s.startswith('TRACE ')];check('ten tuples cover defaults UI endpoints sentinels and integer boundaries',len(report['tuple_traces'])==10)
  patches=[json.loads(s[8:])for s in lines if s.startswith('PATCHES ')];report['private_fixture_patches']=patches
  for kind in EXECUTED:
   rows=[v for v in patches if v['image']==kind];check(kind+' has one explicit patch journal',len(rows)==1)
   for f in [v for v in full if v['image']==kind]:
    va=int(f['va'],16);size=f['size'];check(kind+' '+f['va']+' full original body excludes every fixture adapter',all(at+n<=va or at>=va+size for at,n in rows[0]['ranges']))
  for name,witness in MUTANTS.items():
   r=run(command+[name],name);rejected=r.returncode==3 and 'FAILED:' in r.stderr and witness in r.stderr;report['mutations'].append({'name':name,'rejected':rejected,'exit_code':r.returncode,'witness':r.stderr.strip(),'log_sha256':sha(out/(name+'.log'))});check('executed original-image mutation rejected: '+name,rejected)
  after={str(p):sha(p)for p in inputs};report['inputs_after']=after;check('all fixture tools and actual ROM inputs unchanged',before==after)
  report['limits']=['Private QEMU Linux ARM64 mapping; no Dart VM, GC, Android/LSPosed injection, database migration or real-device execution.','Writer allocation, outgoing pdeCallFfi transport, TaskInfo formatting, boost-rule lookup, queue/allocation/free and panic edges are explicit adapters. The normal original complete codec/dispatch bodies execute unchanged.','Queue capture is followed by a fixture call to original save_device_param; no claim that the original async model task or its database/preview scheduling was executed.','Synthetic tagged objects implement only verified accessed layouts. Smi/Mint variants exercise the addAll count metadata; DeviceParam fields passed to putInt32 are raw unboxed integers.','The generated packet contains no mode, database pool, revision or authentication identity; this experiment cannot authorize removing PhoneGrid unsupported masks.','Panic adapters use host longjmp at explicit external boundaries; they prove which original error edge was selected, not production exception/unwinding behavior.']
  report['passed']=all(c['passed']for c in report['checks'])
 except Exception as e:report['error']=repr(e);raise
 finally:(out/'report.json').write_text(json.dumps(report,indent=2)+'\n')
 print(json.dumps({'passed':report['passed'],'checks':len(report['checks']),'execution':report.get('execution'),'negative_controls':len(report['mutations']),'report_sha256':sha(out/'report.json')}))
if __name__=='__main__':main()
