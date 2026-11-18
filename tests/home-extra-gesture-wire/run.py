#!/usr/bin/env python3
# SPDX-License-Identifier: AGPL-3.0-or-later
from pathlib import Path
import subprocess,tempfile
here=Path(__file__).resolve().parent
root=here.parents[1]
source=root/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/gesture'
with tempfile.TemporaryDirectory(prefix='hc-extra-wire-') as directory:
    out=Path(directory);packets=out/'snapshots.bin';binary=out/'wire-test'
    subprocess.run(['javac','-d',str(out),str(source/'HomeExtraGestureState.java'),str(source/'HomeExtraGestureWire.java'),str(source/'HomeExtraGestureHandoff.java'),str(here/'Golden.java'),str(here/'HandoffTest.java'),str(here/'ControlGolden.java'),str(here/'LeaseGolden.java')],check=True)
    with packets.open('wb') as data:subprocess.run(['java','-ea','-cp',str(out),'Golden'],stdout=data,check=True)
    subprocess.run(['c++','-std=c++20','-Wall','-Wextra','-Werror','-fsanitize=address,undefined',
        '-I'+str(root/'app/src/main/cpp'),str(here/'wire_test.cpp'),'-o',str(binary)],check=True)
    with packets.open('rb') as data:subprocess.run([str(binary)],stdin=data,check=True)
    subprocess.run(['java','-ea','-cp',str(out),'HandoffTest'],check=True)
    control=out/'control-test';frames=out/'controls.bin'
    with frames.open('wb') as data:subprocess.run(['java','-ea','-cp',str(out),'ControlGolden'],stdout=data,check=True)
    subprocess.run(['c++','-std=c++20','-Wall','-Wextra','-Werror','-fsanitize=address,undefined','-I'+str(root/'app/src/main/cpp'),str(here/'control_test.cpp'),'-o',str(control)],check=True)
    with frames.open('rb') as data:subprocess.run([str(control)],stdin=data,check=True)
    lease=out/'lease-test';cases=out/'permissions.txt'
    with cases.open('w') as data:subprocess.run(['java','-ea','-cp',str(out),'LeaseGolden'],stdout=data,check=True)
    subprocess.run(['c++','-std=c++20','-Wall','-Wextra','-Werror','-fsanitize=address,undefined','-I'+str(root/'app/src/main/cpp'),str(here/'lease_test.cpp'),'-o',str(lease)],check=True)
    with cases.open() as data:subprocess.run([str(lease)],stdin=data,check=True)
