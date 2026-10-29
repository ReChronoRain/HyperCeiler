#!/usr/bin/env python3
# SPDX-License-Identifier: AGPL-3.0-or-later
import os,shutil,subprocess,sys,tempfile
from pathlib import Path
root=Path(__file__).resolve().parents[2]
lib=root/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook'
tests=root/'tests'
jdk=Path(os.environ.get('JAVA_HOME','/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home'))/'bin'
def run(args): subprocess.run([str(x) for x in args],check=True)
with tempfile.TemporaryDirectory(prefix='hc-recents-bar-') as directory:
    out=Path(directory)
    sources=[p for tree in ['home-layout-native/stubs','home-dock-window/stubs'] for p in (tests/tree).rglob('*.java') if p.name!='ModuleLog.java']
    sources += [lib/x for x in ['base/FrameworkHookGate.java','provider/HomeLayoutPrefsSnapshot.java','provider/HomeGesturePrefsSnapshot.java','rules/home/dock/HomeLayoutNativeEndpointOS4.java','rules/home/dock/HomeRecentsBarState.java']]
    sources += [root/'library/common/src/main/java/com/sevtinge/hyperceiler/common/log/ModuleLog.java']
    sources += [Path(__file__).with_name('StateTest.java'),Path(__file__).with_name('PreferenceTest.java')]
    run([jdk/'javac','-encoding','UTF-8','-d',out,*sources])
    for name in ['StateTest','com.sevtinge.hyperceiler.libhook.rules.home.dock.PreferenceTest']:run([jdk/'java','-ea','-cp',out,name])
    run([shutil.which('c++'),'-std=c++20','-O1','-fsanitize=address,undefined','-I'+str(root/'app/src/main/cpp'),Path(__file__).with_name('resolver_test.cpp'),'-o',out/'resolver'])
    run([out/'resolver',*sys.argv[1:]])
