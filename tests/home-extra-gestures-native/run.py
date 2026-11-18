#!/usr/bin/env python3
# SPDX-License-Identifier: AGPL-3.0-or-later
from pathlib import Path
import subprocess,tempfile
here=Path(__file__).resolve().parent
root=here.parents[1]
with tempfile.TemporaryDirectory(prefix='hc-native-extra-gestures-') as directory:
    out=Path(directory);commands=out/'commands.txt';binary=out/'gesture-test'
    original=root/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/gesture/HomeExtraGestureState.java'
    subprocess.run(['javac','-d',str(out),str(original),str(here/'Golden.java')],check=True)
    with commands.open('w') as data:subprocess.run(['java','-ea','-cp',str(out),'Golden'],stdout=data,check=True)
    subprocess.run(['c++','-std=c++20','-Wall','-Wextra','-Werror','-fsanitize=address,undefined','-I'+str(root/'app/src/main/cpp'),str(here/'golden_test.cpp'),'-o',str(binary)],check=True)
    with commands.open() as data:subprocess.run([str(binary)],stdin=data,check=True)
    observer=out/'observer-test'
    subprocess.run(['c++','-std=c++20','-Wall','-Wextra','-Werror','-fsanitize=address,undefined','-I'+str(root/'app/src/main/cpp'),str(here/'observer_test.cpp'),'-o',str(observer)],check=True)
    subprocess.run([str(observer)],check=True)
    timer=out/'timer-test'
    subprocess.run(['c++','-std=c++20','-Wall','-Wextra','-Werror','-pthread','-fsanitize=address,undefined','-I'+str(root/'app/src/main/cpp'),str(here/'timer_test.cpp'),'-o',str(timer)],check=True)
    subprocess.run([str(timer)],check=True,timeout=20)
