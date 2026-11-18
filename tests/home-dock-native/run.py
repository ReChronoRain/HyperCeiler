#!/usr/bin/env python3
# SPDX-License-Identifier: AGPL-3.0-or-later
from pathlib import Path
import subprocess,tempfile
here=Path(__file__).resolve().parent
root=here.parents[1]
original=root/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/dock'
with tempfile.TemporaryDirectory(prefix='hc-dock-native-') as directory:
    out=Path(directory);golden=out/'golden.txt';binary=out/'geometry-test'
    subprocess.run(['javac','-d',str(out),str(original/'DockWindowPolicy.java'),str(original/'DockGlassPreset.java'),str(here/'Golden.java')],check=True)
    with golden.open('w') as data:subprocess.run(['java','-cp',str(out),'Golden'],stdout=data,check=True)
    subprocess.run(['c++','-std=c++20','-Wall','-Wextra','-Werror','-fsanitize=address,undefined','-I'+str(root/'app/src/main/cpp'),str(here/'geometry_test.cpp'),'-o',str(binary)],check=True)
    with golden.open() as data:subprocess.run([str(binary)],stdin=data,check=True)
