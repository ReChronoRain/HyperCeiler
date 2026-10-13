#!/usr/bin/env python3
"""Compile the current production bridge and test same-path ART loader ownership."""
from pathlib import Path
import argparse, hashlib, json, os, subprocess, time, zipfile

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[2]))
import hyperceiler_test_paths as test_paths


HERE = Path(__file__).resolve().parent
ROOT = test_paths.TEST_DATA_ROOT
CACHE = test_paths.BUILD_CACHE
SDK = test_paths.ANDROID_HOME
NDK = test_paths.ANDROID_NDK_HOME / 'toolchains/llvm/prebuilt/linux-x86_64/bin'
JAVA = test_paths.JAVA_HOME / 'bin'
BT = SDK / 'build-tools/37.0.0'
ANDROID = SDK / 'platforms/android-37.0/android.jar'
CORE = test_paths.SOURCE_ROOT / 'app/src/main/cpp/targets/systemui'
PKG = 'local.hyperceiler.ambientreloadprobe'
sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
p = argparse.ArgumentParser()
p.add_argument('--build', required=True, type=Path)
p.add_argument('--output', required=True, type=Path)
a = p.parse_args()
b, o = a.build.resolve(), a.output.resolve()
b.mkdir(parents=True, exist_ok=False)
o.mkdir(parents=True, exist_ok=False)
files = [*sorted(HERE.glob('*.*')), CORE/'ambient_color_os4.cpp',
         CORE/'ambient_sha256.h', CORE/'ambient_bindings.generated.h']
inputs = {str(q): sha(q) for q in files if q.is_file()}
commands = []
def run(cmd, check=True):
    cmd = list(map(str, cmd)); commands.append(cmd)
    r = subprocess.run(cmd, capture_output=True, timeout=90,
        env=dict(os.environ, JAVA_HOME=str(JAVA.parent), PATH=str(JAVA)+os.pathsep+os.environ['PATH']))
    with (o/'commands.log').open('ab') as f: f.write(r.stdout+r.stderr)
    if check: assert r.returncode == 0, (cmd, r.stdout, r.stderr)
    return r
def adb(*args, **kwargs):
    return run([SDK/'platform-tools/adb', '-s', 'emulator-5580', *args], **kwargs)
assert adb('emu', 'avd', 'name').stdout.decode().splitlines()[0] == 'hyperceiler_os4_api37'
(o/'properties.txt').write_bytes(adb('shell', 'getprop').stdout)
for d in ['classes', 'helper-classes', 'dex', 'helper-dex']: (b/d).mkdir()
run([NDK/'aarch64-linux-android35-clang++', '-std=c++20', '-O2', '-shared', '-fPIC',
     '-Wall', '-Wextra', '-Werror', '-fvisibility=hidden', CORE/'ambient_color_os4.cpp',
     '-o', b/'libHyperCeilerAmbientOS4.so', '-ldl', '-llog'])
run([JAVA/'javac', '--release', '8', '-d', b/'helper-classes', HERE/'AmbientNativeColorOS4.java'])
run([JAVA/'jar', '--create', '--file', b/'helper.jar', '-C', b/'helper-classes', '.'])
run([BT/'d8', '--min-api', '35', '--lib', ANDROID, '--output', b/'helper-dex', b/'helper.jar'])
run([JAVA/'javac', '--release', '8', '-cp', ANDROID, '-d', b/'classes', HERE/'MainActivity.java'])
run([JAVA/'jar', '--create', '--file', b/'classes.jar', '-C', b/'classes', '.'])
run([BT/'d8', '--min-api', '35', '--lib', ANDROID, '--output', b/'dex', b/'classes.jar'])
run([BT/'aapt2', 'link', '-I', ANDROID, '--manifest', HERE/'AndroidManifest.xml', '-o', b/'resources.apk'])
with zipfile.ZipFile(b/'resources.apk') as z, zipfile.ZipFile(b/'unaligned.apk', 'w') as w:
    for i in z.infolist(): w.writestr(i, z.read(i.filename))
    for f, name in [(b/'dex/classes.dex','classes.dex'),(b/'helper-dex/classes.dex','assets/helper.dex'),
        (b/'libHyperCeilerAmbientOS4.so','lib/arm64-v8a/libHyperCeilerAmbientOS4.so'),
        (NDK.parent/'sysroot/usr/lib/aarch64-linux-android/libc++_shared.so','lib/arm64-v8a/libc++_shared.so')]:
        w.write(f, name, compress_type=zipfile.ZIP_DEFLATED)
run([BT/'zipalign', '-f', '-P', '16', '4', b/'unaligned.apk', b/'aligned.apk'])
run([BT/'apksigner', 'sign', '--ks', test_paths.TEST_DATA_ROOT / 'emulation/native-probe/build/test-only.keystore',
     '--ks-key-alias', 'nativeprobe', '--ks-pass', 'pass:android', '--key-pass', 'pass:android',
     '--out', b/'probe.apk', b/'aligned.apk'])
run([BT/'apksigner', 'verify', '--verbose', b/'probe.apk'])
adb('install', '-r', '--abi', 'arm64-v8a', b/'probe.apk')
adb('shell', 'am', 'force-stop', PKG)
adb('shell', 'run-as', PKG, 'rm', '-f', 'files/report.json')
adb('shell', 'am', 'start', '-W', '-n', PKG+'/.MainActivity')
raw = b''; end = time.monotonic()+60
while time.monotonic() < end:
    r = adb('exec-out', 'run-as', PKG, 'cat', 'files/report.json', check=False)
    if r.returncode == 0 and r.stdout.startswith(b'{'): raw = r.stdout; break
    time.sleep(1)
(o/'logcat.txt').write_bytes(adb('logcat', '-d', '-t', '300', '-s',
    'AmbientReloadProbe:I', 'AndroidRuntime:E', 'art:I', 'linker:E').stdout)
assert raw, 'No probe report; inspect retained command/logcat evidence'
(o/'android-report.json').write_bytes(raw)
d = json.loads(raw)
unchanged = all(sha(Path(q)) == s for q,s in inputs.items())
report = {'passed': d['passed'] and unchanged, 'checks': d['checks'], 'observations': d['observations'],
          'inputs': inputs, 'inputs_unchanged': unchanged, 'commands': commands,
          'bridge_sha256': sha(b/'libHyperCeilerAmbientOS4.so'), 'apk_sha256': sha(b/'probe.apk'),
          'scope': d['scope'], 'device_status': 'NOT_TESTED', 'physical_device_pass_count': 0}
(o/'report.json').write_text(json.dumps(report, indent=2)+'\n')
print(json.dumps(report, indent=2))
raise SystemExit(0 if report['passed'] else 1)
