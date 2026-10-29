#!/usr/bin/env python3
"""Run production runtime; only NDK transport is replaced on non-Android hosts."""
from pathlib import Path
import subprocess,tempfile,sys
HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[1]
with tempfile.TemporaryDirectory(prefix='hc-haptic-worker-') as work:
    binary=str(Path(work)/'runtime')
    subprocess.run(['c++','-std=c++20','-O1','-g','-Wall','-Wextra','-Werror','-pthread',
        '-fsanitize=address,undefined','-fno-omit-frame-pointer',
        '-Wl,-export_dynamic' if sys.platform=='darwin' else '-Wl,--export-dynamic',
        '-I'+str(HERE/'runtime-stubs'),'-I'+str(ROOT/'app/src/main/cpp'),
        str(HERE/'runtime_test.cpp'),str(ROOT/'app/src/main/cpp/targets/home/home_recents_haptic_runtime.cpp'),'-o',binary],check=True)
    for scenario in sys.argv[1:] or ['both','scroll','close','off','v1']:
        subprocess.run([binary,scenario],check=True)
