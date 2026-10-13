#!/usr/bin/env python3
"""Execute production JNI translation unit, with test-only path and loader faults.
Actual OTA ARM64 algorithm through API37 libndk_translation, never a phone PASS.
"""
from pathlib import Path
import argparse,hashlib,json,os,subprocess,time,zipfile

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

HERE=Path(__file__).resolve().parent;ROOT=test_paths.TEST_DATA_ROOT;SRC=test_paths.SOURCE_ROOT;CACHE=test_paths.BUILD_CACHE;SDK=test_paths.ANDROID_HOME;BT=SDK/'build-tools/37.0.0';JAVA=test_paths.JAVA_HOME / 'bin';NDK=test_paths.ANDROID_NDK_HOME / 'toolchains/llvm/prebuilt/linux-x86_64/bin';LIB=test_paths.EXTRACTED_ROM_ROOT / 'system_ext/lib64/libMiMainColor.so';ADB=SDK/'platform-tools/adb';PKG='local.hyperceiler.colorprobe';CORE=SRC/'app/src/main/cpp/targets/systemui';PATH='/data/user/0/'+PKG+'/files/libMiMainColor.so'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
p=argparse.ArgumentParser();p.add_argument('--build',type=Path,required=True);p.add_argument('--output',type=Path,required=True);p.add_argument('--scenarios',default=','.join(map(str,range(24))));a=p.parse_args();b=a.build.resolve();o=a.output.resolve();b.mkdir(parents=True,exist_ok=False);o.mkdir(parents=True,exist_ok=False)
inputs={str(q):sha(q)for q in [*sorted(HERE.glob('*.*')),*sorted(CORE.glob('*.*')),SRC/'app/src/main/cpp/CMakeLists.txt',LIB] if q.is_file()};assert sha(LIB)=='652eb3fb8850b46a4df0865c1abf4f3ca5b0084d828dceb5ce547be86e1d1852'
commands=[]
def run(cmd,check=True,timeout=90):
 commands.append(list(map(str,cmd)));r=subprocess.run(list(map(str,cmd)),capture_output=True,timeout=timeout,env=dict(os.environ,JAVA_HOME=str(JAVA.parent),PATH=str(JAVA)+os.pathsep+os.environ['PATH']));(o/'commands.log').open('ab').write(r.stdout+r.stderr)
 if check:assert r.returncode==0,(cmd,r.stdout,r.stderr)
 return r
def adb(*args,**kw):return run([ADB,'-s','emulator-5580',*args],**kw)
assert adb('emu','avd','name').stdout.decode().splitlines()[0]=='hyperceiler_os4_api37'
props=adb('shell','getprop').stdout;(o/'android-properties.txt').write_bytes(props);assert b'[ro.product.cpu.abilist]: [x86_64,arm64-v8a]'in props and b'libndk_translation.so'in props
(b/'classes').mkdir();(b/'dex').mkdir()
base=[NDK/'aarch64-linux-android35-clang++','-std=c++20','-O2','-shared','-fPIC','-Wall','-Wextra','-Werror','-fvisibility=hidden']
# Compile production target independently, without any path or fault adapters.
run(base+[CORE/'ambient_color_os4.cpp','-o',b/'production-libHyperCeilerAmbientOS4.so','-ldl','-llog'])
run(base+['-DHYPERCEILER_AMBIENT_TEST_PATH="'+PATH+'"',CORE/'ambient_color_os4.cpp',HERE/'bridge_faults.cpp','-o',b/'libHyperCeilerAmbientOS4.so','-ldl','-llog']+['-Wl,--wrap='+x for x in ['dlsym','dlopen','android_dlopen_ext','malloc','pread','dl_iterate_phdr']])
run(base+['-fno-exceptions','-fno-rtti','-nostdlib++',HERE/'oem.cpp','-o',b/'liboemprobe.so','-ldl'])
run([JAVA/'javac','--release','8','-cp',SDK/'platforms/android-37.0/android.jar','-d',b/'classes',HERE/'MainActivity.java',HERE/'AmbientNativeColorOS4.java']);run([JAVA/'jar','--create','--file',b/'classes.jar','-C',b/'classes','.']);run([BT/'d8','--min-api','35','--lib',SDK/'platforms/android-37.0/android.jar','--output',b/'dex',b/'classes.jar']);run([BT/'aapt2','link','-I',SDK/'platforms/android-37.0/android.jar','--manifest',HERE/'AndroidManifest.xml','-o',b/'resources.apk'])
with zipfile.ZipFile(b/'resources.apk')as z,zipfile.ZipFile(b/'unaligned.apk','w')as w:
 for i in z.infolist():w.writestr(i,z.read(i.filename))
 w.write(b/'dex/classes.dex','classes.dex',compress_type=zipfile.ZIP_DEFLATED)
 for q,n in [(LIB,'libMiMainColor.so'),(b/'libHyperCeilerAmbientOS4.so','libHyperCeilerAmbientOS4.so'),(b/'liboemprobe.so','liboemprobe.so'),(NDK.parent/'sysroot/usr/lib/aarch64-linux-android/libc++_shared.so','libc++_shared.so')]:w.write(q,'lib/arm64-v8a/'+n,compress_type=zipfile.ZIP_DEFLATED)
