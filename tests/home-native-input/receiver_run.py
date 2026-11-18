#!/usr/bin/env python3
# SPDX-License-Identifier: AGPL-3.0-or-later
from pathlib import Path
import hashlib,subprocess,sys,tempfile
here=Path(__file__).resolve().parent
root=here.parents[1]
sys.path.insert(0,str(here.parent/'home-recent-cards-sdk'))
from rename_fixture import rename
if len(sys.argv)!=3:raise SystemExit('Usage: python3 receiver_run.py <read-only SDK samples> <receiver dependency samples>')
samples,extra=map(Path,sys.argv[1:])
files=[samples/name for name in ['libhyper_os_ui_public.so','libhyper_os_ui_private.dylib.so',
    'libhyper_os_aidl.dylib.so','libstd.dylib.so','libhyper_os_sys.dylib.so']]
files.extend(extra/name for name in ['libhyper_os_bundle_private.dylib.so','libhyper_os_graphics_private.dylib.so'])
for file in files:print(file.name,hashlib.sha256(file.read_bytes()).hexdigest(),flush=True)
with tempfile.TemporaryDirectory(prefix='hc-native-receiver-') as directory:
    binary=Path(directory)/'receiver-test'
    subprocess.run(['c++','-std=c++20','-Wall','-Wextra','-Werror','-fsanitize=address,undefined',
        '-I'+str(root/'app/src/main/cpp'),str(here/'receiver_test.cpp'),'-o',str(binary)],check=True)
    subprocess.run([str(binary),*map(str,files)],check=True,timeout=120)
    renamed=Path(directory)/'renamed';renamed.mkdir()
    for file in files:(renamed/file.name).write_bytes(rename(file.read_bytes()))
    subprocess.run([str(binary),*(str(renamed/file.name) for file in files)],check=True,timeout=120)
    print('PASS receiver contract with private linkage renamed and rebuilt GNU hashes',flush=True)
