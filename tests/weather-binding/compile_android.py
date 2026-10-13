#!/usr/bin/env python3
from pathlib import Path
import argparse,hashlib,json,subprocess

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

p=argparse.ArgumentParser();p.add_argument('--output',type=Path,required=True);a=p.parse_args();a.output.mkdir(parents=True,exist_ok=True);b=Path(__file__).resolve().parent;src=b.parents[1];llvm=test_paths.ANDROID_NDK_HOME / 'toolchains/llvm/prebuilt/linux-x86_64/bin';test=a.output/'compile.cpp';test.write_text('''#include "targets/weather/weather_binding.h"
using Read = bool(*)(uintptr_t,std::span<std::byte>);
extern "C" bool weather_test_resolve(uintptr_t base,Read read,weather_binding::Binding& out){return weather_binding::resolve(base,read,out);}
extern "C" bool weather_test_verify(uintptr_t base,Read read){return weather_binding::verify_weather(base,read);}
extern "C" bool weather_test_dart(uintptr_t base,Read read){return weather_binding::verify_dart(base,read);}
''');obj=a.output/'compile.o';cmd=[str(llvm/'aarch64-linux-android35-clang++'),'-std=c++20','-O2','-Wall','-Wextra','-Werror','-fno-exceptions','-fno-rtti','-I',str(src/'app/src/main/cpp'),'-c',str(test),'-o',str(obj)];z=subprocess.run(cmd,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True);(a.output/'compile.txt').write_text(z.stdout);print(z.stdout,end='');sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();files=[src/'app/src/main/cpp/targets/weather'/f for f in ['weather_binding.h','weather_guard.generated.h','weather_device_level.h','weather_snapshot.h']];x={'passed':z.returncode==0,'exit_code':z.returncode,'command':cmd,'production_inputs':{str(f):sha(f)for f in files},'compiler_source':{'path':str(test),'sha256':sha(test)},'object_sha256':sha(obj)if obj.exists()else None,'limits':'NDK28 API35 arm64 actual resolver/verify template instantiation compile only, no execution or loader/phone evidence'};(a.output/'report.json').write_text(json.dumps(x,indent=2)+'\n');raise SystemExit(z.returncode)
