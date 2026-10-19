#!/usr/bin/env python3
import json,subprocess,tempfile
from pathlib import Path
root=Path(__file__).resolve().parents[2]
fixture=json.loads(Path(__file__).with_name('launcher-7751.json').read_text())
with tempfile.TemporaryDirectory(prefix='hc-independent-scale-') as temp:
    temp=Path(temp)
    defines=['#include "targets/home/home_dynamic_layout.h"']
    for key,value in fixture.items():
        if isinstance(value,dict):
            defines += [f'#define {key.upper()} home_dynamic::Body'+'{'+','.join(value['words'])+'}',f'#define {key.upper()}_VA {value["va"]}u']
        else:defines += [f'#define {key.upper()} {value}u']
    (temp/'fixture.h').write_text('\n'.join(defines))
    subprocess.run(['c++','-std=c++20','-Wall','-Wextra','-Werror','-I'+str(root/'app/src/main/cpp'),'-I'+str(temp),str(Path(__file__).with_name('test.cpp')),'-o',str(temp/'test')],check=True)
    subprocess.run([str(temp/'test')],check=True)

# Compile the production endpoint and complete provider against the existing host Android stubs.
with tempfile.TemporaryDirectory(prefix='hc-scale-endpoint-') as temp:
    sources=list((root/'tests/home-layout-native/stubs').rglob('*.java'))
    sources+=list((root/'tests/home-dock-window/stubs').rglob('*.java'))
    lib=root/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook'
    sources += [lib/'provider/HomeLayoutPrefsSnapshot.java',lib/'provider/HomeGesturePrefsSnapshot.java',
        lib/'rules/home/dock/HomeLayoutNativeEndpointOS4.java',
        root/'library/common/src/main/java/com/sevtinge/hyperceiler/common/log/ModuleLog.java',
        Path(__file__).with_name('ObjectScaleEndpointTest.java')]
    subprocess.run(['javac','-encoding','UTF-8','-d',temp]+[str(p) for p in sources],check=True)
    subprocess.run(['java','-ea','-cp',temp,'com.sevtinge.hyperceiler.libhook.rules.home.dock.ObjectScaleEndpointTest'],check=True)
