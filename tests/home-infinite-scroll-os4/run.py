#!/usr/bin/env python3
"""Shipping discovery, pointer/frame policy and Provider/Binder snapshot checks."""
import os,json,subprocess,tempfile,shutil,argparse
from pathlib import Path
root=Path(__file__).resolve().parents[2];here=Path(__file__).parent;cpp=root/'app/src/main/cpp'
parser=argparse.ArgumentParser();parser.add_argument('--image',type=Path);args=parser.parse_args()
def run(cmd):subprocess.run(list(map(str,cmd)),check=True)
with tempfile.TemporaryDirectory(prefix='hc-infinite-scroll-')as tmp:
    out=Path(tmp);fixture=json.loads((here/'fixture-8584.json').read_text());header='static Functions infinite_fixture(){return {\n'
    for f in fixture['functions']:header+='{'+str(f['va'])+',{'+','.join(map(str,f['words']))+'}},\n'
    (out/'infinite_fixture.h').write_text(header+'};}\n')
    run(['c++','-std=c++20','-O1','-g','-Wall','-Wextra','-Werror','-fsanitize=address,undefined','-I'+str(cpp),'-I'+str(root/'tests/runtime-feature-addressing'),'-I'+str(out),here/'native_test.cpp','-o',out/'native'])
    run([out/'native']+([args.image]if args.image else[]))
    jdk=Path(os.environ.get('JAVA_HOME','/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home'))/'bin'
    stubs=[p for p in (root/'tests/home-layout-native/stubs').rglob('*.java')if p.name!='ContentResolver.java']+list((root/'tests/hyos-control/java').rglob('*.java'))
    stubs += [root/'tests/home-dock-window/stubs/android/os'/f for f in ['Binder.java','IBinder.java','Parcel.java']]
    lib=root/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook'
    prod=[lib/f for f in ['base/FrameworkHookGate.java','provider/HomeLayoutPrefsSnapshot.java','provider/HomeGesturePrefsSnapshot.java','rules/home/dock/HomeLayoutNativeEndpointOS4.java']]
    prod += [root/'library/common/src/main/java/com/sevtinge/hyperceiler/common/log/ModuleLog.java']
    run([jdk/'javac','--release','21','-encoding','UTF-8','-d',out,*stubs,*prod,here/'EndpointTest.java'])
    run([jdk/'java','-ea','-cp',out,'com.sevtinge.hyperceiler.libhook.rules.home.dock.EndpointTest'])
source=(cpp/'targets/home/home_layout_hooks.cpp').read_text();binding=source[source.index('bool bind_infinite_scroll()'):source.index('bool bind_prestart()')]
assert 'HomeLayoutEngineFindSymbol' not in binding and 'bind_dart_target' in binding
asm=(cpp/'targets/home/home_layout_dart_arm64.S').read_text();guard=asm[asm.index('.ifc \\family,infinite'):asm.index('    sub sp, sp, #800',asm.index('.ifc \\family,infinite'))]
assert 'hc_infinite_spring_callers' in guard and 'ldarb' in guard and 'bl ' not in guard
assert '"prefs_key_home_other_infinite_scroll"' in (root/'library/core/src/main/java/com/sevtinge/hyperceiler/hooker/home/HomeOS4AdaptationGate.java').read_text()
assert 'writeInfiniteScroll(reply, result?.infiniteScroll() ?: false)' in (lib/'rules/home/dock/HomeDockWindow.kt').read_text()
print('Infinite scroll integration PASS, scoped runtime bank, cold-disabled/unrelated fast pass, OS4 UI and full Binder writer')
