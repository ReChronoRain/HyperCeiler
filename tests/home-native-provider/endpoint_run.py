#!/usr/bin/env python3
# SPDX-License-Identifier: AGPL-3.0-or-later
from pathlib import Path
import hashlib,subprocess,sys,tempfile
here=Path(__file__).resolve().parent
root=here.parents[1]
sys.path.insert(0,str(here.parent/'home-recent-cards-sdk'))
from rename_fixture import rename
if len(sys.argv)!=2:raise SystemExit('Usage: python3 endpoint_run.py <read-only SDK samples>')
samples=Path(sys.argv[1])
names=['libhyper_os_core_data_public.so','libhyper_os_core_data_private.dylib.so',
    'libhyper_os_foundation_private.dylib.so','libhyper_os_sys.dylib.so','libstd.dylib.so','libbinder_rs.dylib.so',
    'libhyper_os_schema_public.so','libhyper_os_aidl.dylib.so','libhyper_os_binder_private.dylib.so']
files=[samples/name for name in names]
for file in files:print(file.name,hashlib.sha256(file.read_bytes()).hexdigest(),flush=True)
with tempfile.TemporaryDirectory(prefix='hc-provider-endpoint-') as directory:
    binary=Path(directory)/'endpoint-test'
    subprocess.run(['c++','-std=c++20','-Wall','-Wextra','-Werror','-fsanitize=address,undefined',
        '-I'+str(root/'app/src/main/cpp'),str(here/'endpoint_contract_test.cpp'),'-o',str(binary)],check=True)
    subprocess.run([str(binary),*map(str,files)],check=True,timeout=60)
    renamed=Path(directory)/'renamed';renamed.mkdir()
    for file in files:(renamed/file.name).write_bytes(rename(file.read_bytes()))
    subprocess.run([str(binary),*(str(renamed/file.name) for file in files)],check=True,timeout=60)
    print('PASS minimal endpoint contract with private linkage renamed',flush=True)
