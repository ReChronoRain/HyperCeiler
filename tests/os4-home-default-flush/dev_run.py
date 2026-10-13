from pathlib import Path
import os,subprocess,resource,sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

resource.setrlimit(resource.RLIMIT_CORE,(0,0));root=test_paths.TEST_DATA_ROOT;here=Path(__file__).resolve().parent;out=test_paths.TEST_DATA_ROOT / 'acceptance/home-grid-next/default-flush-test16/development-v1';arm=test_paths.BUILD_CACHE / 'arm64-emulation/root';env=dict(os.environ,LD_LIBRARY_PATH=str(arm/'usr/lib/x86_64-linux-gnu'),TRACE_ADAPTER='1');b=out/'flush_test'
r=subprocess.run([str(arm/'usr/bin/aarch64-linux-gnu-g++-13'),'--sysroot='+str(arm),'-std=c++20','-O2','-Wall','-Wextra','-Werror',str(here/'flush_test.cpp'),str(here/'placement_probe.S'),'-o',str(b)],env=env,capture_output=True,text=True);(out/'build.log').write_text(r.stdout+r.stderr);assert r.returncode==0,r.stderr
r=subprocess.run([str(arm/'usr/bin/qemu-aarch64-static'),'-L',str(arm/'usr/aarch64-linux-gnu'),str(b),str(test_paths.ROM_ROOT / 'launcher-native/lib/arm64-v8a/libapp.so')],env=env,capture_output=True,text=True,timeout=30);(out/'run.log').write_text(r.stdout+r.stderr);print('EXIT',r.returncode,r.stdout[-1500:],r.stderr[-3500:]);sys.exit(r.returncode)
