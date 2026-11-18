#!/usr/bin/env python3
# SPDX-License-Identifier: AGPL-3.0-or-later
from pathlib import Path
import hashlib,subprocess,sys,tempfile
here=Path(__file__).resolve().parent
root=here.parents[1]
sys.path.insert(0,str(here.parent/'home-recent-cards-sdk'))
from rename_fixture import rename
if len(sys.argv)!=2:raise SystemExit('Usage: python3 run.py <read-only SDK samples>')
samples=Path(sys.argv[1])
names=['libhyper_os_core_data_public.so','libhyper_os_core_data_private.dylib.so',
       'libhyper_os_foundation_private.dylib.so','libhyper_os_sys.dylib.so','libstd.dylib.so','libbinder_rs.dylib.so']
files=[samples/name for name in names]
for file in files:print(file.name,hashlib.sha256(file.read_bytes()).hexdigest(),flush=True)
with tempfile.TemporaryDirectory(prefix='hc-native-provider-') as directory:
    out=Path(directory)/'provider-contract-test'
    subprocess.run(['c++','-std=c++20','-Wall','-Wextra','-Werror','-fsanitize=address,undefined','-I'+str(root/'app/src/main/cpp'),str(here/'contract_test.cpp'),'-o',str(out)],check=True)
    subprocess.run([str(out),*map(str,files)],check=True,timeout=60)
    call=Path(directory)/'provider-call-test'
    subprocess.run(['c++','-std=c++20','-Wall','-Wextra','-Werror','-fsanitize=address,undefined','-I'+str(root/'app/src/main/cpp'),str(here/'call_test.cpp'),'-o',str(call)],check=True)
    subprocess.run([str(call),*map(str,files)],check=True,timeout=60)
    schema=samples/'libhyper_os_schema_public.so'
    print(schema.name,hashlib.sha256(schema.read_bytes()).hexdigest(),flush=True)
    bundle=Path(directory)/'provider-bundle-test'
    subprocess.run(['c++','-std=c++20','-Wall','-Wextra','-Werror','-fsanitize=address,undefined','-I'+str(root/'app/src/main/cpp'),str(here/'bundle_test.cpp'),'-o',str(bundle)],check=True)
    subprocess.run([str(bundle),*map(str,files),str(schema)],check=True,timeout=60)
    aidl=samples/'libhyper_os_aidl.dylib.so'
    print(aidl.name,hashlib.sha256(aidl.read_bytes()).hexdigest(),flush=True)
    parcel=Path(directory)/'provider-parcel-test'
    subprocess.run(['c++','-std=c++20','-Wall','-Wextra','-Werror','-fsanitize=address,undefined','-I'+str(root/'app/src/main/cpp'),str(here/'parcel_test.cpp'),'-o',str(parcel)],check=True)
    subprocess.run([str(parcel),*map(str,files),str(schema),str(aidl)],check=True,timeout=60)
    surface_files=[samples/name for name in ['libhyper_os_ui_public.so','libhyper_os_ui_private.dylib.so','libhyper_os_schema_private.dylib.so']]
    for file in surface_files:print(file.name,hashlib.sha256(file.read_bytes()).hexdigest(),flush=True)
    surface=Path(directory)/'provider-surface-test'
    subprocess.run(['c++','-std=c++20','-Wall','-Wextra','-Werror','-fsanitize=address,undefined','-I'+str(root/'app/src/main/cpp'),str(here/'surface_test.cpp'),'-o',str(surface)],check=True)
    subprocess.run([str(surface),*map(str,files),str(schema),str(aidl),*map(str,surface_files)],check=True,timeout=60)
    display_files=[samples/name for name in ['libhyper_os_app_public.so','libhyper_os_app_private.dylib.so']]
    for file in display_files:print(file.name,hashlib.sha256(file.read_bytes()).hexdigest(),flush=True)
    display=Path(directory)/'provider-display-test'
    subprocess.run(['c++','-std=c++20','-Wall','-Wextra','-Werror','-fsanitize=address,undefined','-I'+str(root/'app/src/main/cpp'),str(here/'display_test.cpp'),'-o',str(display)],check=True)
    subprocess.run([str(display),*map(str,files),str(schema),str(aidl),*map(str,surface_files),*map(str,display_files)],check=True,timeout=60)
    binder_file=samples/'libhyper_os_binder_private.dylib.so'
    print(binder_file.name,hashlib.sha256(binder_file.read_bytes()).hexdigest(),flush=True)
    binder=Path(directory)/'provider-binder-test'
    subprocess.run(['c++','-std=c++20','-Wall','-Wextra','-Werror','-fsanitize=address,undefined','-I'+str(root/'app/src/main/cpp'),str(here/'binder_test.cpp'),'-o',str(binder)],check=True)
    subprocess.run([str(binder),*map(str,files),str(schema),str(aidl),*map(str,surface_files),*map(str,display_files),str(binder_file)],check=True,timeout=60)
    effects=Path(directory)/'provider-effect-test'
    subprocess.run(['c++','-std=c++20','-Wall','-Wextra','-Werror','-fsanitize=address,undefined','-I'+str(root/'app/src/main/cpp'),str(here/'effect_test.cpp'),'-o',str(effects)],check=True)
    subprocess.run([str(effects),*map(str,files),str(schema),str(aidl),*map(str,surface_files),*map(str,display_files),str(binder_file)],check=True,timeout=60)
    integer=Path(directory)/'provider-integer-test'
    subprocess.run(['c++','-std=c++20','-Wall','-Wextra','-Werror','-fsanitize=address,undefined','-I'+str(root/'app/src/main/cpp'),str(here/'integer_test.cpp'),'-o',str(integer)],check=True)
    subprocess.run([str(integer),*map(str,files),str(schema),str(aidl),*map(str,surface_files),*map(str,display_files),str(binder_file)],check=True,timeout=60)
    renamed=Path(directory)/'renamed';renamed.mkdir()
    for file in files:(renamed/file.name).write_bytes(rename(file.read_bytes()))
    (renamed/schema.name).write_bytes(rename(schema.read_bytes()))
    (renamed/aidl.name).write_bytes(rename(aidl.read_bytes()))
    for file in surface_files:(renamed/file.name).write_bytes(rename(file.read_bytes()))
    for file in display_files:(renamed/file.name).write_bytes(rename(file.read_bytes()))
    (renamed/binder_file.name).write_bytes(rename(binder_file.read_bytes()))
    subprocess.run([str(out),*(str(renamed/name) for name in names)],check=True,timeout=60)
    subprocess.run([str(call),*(str(renamed/name) for name in names)],check=True,timeout=60)
    subprocess.run([str(bundle),*(str(renamed/name) for name in names),str(renamed/schema.name)],check=True,timeout=60)
    subprocess.run([str(parcel),*(str(renamed/name) for name in names),str(renamed/schema.name),str(renamed/aidl.name)],check=True,timeout=60)
    subprocess.run([str(surface),*(str(renamed/name) for name in names),str(renamed/schema.name),str(renamed/aidl.name),*(str(renamed/file.name) for file in surface_files)],check=True,timeout=60)
    subprocess.run([str(display),*(str(renamed/name) for name in names),str(renamed/schema.name),str(renamed/aidl.name),*(str(renamed/file.name) for file in surface_files),*(str(renamed/file.name) for file in display_files)],check=True,timeout=60)
    subprocess.run([str(binder),*(str(renamed/name) for name in names),str(renamed/schema.name),str(renamed/aidl.name),*(str(renamed/file.name) for file in surface_files),*(str(renamed/file.name) for file in display_files),str(renamed/binder_file.name)],check=True,timeout=60)
    subprocess.run([str(effects),*(str(renamed/name) for name in names),str(renamed/schema.name),str(renamed/aidl.name),*(str(renamed/file.name) for file in surface_files),*(str(renamed/file.name) for file in display_files),str(renamed/binder_file.name)],check=True,timeout=60)
    subprocess.run([str(integer),*(str(renamed/name) for name in names),str(renamed/schema.name),str(renamed/aidl.name),*(str(renamed/file.name) for file in surface_files),*(str(renamed/file.name) for file in display_files),str(renamed/binder_file.name)],check=True,timeout=60)
    print('PASS private Rust linkage renamed with rebuilt GNU hash',flush=True)
