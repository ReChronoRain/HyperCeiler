#!/usr/bin/env python3
"""Actual ARM production-source counterfactuals against the same Android fixture.
Each mutant is separately compiled, installed only into the dedicated probe,
and must produce an explicit failed assertion/result (not a compiler failure).
"""
from pathlib import Path
import argparse,hashlib,json,os,subprocess,time,zipfile

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

HERE=Path(__file__).resolve().parent;ROOT=test_paths.TEST_DATA_ROOT;SDK=test_paths.ANDROID_HOME;BIN=test_paths.ANDROID_NDK_HOME / 'toolchains/llvm/prebuilt/linux-x86_64/bin';BT=SDK/'build-tools/37.0.0';ADB=SDK/'platform-tools/adb';JAVA=test_paths.JAVA_HOME;CORE=test_paths.SOURCE_ROOT / 'app/src/main/cpp/targets/systemui';PKG='local.hyperceiler.colorprobe';sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
p=argparse.ArgumentParser();p.add_argument('--baseline',type=Path,required=True);p.add_argument('--build',type=Path,required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args();a.build.mkdir(parents=True,exist_ok=False);a.output.mkdir(parents=True,exist_ok=False)
base=json.loads(a.baseline.read_text());assert base['passed'] and len(base['execution'])==24;apk=Path(base['apk']['path']);assert sha(apk)==base['apk']['sha256'];source=(CORE/'ambient_color_os4.cpp').read_text()
mutants=[
 ('global_algorithm_interposition',1,'if(dlsym(RTLD_DEFAULT,binding.name)!=nullptr)return fail(Stage::GlobalSymbolVisible);','(void)dlsym(RTLD_DEFAULT,binding.name);'),
 ('foreign_self_got',5,'if(value!=private_base+binding.target)return false;','(void)value;'),
 ('constructor_configuration',7,'if(tone!=20||chroma!=20||mode!=1)return fail(Stage::ConstructorDefaults);','(void)tone;(void)chroma;(void)mode;'),
 ('failed_initialization_retried',2,'ambient_os4::attempted=true;','ambient_os4::attempted=false;'),
 ('poisoned_algorithm_reused',14,'ambient_os4::available=false;\n            ambient_os4::stage=ambient_os4::Stage::AlgorithmException;','ambient_os4::stage=ambient_os4::Stage::AlgorithmException;'),
 ('transparent_zero_lost',15,'return static_cast<jlong>(UINT64_C(0x100000000)|color);','return static_cast<jlong>(color);'),
 ('input_limit_lost',16,'if(count>25088||count>env->GetArrayLength(pixels))return 0;','if(count>env->GetArrayLength(pixels))return 0;'),
]
inputs={str(q):sha(q)for q in [Path(__file__),a.baseline,apk,*CORE.glob('*.*'),*HERE.glob('*.*')]if q.is_file()};commands=[];results=[]
def run(c,check=True):
 commands.append(list(map(str,c)));r=subprocess.run(list(map(str,c)),capture_output=True,timeout=90,env=dict(os.environ,JAVA_HOME=str(JAVA),PATH=str(JAVA/'bin')+os.pathsep+os.environ['PATH']));(a.output/'commands.log').open('ab').write(r.stdout+r.stderr)
 if check:assert r.returncode==0,(c,r.stderr)
 return r
def adb(*args,**kwargs):return run([ADB,'-s','emulator-5580',*args],**kwargs)
for name,scenario,old,new in mutants:
 assert source.count(old)==1,name;b=a.build/name;b.mkdir();cpp=b/'mutant.cpp';cpp.write_text(source.replace(old,new));so=b/'libHyperCeilerAmbientOS4.so'
 run([BIN/'aarch64-linux-android35-clang++','-std=c++20','-O2','-shared','-fPIC','-Wall','-Wextra','-Werror','-fvisibility=hidden','-I',CORE,'-DHYPERCEILER_AMBIENT_TEST_PATH="/data/user/0/'+PKG+'/files/libMiMainColor.so"',cpp,HERE/'bridge_faults.cpp','-o',so,'-ldl','-llog']+['-Wl,--wrap='+x for x in ['dlsym','dlopen','android_dlopen_ext','malloc','pread','dl_iterate_phdr']])
 with zipfile.ZipFile(apk)as z,zipfile.ZipFile(b/'raw.apk','w')as w:
  for i in z.infolist():
   if i.filename.startswith('META-INF/'):continue
   w.writestr(i,so.read_bytes()if i.filename=='lib/arm64-v8a/libHyperCeilerAmbientOS4.so'else z.read(i.filename))
 run([BT/'zipalign','-f','-P','16','4',b/'raw.apk',b/'aligned.apk']);run([BT/'apksigner','sign','--ks',test_paths.TEST_DATA_ROOT / 'emulation/native-probe/build/test-only.keystore','--ks-key-alias','nativeprobe','--ks-pass','pass:android','--key-pass','pass:android','--out',b/'probe.apk',b/'aligned.apk']);adb('install','--no-incremental','-r','--abi','arm64-v8a',b/'probe.apk');adb('shell','am','force-stop',PKG);adb('shell','run-as',PKG,'rm','-f','files/report.json');adb('shell','am','start','-S','-W','-n',PKG+'/.MainActivity','--ei','scenario',scenario)
 end=time.monotonic()+60;raw=b''
 while time.monotonic()<end:
  r=adb('exec-out','run-as',PKG,'cat','files/report.json',check=False)
  if r.returncode==0 and r.stdout.startswith(b'{'):raw=r.stdout;break
  time.sleep(1)
 assert raw,'No explicit failure witness for '+name
 d=json.loads(raw);assert d.get('scenario',scenario)==scenario,(name,'wrong scenario',d);(a.output/(name+'.json')).write_bytes(raw)
 if 'error'in d:rejected=d['error'].startswith('java.lang.AssertionError: failure reopened')
 elif scenario==2:rejected=d.get('no_retry')is False and d.get('init_attempts',0)>1
 elif scenario==14:rejected=d.get('special_contract')!=1
 elif scenario==15:rejected=d.get('special_contract')!=0x100000000
 elif scenario==16:rejected=d.get('above_max_rejected')is False
 else:rejected=d.get('all_failed')is False
 results.append(dict(name=name,scenario=scenario,rejected=rejected,actual=d,source_sha256=sha(cpp),so_sha256=sha(so)));print(name,rejected,flush=True)
r=dict(passed=all(x['rejected']for x in results)and all(sha(Path(n))==h for n,h in inputs.items()),controls=results,count=len(results),inputs=inputs,commands=commands,device_status='NOT_TESTED',physical_device_pass_count=0,boundary='7 separately compiled production-source mutants executed in dedicated API37 ARM-nativebridge probe; no physical SystemUI proof.')
(a.output/'report.json').write_text(json.dumps(r,indent=2)+'\n');assert r['passed']
