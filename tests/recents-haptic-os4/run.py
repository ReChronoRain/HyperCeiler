#!/usr/bin/env python3
"""Test shipping resolver, borrowed-object reads and card-crossing state."""
import json
import os
import subprocess
import tempfile
import sys
from pathlib import Path
HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[1]
for version in ('8583','8584','close-8584'):
    data=json.loads((HERE/f'{version}.json').read_text())
    with tempfile.TemporaryDirectory(prefix='hc-haptic-') as work:
        out=Path(work)
        fixture='static Functions fixture(){return {\n'
        for f in data['functions']:
            fixture+='{'+str(f['va'])+',{'+','.join(map(str,f['words']))+'}},\n'
        (out/'fixture.h').write_text(fixture+'};}\n')
        subprocess.run(['c++','-std=c++20','-O1','-g','-Wall','-Wextra','-Werror',
                        '-fsanitize=address,undefined','-fno-omit-frame-pointer',
                        '-I'+str(ROOT/'app/src/main/cpp'),'-I'+work,
                        str(HERE/('close_test.cpp' if version.startswith('close-') else 'test.cpp')),'-o',str(out/'test')],check=True)
        subprocess.run([str(out/'test')],check=True)
with tempfile.TemporaryDirectory(prefix='hc-haptic-pattern-') as work:
    jdk=Path(os.environ['JAVA_HOME'])
    source=ROOT/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/recent/HomeRecentsHapticPattern.java'
    subprocess.run([str(jdk/'bin/javac'),'-d',work,str(source),str(source.with_name('HomeRecentsHapticProtocol.java')),str(HERE/'PatternTest.java')],check=True)
    subprocess.run([str(jdk/'bin/java'),'-cp',work,'PatternTest'],check=True)
with tempfile.TemporaryDirectory(prefix='hc-haptic-prefs-') as work:
    stubs=ROOT/'tests/home-layout-native/stubs'
    sources=[stubs/p for p in ['android/content/SharedPreferences.java','android/database/Cursor.java','android/database/MatrixCursor.java','android/os/Bundle.java']]
    sources += [ROOT/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/provider/HomeRecentsHapticPrefs.java',HERE/'PrefsTest.java']
    subprocess.run([str(jdk/'bin/javac'),'-d',work,*map(str,sources)],check=True)
    subprocess.run([str(jdk/'bin/java'),'-cp',work,'PrefsTest'],check=True)

subprocess.run([sys.executable,str(HERE/"runtime_test.py")],check=True)
