#!/usr/bin/env python3
"""Run the bounded, scalar-only closed-folder preview return state checks."""
from pathlib import Path
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[2]
with tempfile.TemporaryDirectory(prefix='hc-folder-preview-return-state-') as directory:
    binary = Path(directory) / 'state-test'
    subprocess.run([shutil.which('c++'), '-std=c++20', '-O2', '-pthread',
                    '-I'+str(ROOT/'app/src/main/cpp'),
                    str(Path(__file__).with_name('state_test.cpp')),
                    '-o', str(binary)], check=True)
    subprocess.run([str(binary)], check=True)