run([BT/'zipalign','-f','-P','16','4',b/'unaligned.apk',b/'aligned.apk']);run([BT/'apksigner','sign','--ks',test_paths.TEST_DATA_ROOT / 'emulation/native-probe/build/test-only.keystore','--ks-key-alias','nativeprobe','--ks-pass','pass:android','--key-pass','pass:android','--out',b/'probe.apk',b/'aligned.apk']);run([BT/'apksigner','verify','--verbose',b/'probe.apk']);adb('install','--no-incremental','-r','--abi','arm64-v8a',b/'probe.apk')
checks=[];results=[]
for scenario in map(int,a.scenarios.split(',')):
 adb('shell','am','force-stop',PKG);adb('shell','run-as',PKG,'rm','-f','files/report.json');adb('shell','am','start','-S','-W','-n',PKG+'/.MainActivity','--ei','scenario',str(scenario));end=time.monotonic()+180;raw=b''
 while time.monotonic()<end:
  r=adb('exec-out','run-as',PKG,'cat','files/report.json',check=False)
  if r.returncode==0 and r.stdout.startswith(b'{'):raw=r.stdout;break
  time.sleep(1)
 (o/('scenario-%02d-logcat.txt'%scenario)).write_bytes(adb('logcat','-d','-t','200','-s','HyperCeilerAmbientOS4:I','ColorProbe:E','AndroidRuntime:E','DEBUG:F').stdout)
 assert raw,'No report for scenario '+str(scenario)
 (o/('scenario-%02d.json'%scenario)).write_bytes(raw);d=json.loads(raw);assert d.get('scenario')==scenario,(scenario,d);results.append(d)
 expected={'invalid_inputs':5,'loads_after_invalid':0,'attempts_after_invalid':0,'init_attempts':1}
 if scenario in (0,10,17):expected.update(serial_equal=136,parallel_calls=1000,parallel_mismatches=0,force_calls=1,private_added_threads=3,private_threads_survive=True,new_tasks_after_repeated=0)
 elif scenario in (13,14,15):expected.update(special_contract=0x100000000 if scenario==15 else 1,force_calls=1)
 elif scenario==16:expected.update(max_allowed_equal=True,above_max_rejected=True,force_calls=1)
 else:expected.update(all_failed=True,no_retry=True,force_calls=1 if scenario in (3,4,5,6,7) else 0)
 if scenario==13:expected['after_jni_exception_recovers']=True
 if scenario==17:expected['first_concurrent_equal']=8
 for key,value in expected.items():checks.append(dict(name='scenario '+str(scenario)+' '+key,passed=d.get(key)==value,actual=d.get(key),expected=value))
 print(json.dumps(d),flush=True)
checks +=[dict(name='all inputs unchanged',passed=all(sha(Path(n))==h for n,h in inputs.items())),dict(name='packaged vendor bytes exact',passed=zipfile.ZipFile(b/'probe.apk').read('lib/arm64-v8a/libMiMainColor.so')==LIB.read_bytes())]
r=dict(passed=all(c['passed']for c in checks),checks=checks,inputs=inputs,commands=commands,execution=results,apk=dict(path=str(b/'probe.apk'),sha256=sha(b/'probe.apk')),production_so=dict(path=str(b/'production-libHyperCeilerAmbientOS4.so'),sha256=sha(b/'production-libHyperCeilerAmbientOS4.so')),device_status='NOT_TESTED',physical_device_pass_count=0,boundary='Actual production core with only fixed-path test macro and external link-time fault adapters; original ARM64 vendor algorithm via Android native bridge. No SystemUI namespace or physical Xiaomi/LSPosed validation. Vendor binary is not distributed with module.')
(o/'report.json').write_text(json.dumps(r,indent=2)+'\n');print(json.dumps(dict(passed=r['passed'],checks=len(checks))));raise SystemExit(0 if r['passed']else 1)
