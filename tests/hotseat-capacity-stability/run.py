#!/usr/bin/env python3
# SPDX-License-Identifier: AGPL-3.0-or-later
"""Capacity source-word lifetime regression, independent of Android/device state."""
import os,shutil,subprocess,tempfile
from pathlib import Path
root=Path(__file__).resolve().parents[2]
with tempfile.TemporaryDirectory(prefix='hc-hotseat-lifetime-') as folder:
    binary=Path(folder)/'lifetime'
    subprocess.run([os.environ.get('CXX',shutil.which('c++')),'-std=c++20','-O1','-g',
        '-Wall','-Wextra','-Werror','-fsanitize=address,undefined',
        '-I'+str(root/'app/src/main/cpp'),str(Path(__file__).with_name('lifetime_test.cpp')),
        '-o',str(binary)],check=True)
    subprocess.run([str(binary)],check=True)
