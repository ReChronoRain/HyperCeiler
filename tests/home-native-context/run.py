#!/usr/bin/env python3
# SPDX-License-Identifier: AGPL-3.0-or-later
from pathlib import Path
import hashlib,subprocess,sys,tempfile
here=Path(__file__).resolve().parent
root=here.parents[1]
sys.path.insert(0,str(here.parent/'home-recent-cards-sdk'))
from rename_fixture import rename
if len(sys.argv)!=2:raise SystemExit('Usage: python3 run.py <read-only SDK samples>')
fixtures=Path(sys.argv[1])
files=[fixtures/name for name in ['libhyper_os_app_public.so','libhyper_os_app_private.dylib.so']]
for file in files:print(file.name,hashlib.sha256(file.read_bytes()).hexdigest(),flush=True)
with tempfile.TemporaryDirectory(prefix='hc-native-context-') as directory:
    out=Path(directory)/'context-test'
    subprocess.run(['c++','-std=c++20','-Wall','-Wextra','-Werror','-fsanitize=address,undefined','-I'+str(root/'app/src/main/cpp'),str(here/'context_test.cpp'),'-o',str(out)],check=True)
    subprocess.run([str(out),*map(str,files)],check=True,timeout=60)
    renamed=Path(directory)/'renamed';renamed.mkdir()
    for file in files:(renamed/file.name).write_bytes(rename(file.read_bytes()))
    subprocess.run([str(out),*(str(renamed/file.name) for file in files)],check=True,timeout=60)
    print('PASS private linkage renamed with rebuilt GNU hash',flush=True)
