#!/usr/bin/env python3
# SPDX-License-Identifier: AGPL-3.0-or-later
from pathlib import Path
import os,re,subprocess,tempfile

root=Path(__file__).resolve().parents[2]
here=Path(__file__).resolve().parent
lib=root/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook'
cpp=root/'app/src/main/cpp'
jdk=Path(os.environ.get('JAVA_HOME','/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home'))/'bin'
source=(cpp/'targets/home/home_layout_config.cpp').read_text()

def function(start,source=source):
    a=source.index(start);end=source.index('(',a);parens=1;end+=1
    while parens:
        if source[end]=='(':parens+=1
        if source[end]==')':parens-=1
        end+=1
    brace=source.index('{',end);depth=1;i=brace+1
    while depth:
        if source[i]=='{':depth+=1
        if source[i]=='}':depth-=1
        i+=1
    return source[a:i]

with tempfile.TemporaryDirectory(prefix='hc-home-local-') as directory:
    out=Path(directory)
    stubs=[p for p in (root/'tests/home-layout-native/stubs').rglob('*.java') if p.name not in ('ContentResolver.java','ModuleLog.java')]
    stubs += list((root/'tests/hyos-control/java').rglob('*.java'))
    stubs += [root/'tests/home-dock-window/stubs/android/os'/f for f in ['Binder.java','IBinder.java','Parcel.java']]
    production=[lib/p for p in ['provider/HomeLayoutPrefsSnapshot.java','provider/HomeRecentsHapticPrefs.java','provider/HomeRecentCardsPrefs.java','rules/home/recent/HomeRecentCardsFilter.java','provider/HomeGesturePrefsSnapshot.java','base/FrameworkHookGate.java','rules/home/dock/HomeLayoutNativeEndpointOS4.java','rules/home/dock/HomeLayoutWire.java']]
    production += [root/'app/src/main/java/com/sevtinge/hyperceiler/home/runtime/HomeLocalWire.java']
    production += [lib/'provider/HomeDockPrefs.java',lib/'rules/home/dock/DockUnlockReveal.java']
    production += [root/'library/common/src/main/java/com/sevtinge/hyperceiler/common/log/ModuleLog.java']
    subprocess.run([str(jdk/'javac'),'--release','21','-encoding','UTF-8','-d',str(out),*map(str,stubs+production+[here/'LocalWireTest.java'])],check=True)
    subprocess.run([str(jdk/'java'),'-ea','-cp',str(out),'com.sevtinge.hyperceiler.libhook.rules.home.dock.LocalWireTest',str(out)],check=True)
    state=source[source.index('struct ProcessConfigCache {'):source.index('// LSPosed invokes the library-loaded callback')]
    actual='namespace home_layout {\n'+re.search(r'constexpr int32_t kLayoutAck = .*?;',source)[0]+'\n'
    actual+=re.search(r'constexpr char kCacheMagic\[4\] = .*?;',source)[0]+'\n'
    actual+=re.search(r'constexpr uint32_t kCacheVersion = .*?;',source)[0]+'\n'
    actual+=function('void serialize_config(')+'\n'
    actual+=function('static bool read_layout_reply(')+'\n'+state+'\n'
    actual+=function('bool query_config(')+'\n'+function('static bool publish_local_reply(const AParcel *parcel')+'\n'+function('bool local_haptic_profile(')+'\n'+function('bool local_dock_config(')+'\n'+function('bool inspect_local_recent_cards(')+'\n'+function('cards::Snapshot local_recent_cards(')+'\n'+function('bool commit_local_recent_cards(')+'\n}\n'
    (out/'actual.h').write_text(actual)
    subprocess.run(['c++','-std=c++20','-Wall','-Wextra','-Werror','-Wno-unused-function','-fsanitize=address,undefined','-I'+str(cpp),'-I'+str(out),str(here/'native_test.cpp'),'-o',str(out/'native')],check=True)
    subprocess.run([str(out/'native'),str(out)],check=True)
    subprocess.run(['c++','-std=c++20','-Wall','-Wextra','-Werror','-fsanitize=address,undefined','-I'+str(cpp),str(here/'channel_test.cpp'),'-o',str(out/'channel')],check=True)
    subprocess.run([str(out/'channel')],check=True)

subprocess.run(['c++','-std=c++20','-Wall','-Wextra','-Werror','-fsanitize=address,undefined','-I'+str(cpp),str(here/'cards_test.cpp'),'-o','/tmp/hc-local-cards-test'],check=True)
subprocess.run(['/tmp/hc-local-cards-test'],check=True)

# The alternate authenticated backend must ignore the standard APK experiment.
with tempfile.TemporaryDirectory(prefix='hc-root-selector-') as directory:
    out=Path(directory)
    selector=function('bool requested(', (cpp/'targets/home/home_local_config_channel.cpp').read_text())
    (out/'selector.cpp').write_text('#include <cassert>\n#include <cstring>\n#define PROP_VALUE_MAX 92\nint reads=0;\nint __system_property_get(const char*,char* out){++reads;std::strcpy(out,"root-socket");return 11;}\nnamespace home_local_config {\n'+selector+'\n}\nint main(){\n#ifdef HYPERCEILER_HYOS_BACKEND\nassert(!home_local_config::requested()&&reads==0);\n#else\nassert(home_local_config::requested()&&reads==1);\n#endif\n}\n')
    for extra in [[],['-DHYPERCEILER_HYOS_BACKEND=1']]:
        subprocess.run(['c++','-std=c++20','-Wall','-Wextra','-Werror',*extra,str(out/'selector.cpp'),'-o',str(out/'selector')],check=True)
        subprocess.run([str(out/'selector')],check=True)
    print('PASS actual backend selection, APK opt-in / authenticated companion isolation')

# Exercise the production selector at the system-property boundary. An explicit
# override still wins, and only the opt-in Root channel gets the new default.
with tempfile.TemporaryDirectory(prefix='hc-haptic-selector-') as directory:
    out=Path(directory)
    source=(cpp/'targets/home/home_native_vibrator.cpp').read_text()
    modes=re.search(r'enum class Backend .*?;',source)[0]
    selector=function('Backend backend(',source)
    (out/'selector.cpp').write_text("""
#include <cassert>
#include <cstring>
#define PROP_VALUE_MAX 92
const char* property="";
bool root_config=false;
int __system_property_get(const char*,char* out) {
    std::strcpy(out,property);
    return std::strlen(property);
}
namespace home_local_config {bool requested(){return root_config;}}
"""+modes+'\n'+selector+"""
int main(){
    assert(backend()==Backend::off);
    root_config=true;
    assert(backend()==Backend::published);
    property="off";assert(backend()==Backend::off);
    property="launcher";assert(backend()==Backend::direct);
    property="launcher-cached";assert(backend()==Backend::cached);
    property="launcher-published";assert(backend()==Backend::published);
    property="unknown";assert(backend()==Backend::off);
}
""")
    subprocess.run(['c++','-std=c++20','-Wall','-Wextra','-Werror',str(out/'selector.cpp'),'-o',str(out/'selector')],check=True)
    subprocess.run([str(out/'selector')],check=True)
    print('PASS actual haptic selector, Root default and explicit overrides')
