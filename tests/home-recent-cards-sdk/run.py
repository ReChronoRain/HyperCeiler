#!/usr/bin/env python3
# SPDX-License-Identifier: AGPL-3.0-or-later
from pathlib import Path
import hashlib,subprocess,sys,tempfile
from rename_fixture import rename
here=Path(__file__).resolve().parent
root=here.parents[1]
if len(sys.argv)!=2:
    raise SystemExit('Usage: python3 run.py <directory containing read-only SDK ELF samples>')
fixtures=Path(sys.argv[1])
names=['ui-public.so','aidl.so','ui-private.so','schema-public.so','binder-rs.so','std.so']
files=[fixtures/name for name in names]
for file in files:
    print(file.name,hashlib.sha256(file.read_bytes()).hexdigest(),flush=True)
with tempfile.TemporaryDirectory(prefix='hc-cards-sdk-') as directory:
    renamed=Path(directory)/'renamed';renamed.mkdir()
    for file in files:(renamed/file.name).write_bytes(rename(file.read_bytes()))
    for source in ['contract_test.cpp','group_test.cpp']:
        out=Path(directory)/source.removesuffix('.cpp')
        subprocess.run(['c++','-std=c++20','-Wall','-Wextra','-Werror','-fsanitize=address,undefined','-I'+str(root/'app/src/main/cpp'),str(here/source),'-o',str(out)],check=True)
        subprocess.run([str(out),*map(str,files)],check=True,timeout=90)
        subprocess.run([str(out),*(str(renamed/name) for name in names)],check=True,timeout=90)
    print('PASS private Rust linkage renamed with rebuilt GNU hash, same feature resolution',flush=True)
